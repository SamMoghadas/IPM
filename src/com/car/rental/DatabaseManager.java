package com.car.rental;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:IPMCarRental.db";
        return DriverManager.getConnection(url);
    }

    // --- ایجاد جدول‌ها ---
    public void initDatabase() {
        String employeeTable = "CREATE TABLE IF NOT EXISTS Employee(" +
                "name TEXT NOT NULL, phone TEXT PRIMARY KEY, telegram_id TEXT)";
        String carTable = "CREATE TABLE IF NOT EXISTS Car(" +
                "name TEXT NOT NULL, plate TEXT PRIMARY KEY, color TEXT NOT NULL)";
        String rentalTable = "CREATE TABLE IF NOT EXISTS CarRental(" +
                "rental_code TEXT, employee_phone TEXT, car_plate TEXT, " +
                "delivery_date TEXT, return_date TEXT, destination TEXT, " +
                "PRIMARY KEY(employee_phone, car_plate, delivery_date), " +
                "FOREIGN KEY(employee_phone) REFERENCES Employee(phone), " +
                "FOREIGN KEY(car_plate) REFERENCES Car(plate))";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(carTable);
            stmt.execute(rentalTable);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed!", e.getMessage());
        }
    }

    // --- ثبت تحویل ماشین ---
    public void insertRental(String rentalCode, String empPhone, String carPlate, String pickupTime, String destination) throws SQLException {
        String sql = "INSERT INTO CarRental(rental_code, employee_phone, car_plate, delivery_date, destination) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, rentalCode);
            stmt.setString(2, empPhone);
            stmt.setString(3, carPlate);
            stmt.setString(4, pickupTime);
            stmt.setString(5, destination);

            stmt.executeUpdate();
        }
    }

    // --- جستجوی کارمند از روی اسم ---
    public String getEmployeePhoneByName(String name) throws SQLException {
        String sql = "SELECT phone FROM Employee WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("phone");
            }
        }
        return null;
    }

    // --- ثبت برگشت ماشین ---
    public boolean returnCar(String rentalCode, String returnDate) throws SQLException {
        String checkSql = "SELECT * FROM CarRental WHERE rental_code = ? AND return_date IS NULL";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            stmt.setString(1, rentalCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String updateSql = "UPDATE CarRental SET return_date = ? WHERE rental_code = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, returnDate);
                    updateStmt.setString(2, rentalCode);
                    updateStmt.executeUpdate();
                    return true; // یعنی ماشین با موفقیت برگشت
                }
            } else {
                return false; // یعنی کد تحویل معتبر نیست یا قبلاً برگشته
            }
        }
    }

    // --- لیست ماشین ها ---
    public List<String> getVehicles() throws SQLException {
        List<String> vehicles = new ArrayList<>();
        String sql = "SELECT name, color, plate FROM Car";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String car = rs.getString("name") + " - " + rs.getString("color") +
                        " - " + rs.getString("plate");
                vehicles.add(car);
            }
        }
        return vehicles;
    }

    // --- لیست کارمند ها ---
    public List<String> getEmployees() throws SQLException {
        List<String> employees = new ArrayList<>();
        String sql = "SELECT name, phone, telegram_id FROM Employee";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                employees.add(rs.getString("name") + " - " + rs.getString("phone") +
                        " - " + rs.getString("telegram_id"));
            }
        }
        return employees;
    }

    // --- افزودن ماشین ---
    public void addVehicle(String name, String plate, String color) throws SQLException {
        String sql = "INSERT INTO Car(name, plate, color) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, plate);
            stmt.setString(3, color);
            stmt.executeUpdate();
        }
    }

    // --- افزودن کارمند ---
    public void addEmployee(String name, String phone, String telegramId) throws SQLException {
        String sql = "INSERT INTO Employee(name, phone, telegram_id) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, phone);
            stmt.setString(3, telegramId);
            stmt.executeUpdate();
        }
    }

    // --- حذف ماشین ---
    public void deleteCar(String plate) throws SQLException {
        String sql = "DELETE FROM Car WHERE plate = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            stmt.executeUpdate();
        }
    }

    // --- حذف کارمند ---
    public void deleteEmployee(String phone) throws SQLException {
        String sql = "DELETE FROM Employee WHERE phone = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            stmt.executeUpdate();
        }
    }
}