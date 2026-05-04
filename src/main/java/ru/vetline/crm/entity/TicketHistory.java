package ru.vetline.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "ticket_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketHistory {
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, columnDefinition = "history_event")
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_from", columnDefinition = "ticket_status")
    private TicketStatus statusFrom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_to", columnDefinition = "ticket_status")
    private TicketStatus statusTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EventType { STATUS_CHANGE, FIELD_UPDATE, NOTIFICATION_SENT, ARCHIVED, CREATED }
}
