package com.car.rental;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.sql.*;

public class AddFrame extends JFrame {
    public AddFrame() {
        // تنظیمات اولیه فریم
        setTitle("اضافه کردن ماشین و کارمند");
        setSize(600, 400);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 0));
        getContentPane().setBackground(Color.WHITE);

        // ----------------- پنل بالایی با دکمه بازگشت -----------------
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);
        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setBackground(new Color(156, 163, 175));
        backButton.setFocusPainted(false);
        backButton.setBackground(new Color(230, 230, 230));
        backButton.addActionListener(e -> {
            dispose();
            new AdminUI();
        });
        topPanel.add(backButton);
        add(topPanel, BorderLayout.NORTH);

        // ----------------- پنل اضافه کردن ماشین -----------------
        JPanel carPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        carPanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن ماشین"));
        carPanel.setBackground(Color.WHITE);

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
        String[] letters = {"الف", "ب", "ج", "د", "س", "ص", "ط", "ق", "گ", "ل", "م", "ن", "و", "هـ", "ی"};
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

        JButton addCarButton = new JButton("اضافه کردن ماشین");
        addCarButton.setBackground(new Color(16, 185, 129));
        addCarButton.setForeground(Color.WHITE);

        carPanel.add(modelLabel);
        carPanel.add(modelField);
        carPanel.add(plateLabel);
        carPanel.add(platePanel);
        carPanel.add(colorLabel);
        carPanel.add(colorField);
        carPanel.add(new JLabel());
        carPanel.add(addCarButton);

        // ----------------- پنل اضافه کردن کارمند -----------------
        JPanel employeePanel = new JPanel(new GridLayout(5, 2, 10, 10));
        employeePanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن کارمند"));
        employeePanel.setBackground(Color.WHITE);

        JLabel personnelIdLabel = new JLabel("شماره پرسنلی:");
        JTextField personnelIdField = new JTextField(20);

        JLabel nameLabel = new JLabel("نام:");
        JTextField nameField = new JTextField(20);
        nameField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel phoneLabel = new JLabel("شماره تماس:");
        JTextField phoneField = new JTextField(20);

        JLabel telIdLabel = new JLabel("آیدی تلگرام:");
        JTextField telIdField = new JTextField(20);

        JButton addEmployeeButton = new JButton("اضافه کردن کارمند");
        addEmployeeButton.setBackground(new Color(16, 185, 129));
        addEmployeeButton.setForeground(Color.WHITE);

        employeePanel.add(personnelIdLabel);
        employeePanel.add(personnelIdField);
        employeePanel.add(nameLabel);
        employeePanel.add(nameField);
        employeePanel.add(phoneLabel);
        employeePanel.add(phoneField);
        employeePanel.add(telIdLabel);
        employeePanel.add(telIdField);
        employeePanel.add(new JLabel());
        employeePanel.add(addEmployeeButton);

        // ----------------- پنل مرکزی برای قرار دادن دو بخش -----------------
        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(carPanel);
        centerPanel.add(employeePanel);
        add(centerPanel, BorderLayout.CENTER);

        // ----------------- دیتابیس -----------------
        DatabaseManager db = new DatabaseManager();

        // ----------------- عملیات اضافه کردن ماشین -----------------
        addCarButton.addActionListener(e -> {
            String name = modelField.getText().trim();
            String plate = cityCode.getText() + " | " + middleThree.getText() + " "
                    + letterCombo.getSelectedItem() + " " + firstTwo.getText();
            String color = colorField.getText().trim();

            if (name.isEmpty() || color.isEmpty()) {
                JOptionPane.showMessageDialog(this, "تمام فیلدهای ماشین باید پر شوند!");
                return;
            }

            try {
                db.addCar(name, plate, color);
                JOptionPane.showMessageDialog(this, "ماشین با موفقیت اضافه شد!");
                modelField.setText("");
                firstTwo.setText("");
                middleThree.setText("");
                cityCode.setText("");
                colorField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "خطا در اضافه کردن ماشین: " + ex.getMessage());
            }
        });

        // ----------------- عملیات اضافه کردن کارمند -----------------
        addEmployeeButton.addActionListener(e -> {
            String personnelIdText = personnelIdField.getText().trim();
            if (personnelIdText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "شماره پرسنلی را وارد کنید!");
                return;
            }

            int personnelId;
            try {
                personnelId = Integer.parseInt(personnelIdText);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "شماره پرسنلی باید عدد باشد!");
                return;
            }

            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String telId = telIdField.getText().trim();

            if (name.isEmpty() || phone.isEmpty() || telId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "تمام فیلدهای کارمند باید پر شوند!");
                return;
            }

            try {
                db.addEmployee(personnelId, name, phone, telId);
                JOptionPane.showMessageDialog(this, "کارمند با موفقیت اضافه شد!");
                personnelIdField.setText("");
                nameField.setText("");
                phoneField.setText("");
                telIdField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "خطا در اضافه کردن کارمند: " + ex.getMessage());
            }
        });

        // نمایش در انتها
        setVisible(true);
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
