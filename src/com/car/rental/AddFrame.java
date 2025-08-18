package com.car.rental;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AddFrame extends JFrame {
    public AddFrame() {
        setTitle("اضافه کردن ماشین و کارمند");
        setSize(600, 400);
        setLayout(new GridLayout(2, 1, 0, 10));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ----------------- پنل اضافه کردن ماشین -----------------
        JPanel vehiclePanel = new JPanel(new GridLayout(4, 2, 10, 10));
        vehiclePanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن ماشین"));
        vehiclePanel.setBackground(new Color(255, 255, 255));

        JLabel modelLabel = new JLabel("نام ماشین:");
        JTextField modelField = new JTextField(20);
        modelField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel plateLabel = new JLabel("پلاک:");
        JTextField plateField = new JTextField(20);
        plateField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel colorLabel = new JLabel("رنگ:");
        JTextField colorField = new JTextField(20);
        colorField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton addVehicleButton = new JButton("اضافه کردن ماشین");

        vehiclePanel.add(modelLabel);
        vehiclePanel.add(modelField);
        vehiclePanel.add(plateLabel);
        vehiclePanel.add(plateField);
        vehiclePanel.add(colorLabel);
        vehiclePanel.add(colorField);
        vehiclePanel.add(new JLabel());
        vehiclePanel.add(addVehicleButton);

        // ----------------- پنل اضافه کردن کارمند -----------------
        JPanel employeePanel = new JPanel(new GridLayout(4, 2, 10, 10));
        employeePanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن کارمند"));
        employeePanel.setBackground(new Color(255, 255, 255));

        JLabel nameLabel = new JLabel("نام:");
        JTextField nameField = new JTextField(20);
        nameField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel phoneLabel = new JLabel("شماره تماس:");
        JTextField phoneField = new JTextField(20);

        JLabel telIdLabel = new JLabel("آیدی تلگرام:");
        JTextField telIdField = new JTextField(20);

        JButton addEmployeeButton = new JButton("اضافه کردن کارمند");

        employeePanel.add(nameLabel);
        employeePanel.add(nameField);
        employeePanel.add(phoneLabel);
        employeePanel.add(phoneField);
        employeePanel.add(telIdLabel);
        employeePanel.add(telIdField);
        employeePanel.add(new JLabel());
        employeePanel.add(addEmployeeButton);

        add(vehiclePanel);
        add(employeePanel);

        // ----------------- عملیات اضافه کردن ماشین -----------------
        addVehicleButton.addActionListener(e -> {
            String name = modelField.getText().trim();
            String plate = plateField.getText().trim();
            String color = colorField.getText().trim();

            if (name.isEmpty() || plate.isEmpty() || color.isEmpty()) {
                JOptionPane.showMessageDialog(null, "تمام فیلدهای ماشین باید پر شوند!");
                return;
            }

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db")) {
                String sql = "INSERT INTO Car(name, plate, color) VALUES(?,?,?)";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, plate);
                preparedStatement.setString(3, color);
                preparedStatement.executeUpdate();

                JOptionPane.showMessageDialog(null, "ماشین با موفقیت اضافه شد!");
                modelField.setText("");
                plateField.setText("");
                colorField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "خطا در اضافه کردن ماشین: " + ex.getMessage());
            }
        });

        // ----------------- عملیات اضافه کردن کارمند -----------------
        addEmployeeButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String telId = telIdField.getText().trim();

            if (name.isEmpty() || phone.isEmpty() || telId.isEmpty()) {
                JOptionPane.showMessageDialog(null, "تمام فیلدهای کارمند باید پر شوند!");
                return;
            }

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:test.db")) {
                String sql = "INSERT INTO Employee(name, phone, telegram_id) VALUES(?,?,?)";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, phone);
                preparedStatement.setString(3, telId);
                preparedStatement.executeUpdate();

                JOptionPane.showMessageDialog(null, "کارمند با موفقیت اضافه شد!");
                nameField.setText("");
                phoneField.setText("");
                telIdField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "خطا در اضافه کردن کارمند: " + ex.getMessage());
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        new AddFrame();
    }
}
