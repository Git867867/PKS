package com.airline.service;

import com.airline.domain.BookingStatus;
import com.airline.error.BadRequestException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class BookingRules {

    private BookingRules() {
    }

    public static void checkFlight(String flight) {
        if (flight == null || flight.isBlank()) {
            throw new BadRequestException("Номер рейса не может быть пустым.");
        }
        if (flight.length() > 10 || !flight.matches("[A-Za-z]{1,3}-\\d{1,4}")) {
            throw new BadRequestException("Номер рейса должен быть в формате XX-1234.");
        }
    }

    public static void checkCities(String departure, String arrival) {
        checkCity("Город вылета", departure);
        checkCity("Город прилета", arrival);
        if (departure.equalsIgnoreCase(arrival)) {
            throw new BadRequestException("Город вылета и прилета не могут совпадать.");
        }
    }

    public static void checkPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Цена билета должна быть больше 0.");
        }
        if (price.scale() > 2) {
            throw new BadRequestException("Цена может содержать не больше двух знаков после запятой.");
        }
    }

    public static void checkDeparture(LocalDateTime date, LocalDateTime previous) {
        if (date == null) {
            throw new BadRequestException("Дата вылета не может быть пустой.");
        }
        boolean unchanged = previous != null && previous.equals(date);
        if (!unchanged && date.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Дата вылета не может быть в прошлом.");
        }
        if (date.isAfter(LocalDateTime.now().plusMonths(6))) {
            throw new BadRequestException("Дата вылета не может быть более чем через 6 месяцев.");
        }
    }

    public static void checkStatusChange(BookingStatus current, BookingStatus next) {
        if (next == null) {
            throw new BadRequestException("Укажите статус.");
        }
        if (!current.canTransitionTo(next)) {
            throw new BadRequestException("Нельзя перевести статус из " + current + " в " + next + ".");
        }
    }

    private static void checkCity(String label, String city) {
        if (city == null || city.isBlank() || city.length() > 100) {
            throw new BadRequestException(label + " обязателен и не длиннее 100 символов.");
        }
        if (!city.matches("[а-яА-ЯёЁa-zA-Z\\s\\-]+")) {
            throw new BadRequestException(label + " может содержать только буквы, пробелы и дефисы.");
        }
    }
}
