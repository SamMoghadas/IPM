

import java.sql.*;

public class DatabaseManager {
    private final String url = "jdbc:mysql://localhost:3306/car_rental?useSSL=false";
    private final String user = "root";
    private final String password = "your_password";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void addVehicle(String model, String licensePlate) throws SQLException {}
    public void assignVehicle(int vehicleId, int employeeId, String pickupTime, String destination, String code) throws SQLException {}
    public void returnVehicle(int assignmentId, String returnTime) throws SQLException {}
    public String[] getStatus() throws SQLException {
        return new String[0];
    }
}