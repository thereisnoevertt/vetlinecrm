package ru.vetline.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "plan_targets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanTarget {
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private int year;

    @Column
    private Integer month;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_by_user")
    private User setByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
