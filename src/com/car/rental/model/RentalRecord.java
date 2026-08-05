package com.car.rental.model;

public class RentalRecord {
    /** Device User ID (PIN on fingerprint terminal), not internal DB id. */
    public String deviceUserId;
    public String employeeName;
    public String carName;
    public String carColor;
    public String plate;
    public String pickupDate;
    public String returnDate;
    public String destination;

    public RentalRecord(String deviceUserId, String employeeName, String carName, String carColor,
                        String plate, String pickupDate, String returnDate, String destination) {
        this.deviceUserId = deviceUserId;
        this.employeeName = employeeName;
        this.carName = carName;
        this.carColor = carColor;
        this.plate = plate;
        this.pickupDate = pickupDate;
        this.returnDate = returnDate == null ? "منتظر برگشت" : returnDate;
        this.destination = destination;
    }
}
