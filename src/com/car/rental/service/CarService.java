package com.car.rental.service;

import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Car;
import com.car.rental.util.DataChangeCallback;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

/**
 * Business logic for fleet / cars.
 */
@Service
public class CarService {

    private final DatabaseManager db;

    public CarService(DatabaseManager db) {
        this.db = db;
    }

    public void addCar(String name, String plate, String color) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("نام ماشین الزامی است");
        }
        if (plate == null || plate.isBlank()) {
            throw new IllegalArgumentException("پلاک الزامی است");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("رنگ الزامی است");
        }
        if (db.isPlateTaken(plate, null)) {
            throw new SQLException("این پلاک قبلاً ثبت شده است");
        }
        db.addCar(name, plate, color);
    }

    public void updateCar(Car car, String oldPlate) throws SQLException {
        if (car == null) {
            throw new IllegalArgumentException("car is null");
        }
        String model = car.getModel();
        String plate = car.getPlate();
        String color = car.getColor();
        if (model == null || model.isBlank()
                || plate == null || plate.isBlank()
                || color == null || color.isBlank()) {
            throw new IllegalArgumentException("تمام فیلدهای ماشین باید پر شوند");
        }
        if (db.isPlateTaken(plate, oldPlate)) {
            throw new SQLException("این پلاک قبلاً برای ماشین دیگری ثبت شده است");
        }
        db.updateCar(car, oldPlate);
    }

    public void deleteCar(String plate, DataChangeCallback callback) throws SQLException {
        db.deleteCar(plate, callback);
    }

    public List<Car> getAvailableCars() throws SQLException {
        return db.listAvailableCars();
    }

    public List<Car> getAllCars() throws SQLException {
        return db.listAllCars();
    }

    public int getCarIdByPlate(String plate) throws SQLException {
        return db.getCarIdByPlate(plate);
    }

    public boolean isPlateTaken(String plate, String excludePlate) throws SQLException {
        return db.isPlateTaken(plate, excludePlate);
    }
}
