package com.car.rental.model;

/**
 * Employee model linked to ZKTeco device user.
 * deviceUserId is the User ID defined on the fingerprint device
 * (the dedicated "second identity" for this software).
 */
public class Employee {
    private final int id;              // internal DB primary key
    private String deviceUserId;       // User ID on ZKTeco device (unique)
    private String name;
    private String phone;
    private boolean active;
    private boolean renting;

    /** Constructor for creating a new employee (not yet saved). */
    public Employee(String deviceUserId, String name, String phone) {
        this.id = 0;
        this.deviceUserId = deviceUserId;
        this.name = name;
        this.phone = phone;
        this.active = true;
        this.renting = false;
    }

    /** Full constructor (when loading from database). */
    public Employee(int id, String deviceUserId, String name, String phone,
                    boolean active, boolean renting) {
        this.id = id;
        this.deviceUserId = deviceUserId;
        this.name = name;
        this.phone = phone;
        this.active = active;
        this.renting = renting;
    }

    public int getId() {
        return id;
    }

    public String getDeviceUserId() {
        return deviceUserId;
    }

    public void setDeviceUserId(String deviceUserId) {
        this.deviceUserId = deviceUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isRenting() {
        return renting;
    }

    public void setRenting(boolean renting) {
        this.renting = renting;
    }

    @Override
    public String toString() {
        return name + " (" + deviceUserId + ")";
    }
}
