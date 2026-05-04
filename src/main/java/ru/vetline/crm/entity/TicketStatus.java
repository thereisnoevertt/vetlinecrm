package ru.vetline.crm.entity;

import java.util.EnumSet;
import java.util.Set;

public enum TicketStatus {
    NEW("Новая") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(IN_PROGRESS, CANCELLED);
        }
    },
    IN_PROGRESS("Принята в работу") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(WORKING, FROZEN, CANCELLED);
        }
    },
    WORKING("В работе") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(ON_APPROVAL, TRANSFERRED, AWAITING_REPLY, IN_DELIVERY, DONE, FROZEN, CANCELLED);
        }
    },
    FROZEN("Заморожена") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(WORKING, CANCELLED);
        }
    },
    ON_APPROVAL("На согласовании") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(WORKING, DONE, CANCELLED);
        }
    },
    TRANSFERRED("Передана в подразделение") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(WORKING, DONE);
        }
    },
    AWAITING_REPLY("Ожидает внешнего ответа") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(WORKING, IN_DELIVERY);
        }
    },
    IN_DELIVERY("В процессе поставки") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.of(DONE, WORKING);
        }
    },
    DONE("Выполнена") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.noneOf(TicketStatus.class);
        }
    },
    CANCELLED("Отменена клиентом") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.noneOf(TicketStatus.class);
        }
    },
    ARCHIVED("Архивирована") {
        @Override public Set<TicketStatus> getAllowedTransitions() {
            return EnumSet.noneOf(TicketStatus.class);
        }
    };

    private final String displayName;
    TicketStatus(String d) { this.displayName = d; }
    public String getDisplayName() { return displayName; }
    public abstract Set<TicketStatus> getAllowedTransitions();
    public boolean isFinal() { return this == DONE || this == CANCELLED || this == ARCHIVED; }
    public String getBadgeClass() {
        return switch (this) {
            case NEW           -> "badge-new";
            case IN_PROGRESS   -> "badge-in-progress";
            case WORKING       -> "badge-working";
            case FROZEN        -> "badge-frozen";
            case ON_APPROVAL   -> "badge-approval";
            case TRANSFERRED   -> "badge-transferred";
            case AWAITING_REPLY-> "badge-waiting";
            case IN_DELIVERY   -> "badge-delivery";
            case DONE          -> "badge-done";
            case CANCELLED     -> "badge-cancelled";
            case ARCHIVED      -> "badge-archived";
        };
    }
}
