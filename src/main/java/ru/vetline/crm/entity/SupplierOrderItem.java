package ru.vetline.crm.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "supplier_order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierOrderItem {
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_order_id", nullable = false)
    private SupplierOrder order;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String unit = "шт.";

    @Column(nullable = false)
    @Builder.Default
    private int position = 1;
}
