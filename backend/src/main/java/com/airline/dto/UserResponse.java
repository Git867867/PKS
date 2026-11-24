package com.airline.dto;

import com.airline.domain.RoleName;
import com.airline.domain.UserAccount;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        Set<RoleName> roles,
        boolean active,
        boolean mustChangePassword,
        LocalDateTime createdAt
) {
    public static UserResponse from(UserAccount account) {
        return new UserResponse(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                Set.copyOf(account.getRoles()),
                account.isActive(),
                account.isMustChangePassword(),
                account.getCreatedAt()
        );
    }
}
