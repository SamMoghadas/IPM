package com.car.rental.service;

import java.time.LocalDateTime;

/**
 * Result of a successful fingerprint verification on the device.
 */
public class VerificationResult {
    private final String deviceUserId;
    private final LocalDateTime deviceTime;
    private final int verifyType;

    public VerificationResult(String deviceUserId, LocalDateTime deviceTime, int verifyType) {
        this.deviceUserId = deviceUserId;
        this.deviceTime = deviceTime;
        this.verifyType = verifyType;
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public LocalDateTime getDeviceTime() {
        return deviceTime;
    }

    public int getVerifyType() {
        return verifyType;
    }

    @Override
    public String toString() {
        return "VerificationResult{userId='" + deviceUserId +
                "', time=" + deviceTime + ", verifyType=" + verifyType + '}';
    }
}
