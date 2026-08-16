package com.car.rental.model;

/**
 * Optional filters for rental report queries.
 * Dates are Jalali calendar strings in {@code yyyy/MM/dd} form (no time required).
 */
public class RentalReportFilter {

    public enum Status {
        ALL,
        OPEN,   // هنوز برگشت نخورده
        CLOSED  // برگشت ثبت شده
    }

    private String employeeName;
    private String plate;
    private String carName;
    private String destination;
    /** Inclusive start day, Jalali yyyy/MM/dd */
    private String dateFrom;
    /** Inclusive end day, Jalali yyyy/MM/dd */
    private String dateTo;
    private Status status = Status.ALL;

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = blankToNull(employeeName);
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = blankToNull(plate);
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = blankToNull(carName);
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = blankToNull(destination);
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = blankToNull(dateFrom);
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = blankToNull(dateTo);
    }

    public Status getStatus() {
        return status == null ? Status.ALL : status;
    }

    public void setStatus(Status status) {
        this.status = status == null ? Status.ALL : status;
    }

    public boolean isEmpty() {
        return employeeName == null
                && plate == null
                && carName == null
                && destination == null
                && dateFrom == null
                && dateTo == null
                && getStatus() == Status.ALL;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
