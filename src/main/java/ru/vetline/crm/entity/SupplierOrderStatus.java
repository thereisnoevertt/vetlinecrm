package ru.vetline.crm.entity;

import java.util.EnumSet;
import java.util.Set;

public enum SupplierOrderStatus {
    DRAFT("Черновик", "badge-new") {
        @Override public Set<SupplierOrderStatus> getAllowedTransitions() {
            return EnumSet.of(SENT, CANCELLED);
        }
    },
    SENT("Отправлен", "badge-in-progress") {
        @Override public Set<SupplierOrderStatus> getAllowedTransitions() {
            return EnumSet.of(CONFIRMED, CANCELLED);
        }
    },
    CONFIRMED("Подтверждён", "badge-working") {
        @Override public Set<SupplierOrderStatus> getAllowedTransitions() {
            return EnumSet.of(IN_DELIVERY, CANCELLED);
        }
    },
    IN_DELIVERY("В доставке", "badge-delivery") {
        @Override public Set<SupplierOrderStatus> getAllowedTransitions() {
            return EnumSet.of(COMPLETED, CANCELLED);
        }
    },
    COMPLETED("Выполнен", "badge-done") {
        @Override public Set<SupplierOrderStatus> getAllowedTransitions() {
            return EnumSet.noneOf(SupplierOrderStatus.class);
        }
    },
    CANCELLED("Отменён", "badge-cancelled") {
        @Override public Set<SupplierOrderStatus> getAllowedTransitions() {
            return EnumSet.noneOf(SupplierOrderStatus.class);
        }
    };

    private final String displayName;
    private final String badgeClass;
    SupplierOrderStatus(String d, String b) { this.displayName = d; this.badgeClass = b; }
    public String getDisplayName() { return displayName; }
    public String getBadgeClass()  { return badgeClass; }
    public abstract Set<SupplierOrderStatus> getAllowedTransitions();
}
