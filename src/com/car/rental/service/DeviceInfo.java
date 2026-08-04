package com.car.rental.service;

public class DeviceInfo {
    private final String firmware;
    private final String serial;
    private final String platform;
    private final String deviceName;

    public DeviceInfo(String firmware, String serial, String platform, String deviceName) {
        this.firmware = firmware;
        this.serial = serial;
        this.platform = platform;
        this.deviceName = deviceName;
    }

    public String getFirmware() {
        return firmware;
    }

    public String getSerial() {
        return serial;
    }

    public String getPlatform() {
        return platform;
    }

    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public String toString() {
        return "DeviceInfo{firmware='" + firmware + "', serial='" + serial +
                "', platform='" + platform + "', name='" + deviceName + "'}";
    }
}
