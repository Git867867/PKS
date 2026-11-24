package com.airline.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Booking {
    private long id;
    private long passengerId;
    private String flightNumber;
    private String departureCity;
    private String arrivalCity;
    private LocalDateTime departureDate;
    private BigDecimal price;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private String passengerFullName;

    public Booking() {}

    public Booking(long passengerId, String flightNumber, String departureCity,
                   String arrivalCity, LocalDateTime departureDate, BigDecimal price) {
        this.passengerId = passengerId;
        this.flightNumber = flightNumber;
        this.departureCity = departureCity;
        this.arrivalCity = arrivalCity;
        this.departureDate = departureDate;
        this.price = price;
        this.status = BookingStatus.CREATED;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPassengerId() { return passengerId; }
    public void setPassengerId(long passengerId) { this.passengerId = passengerId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }

    public String getArrivalCity() { return arrivalCity; }
    public void setArrivalCity(String arrivalCity) { this.arrivalCity = arrivalCity; }

    public LocalDateTime getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDateTime departureDate) { this.departureDate = departureDate; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPassengerFullName() { return passengerFullName; }
    public void setPassengerFullName(String passengerFullName) { this.passengerFullName = passengerFullName; }

    public String getRoute() {
        return departureCity + " - " + arrivalCity;
    }

    @Override
    public String toString() {
        return String.format(
                "Бронирование #%d | Рейс: %s | %s - %s | Дата: %s | Цена: %.2f руб. | Статус: %s | Пассажир: %s",
                id, flightNumber, departureCity, arrivalCity,
                departureDate != null ? departureDate.toString() : "-",
                price, status.getDisplayName(),
                passengerFullName != null ? passengerFullName : "ID=" + passengerId
        );
    }
}