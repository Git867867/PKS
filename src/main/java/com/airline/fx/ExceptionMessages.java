package com.airline.fx;

import java.sql.SQLException;

public final class ExceptionMessages {
    private ExceptionMessages() {}

    public static String toUserText(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException) {
                SQLException sql = (SQLException) current;
                if ("23503".equals(sql.getSQLState())) {
                    return "Нельзя удалить пассажира, пока на него есть бронирования. Сначала удалите эти бронирования.";
                }
                if ("23505".equals(sql.getSQLState())) {
                    return "Такая запись уже есть в базе. Проверьте email и номер паспорта.";
                }
            }
            current = current.getCause();
        }
        if (error.getMessage() == null || error.getMessage().isBlank()) {
            return "Произошла неизвестная ошибка.";
        }
        return error.getMessage();
    }
}
