package com.car.rental;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.sql.*;

public class AddFrame extends JFrame {
    public AddFrame() {
        setTitle("اضافه کردن ماشین و کارمند");
        setSize(600, 400);
        setLayout(new GridLayout(2, 1, 0, 10));
        setVisible(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // ----------------- پنل اضافه کردن ماشین -----------------
        JPanel vehiclePanel = new JPanel(new GridLayout(4, 2, 10, 10));
        vehiclePanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن ماشین"));
        vehiclePanel.setBackground(Color.WHITE);

        JLabel modelLabel = new JLabel("نام ماشین:");
        JTextField modelField = new JTextField(20);
        modelField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel plateLabel = new JLabel("پلاک:");
        JPanel platePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        platePanel.setBackground(Color.WHITE);

        // ۲ رقم اول
        JTextField firstTwo = new JTextField(2);
        firstTwo.setDocument(new LimitDigitsDocument(2));

        // حرف
        String[] letters = {
                "الف", "ب", "ج", "د", "س", "ص", "ط", "ق",
                "گ", "ل", "م", "ن", "و", "هـ", "ی"
        };
        JComboBox<String> letterCombo = new JComboBox<>(letters);

        // ۳ رقم وسط
        JTextField middleThree = new JTextField(3);
        middleThree.setDocument(new LimitDigitsDocument(3));

        // ۲ رقم آخر (کد شهر)
        JTextField cityCode = new JTextField(2);
        cityCode.setDocument(new LimitDigitsDocument(2));

        platePanel.add(firstTwo);
        platePanel.add(letterCombo);
        platePanel.add(middleThree);
        platePanel.add(new JLabel("ایران"));
        platePanel.add(cityCode);

        JLabel colorLabel = new JLabel("رنگ:");
        JTextField colorField = new JTextField(20);
        colorField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton addVehicleButton = new JButton("اضافه کردن ماشین");

        vehiclePanel.add(modelLabel);
        vehiclePanel.add(modelField);
        vehiclePanel.add(plateLabel);
        vehiclePanel.add(platePanel);
        vehiclePanel.add(colorLabel);
        vehiclePanel.add(colorField);
        vehiclePanel.add(new JLabel());
        vehiclePanel.add(addVehicleButton);

        // ----------------- پنل اضافه کردن کارمند -----------------
        JPanel employeePanel = new JPanel(new GridLayout(4, 2, 10, 10));
        employeePanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن کارمند"));
        employeePanel.setBackground(Color.WHITE);

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
            String plate = firstTwo.getText() + " "
                    + letterCombo.getSelectedItem() + " "
                    + middleThree.getText() + " | "
                    + cityCode.getText();
            String color = colorField.getText().trim();

            if (name.isEmpty() || color.isEmpty()) {
                JOptionPane.showMessageDialog(null, "تمام فیلدهای ماشین باید پر شوند!");
                return;
            }

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db")) {
                String sql = "INSERT INTO Car(name, plate, color) VALUES(?,?,?)";
                PreparedStatement preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, plate);
                preparedStatement.setString(3, color);
                preparedStatement.executeUpdate();

                JOptionPane.showMessageDialog(null, "ماشین با موفقیت اضافه شد!");
                modelField.setText("");
                firstTwo.setText("");
                middleThree.setText("");
                cityCode.setText("");
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

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:IPMCarRental.db")) {
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
    }

    // ----------------- کنترل ارقام پلاک -----------------
    static class LimitDigitsDocument extends PlainDocument {
        private final int limit;
        public LimitDigitsDocument(int limit) {
            this.limit = limit;
        }

        @Override
        public void insertString(int offset, String str, AttributeSet attr)
                throws BadLocationException {
            if (str == null) return;

            if ((getLength() + str.length()) <= limit && str.matches("\\d+")) {
                super.insertString(offset, str, attr);
            }
        }
    }
}
