package com.airline.dto;

import com.airline.domain.AuditEvent;
import com.airline.domain.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record CreateUserRequest(
            @NotBlank(message = "Введите email.") @Email(message = "Некорректный email.") @Size(max = 150) String email,
            @NotBlank(message = "Введите имя.") @Size(max = 150) String name,
            @NotEmpty(message = "Выберите хотя бы одну роль.") Set<RoleName> roles
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Введите email.") @Email(message = "Некорректный email.") @Size(max = 150) String email,
            @NotBlank(message = "Введите имя.") @Size(max = 150) String name,
            @NotEmpty(message = "Выберите хотя бы одну роль.") Set<RoleName> roles,
            boolean active
    ) {
    }

    public record CreatedUserResponse(UserResponse user, String temporaryPassword) {
    }

    public record AuditResponse(
            UUID id,
            String eventType,
            String entityType,
            UUID entityId,
            String description,
            String userEmail,
            String correlationId,
            LocalDateTime createdAt
    ) {
        public static AuditResponse from(AuditEvent event) {
            String email = event.getUser() == null ? null : event.getUser().getEmail();
            return new AuditResponse(
                    event.getId(),
                    event.getEventType(),
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getDescription(),
                    email,
                    event.getCorrelationId(),
                    event.getCreatedAt()
            );
        }
    }

    public record ImportRowError(int row, String message) {
    }

    public record ImportPreviewResponse(int validCount, int errorCount, java.util.List<ImportRowError> errors) {
    }

    public record ImportApplyResponse(int created, int updated, int errorCount, java.util.List<ImportRowError> errors) {
    }

    public record GroupCount(String name, long count) {
    }

    public record AnalyticsResponse(
            long total,
            long active,
            long archived,
            long refundable,
            java.math.BigDecimal averagePrice,
            java.util.List<GroupCount> byPassenger,
            java.util.List<GroupCount> byStatus,
            java.util.List<GroupCount> byPriority
    ) {
    }
}
