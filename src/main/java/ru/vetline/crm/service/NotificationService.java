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

/**
 * Уведомления клиентов через VK Bot API. ТЗ Ф-06.
 * Шаблон: «Здравствуйте, {Имя}! Ваша заявка №{Номер}
 *          переведена в статус «{Статус}». Команда «Ветлайн ВК».»
 * Retry: 3 попытки с интервалом 30 сек.
 *
 * Особенность VK: сообщество может отправлять сообщения только тем
 * пользователям, которые ранее писали в сообщество. Если такого факта
 * нет — VK API вернёт ошибку 901 (Can't send messages to this user
 * due to their privacy settings); в этом случае уведомление пометится
 * статусом ERROR и менеджеру выводится предупреждение.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final TicketHistoryRepository historyRepo;
    private final Optional<VkBot> bot;

    @Value("${app.vk-retry-attempts:3}")
    private int maxAttempts;

    private static final String TEMPLATE =
            "Здравствуйте, %s!\n" +
            "Ваша заявка №%s переведена в статус «%s».\n" +
            "Благодарим вас за обращение!\n\nКоманда «Ветлайн ВК»";

    @Async
    @Transactional
    public void sendNotification(Ticket ticket, User initiator) {
        Client client = ticket.getClient();
        if (client.getVkId() == null) {
            log.warn("VK ID не заполнен, клиент {}", client.getId());
            return;
        }
        String text = String.format(TEMPLATE,
                firstName(client.getFullName()),
                ticket.getNumber(),
                ticket.getStatus().getDisplayName());

        Notification notif = notifRepo.save(Notification.builder()
                .ticket(ticket).client(client).messageText(text).build());

        doSend(notif, ticket, initiator);
    }

    /** Повторные попытки каждые 30 сек (ТЗ Ф-06) */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void retryPending() {
        List<Notification> pending = notifRepo.findByDeliveryStatusAndAttemptsLessThan(
                Notification.DeliveryStatus.PENDING, maxAttempts);
        pending.forEach(n -> doSend(n, n.getTicket(), null));
    }

    private void doSend(Notification notif, Ticket ticket, User initiator) {
        notif.setAttempts(notif.getAttempts() + 1);
        if (bot.isEmpty()) {
            log.warn("VK-бот не настроен, уведомление {} пропущено", notif.getId());
            notif.setDeliveryStatus(Notification.DeliveryStatus.ERROR);
            notifRepo.save(notif);
            return;
        }
        try {
            bot.get().sendMessage(notif.getClient().getVkId(), notif.getMessageText());
            notif.setDeliveryStatus(Notification.DeliveryStatus.SENT);
            notif.setSentAt(LocalDateTime.now());
            notifRepo.save(notif);
            saveHistory(ticket, initiator, "Уведомление отправлено. Статус: "
                    + ticket.getStatus().getDisplayName());
            log.info("Уведомление отправлено, заявка {}", ticket.getNumber());
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления {}: {}", notif.getId(), e.getMessage());
            if (notif.getAttempts() >= maxAttempts) {
                notif.setDeliveryStatus(Notification.DeliveryStatus.ERROR);
                saveHistory(ticket, initiator, "Ошибка доставки уведомления (" + maxAttempts + " попыток)");
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
