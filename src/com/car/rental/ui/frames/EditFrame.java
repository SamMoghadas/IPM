package com.car.rental.ui.frames;

import com.car.rental.config.SpringContext;
import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.service.CarService;
import com.car.rental.service.EmployeeService;
import com.car.rental.service.FingerprintService;
import com.car.rental.service.ZkFingerprintService;
import com.car.rental.ui.components.PlateInputPanel;

import javax.swing.*;
import java.awt.*;

public class EditFrame extends JFrame {
    public enum EditMode {
        CAR,
        EMPLOYEE
    }

    private static final String[] FINGER_LABELS = {
            "0 — انگشت کوچک دست چپ",
            "1 — انگشت حلقه دست چپ",
            "2 — انگشت وسط دست چپ",
            "3 — انگشت اشاره دست چپ",
            "4 — شست دست چپ",
            "5 — شست دست راست",
            "6 — انگشت اشاره دست راست",
            "7 — انگشت وسط دست راست",
            "8 — انگشت حلقه دست راست",
            "9 — انگشت کوچک دست راست"
    };

    private final EditMode mode;
    private final CarService carService;
    private final EmployeeService employeeService;

    private JTextField carModelField;
    private PlateInputPanel carPlateField;
    private JTextField carColorField;

    private JTextField deviceUserIdField;
    private JTextField nameField;
    private JTextField phoneField;
    private JComboBox<String> fingerCombo;
    private JButton addFingerButton;
    private JLabel employeeStatusLabel;

    private JButton saveButton;
    private JButton cancelButton;
    private Car car;
    private Employee employee;

    public EditFrame(Car car) {
        this.mode = EditMode.CAR;
        this.car = car;
        this.carService = resolveCarService();
        this.employeeService = resolveEmployeeService();
        initFrame();
        initComponents();
        initLayout();
        initActions();
        fillFields();
    }

    public EditFrame(Employee emp) {
        this.mode = EditMode.EMPLOYEE;
        this.employee = emp;
        this.carService = resolveCarService();
        this.employeeService = resolveEmployeeService();
        initFrame();
        initComponents();
        initLayout();
        initActions();
        fillFields();
    }

    private static CarService resolveCarService() {
        if (SpringContext.isActive()) {
            try {
                return SpringContext.getBean(CarService.class);
            } catch (Exception ignored) {
            }
        }
        return new CarService(new DatabaseManager());
    }

    private static EmployeeService resolveEmployeeService() {
        if (SpringContext.isActive()) {
            try {
                return SpringContext.getBean(EmployeeService.class);
            } catch (Exception ignored) {
            }
        }
        FingerprintService fp = new ZkFingerprintService("192.168.1.200", 4370);
        return new EmployeeService(new DatabaseManager(), fp);
    }

