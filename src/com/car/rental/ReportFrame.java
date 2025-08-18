package com.car.rental;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportFrame extends JFrame {
    private static final Logger logger = Logger.getLogger(ReportFrame.class.getName());

    public ReportFrame() {
        setTitle("گزارش سفرهای ماشین");
        setSize(700, 400);
        setLayout(new BorderLayout(10, 10));
        setVisible(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(reportArea);
        add(scrollPane);

        // اتصال به دیتابیس و گرفتن گزارش
        String sql = "SELECT r.rental_code, e.name AS employee_name, c.name AS car_name, c.plate, " +
                "r.delivery_date, r.return_date, r.destination " +
                "FROM CarRental r JOIN Employee e ON r.employee_phone = e.phone " +
                "LEFT JOIN Car c ON r.car_plate = c.plate";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            StringBuilder reportText = new StringBuilder(); // ساختن رشته برای جمع‌آوری گزارش

            while (rs.next()) {
                String employeeName = rs.getString("employee_name");
                String carName = rs.getString("car_name");
                String delivery = rs.getString("delivery_date");
                String returnDate = rs.getString("return_date");
                String destination = rs.getString("destination");

                reportText.append(employeeName)
                        .append(" | ")
                        .append(carName)
                        .append(" | ")
                        .append(delivery)
                        .append(" | ")
                        .append(returnDate == null ? "منتظر بازگشت" : returnDate)
                        .append(" | ")
                        .append(destination)
                        .append("\n"); // هر رکورد رو توی خط جدا قرار میده
            }
            reportArea.setText(reportText.toString());

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed!", e.getMessage());
        }
    }
}
