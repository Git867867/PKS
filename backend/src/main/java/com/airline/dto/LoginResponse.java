package com.airline.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        boolean mustChangePassword,
        UserResponse user
) {
}
