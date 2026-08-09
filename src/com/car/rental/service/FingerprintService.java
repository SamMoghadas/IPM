package com.car.rental.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Abstraction over the ZKTeco fingerprint terminal.
 * UI and business logic only talk to this interface.
 */
public interface FingerprintService {

    void connect() throws FingerprintException;

    void disconnect();

    boolean isConnected();

    /**
     * Start listening for the next successful verification (one-shot).
     * Implementations should use a short-lived connection and release the device
     * when verification finishes, times out, or is cancelled.
     */
    void listenForVerification(
            int timeoutSeconds,
            Consumer<VerificationResult> onVerified,
            Runnable onTimeout,
            Consumer<FingerprintException> onError
    );

    void cancelListen();

    List<DeviceUser> getUsers() throws FingerprintException;

    void createUser(String deviceUserId, String name) throws FingerprintException;

    void updateUserName(String deviceUserId, String name) throws FingerprintException;

    void deleteUser(String deviceUserId) throws FingerprintException;

    /**
     * Create user on device and enroll one finger (blocking until done or failed).
     * On enroll failure the implementation should roll back the device user when possible.
     */
    void registerUserWithFingerprint(String deviceUserId, String name, int fingerIndex)
            throws FingerprintException;

    /** Enroll an additional finger for an existing device user (blocking). */
    void enrollFingerOnly(String deviceUserId, int fingerIndex) throws FingerprintException;

    void startEnroll(String deviceUserId, int fingerIndex,
                     Consumer<EnrollResult> onFinished,
                     Consumer<FingerprintException> onError) throws FingerprintException;

    DeviceInfo getDeviceInfo() throws FingerprintException;
}
