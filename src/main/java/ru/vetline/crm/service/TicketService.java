package ru.vetline.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vetline.crm.dto.CreateTicketRequest;
import ru.vetline.crm.dto.WebhookTicketRequest;
import ru.vetline.crm.entity.*;
import ru.vetline.crm.repository.*;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository     ticketRepo;
    private final ClientRepository     clientRepo;
    private final UserRepository       userRepo;
    private final SourceRepository     sourceRepo;
    private final CategoryRepository   categoryRepo;
    private final AuditService         audit;

    // ── Ф-01: Автоматический приём заявки с сайта ────────────────────────────
    @Transactional
    public Ticket createFromWebhook(WebhookTicketRequest req) {
        Client client = findOrCreateClient(req.getName(), req.getPhone(),
                                           req.getEmail(), null);

        Source siteSource = sourceRepo.findAllByOrderByNameAsc().stream()
                .filter(s -> "Сайт".equals(s.getName())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Источник 'Сайт' не найден"));

        User manager = userRepo.findByRoleAndActiveTrueOrderByFullNameAsc(UserRole.MANAGER)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет активных менеджеров"));

        Ticket ticket = Ticket.builder()
                .number(generateNumber())
                .client(client).manager(manager)
                .status(TicketStatus.NEW).source(siteSource)
                .description(req.getDescription()).build();

        ticket = ticketRepo.save(ticket);
        addHistory(ticket, TicketHistory.EventType.CREATED, null, TicketStatus.NEW,
                   manager, "Заявка создана автоматически с сайта");
        audit.log(manager, "TICKET_WEBHOOK", "Ticket", ticket.getId(), null);
        log.info("Webhook: создана заявка {}", ticket.getNumber());
        return ticket;
    }

    // ── Ф-02: Ручная регистрация ──────────────────────────────────────────────
    @Transactional
    public Ticket createManually(CreateTicketRequest req, User currentUser) {
        Client client = findOrCreateClient(req.getFullName(), req.getPhone(),
                                           req.getEmail(), req.getOrganization());
        Source   src  = req.getSourceId()   != null ? sourceRepo.findById(req.getSourceId()).orElse(null)   : null;
        Category cat  = req.getCategoryId() != null ? categoryRepo.findById(req.getCategoryId()).orElse(null) : null;

        Ticket ticket = Ticket.builder()
                .number(generateNumber())
                .client(client).manager(currentUser)
                .status(TicketStatus.NEW).source(src).category(cat)
                .description(req.getDescription()).noteClient(req.getNoteClient()).build();

        ticket = ticketRepo.save(ticket);
        addHistory(ticket, TicketHistory.EventType.CREATED, null, TicketStatus.NEW,
                   currentUser, "Заявка создана вручную");
        audit.log(currentUser, "TICKET_MANUAL", "Ticket", ticket.getId(), null);
        return ticket;
    }

    // ── Ф-05: Смена статуса ───────────────────────────────────────────────────
    @Transactional
    public Ticket changeStatus(UUID ticketId, TicketStatus newStatus, User user) {
        Ticket ticket = findById(ticketId);
        if (!ticket.canTransitionTo(newStatus))
            throw new IllegalStateException(
                "Переход «" + ticket.getStatus().getDisplayName() +
                "» → «" + newStatus.getDisplayName() + "» недопустим");

        TicketStatus old = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepo.save(ticket);
        addHistory(ticket, TicketHistory.EventType.STATUS_CHANGE, old, newStatus, user,
                   "Статус: «" + old.getDisplayName() + "» → «" + newStatus.getDisplayName() + "»");
        audit.log(user, "STATUS_CHANGE", "Ticket", ticket.getId(),
                  "from=" + old + " to=" + newStatus);
        return ticket;
    }

    // ── Ф-07: Архивирование ───────────────────────────────────────────────────
    @Transactional
    public Ticket archive(UUID ticketId, String comment, User user) {
        Ticket ticket = findById(ticketId);
        if (!ticket.canBeArchived())
            throw new IllegalStateException(
                "Архивировать можно только заявки со статусом «Выполнена» или «Отменена клиентом»");

        ticket.setArchived(true);
        ticket.setArchivedAt(LocalDateTime.now());
        if (comment != null && !comment.isBlank()) ticket.setNoteManager(comment);
        ticket = ticketRepo.save(ticket);
        addHistory(ticket, TicketHistory.EventType.ARCHIVED,
                   ticket.getStatus(), TicketStatus.ARCHIVED, user, "Заявка архивирована. " + comment);
        audit.log(user, "TICKET_ARCHIVED", "Ticket", ticket.getId(), null);
        return ticket;
    }

    // ── Редактирование содержания заявки (Прецедент №3 — расширение) ─────────
    /**
     * Обновляет текстовые поля карточки заявки. Правила:
     *  • description и noteClient — редактируются всегда, пока заявка не в архиве;
     *  • noteManager — редактируется только на статусах DONE и CANCELLED
     *    (до архивирования);
     *  • архивированные заявки неизменны.
     * В журнал истории добавляется одна запись со списком фактически
     * изменённых полей.
     */
    @Transactional
    public Ticket updateContent(UUID ticketId,
                                String description,
                                String noteClient,
                                String noteManager,
                                User user) {
        Ticket t = findById(ticketId);
        if (t.isArchived())
            throw new IllegalStateException(
                "Архивированная заявка недоступна для редактирования");

        boolean canEditManagerNote =
                t.getStatus() == TicketStatus.DONE ||
                t.getStatus() == TicketStatus.CANCELLED;

        java.util.List<String> changed = new java.util.ArrayList<>();
        if (!java.util.Objects.equals(nullToEmpty(t.getDescription()),
                                      nullToEmpty(description))) {
            t.setDescription(description);
            changed.add("описание потребности");
        }
        if (!java.util.Objects.equals(nullToEmpty(t.getNoteClient()),
                                      nullToEmpty(noteClient))) {
            t.setNoteClient(noteClient);
            changed.add("примечание клиента");
        }
        if (canEditManagerNote &&
            !java.util.Objects.equals(nullToEmpty(t.getNoteManager()),
                                      nullToEmpty(noteManager))) {
            t.setNoteManager(noteManager);
            changed.add("итоговый комментарий менеджера");
        }
        if (changed.isEmpty()) return t;

        t.setUpdatedAt(LocalDateTime.now());
        t = ticketRepo.save(t);
        addHistory(t, TicketHistory.EventType.FIELD_UPDATE, null, null, user,
                "Обновлены поля: " + String.join("; ", changed));
        audit.log(user, "TICKET_CONTENT_UPDATE", "Ticket", t.getId(),
                String.join(", ", changed));
        return t;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    // ── Чтение ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Ticket findById(UUID id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка не найдена: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Ticket> search(boolean archived, TicketStatus status,
                               UUID managerId, String search, int page) {
        return ticketRepo.search(archived, status, managerId,
                                 (search != null && !search.isBlank()) ? search : null,
                                 PageRequest.of(page, 20));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Client findOrCreateClient(String name, String phone, String email, String org) {
        return clientRepo.findByPhone(phone).orElseGet(() ->
            clientRepo.save(Client.builder()
                    .fullName(name).phone(phone).email(email).organization(org).build()));
    }

    private synchronized String generateNumber() {
        long seq = ticketRepo.count() + 1;
        return String.format("VL-%d-%06d", Year.now().getValue(), seq);
    }

    private void addHistory(Ticket t, TicketHistory.EventType type,
                            TicketStatus from, TicketStatus to, User user, String desc) {
        t.getHistory().add(TicketHistory.builder()
                .ticket(t).eventType(type).statusFrom(from).statusTo(to)
                .user(user).description(desc).build());
    }
}
