package ru.vetline.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vetline.crm.entity.*;
import ru.vetline.crm.repository.*;
import ru.vetline.crm.vk.VkBot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Уведомления клиентов через VK Bot API. ТЗ Ф-06.
 * <p>
 * Шаблон: «Здравствуйте, {Имя}! Ваша заявка №{Номер}
 *          переведена в статус «{Статус}». Команда «Ветлайн ВК».»
 * Retry: 3 попытки с интервалом 30 сек.
 * <p>
 * Особенность реализации: текст уведомления формируется в момент
 * фактической отправки (а не при создании записи Notification).
 * Это устраняет два класса проблем:
 *   (1) race condition между POST /status и POST /notify — поскольку
 *       sendNotification принимает только идентификатор заявки и читает
 *       её повторно в собственной транзакции, к моменту чтения
 *       параллельная смена статуса уже закоммичена;
 *   (2) при retryPending() сообщение генерируется по актуальному
 *       состоянию заявки, а не по тому, что было при первой попытке.
 * <p>
 * Особенность VK: сообщество может отправлять сообщения только тем
 * пользователям, которые ранее писали в сообщество. Иначе VK API
 * вернёт ошибку 901; уведомление пометится статусом ERROR и менеджеру
 * выводится предупреждение.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final TicketRepository       ticketRepo;
    private final UserRepository         userRepo;
    private final TicketHistoryRepository historyRepo;
    private final Optional<VkBot>        bot;

    @Value("${app.vk-retry-attempts:3}")
    private int maxAttempts;

    private static final String TEMPLATE =
            "Здравствуйте, %s!\n" +
            "Ваша заявка №%s переведена в статус «%s».\n" +
            "Благодарим вас за обращение!\n\nКоманда «Ветлайн ВК»";

    /**
     * Постановка уведомления в очередь. Принимает только идентификатор
     * заявки и инициатора — это устраняет проблему передачи detached-
     * сущностей между потоками и гарантирует, что async-метод прочитает
     * актуальное состояние заявки.
     */
    @Async
    @Transactional
    public void sendNotification(UUID ticketId, UUID initiatorId) {
        Ticket ticket = ticketRepo.findById(ticketId).orElse(null);
        if (ticket == null) {
            log.warn("sendNotification: заявка {} не найдена", ticketId);
            return;
        }
        Client client = ticket.getClient();
        if (client.getVkId() == null) {
            log.warn("VK ID не заполнен, клиент {}", client.getId());
            return;
        }
        // ВАЖНО: текст НЕ формируем здесь — он будет собран в момент
        // отправки, чтобы при retry использовался актуальный статус.
        Notification notif = notifRepo.save(Notification.builder()
                .ticket(ticket).client(client).messageText("").build());

        User initiator = initiatorId != null
                ? userRepo.findById(initiatorId).orElse(null)
                : null;
        doSend(notif.getId(), initiator);
    }

    /** Повторные попытки каждые 30 сек (ТЗ Ф-06) */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void retryPending() {
        List<Notification> pending = notifRepo.findByDeliveryStatusAndAttemptsLessThan(
                Notification.DeliveryStatus.PENDING, maxAttempts);
        // Уведомления старше 6 часов помечаем ERROR без отправки —
        // они с большой вероятностью неактуальны.
        LocalDateTime cutoff = LocalDateTime.now().minusHours(6);
        for (Notification n : pending) {
            if (n.getCreatedAt() != null && n.getCreatedAt().isBefore(cutoff)) {
                n.setDeliveryStatus(Notification.DeliveryStatus.ERROR);
                notifRepo.save(n);
                log.warn("Уведомление {} устарело (>6 ч), помечено ERROR",
                         n.getId());
                continue;
            }
            doSend(n.getId(), null);
        }
    }

    /**
     * Фактическая отправка. Перечитывает Notification и Ticket из БД,
     * чтобы получить актуальное состояние, формирует текст и пытается
     * доставить через VK.
     */
    @Transactional
    public void doSend(UUID notifId, User initiator) {
        Notification notif = notifRepo.findById(notifId).orElse(null);
        if (notif == null) {
            log.warn("doSend: notification {} не найдена", notifId);
            return;
        }
        Ticket ticket = notif.getTicket();
        if (ticket == null) {
            log.warn("doSend: у уведомления {} не указана заявка", notifId);
            notif.setDeliveryStatus(Notification.DeliveryStatus.ERROR);
            notifRepo.save(notif);
            return;
        }
        // Свежий снимок заявки и клиента — на случай, если статус был
        // изменён между постановкой в очередь и фактической отправкой.
        Ticket fresh = ticketRepo.findById(ticket.getId()).orElse(ticket);
        Client client = fresh.getClient();

        notif.setAttempts(notif.getAttempts() + 1);
        if (bot.isEmpty()) {
            log.warn("VK-бот не настроен, уведомление {} пропущено",
                     notif.getId());
            notif.setDeliveryStatus(Notification.DeliveryStatus.ERROR);
            notifRepo.save(notif);
            return;
        }
        if (client.getVkId() == null) {
            log.warn("VK ID не заполнен у клиента {} при отправке", client.getId());
            notif.setDeliveryStatus(Notification.DeliveryStatus.ERROR);
            notifRepo.save(notif);
            return;
        }

        // Формируем текст ровно сейчас, по актуальному статусу.
        String text = String.format(TEMPLATE,
                firstName(client.getFullName()),
                fresh.getNumber(),
                fresh.getStatus().getDisplayName());
        notif.setMessageText(text);

        try {
            bot.get().sendMessage(client.getVkId(), text);
            notif.setDeliveryStatus(Notification.DeliveryStatus.SENT);
            notif.setSentAt(LocalDateTime.now());
            notifRepo.save(notif);
            saveHistory(fresh, initiator, "Уведомление отправлено. Статус: "
                    + fresh.getStatus().getDisplayName());
            log.info("Уведомление отправлено, заявка {} → статус «{}»",
                     fresh.getNumber(),
                     fresh.getStatus().getDisplayName());
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления {}: {}",
                      notif.getId(), e.getMessage());
            if (notif.getAttempts() >= maxAttempts) {
                notif.setDeliveryStatus(Notification.DeliveryStatus.ERROR);
                saveHistory(fresh, initiator,
                        "Ошибка доставки уведомления (" + maxAttempts + " попыток)");
            }
            notifRepo.save(notif);
        }
    }

    private void saveHistory(Ticket ticket, User user, String desc) {
        historyRepo.save(TicketHistory.builder()
                .ticket(ticket)
                .eventType(TicketHistory.EventType.NOTIFICATION_SENT)
                .user(user).description(desc).build());
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "клиент";
        String[] p = fullName.trim().split("\\s+");
        return p.length > 1 ? p[1] : p[0];
    }
}
