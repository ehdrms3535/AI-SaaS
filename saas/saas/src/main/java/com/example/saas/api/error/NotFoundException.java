package com.example.saas.api.error;

public class NotFoundException extends RuntimeException {
    private final String error;

    public NotFoundException(String error) {
        super(error);
        this.error = error;
    }

    public NotFoundException(String error, String message) {
        super(message);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}