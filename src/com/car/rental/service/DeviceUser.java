package com.car.rental.service;

/**
 * A user stored on the ZKTeco device.
 */
public class DeviceUser {
    private final String deviceUserId;
    private final String name;
    private final int privilege;

    public DeviceUser(String deviceUserId, String name, int privilege) {
        this.deviceUserId = deviceUserId;
        this.name = name;
        this.privilege = privilege;
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public String getName() {
        return name;
    }

    public int getPrivilege() {
        return privilege;
    }

    @Override
    public String toString() {
        return name + " (" + deviceUserId + ")";
    }
}
