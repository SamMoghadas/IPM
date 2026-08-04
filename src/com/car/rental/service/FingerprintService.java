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
     * When a finger is verified, onVerified is called once, then listening stops.
     *
     * @param timeoutSeconds max wait time in seconds
     * @param onVerified     success callback with device user id + device time
     * @param onTimeout      called if no verification within timeout
     * @param onError        device / connection errors while listening
     */
    void listenForVerification(
            int timeoutSeconds,
            Consumer<VerificationResult> onVerified,
            Runnable onTimeout,
            Consumer<FingerprintException> onError
    );

    /** Cancel an ongoing listenForVerification. */
    void cancelListen();

    List<DeviceUser> getUsers() throws FingerprintException;

    void createUser(String deviceUserId, String name) throws FingerprintException;

    void deleteUser(String deviceUserId) throws FingerprintException;

    /**
     * Start fingerprint enrollment on the device.
     * The employee must place the finger on the sensor (usually 3 times).
     */
    void startEnroll(String deviceUserId, int fingerIndex,
                     Consumer<EnrollResult> onFinished,
                     Consumer<FingerprintException> onError) throws FingerprintException;

    DeviceInfo getDeviceInfo() throws FingerprintException;
}
