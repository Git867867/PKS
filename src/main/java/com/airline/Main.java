package com.airline;

import com.airline.repository.BookingRepository;
import com.airline.repository.PassengerRepository;
import com.airline.service.BookingService;
import com.airline.service.PassengerService;
import com.airline.ui.ConsoleMenu;
import com.airline.util.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager.getConnection();
            System.out.println("Подключение к базе данных установлено.");

            PassengerRepository passengerRepository = new PassengerRepository();
            BookingRepository bookingRepository = new BookingRepository();

            PassengerService passengerService = new PassengerService(passengerRepository);
            BookingService bookingService = new BookingService(bookingRepository, passengerRepository);

            ConsoleMenu menu = new ConsoleMenu(bookingService, passengerService);
            menu.run();

        } catch (Exception e) {
            System.err.println("КРИТИЧЕСКАЯ ОШИБКА: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseManager.closeConnection();
            System.out.println("Соединение с базой данных закрыто.");
        }
    }
}