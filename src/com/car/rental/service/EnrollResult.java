package com.car.rental.service;

public class EnrollResult {
    private final boolean success;
    private final String message;

    public EnrollResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
