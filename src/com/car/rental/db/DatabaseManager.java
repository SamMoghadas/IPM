package com.car.rental.db;

import com.car.rental.config.SpringContext;
import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data access layer (JDBC). Prefer domain services ({@code EmployeeService},
 * {@code CarService}, {@code RentalService}) from UI code.
 */
@Repository
public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    private static final int DEVICE_USER_ID_START = 1001;
    private static final String FALLBACK_URL = "jdbc:sqlite:CarRental.db";

    private final DataSource dataSource;

    @Autowired
    public DatabaseManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Legacy no-arg constructor used by existing Swing frames. */
    public DatabaseManager() {
        DataSource ds = null;
        if (SpringContext.isActive()) {
            try {
                ds = SpringContext.getBean(DataSource.class);
            } catch (Exception ignored) {
            }
        }
        if (ds != null) {
            this.dataSource = ds;
        } else {
            this.dataSource = new SimpleDataSource(FALLBACK_URL);
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
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

    public String getNextDeviceUserId() throws SQLException {
        String sql = "SELECT device_user_id FROM EmployeeTable";
        int max = DEVICE_USER_ID_START - 1;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String raw = rs.getString("device_user_id");
                if (raw == null) continue;
                try {
                    int v = Integer.parseInt(raw.trim());
                    if (v > max) {
                        max = v;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int next = Math.max(max + 1, DEVICE_USER_ID_START);
        if (next > 65535) {
            throw new SQLException("Device user id range exhausted (max 65535)");
        }
        return String.valueOf(next);
    }

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

    public void deleteEmployeeByDeviceUserId(String deviceUserId) throws SQLException {
        String sql = "UPDATE EmployeeTable SET is_active = 0, " +
                "updated_at = datetime('now','localtime') WHERE device_user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deviceUserId);
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

    /**
     * Resolves the active (non-deleted) car by plate.
     * Soft-deleted rows with the same plate must not be used for rental updates.
     */
    public int getCarIdByPlate(String plate) throws SQLException {
        String sql = "SELECT id FROM CarTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("ماشین فعالی با این پلاک یافت نشد: " + plate);
                }
            }
        }
    }

    public boolean isPlateTaken(String plate, String excludePlate) throws SQLException {
        if (plate == null || plate.isBlank()) {
            return false;
        }
        if (excludePlate != null && !excludePlate.isBlank() && excludePlate.equals(plate)) {
            return false;
        }
        String sql = "SELECT 1 FROM CarTable WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, plate);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return excludePlate == null || !excludePlate.equals(plate);
            }
        }
    }

    public List<Car> listAvailableCars() throws SQLException {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT name, color, plate FROM CarTable WHERE is_deleted = 0 AND is_rented = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cars.add(new Car(
                        rs.getString("name"),
                        rs.getString("plate"),
                        rs.getString("color"),
                        "آزاد"
                ));
            }
        }
        return cars;
    }

    public List<Car> listAllCars() throws SQLException {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT name, color, plate, is_rented FROM CarTable WHERE is_deleted = 0 ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean rented = rs.getInt("is_rented") == 1;
                cars.add(new Car(
                        rs.getString("name"),
                        rs.getString("plate"),
                        rs.getString("color"),
                        rented ? "در ماموریت" : "آزاد"
                ));
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
        String sql = "UPDATE CarTable SET name = ?, color = ?, plate = ? " +
                "WHERE plate = ? AND is_deleted = 0";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, car.getModel());
            stmt.setString(2, car.getColor());
            stmt.setString(3, car.getPlate());
            stmt.setString(4, oldPlate);
            int n = stmt.executeUpdate();
            if (n == 0) {
                throw new SQLException("ماشین فعالی برای به‌روزرسانی یافت نشد");
            }
        }
    }

    public void deleteCar(String plate) throws SQLException {
        // Refuse soft-delete while on mission (DB-level guard; UI also checks)
        String checkSql = "SELECT is_rented FROM CarTable WHERE plate = ? AND is_deleted = 0";
        String sql = "UPDATE CarTable SET is_deleted = 1 WHERE plate = ? AND is_deleted = 0 AND is_rented = 0";
        try (Connection conn = getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, plate);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("ماشین فعالی یافت نشد");
                    }
                    if (rs.getInt("is_rented") == 1) {
                        throw new SQLException("ماشین در مأموریت است و قابل حذف نیست");
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, plate);
                int n = stmt.executeUpdate();
                if (n == 0) {
                    throw new SQLException("حذف ماشین انجام نشد");
                }
            }
        }
    }

    public void insertRental(String deviceUserId,
                             String carPlate,
                             String pickupTime,
                             String destination) throws SQLException {

        String empSql = "SELECT id, is_renting FROM EmployeeTable " +
                "WHERE device_user_id = ? AND is_active = 1";
        String carSql = "SELECT id, is_rented FROM CarTable " +
                "WHERE plate = ? AND is_deleted = 0";
        String insertSql = "INSERT INTO RentalTable(employee_id, car_id, pickup_date, destination) " +
                "VALUES (?, ?, ?, ?)";
        String updateCarSql = "UPDATE CarTable SET is_rented = 1 WHERE id = ? AND is_rented = 0 AND is_deleted = 0";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 1, " +
                "updated_at = datetime('now','localtime') WHERE id = ? AND is_renting = 0";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int empId;
                try (PreparedStatement empStmt = conn.prepareStatement(empSql)) {
                    empStmt.setString(1, deviceUserId);
                    try (ResultSet rs = empStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("کارمند با شناسه دستگاه یافت نشد: " + deviceUserId);
                        }
                        if (rs.getInt("is_renting") == 1) {
                            throw new SQLException("این کارمند در حال حاضر در مأموریت است");
                        }
                        empId = rs.getInt("id");
                    }
                }

                int carId;
                try (PreparedStatement carStmt = conn.prepareStatement(carSql)) {
                    carStmt.setString(1, carPlate);
                    try (ResultSet rs = carStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("ماشین فعالی با این پلاک یافت نشد: " + carPlate);
                        }
                        if (rs.getInt("is_rented") == 1) {
                            throw new SQLException("این ماشین هم‌اکنون در مأموریت است");
                        }
                        carId = rs.getInt("id");
                    }
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                     PreparedStatement updateCarStmt = conn.prepareStatement(updateCarSql);
                     PreparedStatement updateEmpStmt = conn.prepareStatement(updateEmpSql)) {

                    insertStmt.setInt(1, empId);
                    insertStmt.setInt(2, carId);
                    insertStmt.setString(3, pickupTime);
                    insertStmt.setString(4, destination);
                    insertStmt.executeUpdate();

                    updateCarStmt.setInt(1, carId);
                    int carUpdated = updateCarStmt.executeUpdate();
                    if (carUpdated != 1) {
                        throw new SQLException("به‌روزرسانی وضعیت ماشین انجام نشد");
                    }

                    updateEmpStmt.setInt(1, empId);
                    int empUpdated = updateEmpStmt.executeUpdate();
                    if (empUpdated != 1) {
                        throw new SQLException("به‌روزرسانی وضعیت کارمند انجام نشد");
                    }
                }

                conn.commit();
                logger.info("Rental inserted: empId=" + empId + " carId=" + carId + " plate=" + carPlate);
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public boolean returnCarByDeviceUserId(String deviceUserId, String returnDate) throws SQLException {
        String empSql = "SELECT id FROM EmployeeTable WHERE device_user_id = ? AND is_active = 1";
        String selectSql = "SELECT id, car_id FROM RentalTable " +
                "WHERE employee_id = ? AND return_date IS NULL AND is_active = 1";
        String updateRentalSql = "UPDATE RentalTable SET return_date = ?, is_active = 0 WHERE id = ?";
        String updateCarSql = "UPDATE CarTable SET is_rented = 0 WHERE id = ?";
        String updateEmpSql = "UPDATE EmployeeTable SET is_renting = 0, " +
                "updated_at = datetime('now','localtime') WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                int empId;
                try (PreparedStatement empStmt = conn.prepareStatement(empSql)) {
                    empStmt.setString(1, deviceUserId);
                    try (ResultSet rs = empStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("کارمند با شناسه دستگاه یافت نشد: " + deviceUserId);
                        }
                        empId = rs.getInt("id");
                    }
                }

                int rentalId;
                int carId;
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setInt(1, empId);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        rentalId = rs.getInt("id");
                        carId = rs.getInt("car_id");
                    }
                }

                try (PreparedStatement updateRentalStmt = conn.prepareStatement(updateRentalSql);
                     PreparedStatement updateCarStmt = conn.prepareStatement(updateCarSql);
                     PreparedStatement updateEmpStmt = conn.prepareStatement(updateEmpSql)) {

                    updateRentalStmt.setString(1, returnDate);
                    updateRentalStmt.setInt(2, rentalId);
                    updateRentalStmt.executeUpdate();

                    updateCarStmt.setInt(1, carId);
                    updateCarStmt.executeUpdate();

                    updateEmpStmt.setInt(1, empId);
                    updateEmpStmt.executeUpdate();

                    conn.commit();
                    return true;
                }
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<RentalRecord> getRentalReport() throws SQLException {
        List<RentalRecord> records = new ArrayList<>();
        String sql = "SELECT e.device_user_id, e.name AS employee_name, " +
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
                        rs.getString("device_user_id"),
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

    public RentalRecord getActiveRentalByDeviceUserId(String deviceUserId) throws SQLException {
        String query = "SELECT e.device_user_id, e.name AS employee_name, " +
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
                            rs.getString("device_user_id"),
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

    private static final class SimpleDataSource implements DataSource {
        private final String url;

        SimpleDataSource(String url) {
            this.url = url;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override public <T> T unwrap(Class<T> iface) { throw new UnsupportedOperationException(); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }
    }
}
