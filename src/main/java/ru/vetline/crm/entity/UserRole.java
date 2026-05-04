package ru.vetline.crm.entity;

public enum UserRole {
    MANAGER("Менеджер"),
    DIRECTOR("Генеральный директор"),
    ADMIN("Администратор");

    private final String displayName;
    UserRole(String d) { this.displayName = d; }
    public String getDisplayName() { return displayName; }
}
