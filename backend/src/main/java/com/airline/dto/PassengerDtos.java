package com.airline.dto;

import com.airline.domain.Passenger;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class PassengerDtos {
    private PassengerDtos() {
    }

    public record PassengerRequest(
            @NotBlank(message = "Введите имя.") @Size(max = 100) String firstName,
            @NotBlank(message = "Введите фамилию.") @Size(max = 100) String lastName,
            @NotBlank(message = "Введите email.") @Size(max = 150) String email,
            @NotBlank(message = "Введите телефон.") @Size(max = 20) String phone,
            @NotBlank(message = "Введите паспорт.") @Size(max = 20) String passportNumber
    ) {
    }

    public record ActiveRequest(boolean active) {
    }

    public record PassengerResponse(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String passportNumber,
            boolean active
    ) {
        public static PassengerResponse from(Passenger passenger) {
            return new PassengerResponse(
                    passenger.getPublicId(),
                    passenger.getFirstName(),
                    passenger.getLastName(),
                    passenger.getEmail(),
                    passenger.getPhone(),
                    passenger.getPassportNumber(),
                    passenger.isActive()
            );
        }
    }
}