    private void initFrame() {
        if (mode == EditMode.CAR) {
            setTitle("ویرایش ماشین");
            setSize(420, 320);
        } else {
            setTitle("ویرایش کارمند");
            setSize(480, 420);
        }
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
        deviceUserIdField = new JTextField();
        deviceUserIdField.setEditable(false);
        deviceUserIdField.setBackground(new Color(240, 240, 240));
        deviceUserIdField.setForeground(Color.GRAY);
        deviceUserIdField.setToolTipText("شناسه دستگاه ثابت است و قابل تغییر نیست");

        nameField = new JTextField();
        nameField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        nameField.setHorizontalAlignment(JTextField.LEFT);
        nameField.setToolTipText("نام را انگلیسی وارد کنید (دستگاه از فارسی پشتیبانی نمی‌کند)");

        phoneField = new JTextField();
        phoneField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        phoneField.setHorizontalAlignment(JTextField.LEFT);

        fingerCombo = new JComboBox<>(FINGER_LABELS);
        fingerCombo.setSelectedIndex(6);
        fingerCombo.setToolTipText("انگشتی که می‌خواهید به پروفایل اضافه شود را انتخاب کنید");

        addFingerButton = new JButton("افزودن اثر انگشت");
        addFingerButton.setBackground(new Color(180, 100, 0));
        addFingerButton.setForeground(Color.WHITE);

        employeeStatusLabel = new JLabel("انگشت موردنظر را انتخاب و «افزودن اثر انگشت» را بزنید");
        employeeStatusLabel.setForeground(new Color(80, 80, 80));

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
        gbc.insets = new Insets(8, 10, 8, 10);
        int row = 0;
        row = addRow(panel, gbc, row, "شناسه دستگاه:", deviceUserIdField);
        row = addRow(panel, gbc, row, "نام (انگلیسی):", nameField);
        row = addRow(panel, gbc, row, "شماره تماس:", phoneField);
        row = addRow(panel, gbc, row, "انگشت:", fingerCombo);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(employeeStatusLabel, gbc);
        row++;
        gbc.gridy = row;
        panel.add(addFingerButton, gbc);
        add(panel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initActions() {
        cancelButton.addActionListener(e -> dispose());
        if (mode == EditMode.CAR) {
            saveButton.addActionListener(e -> saveCarChanges());
        } else {
            saveButton.addActionListener(e -> saveEmployeeChanges());
            addFingerButton.addActionListener(e -> addFingerprint());
        }
    }

    private void fillFields() {
        if (mode == EditMode.CAR) {
            carModelField.setText(car.getModel() != null ? car.getModel() : "");
            carColorField.setText(car.getColor() != null ? car.getColor() : "");
        } else {
            deviceUserIdField.setText(employee.getDeviceUserId());
            nameField.setText(employee.getName() != null ? employee.getName() : "");
            phoneField.setText(employee.getPhone() != null ? employee.getPhone() : "");
        }
    }

    private int addRow(JPanel panel, GridBagConstraints gbc, int row,
                       String labelText, JComponent component) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
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

    private void saveCarChanges() {
        String model = carModelField.getText().strip();
        String plate = carPlateField.getPlate().strip();
        String color = carColorField.getText().strip();
        String oldPlate = car.getPlate();
        try {
            car.setModel(model);
            car.setPlate(plate);
            car.setColor(color);
            carService.updateCar(car, oldPlate);
            JOptionPane.showMessageDialog(this, "تغییرات ماشین ذخیره شد!");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطا در ذخیره تغییرات: " + ex.getMessage());
        }
    }

    private void saveEmployeeChanges() {
        String name = nameField.getText().strip();
        String phone = phoneField.getText().strip();

        setEmployeeFormEnabled(false);
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("در حال به‌روزرسانی...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                employee.setName(name);
                employee.setPhone(phone);
                employeeService.updateEmployee(employee);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    employeeStatusLabel.setForeground(new Color(0, 128, 0));
                    employeeStatusLabel.setText("نام روی دستگاه و در سیستم به‌روز شد");
                    JOptionPane.showMessageDialog(EditFrame.this, "تغییرات کارمند ذخیره شد!");
                    dispose();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    employeeStatusLabel.setForeground(Color.RED);
                    employeeStatusLabel.setText("ذخیره ناموفق");
                    JOptionPane.showMessageDialog(EditFrame.this,
                            "به‌روزرسانی ناموفق بود:\n" + msg,
                            "خطا",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setEmployeeFormEnabled(true);
                }
            }
        }.execute();
    }

    private void addFingerprint() {
        int fingerIndex = fingerCombo.getSelectedIndex();
        String deviceUserId = employee.getDeviceUserId();

        setEmployeeFormEnabled(false);
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("انگشت را روی سنسور بگذارید...");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("ثبت اثر انگشت...");
                employeeService.addFingerprint(deviceUserId, fingerIndex);
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    employeeStatusLabel.setText(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    employeeStatusLabel.setForeground(new Color(0, 128, 0));
                    employeeStatusLabel.setText("اثر انگشت اضافه شد: " + FINGER_LABELS[fingerIndex]);
                    JOptionPane.showMessageDialog(EditFrame.this,
                            "اثر انگشت به پروفایل اضافه شد ✅\n" + FINGER_LABELS[fingerIndex]);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    employeeStatusLabel.setForeground(Color.RED);
                    employeeStatusLabel.setText("افزودن اثر انگشت ناموفق");
                    JOptionPane.showMessageDialog(EditFrame.this,
                            "افزودن اثر انگشت ناموفق بود:\n" + msg,
                            "خطا",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setEmployeeFormEnabled(true);
                }
            }
        }.execute();
    }

    private void setEmployeeFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        fingerCombo.setEnabled(enabled);
        addFingerButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }
}
