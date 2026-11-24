package com.airline.dto;

import com.airline.domain.Booking;
import com.airline.domain.BookingStatus;
import com.airline.domain.BulkAction;
import com.airline.domain.PriorityLevel;
import com.airline.domain.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class BookingDtos {
    private BookingDtos() {
    }

    public record BookingRequest(
            @NotNull(message = "Выберите пассажира.") UUID passengerId,
            @NotBlank(message = "Укажите номер рейса.") @Size(max = 10) String flightNumber,
            @NotBlank(message = "Укажите город вылета.") @Size(max = 100) String departureCity,
            @NotBlank(message = "Укажите город прилета.") @Size(max = 100) String arrivalCity,
            @NotNull(message = "Укажите дату вылета.") LocalDateTime departureDate,
            @NotNull(message = "Укажите цену.") @DecimalMin(value = "0.01", message = "Цена должна быть больше 0.") BigDecimal price,
            boolean refundable,
            List<@Size(max = 40) String> tags
    ) {
    }

    public record BookingUpdateRequest(
            @NotNull(message = "Выберите пассажира.") UUID passengerId,
            @NotBlank(message = "Укажите номер рейса.") @Size(max = 10) String flightNumber,
            @NotBlank(message = "Укажите город вылета.") @Size(max = 100) String departureCity,
            @NotBlank(message = "Укажите город прилета.") @Size(max = 100) String arrivalCity,
            @NotNull(message = "Укажите дату вылета.") LocalDateTime departureDate,
            @NotNull(message = "Укажите цену.") @DecimalMin(value = "0.01", message = "Цена должна быть больше 0.") BigDecimal price,
            @NotNull(message = "Укажите статус.") BookingStatus status,
            boolean refundable,
            List<@Size(max = 40) String> tags,
            @NotNull(message = "Не передана версия записи.") Long version
    ) {
    }

    public record BookingResponse(
            UUID id,
            String bookingCode,
            UUID passengerId,
            String passengerName,
            String flightNumber,
            String departureCity,
            String arrivalCity,
            LocalDateTime departureDate,
            BigDecimal price,
            BookingStatus status,
            boolean refundable,
            PriorityLevel priority,
            List<String> tags,
            boolean archived,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long version
    ) {
        public static BookingResponse from(Booking booking) {
            return new BookingResponse(
                    booking.getPublicId(),
                    booking.getBookingCode(),
                    booking.getPassenger().getPublicId(),
                    booking.getPassenger().fullName(),
                    booking.getFlightNumber(),
                    booking.getDepartureCity(),
                    booking.getArrivalCity(),
                    booking.getDepartureDate(),
                    booking.getPrice(),
                    booking.getStatus(),
                    booking.isRefundable(),
                    booking.getPriority(),
                    booking.getTags().stream().map(Tag::getName).sorted().toList(),
                    booking.isArchived(),
                    booking.getCreatedAt(),
                    booking.getUpdatedAt(),
                    booking.getVersion() == null ? 0 : booking.getVersion()
            );
        }
    }

    public record BulkRequest(
            @NotNull(message = "Укажите действие.") BulkAction action,
            @NotEmpty(message = "Выберите хотя бы одну запись.") @Size(max = 100) List<UUID> ids,
            BookingStatus status,
            @Size(max = 40) String tag
    ) {
    }

    public record BulkFailure(UUID id, String message) {
    }

    public record BulkResponse(List<UUID> succeeded, List<BulkFailure> failed) {
    }
}
