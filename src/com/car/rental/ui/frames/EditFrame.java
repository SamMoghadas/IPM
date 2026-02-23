package com.car.rental.ui.frames;

import com.car.rental.model.Car;
import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import com.car.rental.ui.components.PlateInputPanel;

import javax.swing.*;
import java.awt.*;

public class EditFrame extends JFrame {
    public enum EditMode {
        CAR,
        EMPLOYEE
    }
    private final EditMode mode;
    private JTextField carModelField;
    private PlateInputPanel carPlateField;
    private JTextField carColorField;
    private JTextField personnelIdField;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField telegramField;
    private JButton saveButton;
    private JButton cancelButton;
    private Car car;
    private Employee employee;

    public EditFrame(Car car) {
        this.mode = EditMode.CAR;
        this.car = car;
        initFrame();
        initComponents();
        initLayout();
        initActions();
        fillFields();
    }

    public EditFrame(Employee emp) {
        this.mode = EditMode.EMPLOYEE;
        this.employee = emp;
        initFrame();
        initComponents();
        initLayout();
        initActions();
        fillFields();
    }

    private void initFrame() {
        setTitle("ویرایش");
        setSize(400, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
    }

    private void initComponents() {
        if (mode == EditMode.CAR)
            initCarComponents();
        else
            initEmployeeComponents();
    }

    private void initCarComponents() {
        carModelField = new JTextField();
        carModelField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        carPlateField = new PlateInputPanel(car.getPlate());
        carColorField = new JTextField();
        carColorField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        saveButton = new JButton("ذخیره تغییرات");
        saveButton.setBackground(new Color(0, 120, 215));
        saveButton.setForeground(Color.WHITE);

        cancelButton = new JButton("انصراف");
        cancelButton.setBackground(Color.GRAY);
        cancelButton.setForeground(Color.WHITE);
    }

    private void initEmployeeComponents() {
        personnelIdField = new JTextField();
        personnelIdField.setEditable(false);
        personnelIdField.setBackground(new Color(240, 240, 240));
        personnelIdField.setForeground(Color.GRAY);

        nameField = new JTextField();
        nameField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        phoneField = new JTextField();

        telegramField = new JTextField();

        saveButton = new JButton("ذخیره تغییرات");
        saveButton.setBackground(new Color(0, 120, 215));
        saveButton.setForeground(Color.WHITE);

        cancelButton = new JButton("انصراف");
        cancelButton.setBackground(Color.GRAY);
        cancelButton.setForeground(Color.WHITE);
    }

    private void initLayout() {
        if (mode == EditMode.CAR)
            initCarLayout();
        else
            initEmployeeLayout();
    }

    private void initCarLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        int row = 0;

        row = addRow(panel, gbc, row, "مدل ماشین:", carModelField);
        row = addRow(panel, gbc, row, "پلاک ماشین:", carPlateField);
        addRow(panel, gbc, row, "رنگ ماشین:", carColorField);

        add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initEmployeeLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        int row = 0;

        row = addRow(panel, gbc, row, "شماره پرسنلی:", personnelIdField);
        row = addRow(panel, gbc, row, "نام و نام خانوادگی:", nameField);
        row = addRow(panel, gbc, row, "شماره تماس:", phoneField);
        addRow(panel, gbc, row, "آیدی تلگرام:", telegramField);

        add(panel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initActions() {
        cancelButton.addActionListener(e -> dispose());
        if (mode == EditMode.CAR)
            initCarActions();
        else
            initEmployeeActions();
    }

    private void initCarActions() {
        saveButton.addActionListener(e -> saveChanges());
    }

    private void initEmployeeActions() {
        saveButton.addActionListener(e -> saveEmployeeChanges());
    }

    private void fillFields() {
        if (mode == EditMode.CAR)
            fillCarFields();
        else
            fillEmployeeFields();
    }

    private void fillCarFields() {
        carModelField.setText(car.getModel());
        carColorField.setText(car.getColor());
    }

    private void fillEmployeeFields() {
        personnelIdField.setText(String.valueOf(employee.getPersonnelId()));
        nameField.setText(employee.getName());
        phoneField.setText(employee.getPhone());
        telegramField.setText(employee.getTelegramId());
    }

    private int addRow(JPanel panel, GridBagConstraints gbc, int row,
                       String labelText, JComponent component) {

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, gbc);

        return row + 1;
    }

    private void saveChanges() {
        String model = carModelField.getText().strip();
        String plate = carPlateField.getPlate().strip();
        String color = carColorField.getText().strip();

        if (model.isEmpty() || plate.isEmpty() || color.isEmpty()) {
            JOptionPane.showMessageDialog(this, "لطفاً تمام فیلدها را پر کنید!");
            return;
        }

        String oldPlate = car.getPlate();
        car.setModel(model);
        car.setPlate(plate);
        car.setColor(color);

        try {
            DatabaseManager db = new DatabaseManager();
            db.updateCar(car, oldPlate);
            JOptionPane.showMessageDialog(this, "تغییرات با موفقیت ذخیره شد!");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطا در ذخیره تغییرات: " + ex.getMessage());
        }
    }

    private void saveEmployeeChanges() {
        String name = nameField.getText().strip();
        String phone = phoneField.getText().strip();
        String telegram = telegramField.getText().strip();

        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "نام و شماره تماس الزامی است!");
            return;
        }

        employee.setName(name);
        employee.setPhone(phone);
        employee.setTelegramId(telegram);

        try {
            DatabaseManager db = new DatabaseManager();
            db.updateEmployee(employee);
            JOptionPane.showMessageDialog(this, "تغییرات با موفقیت ذخیره شد!");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطا در ذخیره اطلاعات: " + ex.getMessage());
        }
    }
}