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
        String employeeTable = "CREATE TABLE IF NOT EXISTS EmployeeTable( " +
                "personnel_id INTEGER, " +
                "name TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "telegram_id TEXT, " +
                "is_deleted INTEGER DEFAULT 0, " +
                "PRIMARY KEY (personnel_id)" +
                ")";

        String carTable = "CREATE TABLE IF NOT EXISTS CarTable( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "plate TEXT NOT NULL, " +
                "color TEXT NOT NULL, " +
                "is_deleted INTEGER DEFAULT 0" +
                ")";

        String rentalTable = "CREATE TABLE IF NOT EXISTS RentalTable( " +
                "confirm_code INTEGER, " +
                "employee_id INTEGER NOT NULL, " +
                "car_id INTEGER NOT NULL, " +
                "pickup_date TEXT, " +
                "return_date TEXT, " +
                "destination TEXT NOT NULL, " +
                "PRIMARY KEY (confirm_code), " +
                "FOREIGN KEY(employee_id) REFERENCES EmployeeTable(personnel_id) ON UPDATE CASCADE, " +
                "FOREIGN KEY(car_id) REFERENCES CarTable(id) ON UPDATE CASCADE" +
                ")";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(carTable);
            stmt.execute(rentalTable);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed!", e.getMessage());
        }
    }

    public int getPersonnelIdByPhone(String phone) throws SQLException {
        String sql = "SELECT personnel_id FROM EmployeeTable WHERE phone = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("personnel_id");
                } else {
                    throw new SQLException("Employee not found for phone: " + phone);
                }
            }
        }
    }

    public int getCarIdByPlate(String plate) throws SQLException {
        String sql = "SELECT id FROM CarTable WHERE plate = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Car not found for plate: " + plate);
                }
            }
        }
    }

    // --- ثبت تحویل ماشین ---
    public void insertRental(String empPhone,
                             String carPlate,
                             String pickupTime,
                             String destination,
                             int confirmCode) throws SQLException {
        String sql = "INSERT INTO RentalTable(employee_id, car_id, pickup_date, destination, confirm_code) " +
                "VALUES (?, ?, ?, ?, ?)";

        int employeeId = getPersonnelIdByPhone(empPhone);
        int carId = getCarIdByPlate(carPlate);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, employeeId);
            stmt.setInt(2, carId);
            stmt.setString(3, pickupTime);
            stmt.setString(4, destination);
            stmt.setInt(5, confirmCode);

            stmt.executeUpdate();
        }
    }

    // --- ثبت برگشت ماشین ---
    public boolean returnCar(int confirmCode, String returnDate) throws SQLException {
        String checkSql = "SELECT confirm_code, return_date FROM RentalTable WHERE confirm_code = ? AND return_date IS NULL";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            stmt.setInt(1, confirmCode);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String updateSql = "UPDATE RentalTable SET return_date = ? WHERE confirm_code = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, returnDate);
                    updateStmt.setInt(2, confirmCode);
                    updateStmt.executeUpdate();
                    return true; // یعنی ماشین با موفقیت برگشت
                }
            } else {
                return false; // یعنی کد تحویل معتبر نیست یا قبلاً برگشته
            }
        }
    }

    // --- لیست ماشین ها ---
    public List<String> getCars() throws SQLException {
        List<String> cars = new ArrayList<>();
        String sql = "SELECT name, color, plate FROM CarTable WHERE is_deleted = 0";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String car = rs.getString("name") + " - " + rs.getString("color") +
                        " - " + rs.getString("plate");
                cars.add(car);
            }
        }
        return cars;
    }

    // --- لیست کارمند ها ---
    public List<String> getEmployees() throws SQLException {
        List<String> employees = new ArrayList<>();
        String sql = "SELECT personnel_id, name, phone, telegram_id FROM EmployeeTable WHERE is_deleted = 0";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                employees.add(rs.getInt("personnel_id") + " - " +
                        rs.getString("name") + " - " +
                        rs.getString("phone") + " - " +
                        rs.getString("telegram_id"));
            }
        }
        return employees;
    }

    // --- افزودن ماشین ---
    public void addCar(String name,
                       String plate,
                       String color,
                       DataChangeCallback callback) throws SQLException {
        String sql = "INSERT INTO CarTable(name, plate, color) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, plate);
            stmt.setString(3, color);
            stmt.executeUpdate();
            callback.onDataChange();
        }
    }

    // --- افزودن کارمند ---
    public void addEmployee(int personnelId,
                            String name,
                            String phone,
                            String telegramId,
                            DataChangeCallback callback) throws SQLException {
        String sql = "INSERT INTO EmployeeTable(personnel_id, name, phone, telegram_id) VALUES(?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, personnelId);
            stmt.setString(2, name);
            stmt.setString(3, phone);
            stmt.setString(4, telegramId);
            stmt.executeUpdate();
            callback.onDataChange();
        }
    }

    // --- حذف ماشین ---
    public void deleteCar(String plate, DataChangeCallback callback) throws SQLException {
        String sql = "UPDATE CarTable SET is_deleted = 1 WHERE plate = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            stmt.executeUpdate();
            callback.onDataChange();
        }
    }

    // --- حذف کارمند ---
    public void deleteEmployee(int personnelId, DataChangeCallback callback) throws SQLException {
        String sql = "UPDATE EmployeeTable SET is_deleted = 1 WHERE personnel_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, personnelId);
            stmt.executeUpdate();
            callback.onDataChange();
        }
    }

    public List<ReportFrame.RentalRecord> getRentalReport() throws SQLException {
        List<ReportFrame.RentalRecord> records = new ArrayList<>();
        String sql = "SELECT e.personnel_id, e.name AS employee_name, " +
                "c.name AS car_name, c.color AS car_color, c.plate, " +
                "r.pickup_date, r.return_date, r.destination " +
                "FROM RentalTable r " +
                "JOIN EmployeeTable e ON r.employee_id = e.personnel_id " +
                "LEFT JOIN CarTable c ON r.car_id = c.id " +
                "ORDER BY r.pickup_date";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(new ReportFrame.RentalRecord(
                        rs.getInt("personnel_id"),
                        rs.getString("employee_name"),
                        rs.getString("car_name"),
                        rs.getString("car_color"),
                        rs.getString("plate"),
                        rs.getString("pickup_date"),
                        rs.getString("return_date"),
                        rs.getString("destination")
                ));
            }
        }
        return records;
    }

    public boolean isCCDuplicated(int confirmCode) throws SQLException  {
        String sql = "SELECT confirm_code FROM RentalTable where confirm_code = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, confirmCode);
            ResultSet rs = stmt.executeQuery();

            return rs.next();
        }
    }
}