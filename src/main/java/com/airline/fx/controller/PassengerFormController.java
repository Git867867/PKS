package com.airline.fx.controller;

import com.airline.exception.BusinessRuleException;
import com.airline.exception.DatabaseException;
import com.airline.exception.EntityNotFoundException;
import com.airline.fx.ExceptionMessages;
import com.airline.fx.FxDialogs;
import com.airline.model.Passenger;
import com.airline.service.PassengerService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;

public class PassengerFormController {
    @FXML private Label titleLabel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField passportField;
    @FXML private Label errorLabel;

    private Stage stage;
    private PassengerService passengerService;
    private Passenger existing;
    private Passenger savedPassenger;

    public static Passenger show(Window owner, PassengerService passengerService, Passenger existing) {
        try {
            URL fxml = PassengerFormController.class.getResource("/fxml/passenger-form.fxml");
            if (fxml == null) {
                throw new IllegalStateException("Не найден файл /fxml/passenger-form.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            PassengerFormController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setTitle(existing == null ? "Новый пассажир" : "Редактирование пассажира");
            stage.setScene(new Scene(root, 560, 480));
            stage.setMinWidth(520);
            stage.setMinHeight(440);
            controller.init(stage, passengerService, existing);
            stage.showAndWait();
            return controller.savedPassenger;
        } catch (Exception e) {
            FxDialogs.showError(owner, ExceptionMessages.toUserText(e));
            return null;
        }
    }

    private void init(Stage stage, PassengerService passengerService, Passenger existing) {
        this.stage = stage;
        this.passengerService = passengerService;
        this.existing = existing;
        if (existing == null) {
            titleLabel.setText("Новый пассажир");
            return;
        }
        titleLabel.setText("Редактирование пассажира");
        firstNameField.setText(existing.getFirstName());
        lastNameField.setText(existing.getLastName());
        emailField.setText(existing.getEmail());
        phoneField.setText(existing.getPhone());
        passportField.setText(existing.getPassportNumber());
    }

    @FXML
    private void onSave() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        try {
            Passenger passenger = readPassenger();
            if (existing == null) {
                passengerService.createPassenger(passenger);
            } else {
                passenger.setId(existing.getId());
                passengerService.updatePassenger(passenger);
            }
            savedPassenger = passenger;
            stage.close();
        } catch (BusinessRuleException | EntityNotFoundException | DatabaseException e) {
            showError(ExceptionMessages.toUserText(e));
        }
    }

    @FXML
    private void onCancel() {
        stage.close();
    }

    private Passenger readPassenger() {
        return new Passenger(
                text(firstNameField),
                text(lastNameField),
                text(emailField),
                text(phoneField),
                text(passportField)
        );
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
