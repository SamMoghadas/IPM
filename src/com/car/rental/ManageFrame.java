package com.car.rental;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class ManageFrame extends JFrame {
    private final DatabaseManager db;
    private JTabbedPane tabbedPane;
    private JList<String> carList, employeeList;
    private DefaultListModel<String> carModel, employeeModel;
    private JTextField editNameField, editPlateField, editColorField, editPhoneField, editTelIdField;
    private String selectedCar, selectedEmployee;

    public ManageFrame() {
        db = new DatabaseManager();
        initializeUI();
        loadDataFromDB();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("مدیریت ماشین‌ها و کارمندها");
        setSize(500, 400);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);


        JPanel carPanel = new JPanel(new BorderLayout(10, 10));
        carModel = new DefaultListModel<>();
        carList = new JList<>(carModel);
        carList.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JScrollPane carScroll = new JScrollPane(carList);
        carPanel.add(carScroll, BorderLayout.CENTER);


        JPanel carButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton editCarButton = new JButton("ویرایش");
        JButton deleteCarButton = new JButton("حذف");
        carButtons.add(editCarButton);
        carButtons.add(deleteCarButton);
        carPanel.add(carButtons, BorderLayout.SOUTH);

        editCarButton.addActionListener(e -> editCar());
        deleteCarButton.addActionListener(e -> deleteCar());


        JPanel empPanel = new JPanel(new BorderLayout(10, 10));
        employeeModel = new DefaultListModel<>();
        employeeList = new JList<>(employeeModel);
        employeeList.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JScrollPane empScroll = new JScrollPane(employeeList);
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
        add(tabbedPane);

        // پنل ادیت
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

    private void loadDataFromDB() {
        carModel.clear();
        employeeModel.clear();
        try {
            for (String car : db.getCars()) {
                carModel.addElement(car);
            }
            for (String emp : db.getEmployees()) {
                employeeModel.addElement(emp);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در بارگذاری داده‌ها: " + e.getMessage());
        }
    }


    private void editCar() {
        selectedCar = carList.getSelectedValue();
        if (selectedCar != null) {
            String[] parts = selectedCar.split(" - ");
            editNameField.setText(parts[0]);
            editPlateField.setText(parts[1]);
            editColorField.setText(parts[2]);
            editPhoneField.setText("");
            editTelIdField.setText("");
            tabbedPane.setVisible(false);
            getContentPane().getComponent(1).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "لطفاً یک ماشین انتخاب کنید!");
        }
    }

    private void deleteCar() {
        String selected = carList.getSelectedValue();
        if (selected != null) {
            boolean confirmed = deleteConfirmDialog(
                    "آیا مطمئن هستید که می‌خواهید این ماشین را حذف کنید؟"
            );
            if (confirmed) {
                try {
                    String plate = selected.split(" - ")[2];
                    AdminUI adminUI = AdminUI.getInstance();
                    db.deleteCar(plate, adminUI::loadCars);
                    carModel.removeElement(selected);
                    JOptionPane.showMessageDialog(this, "ماشین حذف شد!");
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "خطا در حذف ماشین: " + e.getMessage());
                }
            }
        }
    }

    private void editEmployee() {
        selectedEmployee = employeeList.getSelectedValue();
        if (selectedEmployee != null) {
            String[] parts = selectedEmployee.split(" - ");
            editNameField.setText(parts[0]);
            editPhoneField.setText(parts[1]);
            editTelIdField.setText(parts[2]);
            editPlateField.setText("");
            editColorField.setText("");
            tabbedPane.setVisible(false);
            getContentPane().getComponent(1).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "لطفاً یک کارمند انتخاب کنید!");
        }
    }

    private void deleteEmployee() {
        String selected = employeeList.getSelectedValue();
        if (selected != null) {
            boolean confirmed = deleteConfirmDialog(
                    "آیا مطمئن هستید که می‌خواهید این کارمند را حذف کنید؟"
            );
            if (confirmed) {
                try {
                    int personnelId = Integer.parseInt(selected.split(" - ")[0]);
                    AdminUI adminUI = AdminUI.getInstance();
                    db.deleteEmployee(personnelId, adminUI::loadEmployees);
                    employeeModel.removeElement(selected);
                    JOptionPane.showMessageDialog(this, "کارمند حذف شد!");
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "خطا در حذف کارمند: " + e.getMessage());
                }
            }
        }
    }

    private void saveChanges() {
        if (selectedCar != null) {
            String newItem = editNameField.getText() + " - " + editPlateField.getText() + " - " + editColorField.getText();
            carModel.set(carModel.indexOf(selectedCar), newItem);
            JOptionPane.showMessageDialog(this, "ماشین ویرایش شد!");
        } else if (selectedEmployee != null) {
            String newItem = editNameField.getText() + " - " + editPhoneField.getText() + " - " + editTelIdField.getText();
            employeeModel.set(employeeModel.indexOf(selectedEmployee), newItem);
            JOptionPane.showMessageDialog(this, "کارمند ویرایش شد!");
        }
        tabbedPane.setVisible(true);
        getContentPane().getComponent(1).setVisible(false);
        editNameField.setText("");
        editPlateField.setText("");
        editColorField.setText("");
        editPhoneField.setText("");
        editTelIdField.setText("");
        selectedCar = null;
        selectedEmployee = null;
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