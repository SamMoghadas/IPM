package com.car.rental;

public class RentalRecord {
    public int personnelId;
    public String employeeName;
    public String carName;
    public String carColor;
    public String plate;
    public String pickupDate;
    public String returnDate;
    public String destination;

    public RentalRecord(int personnelId, String employeeName, String carName, String carColor,
                        String plate, String pickupDate, String returnDate, String destination) {
        this.personnelId = personnelId;
        this.employeeName = employeeName;
        this.carName = carName;
        this.carColor = carColor;
        this.plate = plate;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate == null ? "منتظر برگشت" : returnDate;
        this.destination = destination;
    }
}
