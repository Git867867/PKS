package com.airline.service;

import com.airline.domain.Passenger;
import com.airline.dto.PassengerDtos.PassengerRequest;
import com.airline.dto.PassengerDtos.PassengerResponse;
import com.airline.error.BadRequestException;
import com.airline.error.NotFoundException;
import com.airline.repository.PassengerRepository;
import com.airline.security.Auth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PassengerService {

    private final PassengerRepository passengers;
    private final AuditService audit;

    public PassengerService(PassengerRepository passengers, AuditService audit) {
        this.passengers = passengers;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PassengerResponse> list() {
        return passengers.findAllByOrderByLastNameAscFirstNameAsc().stream()
                .map(PassengerResponse::from)
                .toList();
    }

    @Transactional
    public PassengerResponse create(PassengerRequest request) {
        Passenger passenger = new Passenger();
        apply(passenger, request, true);
        passengers.save(passenger);
        audit.record(Auth.require().getId(), "PASSENGER_CREATED", "Passenger", passenger.getPublicId(),
                "Создан пассажир " + passenger.fullName());
        return PassengerResponse.from(passenger);
    }

    @Transactional
    public PassengerResponse update(UUID id, PassengerRequest request) {
        Passenger passenger = require(id);
        apply(passenger, request, false);
        audit.record(Auth.require().getId(), "PASSENGER_UPDATED", "Passenger", passenger.getPublicId(),
                "Изменен пассажир " + passenger.fullName());
        return PassengerResponse.from(passenger);
    }

    @Transactional
    public PassengerResponse setActive(UUID id, boolean active) {
        Passenger passenger = require(id);
        passenger.setActive(active);
        audit.record(Auth.require().getId(), "PASSENGER_STATUS", "Passenger", passenger.getPublicId(),
                (active ? "Включен" : "Отключен") + " пассажир " + passenger.fullName());
        return PassengerResponse.from(passenger);
    }

    private void apply(Passenger passenger, PassengerRequest request, boolean creating) {
        String first = request.firstName().trim();
        String last = request.lastName().trim();
        String email = request.email().trim();
        String phone = request.phone().trim();
        String passport = request.passportNumber().trim();
        checkName("Имя", first);
        checkName("Фамилия", last);
        if (!email.matches("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")) {
            throw new BadRequestException("Некорректный формат email.");
        }
        if (!phone.matches("\\+7-\\d{3}-\\d{3}-\\d{4}")) {
            throw new BadRequestException("Телефон должен быть в формате +7-XXX-XXX-XXXX.");
        }
        if (!passport.matches("\\d{4} \\d{6}")) {
            throw new BadRequestException("Паспорт должен быть в формате XXXX XXXXXX.");
        }
        if (creating) {
            if (passengers.existsByEmailIgnoreCase(email)) {
                throw new BadRequestException("Пассажир с таким email уже существует.");
            }
            if (passengers.existsByPassportNumber(passport)) {
                throw new BadRequestException("Пассажир с таким паспортом уже существует.");
            }
        } else {
            if (passengers.existsByEmailIgnoreCaseAndIdNot(email, passenger.getId())) {
                throw new BadRequestException("Email уже занят другим пассажиром.");
            }
            if (passengers.existsByPassportNumberAndIdNot(passport, passenger.getId())) {
                throw new BadRequestException("Паспорт уже занят другим пассажиром.");
            }
        }
        passenger.setFirstName(first);
        passenger.setLastName(last);
        passenger.setEmail(email);
        passenger.setPhone(phone);
        passenger.setPassportNumber(passport);
    }

    private void checkName(String label, String value) {
        if (!value.matches("[а-яА-ЯёЁa-zA-Z\\-]+")) {
            throw new BadRequestException(label + " может содержать только буквы и дефис.");
        }
    }

    private Passenger require(UUID id) {
        return passengers.findByPublicId(id).orElseThrow(() -> new NotFoundException("Пассажир не найден."));
    }
}
