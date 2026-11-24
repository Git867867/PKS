package com.airline.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.net.URL;
import java.util.Optional;

public final class FxDialogs {
    private FxDialogs() {}

    public static void showError(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        prepare(alert, owner);
        alert.showAndWait();
    }

    public static void showInfo(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Готово");
        alert.setHeaderText(null);
        alert.setContentText(message);
        prepare(alert, owner);
        alert.showAndWait();
    }

    public static boolean confirmDelete(Window owner, String message) {
        ButtonType delete = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, delete, cancel);
        alert.setTitle("Подтверждение удаления");
        alert.setHeaderText(null);
        prepare(alert, owner);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == delete;
    }

    private static void prepare(Alert alert, Window owner) {
        if (owner != null) {
            alert.initOwner(owner);
        }
        URL css = FxDialogs.class.getResource("/css/app.css");
        if (css != null) {
            alert.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
    }
}
