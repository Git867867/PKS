package com.airline.error;

public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(403, message);
    }

    public ForbiddenException(String message, String code) {
        super(403, message, code);
    }
}
