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
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(245, 245, 250));
        frame.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای کل فریم

        // --- بخش تحویل ---
        JPanel pickupPanel = new JPanel(new GridBagLayout());
        pickupPanel.setBorder(BorderFactory.createTitledBorder("ثبت تحویل ماشین"));
        pickupPanel.setBackground(Color.WHITE);
        pickupPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای پانل

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 5); // فاصله بیشتر بین برچسب و فیلد
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ماشین
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST; // فیلد سمت راست
        vehicleCombo = new JComboBox<>();
        vehicleCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        vehicleCombo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای کومبو
        loadVehicles();
        pickupPanel.add(vehicleCombo, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; // برچسب سمت چپ فیلد
        JLabel vehicleLabel = new JLabel("ماشین:");
        vehicleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(vehicleLabel, gbc);

        // کارمند
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        employeeCombo = new JComboBox<>();
        employeeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        employeeCombo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای کومبو
        loadEmployees();
        pickupPanel.add(employeeCombo, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        JLabel employeeLabel = new JLabel("کارمند:");
        employeeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(employeeLabel, gbc);

        // زمان تحویل
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        JPanel pickupTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pickupTimePanel.setBackground(Color.WHITE);
        pickupTimePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای پانل زمان
        pickupTimeField = new JTextField(15);
        pickupTimeField.setFont(new Font("Arial", Font.PLAIN, 14));
        pickupTimeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای فیلد
        JButton nowPickupButton = new JButton("الان");
        nowPickupButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nowPickupButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای دکمه
        nowPickupButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            pickupTimeField.setText(now.format(formatter));
        });
        pickupTimePanel.add(pickupTimeField);
        pickupTimePanel.add(nowPickupButton);
        pickupPanel.add(pickupTimePanel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        JLabel pickupTimeLabel = new JLabel("زمان تحویل:");
        pickupTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(pickupTimeLabel, gbc);

        // مقصد
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        destinationField = new JTextField(15);
        destinationField.setFont(new Font("Arial", Font.PLAIN, 14));
        destinationField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای فیلد
        pickupPanel.add(destinationField, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        JLabel destinationLabel = new JLabel("مقصد:");
        destinationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(destinationLabel, gbc);

        // دکمه ثبت تحویل
        gbc.gridx = 0; gbc.gridy = 4; gbc.anchor = GridBagConstraints.CENTER;
        JButton pickupButton = new JButton("ثبت تحویل");
        pickupButton.setFont(new Font("Arial", Font.BOLD, 14));
        pickupButton.setBackground(new Color(0, 120, 215));
        pickupButton.setForeground(Color.WHITE);
        pickupButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای دکمه
        pickupButton.addActionListener(e -> {
            pickupCar();
            pickupTimeField.setText("");
            destinationField.setText("");
        });
        pickupPanel.add(pickupButton, gbc);

        // --- بخش بازگشت ---
        JPanel returnPanel = new JPanel(new GridBagLayout());
        returnPanel.setBorder(BorderFactory.createTitledBorder("ثبت بازگشت ماشین"));
        returnPanel.setBackground(Color.WHITE);
        returnPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای پانل

        // کد تحویل
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        rentalCodeField = new JTextField(15);
        rentalCodeField.setFont(new Font("Arial", Font.PLAIN, 14));
        rentalCodeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای فیلد
        returnPanel.add(rentalCodeField, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        JLabel rentalIdLabel = new JLabel("کد تحویل:");
        rentalIdLabel.setFont(new Font("Arial", Font.BOLD, 14));
        returnPanel.add(rentalIdLabel, gbc);

        // زمان بازگشت
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        JPanel returnTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        returnTimePanel.setBackground(Color.WHITE);
        returnTimePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای پانل زمان
        returnTimeField = new JTextField(15);
        returnTimeField.setFont(new Font("Arial", Font.PLAIN, 14));
        returnTimeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای فیلد
        JButton nowReturnButton = new JButton("الان");
        nowReturnButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nowReturnButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای دکمه
        nowReturnButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            returnTimeField.setText(now.format(formatter));
        });
        returnTimePanel.add(returnTimeField);
        returnTimePanel.add(nowReturnButton);
        returnPanel.add(returnTimePanel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        JLabel returnTimeLabel = new JLabel("زمان بازگشت:");
        returnTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        returnPanel.add(returnTimeLabel, gbc);

        // دکمه ثبت بازگشت
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.CENTER;
        JButton returnButton = new JButton("ثبت بازگشت");
        returnButton.setFont(new Font("Arial", Font.BOLD, 14));
        returnButton.setBackground(new Color(0, 120, 215));
        returnButton.setForeground(Color.WHITE);
        returnButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای دکمه
        returnButton.addActionListener(e -> {
            returnCar();
            rentalCodeField.setText("");
            returnTimeField.setText("");
        });
        returnPanel.add(returnButton, gbc);

        // --- دکمه‌ها ---
        JButton addItemsButton = new JButton("اضافه کردن ماشین / کارمند");
        addItemsButton.setFont(new Font("Arial", Font.BOLD, 14));
        addItemsButton.setBackground(new Color(34, 139, 34));
        addItemsButton.setForeground(Color.WHITE);
        addItemsButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای دکمه
        addItemsButton.addActionListener(e -> new AddFrame());
        JButton reportButton = new JButton("نمایش گزارش");
        reportButton.setFont(new Font("Arial", Font.BOLD, 14));
        reportButton.setBackground(new Color(139, 69, 19));
        reportButton.setForeground(Color.WHITE);
        reportButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای دکمه
        reportButton.addActionListener(e -> new ReportFrame());

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای پانل دکمه‌ها
        buttonsPanel.add(addItemsButton);
        buttonsPanel.add(reportButton);

        // چیدمان اصلی
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT); // RTL برای پانل اصلی
        mainPanel.add(pickupPanel);
        mainPanel.add(returnPanel);
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(buttonsPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void loadVehicles() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db")) {
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
    }

    private void loadEmployees() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name FROM Employee");
            while (rs.next()) {
                String employee = rs.getString("name");
                employeeCombo.addItem(employee);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا در خواندن کارمند‌ها: " + e.getMessage());
        }
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
            String selectedEmpPhone = null;
            try (PreparedStatement empStmt = conn.prepareStatement("SELECT phone FROM Employee WHERE name = ?")) {
                empStmt.setString(1, (String) employeeCombo.getSelectedItem());
                ResultSet rs = empStmt.executeQuery();
                if (rs.next()) {
                    selectedEmpPhone = rs.getString("phone");
                }
            }

            String selectedCar = (String) vehicleCombo.getSelectedItem();
            String selectedCarPlate = Optional.ofNullable(selectedCar)
                    .map(s -> s.substring(s.lastIndexOf('-') + 2)).orElse("Unknown");

            String pickupTime = pickupTimeField.getText();
            String destination = destinationField.getText();
            if (pickupTime.isEmpty() || destination.isEmpty()) {
                JOptionPane.showMessageDialog(null, "تمام فیلدها باید پر شوند!");
                return;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, rentalCode);
                stmt.setString(2, selectedEmpPhone);
                stmt.setString(3, selectedCarPlate);
                stmt.setString(4, pickupTimeField.getText());
                stmt.setString(5, destinationField.getText());
                stmt.executeUpdate();
            }

            JOptionPane.showMessageDialog(null, "ماشین تحویل داده شد ✅ \nکد تحویل: " + rentalCode);

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