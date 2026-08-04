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

    public void initDatabase() {
        String employeeTable =
                "CREATE TABLE IF NOT EXISTS EmployeeTable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "device_user_id TEXT NOT NULL UNIQUE, " +
                "name TEXT NOT NULL, " +
                "phone TEXT, " +
                "is_active INTEGER DEFAULT 1, " +
                "is_renting INTEGER DEFAULT 0, " +
                "created_at TEXT DEFAULT (datetime('now','localtime')), " +
                "updated_at TEXT DEFAULT (datetime('now','localtime'))" +
                ")";

        String carTable =
                "CREATE TABLE IF NOT EXISTS CarTable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "plate TEXT NOT NULL, " +
                "color TEXT NOT NULL, " +
                "is_deleted INTEGER DEFAULT 0, " +
                "is_rented INTEGER DEFAULT 0" +
                ")";

        String rentalTable =
                "CREATE TABLE IF NOT EXISTS RentalTable (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "employee_id INTEGER NOT NULL, " +
                "car_id INTEGER NOT NULL, " +
                "pickup_date TEXT, " +
                "return_date TEXT, " +
                "destination TEXT NOT NULL, " +
                "is_active INTEGER DEFAULT 1, " +
                "FOREIGN KEY(employee_id) REFERENCES EmployeeTable(id) ON UPDATE CASCADE, " +
                "FOREIGN KEY(car_id) REFERENCES CarTable(id) ON UPDATE CASCADE" +
                ")";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(carTable);
            stmt.execute(rentalTable);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database init failed!", e);
        }
    }

    // ==================== Employee ====================

    public void addEmployee(String deviceUserId, String name, String phone) throws SQLException {
        String sql = "INSERT INTO EmployeeTable(device_user_id, name, phone) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            stmt.setString(2, name);
            stmt.setString(3, phone);
            stmt.executeUpdate();
        }
    }

    public Employee findByDeviceUserId(String deviceUserId) throws SQLException {
        String sql = "SELECT id, device_user_id, name, phone, is_active, is_renting " +
                "FROM EmployeeTable WHERE device_user_id = ? AND is_active = 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapEmployee(rs);
                }
                return null;
            }
        }
    }

    public Employee findEmployeeById(int id) throws SQLException {
        String sql = "SELECT id, device_user_id, name, phone, is_active, is_renting " +
                "FROM EmployeeTable WHERE id = ? AND is_active = 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapEmployee(rs);
                }
                return null;
            }
        }
    }

    public List<Employee> getAvailableEmployees() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT id, device_user_id, name, phone, is_active, is_renting " +
                "FROM EmployeeTable WHERE is_active = 1 AND is_renting = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapEmployee(rs));
            }
        }
        return list;
    }

    public List<Employee> getAllEmployees() throws SQLException {
        List<Employee> list = new ArrayList<>();
        String sql = "SELECT id, device_user_id, name, phone, is_active, is_renting " +
                "FROM EmployeeTable WHERE is_active = 1 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapEmployee(rs));
            }
        }
        return list;
    }

    public void updateEmployee(Employee emp) throws SQLException {
        String sql = "UPDATE EmployeeTable SET name = ?, phone = ?, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, emp.getName());
            stmt.setString(2, emp.getPhone());
            stmt.setInt(3, emp.getId());
            stmt.executeUpdate();
        }
    }

    public void deleteEmployee(int id, DataChangeCallback callback) throws SQLException {
        String sql = "UPDATE EmployeeTable SET is_active = 0, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            if (callback != null) {
                callback.onDataChange();
            }
        }
    }

    public void deleteEmployeeByDeviceUserId(String deviceUserId, DataChangeCallback callback) throws SQLException {
        String sql = "UPDATE EmployeeTable SET is_active = 0, " +
                "updated_at = datetime('now','localtime') WHERE device_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            stmt.executeUpdate();
            if (callback != null) {
                callback.onDataChange();
            }
        }
    }

    public void setRentingStatus(int employeeId, boolean renting) throws SQLException {
        String sql = "UPDATE EmployeeTable SET is_renting = ?, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, renting ? 1 : 0);
            stmt.setInt(2, employeeId);
            stmt.executeUpdate();
        }
    }

    public void setRentingStatusByDeviceUserId(String deviceUserId, boolean renting) throws SQLException {
        String sql = "UPDATE EmployeeTable SET is_renting = ?, " +
                "updated_at = datetime('now','localtime') WHERE device_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, renting ? 1 : 0);
            stmt.setString(2, deviceUserId);
            stmt.executeUpdate();
        }
    }

    public boolean isDeviceUserIdExists(String deviceUserId) throws SQLException {
        String sql = "SELECT 1 FROM EmployeeTable WHERE device_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Employee mapEmployee(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("id"),
                rs.getString("device_user_id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getInt("is_active") == 1,
                rs.getInt("is_renting") == 1
        );
    }

    // ==================== Car ====================

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

    public void addCar(String name, String plate, String color) throws SQLException {
        String sql = "INSERT INTO CarTable(name, plate, color) VALUES(?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, plate);
            stmt.setString(3, color);
            stmt.executeUpdate();
        }
    }

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

    public void deleteCar(String plate, DataChangeCallback callback) throws SQLException {
        String sql = "UPDATE CarTable SET is_deleted = 1 WHERE plate = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            stmt.executeUpdate();
            if (callback != null) {
                callback.onDataChange();
            }
        }
    }

    // ==================== Rental (no confirm_code) ====================

    /**
     * Register car pickup using fingerprint device user id and device time.
     */
    public void insertRental(String deviceUserId,
                             String carPlate,
                             String pickupTime,
                             String destination) throws SQLException {

        Employee emp = findByDeviceUserId(deviceUserId);
        if (emp == null) {
            throw new SQLException("کارمند با شناسه دستگاه یافت نشد: " + deviceUserId);
        }
        if (emp.isRenting()) {
            throw new SQLException("این کارمند در حال حاضر در مأموریت است");
        }

        int carId = getCarIdByPlate(carPlate);

        String insertSql = "INSERT INTO RentalTable(employee_id, car_id, pickup_date, destination) " +
                "VALUES (?, ?, ?, ?)";
        String updateCarSql = "UPDATE CarTable SET is_rented = 1 WHERE id = ?";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 1, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                 PreparedStatement updateCarStmt = conn.prepareStatement(updateCarSql);
                 PreparedStatement updateEmpStmt = conn.prepareStatement(updateEmpSql)) {

                insertStmt.setInt(1, emp.getId());
                insertStmt.setInt(2, carId);
                insertStmt.setString(3, pickupTime);
                insertStmt.setString(4, destination);
                insertStmt.executeUpdate();

                updateCarStmt.setInt(1, carId);
                updateCarStmt.executeUpdate();

                updateEmpStmt.setInt(1, emp.getId());
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

    /**
     * Return car by device_user_id (fingerprint).
     * Closes the active rental of this employee.
     */
    public boolean returnCarByDeviceUserId(String deviceUserId, String returnDate) throws SQLException {
        Employee emp = findByDeviceUserId(deviceUserId);
        if (emp == null) {
            throw new SQLException("کارمند با شناسه دستگاه یافت نشد: " + deviceUserId);
        }

        String selectSql = "SELECT id, car_id FROM RentalTable " +
                "WHERE employee_id = ? AND return_date IS NULL AND is_active = 1";
        String updateRentalSql = "UPDATE RentalTable SET return_date = ?, is_active = 0 WHERE id = ?";
        String updateCarSql = "UPDATE CarTable SET is_rented = 0 WHERE id = ?";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 0, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, emp.getId());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    int rentalId = rs.getInt("id");
                    int carId = rs.getInt("car_id");

                    try (PreparedStatement updateRentalStmt = conn.prepareStatement(updateRentalSql);
                         PreparedStatement updateCarStmt = conn.prepareStatement(updateCarSql);
                         PreparedStatement updateEmpStmt = conn.prepareStatement(updateEmpSql)) {

                        updateRentalStmt.setString(1, returnDate);
                        updateRentalStmt.setInt(2, rentalId);
                        updateRentalStmt.executeUpdate();

                        updateCarStmt.setInt(1, carId);
                        updateCarStmt.executeUpdate();

                        updateEmpStmt.setInt(1, emp.getId());
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

    public List<RentalRecord> getRentalReport() throws SQLException {
        List<RentalRecord> records = new ArrayList<>();
        String sql = "SELECT e.id AS employee_id, e.device_user_id, e.name AS employee_name, " +
                "c.name AS car_name, c.color AS car_color, c.plate, " +
                "r.pickup_date, r.return_date, r.destination " +
                "FROM RentalTable r " +
                "JOIN EmployeeTable e ON r.employee_id = e.id " +
                "LEFT JOIN CarTable c ON r.car_id = c.id " +
                "ORDER BY r.pickup_date";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(new RentalRecord(
                        rs.getInt("employee_id"),
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

    /** Get active rental info for an employee (by device_user_id). */
    public RentalRecord getActiveRentalByDeviceUserId(String deviceUserId) throws SQLException {
        String query = "SELECT e.id AS employee_id, e.name AS employee_name, " +
                "c.name AS car_name, c.color AS car_color, c.plate, " +
                "r.pickup_date, r.return_date, r.destination " +
                "FROM RentalTable r " +
                "JOIN EmployeeTable e ON r.employee_id = e.id " +
                "JOIN CarTable c ON r.car_id = c.id " +
                "WHERE e.device_user_id = ? AND r.return_date IS NULL AND r.is_active = 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, deviceUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new RentalRecord(
                            rs.getInt("employee_id"),
                            rs.getString("employee_name"),
                            rs.getString("car_name"),
                            rs.getString("car_color"),
                            rs.getString("plate"),
                            rs.getString("pickup_date"),
                            rs.getString("return_date"),
                            rs.getString("destination")
                    );
                }
                return null;
            }
        }
    }
}
