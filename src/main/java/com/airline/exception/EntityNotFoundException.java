package com.airline.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
    public EntityNotFoundException(String entityName, long id) {
        super(entityName + " с ID=" + id + " не найден(а).");
    }
}