package com.airline.fx;

import com.airline.fx.controller.MainController;
import com.airline.repository.BookingRepository;
import com.airline.repository.PassengerRepository;
import com.airline.service.BookingService;
import com.airline.service.PassengerService;
import com.airline.util.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class AirlineFxApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            DatabaseManager.getConnection();

            PassengerRepository passengerRepository = new PassengerRepository();
            BookingRepository bookingRepository = new BookingRepository();
            PassengerService passengerService = new PassengerService(passengerRepository);
            BookingService bookingService = new BookingService(bookingRepository, passengerRepository);

            URL fxml = getClass().getResource("/fxml/main-view.fxml");
            if (fxml == null) {
                throw new IllegalStateException("Не найден файл /fxml/main-view.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.init(bookingService, passengerService);

            stage.setTitle("Бронирование авиабилетов");
            stage.setScene(new Scene(root, 1100, 720));
            stage.setMinWidth(960);
            stage.setMinHeight(640);
            stage.setOnCloseRequest(event -> DatabaseManager.closeConnection());
            stage.show();
        } catch (Exception e) {
            String message = ExceptionMessages.toUserText(e);
            System.err.println(message);
            FxDialogs.showError(null, message);
            DatabaseManager.closeConnection();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
