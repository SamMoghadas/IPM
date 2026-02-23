package com.car.rental.model;

public class Employee {
    private final int personnelId;
    private String name;
    private String phone;
    private String telegramId;

    public Employee(int personnelId, String name, String phone, String telegramId) {
        this.personnelId = personnelId;
        this.name = name;
        this.phone = phone;
        this.telegramId = telegramId;
    }

    public int getPersonnelId() {
        return personnelId;
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

    public String getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }
}
