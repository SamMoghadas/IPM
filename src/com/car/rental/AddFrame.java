package com.car.rental;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
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

        JTextField modelField = createRTLField();

        // پلاک
        JPanel plateInputPanel = createPlateInputPanel();

        JTextField colorField = createRTLField();

        JButton addCarButton = createActionButton("اضافه کردن ماشین", new Color(34, 139, 34));

        addFormRow(carFormPanel, "نام ماشین:", modelField);
        addFormRow(carFormPanel, "پلاک ماشین:", plateInputPanel);
        addFormRow(carFormPanel, "رنگ ماشین:", colorField);
        addFormRow(carFormPanel, "", addCarButton);

        add(carFormPanel);

        // ----------------- Employee Form Panel -----------------
        JPanel employeeFormPanel = createTitledPanel("اضافه کردن کارمند");

        JTextField personnelIdField = new JTextField(20);
        JTextField nameField = createRTLField();
        JTextField phoneField = new JTextField(20);
        JTextField telIdField = new JTextField(20);

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
            String name = modelField.getText().trim();
            String plate = buildPlateString(plateInputPanel);
            String color = colorField.getText().trim();

            if (name.isEmpty() || color.isEmpty()) {
                JOptionPane.showMessageDialog(this, "تمام فیلدهای ماشین باید پر شوند!");
                return;
            }

            try {
                db.addCar(name, plate, color);
                JOptionPane.showMessageDialog(this, "ماشین با موفقیت اضافه شد!");

                modelField.setText("");
                clearPlateFields(plateInputPanel);
                colorField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "خطا در اضافه کردن ماشین: " + ex.getMessage());
            }
        });

        // ----------------- Add Employee Logic -----------------
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

        setVisible(true);
    }

    // ----------------- Helper Methods -----------------

    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleJustification(TitledBorder.RIGHT);
        panel.setBorder(border);
        return panel;
    }

    private void addFormRow(JPanel parent, String labelText, Component field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        row.setBackground(Color.WHITE);

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(75, 25));
        row.add(field);
        row.add(label);

        parent.add(row);
    }

    private JTextField createRTLField() {
        JTextField field = new JTextField(20);
        field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        return field;
    }

    private JButton createActionButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    private JPanel createPlateInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBackground(Color.WHITE);

        Dimension fieldSize = new Dimension(40, 28);

        JTextField firstTwo = new JTextField(2);
        firstTwo.setDocument(new LimitDigitsDocument(2));
        firstTwo.setPreferredSize(fieldSize);

        String[] letters = {"الف", "ب", "ج", "د", "س", "ص", "ط", "ق", "گ", "ل", "م", "ن", "و", "هـ", "ی"};
        JComboBox<String> letterCombo = new JComboBox<>(letters);
        letterCombo.setPreferredSize(new Dimension(50, 28));

        JTextField middleThree = new JTextField(3);
        middleThree.setDocument(new LimitDigitsDocument(3));
        middleThree.setPreferredSize(new Dimension(50, 28));

        JTextField cityCode = new JTextField(2);
        cityCode.setDocument(new LimitDigitsDocument(2));
        cityCode.setPreferredSize(fieldSize);

        panel.add(firstTwo);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(letterCombo);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(middleThree);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(new JLabel("ایران"));
        panel.add(Box.createHorizontalStrut(5));
        panel.add(cityCode);

        return panel;
    }

    private String buildPlateString(JPanel platePanel) {
        Component[] comps = platePanel.getComponents();
        JTextField firstTwo = (JTextField) comps[0];
        JComboBox<?> letterCombo = (JComboBox<?>) comps[2];
        JTextField middleThree = (JTextField) comps[4];
        JTextField cityCode = (JTextField) comps[8];

        return cityCode.getText() + " ایران " + middleThree.getText() + " "
                + letterCombo.getSelectedItem() + " " + firstTwo.getText();
    }

    private void clearPlateFields(JPanel platePanel) {
        for (Component c : platePanel.getComponents()) {
            if (c instanceof JTextField tf) tf.setText("");
        }
    }

    // کنترل ارقام
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
