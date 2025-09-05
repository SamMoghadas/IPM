package com.car.rental;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

public class AdminUI {
    private Connection conn;

    private JComboBox<String> vehicleCombo;
    private JComboBox<String> employeeCombo;
    private JTextField pickupTimeField, destinationField;
    private JTextField rentalCodeField, returnTimeField;
    private static final Logger logger = Logger.getLogger(AdminUI.class.getName());

    public AdminUI() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db");
            createTables();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database connection failed!", e.getMessage());

        }
        createMainUI();
    }

    private void createTables() throws SQLException {
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

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(employeeTable);
            stmt.execute(carTable);
            stmt.execute(rentalTable);
        }
    }

    private void createMainUI() {
        JFrame frame = new JFrame("سیستم مدیریت ماشین شرکت");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(240, 240, 240));

        // --- بخش تحویل ---
        JPanel pickupPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        pickupPanel.setBorder(BorderFactory.createTitledBorder("ثبت تحویل ماشین"));
        pickupPanel.setBackground(Color.WHITE);

        JLabel vehicleLabel = new JLabel("ماشین:");
        vehicleCombo = new JComboBox<>();
        ((JLabel)vehicleCombo.getRenderer()).setHorizontalAlignment(SwingConstants.RIGHT);
        try(Connection conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name, color, plate FROM Car");

            while (rs.next()) {
                String car = rs.getString("name") + " - " + rs.getString("color") +
                        " - " + rs.getString("plate");
                vehicleCombo.addItem(car);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا در خواندن ماشین‌ها: " + e.getMessage());
        }

        JLabel employeeLabel = new JLabel("کارمند:");
        employeeCombo = new JComboBox<>();
        ((JLabel)employeeCombo.getRenderer()).setHorizontalAlignment(SwingConstants.RIGHT);
        try(Connection conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM Employee");

            while (rs.next()) {
                String employee = rs.getString("name");
                employeeCombo.addItem(employee);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا در خواندن کارمند‌ها: " + e.getMessage());
        }

        JLabel pickupTimeLabel = new JLabel("زمان تحویل:");
        JPanel pickupTimePanel = new JPanel(new GridLayout(1, 2, 5, 0));
        pickupTimePanel.setBackground(Color.WHITE);
        pickupTimeField = new JTextField(20);
        JButton nowPickupButton = new JButton("الان");
        nowPickupButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            pickupTimeField.setText(now.format(formatter));
        });

        pickupTimePanel.add(pickupTimeField);
        pickupTimePanel.add(nowPickupButton);

        JLabel destinationLabel = new JLabel("مقصد:");
        destinationField = new JTextField(20);

        JButton pickupButton = new JButton("ثبت تحویل");
        pickupButton.addActionListener(e ->  {
            pickupCar();
            pickupTimeField.setText("");
            destinationField.setText("");
        });

        pickupPanel.add(vehicleLabel);
        pickupPanel.add(vehicleCombo);
        pickupPanel.add(employeeLabel);
        pickupPanel.add(employeeCombo);
        pickupPanel.add(pickupTimeLabel);
        pickupPanel.add(pickupTimePanel);
        pickupPanel.add(destinationLabel);
        pickupPanel.add(destinationField);
        pickupPanel.add(new JLabel());
        pickupPanel.add(pickupButton);

        // --- بخش بازگشت ---
        JPanel returnPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        returnPanel.setBorder(BorderFactory.createTitledBorder("ثبت بازگشت ماشین"));
        returnPanel.setBackground(Color.WHITE);

        JLabel rentalIdLabel = new JLabel("کد تحویل:");
        rentalCodeField = new JTextField(20);

        JLabel returnTimeLabel = new JLabel("زمان بازگشت:");
        JPanel returnTimePanel = new JPanel(new GridLayout(1, 2, 5, 0));
        returnTimePanel.setBackground(Color.WHITE);
        returnTimeField = new JTextField(20);
        JButton nowReturnButton = new JButton("الان");
        nowReturnButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            returnTimeField.setText(now.format(formatter));
        });

        returnTimePanel.add(returnTimeField);
        returnTimePanel.add(nowReturnButton);

        JButton returnButton = new JButton("ثبت بازگشت");
        returnButton.addActionListener(e -> {
            returnCar();
            rentalCodeField.setText("");
            returnTimeField.setText("");
        });

        returnPanel.add(rentalIdLabel);
        returnPanel.add(rentalCodeField);
        returnPanel.add(returnTimeLabel);
        returnPanel.add(returnTimePanel);
        returnPanel.add(new JLabel());
        returnPanel.add(returnButton);

        // --- دکمه‌ها ---
        JButton addItemsButton = new JButton("اضافه کردن ماشین / کارمند");
        addItemsButton.addActionListener(e -> new AddFrame());
        JButton reportButton = new JButton("نمایش گزارش");
        reportButton.addActionListener(e -> new ReportFrame());

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        mainPanel.add(pickupPanel);
        mainPanel.add(returnPanel);
        frame.add(mainPanel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonsPanel.add(addItemsButton);
        buttonsPanel.add(reportButton);
        frame.add(buttonsPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private String generateRentalCode() {
        Random rand = new Random();
        int num = 10000 + rand.nextInt(90000);
        return String.valueOf(num);
    }

    private void pickupCar() {
        String rentalCode = generateRentalCode();
        String sql = "INSERT INTO CarRental(rental_code, employee_phone, car_plate, delivery_date, destination) " +
                "VALUES (?, ?, ?, ?, ?)";
        try {
            // 1. گرفتن شماره تلفن کارمند از کومبو
            String selectedEmpPhone = null;
            String empSql = "SELECT phone FROM Employee WHERE name = ?";
            try (PreparedStatement empStmt = conn.prepareStatement(empSql)) {
                empStmt.setString(1, (String) employeeCombo.getSelectedItem());
                ResultSet rs = empStmt.executeQuery();
                if (rs.next()) {
                    selectedEmpPhone = rs.getString("phone");
                }
            }

            // 2. گرفتن پلاک ماشین از کومبو
            String selectedCar = (String) vehicleCombo.getSelectedItem();
            String selectedCarPlate = Optional.ofNullable(selectedCar)
                    .map(s -> s.substring(s.lastIndexOf('-') + 2)).orElse("Unknown");

            String pickupTime = pickupTimeField.getText();
            String destination = destinationField.getText();
            if (pickupTime.isEmpty() || destination.isEmpty()) {
                JOptionPane.showMessageDialog(null, "تمام فیلدها باید پر شوند!");
                return;
            }

            // 3. وارد کردن داده‌ها به جدول CarRental
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, rentalCode);
                stmt.setString(2, selectedEmpPhone);
                stmt.setString(3, selectedCarPlate);
                stmt.setString(4, pickupTimeField.getText());
                stmt.setString(5, destinationField.getText());
                stmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(null, "ماشین تحویل داده شد ✅ \nکد تحویل: " + rentalCode);

            // خالی کردن فیلدها بعد از ثبت
            pickupTimeField.setText("");
            destinationField.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا: " + e.getMessage());
        }
    }


    private void returnCar() {
        String checkSql = "SELECT * FROM CarRental WHERE rental_code = ? AND return_date IS NULL";
        String returnTime = returnTimeField.getText();
        String rentalCode = rentalCodeField.getText();
        if (returnTime.isEmpty() || rentalCode.isEmpty()) {
            JOptionPane.showMessageDialog(null, "تمام فیلدها باید پر شوند!");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
            stmt.setString(1, rentalCodeField.getText());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String updateSql = "UPDATE CarRental SET return_date = ? WHERE rental_code = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, returnTime);
                    updateStmt.setString(2, rentalCode);
                    updateStmt.executeUpdate();
                    JOptionPane.showMessageDialog(null, "ماشین بازگشت داده شد ✅");
                }
            } else {
                JOptionPane.showMessageDialog(null, "کد تحویل نامعتبر یا قبلاً ثبت شده است!");
            }

            // خالی کردن فیلدها بعد از ثبت
            returnTimeField.setText("");
            rentalCodeField.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdminUI::new);
    }
}
