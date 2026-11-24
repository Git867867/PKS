package com.airline.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthRequests {
    private AuthRequests() {
    }

    public record LoginRequest(
            @NotBlank(message = "Введите email.") String email,
            @NotBlank(message = "Введите пароль.") String password
    ) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record PasswordChangeRequest(
            @NotBlank(message = "Введите текущий пароль.") String currentPassword,
            @NotBlank(message = "Введите новый пароль.") String newPassword
    ) {
    }
}
