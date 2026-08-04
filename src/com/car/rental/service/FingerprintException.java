package com.car.rental.service;

public class FingerprintException extends Exception {

    public FingerprintException(String message) {
        super(message);
    }

    public FingerprintException(String message, Throwable cause) {
        super(message, cause);
    }
}
