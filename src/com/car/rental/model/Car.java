package com.car.rental.model;

public class Car {
    private String model;
    private String plate;
    private String color;
    private String status;

    /** Standard order: model, plate, color */
    public Car(String model, String plate, String color) {
        this.model = model;
        this.plate = plate;
        this.color = color;
    }

    public Car(String model, String plate, String color, String status) {
        this(model, plate, color);
        this.status = status;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
