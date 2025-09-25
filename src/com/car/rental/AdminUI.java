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
    private DatabaseManager dbManager;
    private JComboBox<String> vehicleCombo;
    private JComboBox<String> employeeCombo;
    private JTextField pickupTimeField, destinationField;
    private JTextField rentalCodeField, returnTimeField;
    private static final Logger logger = Logger.getLogger(AdminUI.class.getName());

    public AdminUI() {
        dbManager = new DatabaseManager();
        createMainUI();
    }

    private void createMainUI() {
        JFrame frame = new JFrame("سیستم مدیریت ماشین شرکت");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(245, 245, 250));
        frame.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // --- بخش تحویل ---
        JPanel pickupPanel = new JPanel(new GridBagLayout());
        pickupPanel.setBorder(BorderFactory.createTitledBorder("ثبت تحویل ماشین"));
        pickupPanel.setBackground(Color.WHITE);
        pickupPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ماشین
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel vehicleLabel = new JLabel("ماشین:");
        vehicleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(vehicleLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        vehicleCombo = new JComboBox<>();
        vehicleCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        vehicleCombo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        loadVehicles();
        pickupPanel.add(vehicleCombo, gbc);

        // کارمند
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        JLabel employeeLabel = new JLabel("کارمند:");
        employeeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(employeeLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        employeeCombo = new JComboBox<>();
        employeeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        employeeCombo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        loadEmployees();
        pickupPanel.add(employeeCombo, gbc);

        // زمان تحویل
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        JLabel pickupTimeLabel = new JLabel("زمان تحویل:");
        pickupTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(pickupTimeLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        JPanel pickupTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pickupTimePanel.setBackground(Color.WHITE);
        pickupTimePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pickupTimeField = new JTextField(15);
        pickupTimeField.setFont(new Font("Arial", Font.PLAIN, 14));
        pickupTimeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton nowPickupButton = new JButton("الان");
        nowPickupButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nowPickupButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        nowPickupButton.addActionListener(e -> pickupTimeField.setText(getDate()));
        pickupTimePanel.add(pickupTimeField);
        pickupTimePanel.add(nowPickupButton);
        pickupPanel.add(pickupTimePanel, gbc);

        // مقصد
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        JLabel destinationLabel = new JLabel("مقصد:");
        destinationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(destinationLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        destinationField = new JTextField(15);
        destinationField.setFont(new Font("Arial", Font.PLAIN, 14));
        destinationField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pickupPanel.add(destinationField, gbc);

        // دکمه ثبت تحویل
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.CENTER;
        JButton pickupButton = new JButton("ثبت تحویل");
        pickupButton.setFont(new Font("Arial", Font.BOLD, 14));
        pickupButton.setBackground(new Color(0, 120, 215));
        pickupButton.setForeground(Color.WHITE);
        pickupButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pickupButton.addActionListener(e -> {
            try {
                pickupCar();
                pickupTimeField.setText("");
                destinationField.setText("");
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Error in pickupCar", ex);
            }
        });
        pickupPanel.add(pickupButton, gbc);

        // --- بخش بازگشت ---
        JPanel returnPanel = new JPanel(new GridBagLayout());
        returnPanel.setBorder(BorderFactory.createTitledBorder("ثبت بازگشت ماشین"));
        returnPanel.setBackground(Color.WHITE);
        returnPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // کد تحویل
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel rentalIdLabel = new JLabel("کد تحویل:");
        rentalIdLabel.setFont(new Font("Arial", Font.BOLD, 14));
        returnPanel.add(rentalIdLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        rentalCodeField = new JTextField(15);
        rentalCodeField.setFont(new Font("Arial", Font.PLAIN, 14));
        rentalCodeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        returnPanel.add(rentalCodeField, gbc);

        // زمان بازگشت
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        JLabel returnTimeLabel = new JLabel("زمان بازگشت:");
        returnTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        returnPanel.add(returnTimeLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        JPanel returnTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        returnTimePanel.setBackground(Color.WHITE);
        returnTimePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        returnTimeField = new JTextField(15);
        returnTimeField.setFont(new Font("Arial", Font.PLAIN, 14));
        returnTimeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton nowReturnButton = new JButton("الان");
        nowReturnButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nowReturnButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        nowReturnButton.addActionListener(e -> returnTimeField.setText(getDate()));
        returnTimePanel.add(returnTimeField);
        returnTimePanel.add(nowReturnButton);
        returnPanel.add(returnTimePanel, gbc);

        // دکمه ثبت بازگشت
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.CENTER;
        JButton returnButton = new JButton("ثبت بازگشت");
        returnButton.setFont(new Font("Arial", Font.BOLD, 14));
        returnButton.setBackground(new Color(0, 120, 215));
        returnButton.setForeground(Color.WHITE);
        returnButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        returnButton.addActionListener(e -> {
            try {
                returnCar();
                rentalCodeField.setText("");
                returnTimeField.setText("");
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Error in returnCar", ex);
            }
        });
        returnPanel.add(returnButton, gbc);

        // --- دکمه‌ها ---
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton addItemsButton = new JButton("اضافه کردن ماشین / کارمند");
        addItemsButton.setFont(new Font("Arial", Font.BOLD, 14));
        addItemsButton.setBackground(new Color(34, 139, 34));
        addItemsButton.setForeground(Color.WHITE);
        addItemsButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        addItemsButton.addActionListener(e -> new AddFrame());
        JButton reportButton = new JButton("نمایش گزارش");
        reportButton.setFont(new Font("Arial", Font.BOLD, 14));
        reportButton.setBackground(new Color(139, 69, 19));
        reportButton.setForeground(Color.WHITE);
        reportButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        reportButton.addActionListener(e -> new ReportFrame());
        JButton manageButton = new JButton("مدیریت ماشین‌ها و کارمندها");
        manageButton.setFont(new Font("Arial", Font.BOLD, 14));
        manageButton.setBackground(new Color(165, 42, 42));
        manageButton.setForeground(Color.WHITE);
        manageButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        manageButton.addActionListener(e -> new ManageFrame());
        buttonsPanel.add(manageButton);
        buttonsPanel.add(addItemsButton);
        buttonsPanel.add(reportButton);

        // چیدمان اصلی
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        mainPanel.add(pickupPanel);
        mainPanel.add(returnPanel);
        frame.add(mainPanel, BorderLayout.CENTER);
        frame.add(buttonsPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void loadVehicles() {
        try (Connection conn = dbManager.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT name, plate, color FROM Car");
            while (rs.next()) {
                String car = rs.getString("name") + " - " + rs.getString("plate") + " - " + rs.getString("color");
                vehicleCombo.addItem(car);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا در خواندن ماشین‌ها: " + e.getMessage());
        }
    }

    private void loadEmployees() {
        try (Connection conn = dbManager.getConnection()) {
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

    private void pickupCar() throws SQLException {
        String rentalCode = generateRentalCode();
        String selectedEmpPhone = getEmployeePhone((String) employeeCombo.getSelectedItem());
        String selectedCar = (String) vehicleCombo.getSelectedItem();
        String selectedCarPlate = Optional.ofNullable(selectedCar)
                .map(s -> s.substring(s.lastIndexOf('-') + 2)).orElse("Unknown");
        String pickupTime = pickupTimeField.getText();
        String destination = destinationField.getText();

        if (pickupTime.isEmpty() || destination.isEmpty()) {
            JOptionPane.showMessageDialog(null, "تمام فیلدها باید پر شوند!");
            return;
        }

        dbManager.assignVehicle(rentalCode, selectedEmpPhone, selectedCarPlate, pickupTime, destination);
        JOptionPane.showMessageDialog(null, "ماشین تحویل داده شد ✅ \nکد تحویل: " + rentalCode);

        pickupTimeField.setText("");
        destinationField.setText("");
    }

    private void returnCar() throws SQLException {
        String rentalCode = rentalCodeField.getText();
        String returnTime = returnTimeField.getText();

        if (returnTime.isEmpty() || rentalCode.isEmpty()) {
            JOptionPane.showMessageDialog(null, "تمام فیلدها باید پر شوند!");
            return;
        }

        dbManager.returnVehicle(rentalCode, returnTime);
        JOptionPane.showMessageDialog(null, "ماشین بازگشت داده شد ✅");

        returnTimeField.setText("");
        rentalCodeField.setText("");
    }

    private String getEmployeePhone(String empName) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT phone FROM Employee WHERE name = ?";
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                preparedStatement.setString(1, empName);
                ResultSet rs = preparedStatement.executeQuery();
                return rs.next() ? rs.getString("phone") : null;
            }
        }
    }

    private static String getDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String[] date_time = now.format(formatter).split(" ");
        String[] date = date_time[0].split("-");
        int[] shams_date = DateConverter.gregorian_to_jalali(
                Integer.parseInt(date[0]), Integer.parseInt(date[1]), Integer.parseInt(date[2]));

        return shams_date[0] + "/" + shams_date[1] + "/" + shams_date[2] + " " + date_time[1];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdminUI::new);
    }

    static class DateConverter {

        /**
         * Gregorian & Jalali (Hijri_Shams,Solar) Date Converter Functions
         * Author: JDF.SCR.IR =>> Download Full Version :  <a href="http://jdf.scr.ir/jdf">...</a>
         * License: GNU/LGPL _ Open Source & Free :: Version: 2.80 : [2020=1399]
         * ---------------------------------------------------------------------
         * 355746=361590-5844 & 361590=(30*33*365)+(30*8) & 5844=(16*365)+(16/4)
         * 355666=355746-79-1 & 355668=355746-79+1 &  1595=605+990 &  605=621-16
         * 990=30*33 & 12053=(365*33)+(32/4) & 36524=(365*100)+(100/4)-(100/100)
         * 1461=(365*4)+(4/4) & 146097=(365*400)+(400/4)-(400/100)+(400/400)
         */

        public static int[] gregorian_to_jalali(int gy, int gm, int gd) {
            int[] out = {
                    (gm > 2) ? (gy + 1) : gy,
                    0,
                    0
            };
            {
                int[] g_d_m = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
                out[2] = 355666 + (365 * gy) + (((out[0] + 3) / 4)) - (((out[0] + 99) / 100)) + (((out[0] + 399) / 400)) + gd + g_d_m[gm - 1];
            }
            out[0] = -1595 + (33 * ((out[2] / 12053)));
            out[2] %= 12053;
            out[0] += 4 * ((out[2] / 1461));
            out[2] %= 1461;
            if (out[2] > 365) {
                out[0] += ((out[2] - 1) / 365);
                out[2] = (out[2] - 1) % 365;
            }
            if (out[2] < 186) {
                out[1] = 1 + (out[2] / 31);
                out[2] = 1 + (out[2] % 31);
            } else {
                out[1] = 7 + ((out[2] - 186) / 30);
                out[2] = 1 + ((out[2] - 186) % 30);
            }
            return out;
        }
    }
}