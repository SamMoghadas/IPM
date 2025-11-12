package com.car.rental;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;

public class ManageFrame extends JFrame {
    private final DatabaseManager db;
    private JTabbedPane tabbedPane;
    private JTable carTable, employeeTable;
    private DefaultTableModel carModel, employeeModel;
    private JTextField editNameField, editPlateField, editColorField, editPhoneField, editTelIdField;
    private int selectedCarRow = -1;
    private int selectedEmployeeRow = -1;

    public ManageFrame() {
        db = new DatabaseManager();
        initializeUI();
        loadDataFromDB();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("مدیریت ماشین‌ها و کارمندها");
        setSize(700, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // --- جدول ماشین‌ها ---
        String[] carColumns = {"نام", "رنگ", "پلاک", "وضعیت"};
        carModel = new DefaultTableModel(carColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        carTable = new JTable(carModel);
        carTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        carTable.setRowHeight(25);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(carModel);
        carTable.setRowSorter(sorter);
        centerColumns(carTable);

        JScrollPane carScroll = new JScrollPane(carTable);

        JPanel carPanel = new JPanel(new BorderLayout(10, 10));
        carPanel.add(carScroll, BorderLayout.CENTER);

        JPanel carButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton editCarButton = new JButton("ویرایش");
        JButton deleteCarButton = new JButton("حذف");
        carButtons.add(editCarButton);
        carButtons.add(deleteCarButton);
        carPanel.add(carButtons, BorderLayout.SOUTH);

        editCarButton.addActionListener(e -> editCar());
        deleteCarButton.addActionListener(e -> deleteCar());

        // --- جدول کارمندها ---
        String[] empColumns = {"شماره پرسنلی", "نام", "شماره تماس", "آیدی تلگرام", "وضعیت"};
        employeeModel = new DefaultTableModel(empColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(employeeModel);
        employeeTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        employeeTable.setRowHeight(25);
        TableRowSorter<DefaultTableModel> empSorter = new TableRowSorter<>(employeeModel);
        employeeTable.setRowSorter(empSorter);
        centerColumns(employeeTable);

        JScrollPane empScroll = new JScrollPane(employeeTable);

        JPanel empPanel = new JPanel(new BorderLayout(10, 10));
        empPanel.add(empScroll, BorderLayout.CENTER);

        JPanel empButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton editEmpButton = new JButton("ویرایش");
        JButton deleteEmpButton = new JButton("حذف");
        empButtons.add(editEmpButton);
        empButtons.add(deleteEmpButton);
        empPanel.add(empButtons, BorderLayout.SOUTH);

        editEmpButton.addActionListener(e -> editEmployee());
        deleteEmpButton.addActionListener(e -> deleteEmployee());

        tabbedPane.addTab("ماشین‌ها", carPanel);
        tabbedPane.addTab("کارمندها", empPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // --- پنل ویرایش ---
        JPanel editPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        editPanel.setVisible(false);
        editNameField = new JTextField(15);
        editPlateField = new JTextField(15);
        editColorField = new JTextField(15);
        editPhoneField = new JTextField(15);
        editTelIdField = new JTextField(15);
        JButton saveButton = new JButton("ذخیره");
        saveButton.addActionListener(e -> saveChanges());
        editPanel.add(new JLabel("نام:"));
        editPanel.add(editNameField);
        editPanel.add(new JLabel("پلاک:"));
        editPanel.add(editPlateField);
        editPanel.add(new JLabel("رنگ:"));
        editPanel.add(editColorField);
        editPanel.add(new JLabel("شماره تماس:"));
        editPanel.add(editPhoneField);
        editPanel.add(new JLabel("آیدی تلگرام:"));
        editPanel.add(editTelIdField);
        editPanel.add(new JLabel());
        editPanel.add(saveButton);

        add(editPanel, BorderLayout.SOUTH);
    }

    private void centerColumns(JTable table) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void loadDataFromDB() {
        carModel.setRowCount(0);
        employeeModel.setRowCount(0);
        try {
            for (String car : db.getAllCars()) {
                String[] parts = car.split(" - ");
                String status = parts[3].equals("1") ? "در ماموریت" : "آزاد";
                carModel.addRow(new Object[]{parts[0], parts[1], parts[2], status});
            }
            for (String emp : db.getAllEmployees()) {
                String[] parts = emp.split(" - ");
                String status = parts[4].equals("1") ? "در ماموریت" : "آزاد";
                employeeModel.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3], status});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در بارگذاری داده‌ها: " + e.getMessage());
        }
    }

    private void editCar() {
        selectedCarRow = carTable.getSelectedRow();
        if (selectedCarRow >= 0) {
            editNameField.setText(carModel.getValueAt(selectedCarRow, 0).toString());
            editPlateField.setText(carModel.getValueAt(selectedCarRow, 1).toString());
            editColorField.setText(carModel.getValueAt(selectedCarRow, 2).toString());
            editPhoneField.setText("");
            editTelIdField.setText("");
            tabbedPane.setVisible(false);
            getContentPane().getComponent(1).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "لطفاً یک ماشین انتخاب کنید!");
        }
    }

    private void deleteCar() {
        selectedCarRow = carTable.getSelectedRow();
        if (selectedCarRow >= 0) {
            if (carModel.getValueAt(selectedCarRow, 3).equals("آزاد")) {
                boolean confirmed = deleteConfirmDialog("آیا مطمئن هستید که این ماشین را حذف کنید؟");
                if (confirmed) {
                    try {
                        String plate = carModel.getValueAt(selectedCarRow, 2).toString();
                        db.deleteCar(plate, this::loadDataFromDB);
                        carModel.removeRow(selectedCarRow);
                        JOptionPane.showMessageDialog(this, "ماشین حذف شد!");
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(this, "خطا در حذف ماشین: " + e.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "ماشین در ماموریت را نمیتوانید حذف کنید!");
            }
        }
    }

    private void editEmployee() {
        selectedEmployeeRow = employeeTable.getSelectedRow();
        if (selectedEmployeeRow >= 0) {
            editNameField.setText(employeeModel.getValueAt(selectedEmployeeRow, 1).toString());
            editPhoneField.setText(employeeModel.getValueAt(selectedEmployeeRow, 2).toString());
            editTelIdField.setText(employeeModel.getValueAt(selectedEmployeeRow, 3).toString());
            editPlateField.setText("");
            editColorField.setText("");
            tabbedPane.setVisible(false);
            getContentPane().getComponent(1).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "لطفاً یک کارمند انتخاب کنید!");
        }
    }

    private void deleteEmployee() {
        selectedEmployeeRow = employeeTable.getSelectedRow();
        if (selectedEmployeeRow >= 0) {
            if (employeeModel.getValueAt(selectedEmployeeRow, 4).equals("آزاد")) {
                boolean confirmed = deleteConfirmDialog("آیا مطمئن هستید که این کارمند را حذف کنید؟");
                if (confirmed) {
                    try {
                        int personnelId = Integer.parseInt(employeeModel.getValueAt(selectedEmployeeRow, 0).toString());
                        db.deleteEmployee(personnelId, this::loadDataFromDB);
                        employeeModel.removeRow(selectedEmployeeRow);
                        JOptionPane.showMessageDialog(this, "کارمند حذف شد!");
                    } catch (SQLException e) {
                        JOptionPane.showMessageDialog(this, "خطا در حذف کارمند: " + e.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "کارمند در ماموریت را نمیتوانید حذف کنید!");
            }
        }
    }

    private void saveChanges() {
        if (selectedCarRow >= 0) {
            carModel.setValueAt(editNameField.getText(), selectedCarRow, 0);
            carModel.setValueAt(editPlateField.getText(), selectedCarRow, 1);
            carModel.setValueAt(editColorField.getText(), selectedCarRow, 2);
            JOptionPane.showMessageDialog(this, "ماشین ویرایش شد!");
        } else if (selectedEmployeeRow >= 0) {
            employeeModel.setValueAt(editNameField.getText(), selectedEmployeeRow, 1);
            employeeModel.setValueAt(editPhoneField.getText(), selectedEmployeeRow, 2);
            employeeModel.setValueAt(editTelIdField.getText(), selectedEmployeeRow, 3);
            JOptionPane.showMessageDialog(this, "کارمند ویرایش شد!");
        }
        tabbedPane.setVisible(true);
        getContentPane().getComponent(1).setVisible(false);
        editNameField.setText("");
        editPlateField.setText("");
        editColorField.setText("");
        editPhoneField.setText("");
        editTelIdField.setText("");
        selectedCarRow = -1;
        selectedEmployeeRow = -1;
    }

    private boolean deleteConfirmDialog(String message) {
        JDialog dialog = new JDialog(this, "تأیید حذف", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JLabel msgLabel = new JLabel(message, SwingConstants.CENTER);
        dialog.add(msgLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton yesButton = new JButton("بله، حذف شود");
        yesButton.setBackground(Color.RED);
        yesButton.setForeground(Color.WHITE);

        JButton noButton = new JButton("خیر، منصرف شدم");
        noButton.setBackground(Color.GRAY);
        noButton.setForeground(Color.WHITE);

        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        final boolean[] confirmed = {false};
        yesButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        noButton.addActionListener(e -> dialog.dispose());

        dialog.setSize(350, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return confirmed[0];
    }
}