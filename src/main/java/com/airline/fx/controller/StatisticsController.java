package com.airline.fx.controller;

import com.airline.fx.ExceptionMessages;
import com.airline.fx.FxDialogs;
import com.airline.model.BookingStatus;
import com.airline.service.BookingService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsController {
    @FXML private Label totalLabel;
    @FXML private Label statusLabel;
    @FXML private Label averageLabel;
    @FXML private Label routeLabel;
    @FXML private Label passengerLabel;
    @FXML private Label revenueLabel;

    private Stage stage;

    public static void show(Window owner, BookingService bookingService) {
        try {
            URL fxml = StatisticsController.class.getResource("/fxml/statistics-view.fxml");
            if (fxml == null) {
                throw new IllegalStateException("Не найден файл /fxml/statistics-view.fxml");
            }
            FXMLLoader loader = new FXMLLoader(fxml);
            Parent root = loader.load();
            StatisticsController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(owner);
            stage.setTitle("Статистика");
            stage.setScene(new Scene(root));
            stage.setMinWidth(480);
            controller.init(stage, bookingService);
            stage.showAndWait();
        } catch (Exception e) {
            FxDialogs.showError(owner, ExceptionMessages.toUserText(e));
        }
    }

    private void init(Stage stage, BookingService bookingService) {
        this.stage = stage;
        totalLabel.setText("1. Всего бронирований: " + bookingService.getTotalBookingsCount());
        statusLabel.setText("2. По статусам: " + statusCounts(bookingService));
        averageLabel.setText(String.format("3. Средняя цена: %.2f руб.", bookingService.getAveragePrice()));
        routeLabel.setText("4. Самый популярный маршрут: " + bookingService.getMostPopularRoute());
        passengerLabel.setText("5. Пассажир с наибольшим числом бронирований: " + bookingService.getTopPassenger());
        revenueLabel.setText(String.format(
                "6. Выручка по статусам «Подтверждено» и «Завершено»: %.2f руб.",
                bookingService.getTotalRevenue()));
    }

    private String statusCounts(BookingService bookingService) {
        Map<String, Long> counts = new HashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            counts.put(status.name(), 0L);
        }
        List<Object[]> rows = bookingService.getBookingsCountByStatus();
        for (Object[] row : rows) {
            counts.put(String.valueOf(row[0]), (Long) row[1]);
        }
        StringBuilder text = new StringBuilder();
        for (BookingStatus status : BookingStatus.values()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(status.getDisplayName()).append(" — ").append(counts.get(status.name()));
        }
        return text.toString();
    }

    @FXML
    private void onClose() {
        stage.close();
    }
}
