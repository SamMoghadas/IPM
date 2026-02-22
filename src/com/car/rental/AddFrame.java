package com.car.rental;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.*;

public class AddFrame extends JFrame {
    public AddFrame() {

        // تنظیمات فریم
        setTitle("اضافه کردن ماشین و کارمند");
        setSize(600, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().setBackground(Color.WHITE);

        // ----------------- Header Panel -----------------
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        headerPanel.setBackground(Color.WHITE);

        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setBackground(new Color(230, 230, 230));
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> {
            dispose();
            new AdminUI();
        });

        headerPanel.add(backButton);
        add(headerPanel);

        // ----------------- Car Form Panel -----------------
        JPanel carFormPanel = createTitledPanel("اضافه کردن ماشین");

        JTextField modelField = createField(true);

        // پلاک
        PlateInputPanel plateInputPanel = new PlateInputPanel();

        JTextField colorField = createField(true);

        JButton addCarButton = createActionButton("اضافه کردن ماشین", new Color(34, 139, 34));

        addFormRow(carFormPanel, "نام ماشین:", modelField);
        addFormRow(carFormPanel, "پلاک ماشین:", plateInputPanel);
        addFormRow(carFormPanel, "رنگ ماشین:", colorField);
        addFormRow(carFormPanel, "", addCarButton);

        add(carFormPanel);

        // ----------------- Employee Form Panel -----------------
        JPanel employeeFormPanel = createTitledPanel("اضافه کردن کارمند");

        JTextField personnelIdField = createField(false);
        JTextField nameField = createField(true);
        JTextField phoneField = createField(false);
        JTextField telIdField = createField(false);

        JButton addEmployeeButton = createActionButton("اضافه کردن کارمند", new Color(34, 139, 34));

        addFormRow(employeeFormPanel, "شماره پرسنلی:", personnelIdField);
        addFormRow(employeeFormPanel, "نام و نام خانوادگی:", nameField);
        addFormRow(employeeFormPanel, "شماره تماس:", phoneField);
        addFormRow(employeeFormPanel, "آیدی تلگرام:", telIdField);
        addFormRow(employeeFormPanel, "", addEmployeeButton);

        add(employeeFormPanel);

        // ----------------- Database -----------------
        DatabaseManager db = new DatabaseManager();

        // ----------------- Add Car Logic -----------------
        addCarButton.addActionListener(e -> {
            String name = modelField.getText().strip();
            String plate = plateInputPanel.getPlate();
            String color = colorField.getText().strip();

            if (name.isEmpty() || color.isEmpty()) {
                JOptionPane.showMessageDialog(this, "تمام فیلدهای ماشین باید پر شوند!");
                return;
            }

            try {
                db.addCar(name, plate, color);
                JOptionPane.showMessageDialog(this, "ماشین با موفقیت اضافه شد!");

                modelField.setText("");
                plateInputPanel.clear();
                colorField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "خطا در اضافه کردن ماشین: " + ex.getMessage());
            }
        });

        // ----------------- Add Employee Logic -----------------
        addEmployeeButton.addActionListener(e -> {
            String personnelIdText = personnelIdField.getText().strip();
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

            String name = nameField.getText().strip();
            String phone = phoneField.getText().strip();
            String telId = telIdField.getText().strip();

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

        setVisible(true);
    }

    // ----------------- Helper Methods -----------------

    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        Border line = new LineBorder(new Color(150, 220, 200), 2, true);
        TitledBorder border = BorderFactory.createTitledBorder(line, title);
        border.setTitleJustification(TitledBorder.RIGHT);
        border.setTitleColor(Color.DARK_GRAY);
        panel.setBorder(border);
        return panel;
    }

    private void addFormRow(JPanel parent, String labelText, Component field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        row.setBackground(Color.WHITE);

        JLabel label = new JLabel(labelText, SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(75, 25));
        row.add(field);
        row.add(label);

        parent.add(row);
    }

    private JTextField createField(boolean RTL) {
        JTextField field = new JTextField(20);
        if (RTL) field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        field.setBorder(BorderFactory.createCompoundBorder(new LineBorder(
                new Color(122, 138, 153), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return field;
    }

    private JButton createActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }
}
