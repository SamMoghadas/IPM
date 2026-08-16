package com.car.rental.service;

import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for car pickup / return and rental reports.
 */
@Service
public class RentalService {

    private final DatabaseManager db;

    public RentalService(DatabaseManager db) {
        this.db = db;
    }

    public void pickup(String deviceUserId, String carPlate, String pickupTime, String destination)
            throws SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کاربر خالی است");
        }
        if (carPlate == null || carPlate.isBlank()) {
            throw new IllegalArgumentException("پلاک ماشین خالی است");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("مقصد الزامی است");
        }
        db.insertRental(deviceUserId, carPlate, pickupTime, destination);
    }

    public boolean returnCar(String deviceUserId, String returnDate) throws SQLException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("شناسه کاربر خالی است");
        }
        return db.returnCarByDeviceUserId(deviceUserId, returnDate);
    }

    public RentalRecord getActiveRentalByDeviceUserId(String deviceUserId) throws SQLException {
        return db.getActiveRentalByDeviceUserId(deviceUserId);
    }

    public List<RentalRecord> getRentalReport() throws SQLException {
        return db.getRentalReport();
    }

    public List<RentalRecord> getRentalReport(RentalReportFilter filter) throws SQLException {
        return db.getRentalReport(filter);
    }

    public Employee findEmployeeByDeviceUserId(String deviceUserId) throws SQLException {
        return db.findByDeviceUserId(deviceUserId);
    }
}
