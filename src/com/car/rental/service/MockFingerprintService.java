package com.car.rental.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Fake implementation for developing / testing UI without a real device.
 */
public class MockFingerprintService implements FingerprintService {

    private boolean connected;
    private final List<DeviceUser> users = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mock-fingerprint");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> pendingListen;
    private volatile boolean listening;

    private String mockUserId = "1001";

    public void setMockUserId(String mockUserId) {
        this.mockUserId = mockUserId;
    }

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public void disconnect() {
        cancelListen();
        connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void listenForVerification(int timeoutSeconds,
                                      Consumer<VerificationResult> onVerified,
                                      Runnable onTimeout,
                                      Consumer<FingerprintException> onError) {
        if (!connected) {
            try {
                connect();
            } catch (Exception e) {
                if (onError != null) {
                    onError.accept(new FingerprintException("Not connected to device"));
                }
                return;
            }
        }
        cancelListen();
        listening = true;

        pendingListen = scheduler.schedule(() -> {
            if (!listening) return;
            listening = false;
            VerificationResult result = new VerificationResult(
                    mockUserId,
                    LocalDateTime.now(),
                    1
            );
            try {
                if (onVerified != null) {
                    onVerified.accept(result);
                }
            } finally {
                disconnect();
            }
        }, 2, TimeUnit.SECONDS);

        scheduler.schedule(() -> {
            if (listening) {
                listening = false;
                if (pendingListen != null) {
                    pendingListen.cancel(false);
                }
                try {
                    if (onTimeout != null) {
                        onTimeout.run();
                    }
                } finally {
                    disconnect();
                }
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void cancelListen() {
        listening = false;
        if (pendingListen != null) {
            pendingListen.cancel(false);
            pendingListen = null;
        }
    }

    @Override
    public List<DeviceUser> getUsers() {
        return new ArrayList<>(users);
    }

    @Override
    public void createUser(String deviceUserId, String name) {
        users.removeIf(u -> u.getDeviceUserId().equals(deviceUserId));
        users.add(new DeviceUser(deviceUserId, name, 0));
    }

    @Override
    public void updateUserName(String deviceUserId, String name) {
        for (int i = 0; i < users.size(); i++) {
            DeviceUser u = users.get(i);
            if (u.getDeviceUserId().equals(deviceUserId)) {
                users.set(i, new DeviceUser(deviceUserId, name, u.getPrivilege()));
                return;
            }
        }
        createUser(deviceUserId, name);
    }

    @Override
    public void deleteUser(String deviceUserId) {
        users.removeIf(u -> u.getDeviceUserId().equals(deviceUserId));
    }

    @Override
    public void registerUserWithFingerprint(String deviceUserId, String name, int fingerIndex) {
        createUser(deviceUserId, name);
    }

    @Override
    public void enrollFingerOnly(String deviceUserId, int fingerIndex) {
        // no-op success for mock
    }

    @Override
    public void startEnroll(String deviceUserId, int fingerIndex,
                            Consumer<EnrollResult> onFinished,
                            Consumer<FingerprintException> onError) {
        scheduler.schedule(() -> {
            if (onFinished != null) {
                onFinished.accept(new EnrollResult(true, "Mock enroll OK for user " + deviceUserId));
            }
        }, 1, TimeUnit.SECONDS);
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        return new DeviceInfo("Mock FW", "MOCK-SERIAL", "MOCK_PLATFORM", "Mock Device");
    }
}
