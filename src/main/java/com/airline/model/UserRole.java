package com.airline.model;

public enum UserRole {
    ADMIN("Администратор"),
    MANAGER("Менеджер"),
    CLIENT("Клиент");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}