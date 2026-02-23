package com.car.rental.db;

import com.car.rental.util.DataChangeCallback;
import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private static Connection connection;

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db");
        }
        return connection;
    }

    // --- ایجاد جدول‌ها ---
    public void initDatabase() {
        String employeeTable = "CREATE TABLE IF NOT EXISTS EmployeeTable( " +
                "personnel_id INTEGER, " +
                "name TEXT NOT NULL, " +
                "phone TEXT NOT NULL, " +
                "telegram_id TEXT, " +
                "is_deleted INTEGER DEFAULT 0, " +
                "is_renting INTEGER DEFAULT 0, " +
                "PRIMARY KEY (personnel_id)" +
                ")";

        String carTable = "CREATE TABLE IF NOT EXISTS CarTable( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "plate TEXT NOT NULL, " +
                "color TEXT NOT NULL, " +
                "is_deleted INTEGER DEFAULT 0, " +
                "is_rented INTEGER DEFAULT 0" +
                ")";

        String rentalTable = "CREATE TABLE IF NOT EXISTS RentalTable( " +
                "confirm_code INTEGER, " +
                "employee_id INTEGER NOT NULL, " +
                "car_id INTEGER NOT NULL, " +
                "pickup_date TEXT, " +
                "return_date TEXT, " +
                "destination TEXT NOT NULL, " +
                "is_active INTEGER DEFAULT 1, " +
                "PRIMARY KEY (confirm_code), " +
                "FOREIGN KEY(employee_id) REFERENCES EmployeeTable(personnel_id) ON UPDATE CASCADE, " +
                "FOREIGN KEY(car_id) REFERENCES CarTable(id) ON UPDATE CASCADE" +
                ")";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(carTable);
            stmt.execute(rentalTable);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed!", e);
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
        String insertSql = "INSERT INTO RentalTable(employee_id, car_id, pickup_date, destination, confirm_code) " +
                "VALUES (?, ?, ?, ?, ?)";
        String updateCarSql = "UPDATE CarTable SET is_rented = 1 WHERE id = ?";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 1 WHERE personnel_id = ?";

        int employeeId = getPersonnelIdByPhone(empPhone);
        int carId = getCarIdByPlate(carPlate);

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // --- ثبت اجاره و تغییر وضعیت ماشین ---
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                 PreparedStatement updateStmt = conn.prepareStatement(updateCarSql);
                 PreparedStatement updateEmpStmt = conn.prepareStatement(updateEmpSql)) {

                insertStmt.setInt(1, employeeId);
                insertStmt.setInt(2, carId);
                insertStmt.setString(3, pickupTime);
                insertStmt.setString(4, destination);
                insertStmt.setInt(5, confirmCode);
                insertStmt.executeUpdate();

                updateStmt.setInt(1, carId);
                updateStmt.executeUpdate();

                updateEmpStmt.setInt(1, employeeId);
                updateEmpStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // --- ثبت برگشت ماشین ---
    public boolean returnCar(int confirmCode, String returnDate) throws SQLException {
        String selectSql = "SELECT car_id, employee_id FROM RentalTable WHERE confirm_code = ? AND return_date IS NULL";
        String updateRentalSql = "UPDATE RentalTable SET return_date = ?, is_active = 0 WHERE confirm_code = ?";
        String updateCarSql = "UPDATE CarTable SET is_rented = 0 WHERE id = ?";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 0 WHERE personnel_id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, confirmCode);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false; // اجاره‌ای وجود نداره یا قبلاً برگشته
                    }

                    int carId = rs.getInt("car_id");
                    int empId = rs.getInt("employee_id");

                    try (PreparedStatement updateRentalStmt = conn.prepareStatement(updateRentalSql);
                         PreparedStatement updateCarStmt = conn.prepareStatement(updateCarSql);
                         PreparedStatement updateEmpStmt = conn.prepareStatement(updateEmpSql)) {

                        updateRentalStmt.setString(1, returnDate);
                        updateRentalStmt.setInt(2, confirmCode);
                        updateRentalStmt.executeUpdate();

                        updateCarStmt.setInt(1, carId);
                        updateCarStmt.executeUpdate();

                        updateEmpStmt.setInt(1, empId);
                        updateEmpStmt.executeUpdate();

                        conn.commit();
                        return true;
                    } catch (SQLException e) {
                        conn.rollback();
                        throw e;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }
            }
        }
    }

    // --- لیست ماشین ها های در دسترس ---
    public List<String> getAvailableCars() throws SQLException {
        List<String> cars = new ArrayList<>();
        String sql = "SELECT name, color, plate FROM CarTable WHERE is_deleted = 0 AND is_rented = 0";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String car = rs.getString("name") + " - " +
                        rs.getString("color") + " - " +
                        rs.getString("plate");
                cars.add(car);
            }
        }
        return cars;
    }

    // --- لیست همه ماشین ها ---
    public List<String> getAllCars() throws SQLException {
        List<String> cars = new ArrayList<>();
        String sql = "SELECT name, color, plate, is_rented FROM CarTable WHERE is_deleted = 0";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String car = rs.getString("name") + " - " +
                        rs.getString("color") + " - " +
                        rs.getString("plate") + " - " +
                        rs.getString("is_rented");
                cars.add(car);
            }
        }
        return cars;
    }

    // --- لیست کارمند ها ---
    public List<String> getAvailableEmployees() throws SQLException {
        List<String> employees = new ArrayList<>();
        String sql = "SELECT personnel_id, name, phone, telegram_id FROM EmployeeTable WHERE is_deleted = 0 AND is_renting = 0";

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

    public List<String> getAllEmployees() throws SQLException {
        List<String> employees = new ArrayList<>();
        String sql = "SELECT personnel_id, name, phone, telegram_id, is_renting FROM EmployeeTable WHERE is_deleted = 0";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                employees.add(rs.getInt("personnel_id") + " - " +
                        rs.getString("name") + " - " +
                        rs.getString("phone") + " - " +
                        rs.getString("telegram_id") + " - " +
                        rs.getString("is_renting"));
            }
        }
        return employees;
    }

    // --- افزودن ماشین ---
    public void addCar(String name,
                       String plate,
                       String color) throws SQLException {
        String sql = "INSERT INTO CarTable(name, plate, color) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, plate);
            stmt.setString(3, color);
            stmt.executeUpdate();
        }
    }

    // --- افزودن کارمند ---
    public void addEmployee(int personnelId,
                            String name,
                            String phone,
                            String telegramId) throws SQLException {
        String sql = "INSERT INTO EmployeeTable(personnel_id, name, phone, telegram_id) VALUES(?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, personnelId);
            stmt.setString(2, name);
            stmt.setString(3, phone);
            stmt.setString(4, telegramId);
            stmt.executeUpdate();
        }
    }

    // --- ویرایش ماشین ---
    public void updateCar(Car car, String oldPlate) throws SQLException {
        String sql = "UPDATE CarTable SET name = ?, color = ?, plate = ? WHERE plate = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, car.getModel());
            stmt.setString(2, car.getColor());
            stmt.setString(3, car.getPlate());

            stmt.setString(4, oldPlate);

            stmt.executeUpdate();
        }
    }

    // --- ویرایش کارمند ---
    public void updateEmployee(Employee emp) throws SQLException {
        String sql = "UPDATE EmployeeTable SET name = ?, phone = ?, telegram_id = ? " +
                "WHERE personnel_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, emp.getName());
            stmt.setString(2, emp.getPhone());
            stmt.setString(3, emp.getTelegramId());
            stmt.setInt(4, emp.getPersonnelId());

            stmt.executeUpdate();
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

    public List<RentalRecord> getRentalReport() throws SQLException {
        List<RentalRecord> records = new ArrayList<>();
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
                records.add(new RentalRecord(
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

    public boolean isCCDuplicated(int confirmCode) throws SQLException {
        String sql = "SELECT confirm_code FROM RentalTable where confirm_code = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, confirmCode);
            ResultSet rs = stmt.executeQuery();

            return rs.next();
        }
    }

    public RentalRecord getRentalRecord(int confirmCode) throws SQLException {
        String query = "SELECT r.confirm_code, e.name AS employeeName, c.name AS carName " +
                "FROM RentalTable r " +
                "JOIN EmployeeTable e ON r.employee_id = e.personnel_id " +
                "JOIN CarTable c ON r.car_id = c.id " +
                "WHERE r.confirm_code = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, confirmCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new RentalRecord(
                        0,
                        rs.getString("employeeName"),
                        rs.getString("carName"),
                        "", "", "", "", "" // باقی فیلدها لازم نیست
                );
            } else {
                return null;
            }
        }
    }
}