package com.car.rental.service;

import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import com.car.rental.util.DataChangeCallback;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business logic for employees: DB + fingerprint device coordination.
 */
@Service
public class EmployeeService {

    private static final Logger logger = Logger.getLogger(EmployeeService.class.getName());

    private final DatabaseManager db;
    private final FingerprintService fingerprintService;

    public EmployeeService(DatabaseManager db, FingerprintService fingerprintService) {
        this.db = db;
        this.fingerprintService = fingerprintService;
    }

    public String getNextDeviceUserId() throws SQLException {
        return db.getNextDeviceUserId();
    }

    public boolean isDeviceUserIdExists(String deviceUserId) throws SQLException {
        return db.isDeviceUserIdExists(deviceUserId);
    }

    public Employee findByDeviceUserId(String deviceUserId) throws SQLException {
        return db.findByDeviceUserId(deviceUserId);
    }

    public Employee findById(int id) throws SQLException {
        return db.findEmployeeById(id);
    }

    public List<Employee> getAllEmployees() throws SQLException {
        return db.getAllEmployees();
    }

    public List<Employee> getAvailableEmployees() throws SQLException {
        return db.getAvailableEmployees();
    }

    /**
     * Enroll on device then save to DB. On enroll failure the device user is rolled back
     * by {@link ZkFingerprintService#registerUserWithFingerprint}.
     */
    public void registerWithFingerprint(String deviceUserId, String name, String phone, int fingerIndex)
            throws SQLException, FingerprintException {
        validateEnglishName(name);
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("deviceUserId is empty");
        }
        if (db.isDeviceUserIdExists(deviceUserId)) {
            throw new SQLException("شناسه دستگاه قبلاً ثبت شده است: " + deviceUserId);
        }

        ensureConnected();
        try {
            if (fingerprintService instanceof ZkFingerprintService zk) {
                zk.registerUserWithFingerprint(deviceUserId, name, fingerIndex);
            } else {
                fingerprintService.createUser(deviceUserId, name);
                fingerprintService.startEnroll(deviceUserId, fingerIndex, r -> {}, e -> {
                    throw new RuntimeException(e);
                });
            }
            db.addEmployee(deviceUserId, name, phone);
        } finally {
            disconnectQuietly();
        }
    }

    /**
     * Update name on device first; only then update local DB.
     */
    public void updateEmployee(Employee emp) throws SQLException, FingerprintException {
        if (emp == null) {
            throw new IllegalArgumentException("employee is null");
        }
        validateEnglishName(emp.getName());

        ensureConnected();
        try {
            if (fingerprintService instanceof ZkFingerprintService zk) {
                zk.updateUserName(emp.getDeviceUserId(), emp.getName());
            } else {
                fingerprintService.createUser(emp.getDeviceUserId(), emp.getName());
            }
            db.updateEmployee(emp);
        } finally {
            disconnectQuietly();
        }
    }

    /** Add another finger template without wiping existing ones. */
    public void addFingerprint(String deviceUserId, int fingerIndex)
            throws FingerprintException {
        if (deviceUserId == null || deviceUserId.isBlank()) {
            throw new IllegalArgumentException("deviceUserId is empty");
        }
        ensureConnected();
        try {
            if (fingerprintService instanceof ZkFingerprintService zk) {
                zk.enrollFingerOnly(deviceUserId, fingerIndex);
            } else {
                throw new FingerprintException("Fingerprint service does not support enrollFingerOnly");
            }
        } finally {
            disconnectQuietly();
        }
    }

    /** Soft-delete in DB and best-effort remove from device. */
    public void deleteEmployee(String deviceUserId, DataChangeCallback callback) throws SQLException {
        try {
            ensureConnected();
            fingerprintService.deleteUser(deviceUserId);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Device deleteUser failed for " + deviceUserId, e);
        } finally {
            disconnectQuietly();
        }
        db.deleteEmployeeByDeviceUserId(deviceUserId, callback);
    }

    private void ensureConnected() throws FingerprintException {
        if (!fingerprintService.isConnected()) {
            fingerprintService.connect();
        }
    }

    private void disconnectQuietly() {
        try {
            if (fingerprintService.isConnected()) {
                fingerprintService.disconnect();
            }
        } catch (Exception ignored) {
        }
    }

    private static void validateEnglishName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("نام الزامی است");
        }
        if (!name.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException(
                    "نام باید انگلیسی باشد (حداقل یک حرف لاتین). دستگاه نام فارسی را درست ذخیره نمی‌کند.");
        }
    }
}
