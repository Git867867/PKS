package com.airline.error;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(400, message);
    }
}
