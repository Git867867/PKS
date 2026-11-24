package com.airline.fx.controller;

import com.airline.exception.BusinessRuleException;
import com.airline.exception.DatabaseException;
import com.airline.exception.EntityNotFoundException;
import com.airline.fx.ExceptionMessages;
import com.airline.fx.FxDialogs;
import com.airline.model.Booking;
import com.airline.model.BookingStatus;
import com.airline.model.Passenger;
import com.airline.service.BookingService;
import com.airline.service.PassengerService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BookingFormController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label titleLabel;
    @FXML private ComboBox<Passenger> passengerCombo;
    @FXML private TextField flightField;
    @FXML private TextField departureField;
    @FXML private TextField arrivalField;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private TextField priceField;
    @FXML private ComboBox<BookingStatus> statusCombo;
    @FXML private Label errorLabel;

    private Stage stage;
    private BookingService bookingService;
    private PassengerService passengerService;
    private Booking existing;
    private boolean saved;

    public static boolean show(Window owner, BookingService bookingService, PassengerService passengerService,
                               Booking existing) {
        try {
            URL fxml = BookingFormController.class.getResource("/fxml/booking-form.fxml");
            if (fxml == null) {
                throw new IllegalStateException("Не найден файл /fxml/booking-form.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            BookingFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setTitle(existing == null ? "Новое бронирование" : "Редактирование бронирования");
            // Ширина и высота окна формы. Запас по высоте нужен, чтобы красная ошибка не прятала кнопки.
            stage.setScene(new Scene(root, 620, 680));
            stage.setMinWidth(580);
            stage.setMinHeight(640);
            controller.init(stage, bookingService, passengerService, existing);
            stage.showAndWait();
            return controller.saved;
        } catch (Exception e) {
            FxDialogs.showError(owner, ExceptionMessages.toUserText(e));
            return false;
        }
    }

    private void init(Stage stage, BookingService bookingService, PassengerService passengerService, Booking existing) {
        this.stage = stage;
        this.bookingService = bookingService;
        this.passengerService = passengerService;
        this.existing = existing;

        passengerCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Passenger passenger) {
                if (passenger == null) {
                    return "";
                }
                return passenger.getFullName() + " (" + passenger.getPassportNumber() + ")";
            }

            @Override
            public Passenger fromString(String value) {
                return null;
            }
        });
        statusCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(BookingStatus status) {
                return status == null ? "" : status.getDisplayName();
            }

            @Override
            public BookingStatus fromString(String value) {
                return null;
            }
        });

        loadPassengers();
        if (existing == null) {
            titleLabel.setText("Новое бронирование");
            statusCombo.getItems().add(BookingStatus.CREATED);
            statusCombo.setValue(BookingStatus.CREATED);
            statusCombo.setDisable(true);
            timeField.setText("08:30");
            return;
        }

        titleLabel.setText("Редактирование бронирования");
        selectPassenger(existing.getPassengerId());
        flightField.setText(existing.getFlightNumber());
        departureField.setText(existing.getDepartureCity());
        arrivalField.setText(existing.getArrivalCity());
        datePicker.setValue(existing.getDepartureDate().toLocalDate());
        timeField.setText(existing.getDepartureDate().toLocalTime().format(TIME_FORMAT));
        priceField.setText(existing.getPrice().toPlainString());
        fillAllowedStatuses(existing.getStatus());
    }

    @FXML
    private void onNewPassenger() {
        Passenger created = PassengerFormController.show(stage, passengerService, null);
        if (created == null) {
            return;
        }
        loadPassengers();
        selectPassenger(created.getId());
    }

    @FXML
    private void onSave() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        try {
            if (existing == null) {
                bookingService.createBooking(readBooking(null));
                saved = true;
                stage.close();
                return;
            }

            BookingStatus previousStatus = existing.getStatus();
            BookingStatus selectedStatus = statusCombo.getValue();
            bookingService.updateBooking(readBooking(previousStatus));
            if (selectedStatus != null && selectedStatus != previousStatus) {
                try {
                    bookingService.changeStatus(existing.getId(), selectedStatus);
                } catch (BusinessRuleException | EntityNotFoundException | DatabaseException e) {
                    saved = true;
                    showError(ExceptionMessages.toUserText(e));
                    return;
                }
            }
            saved = true;
            stage.close();
        } catch (BusinessRuleException | EntityNotFoundException | DatabaseException e) {
            showError(ExceptionMessages.toUserText(e));
        }
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    private void loadPassengers() {
        Passenger selected = passengerCombo.getValue();
        passengerCombo.getItems().setAll(passengerService.getAllPassengers());
        if (selected != null) {
            selectPassenger(selected.getId());
        }
    }

    private void selectPassenger(long passengerId) {
        for (Passenger passenger : passengerCombo.getItems()) {
            if (passenger.getId() == passengerId) {
                passengerCombo.setValue(passenger);
                return;
            }
        }
    }

    private void fillAllowedStatuses(BookingStatus current) {
        statusCombo.getItems().clear();
        statusCombo.getItems().add(current);
        for (BookingStatus status : BookingStatus.values()) {
            if (current.canTransitionTo(status)) {
                statusCombo.getItems().add(status);
            }
        }
        statusCombo.setValue(current);
    }

    private Booking readBooking(BookingStatus status) {
        Passenger passenger = passengerCombo.getValue();
        if (passenger == null) {
            throw new BusinessRuleException("Выберите пассажира из списка.");
        }
        Booking booking = new Booking(
                passenger.getId(),
                text(flightField).toUpperCase(),
                text(departureField),
                text(arrivalField),
                readDeparture(),
                readPrice()
        );
        if (existing != null) {
            booking.setId(existing.getId());
            booking.setStatus(status);
        }
        return booking;
    }

    private LocalDateTime readDeparture() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            throw new BusinessRuleException("Укажите дату вылета.");
        }
        String timeText = text(timeField);
        if (!timeText.matches("\\d{2}:\\d{2}")) {
            throw new BusinessRuleException("Время вылета укажите в формате ЧЧ:ММ, например 08:30.");
        }
        int hour = Integer.parseInt(timeText.substring(0, 2));
        int minute = Integer.parseInt(timeText.substring(3, 5));
        if (hour > 23 || minute > 59) {
            throw new BusinessRuleException("Время вылета указано неверно. Часы от 00 до 23, минуты от 00 до 59.");
        }
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }

    private BigDecimal readPrice() {
        String value = text(priceField).replace(',', '.');
        if (value.isEmpty()) {
            throw new BusinessRuleException("Укажите цену билета.");
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Цена должна быть числом, например 5500 или 5500.50.");
        }
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        FxDialogs.showError(stage, message);
    }
}
