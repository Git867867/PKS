package com.airline.model;

public class Passenger {
    private long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String passportNumber;

    public Passenger() {}

    public Passenger(String firstName, String lastName, String email,
                     String phone, String passportNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.passportNumber = passportNumber;
    }

    public Passenger(long id, String firstName, String lastName, String email,
                     String phone, String passportNumber) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.passportNumber = passportNumber;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format(
                "Пассажир #%d: %s %s | Email: %s | Тел: %s | Паспорт: %s",
                id, firstName, lastName, email, phone, passportNumber
        );
    }
}