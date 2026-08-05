package com.car.rental.ui.frames;

import com.car.rental.model.Car;
import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import com.car.rental.service.ZkFingerprintService;
import com.car.rental.ui.components.PlateInputPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
    private final DatabaseManager db = new DatabaseManager();
    private ZkFingerprintService fingerprintService;

    private JTextField carModelField;
    private PlateInputPanel carPlateField;
    private JTextField carColorField;

    private JTextField deviceUserIdField;
    private JTextField nameField;
    private JTextField phoneField;
    private JComboBox<String> fingerCombo;
    private JButton reEnrollButton;
    private JLabel employeeStatusLabel;

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
        this.fingerprintService = new ZkFingerprintService("192.168.1.200", 4370);
        initFrame();
        initComponents();
        initLayout();
        initActions();
        fillFields();
        loadEnrolledFingerFromDevice();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                try {
                    if (fingerprintService != null) {
                        fingerprintService.disconnect();
                    }
                } catch (Exception ignored) {
                }
            }
        });
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
        // temporary default until device reports enrolled finger
        fingerCombo.setSelectedIndex(6);

        reEnrollButton = new JButton("ثبت مجدد اثر انگشت");
        reEnrollButton.setBackground(new Color(180, 100, 0));
        reEnrollButton.setForeground(Color.WHITE);

        employeeStatusLabel = new JLabel(" ");
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
        panel.add(reEnrollButton, gbc);

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
            reEnrollButton.addActionListener(e -> reEnrollFingerprint());
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

    /** Query device for which fingers are enrolled and select the first in the combo. */
    private void loadEnrolledFingerFromDevice() {
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("در حال خواندن انگشت ثبت‌شده از دستگاه...");
        fingerCombo.setEnabled(false);

        new SwingWorker<List<Integer>, Void>() {
            @Override
            protected List<Integer> doInBackground() throws Exception {
                fingerprintService.connect();
                return fingerprintService.getEnrolledFingerIndexes(employee.getDeviceUserId());
            }

            @Override
            protected void done() {
                try {
                    List<Integer> fingers = get();
                    if (fingers != null && !fingers.isEmpty()) {
                        int primary = fingers.get(0);
                        if (primary >= 0 && primary <= 9) {
                            fingerCombo.setSelectedIndex(primary);
                        }
                        if (fingers.size() == 1) {
                            employeeStatusLabel.setForeground(new Color(0, 128, 0));
                            employeeStatusLabel.setText("انگشت فعلی روی دستگاه: " + FINGER_LABELS[primary]);
                        } else {
                            employeeStatusLabel.setForeground(new Color(0, 128, 0));
                            employeeStatusLabel.setText(
                                    "انگشت‌های ثبت‌شده: " + fingers + " — انتخاب‌شده: " + primary);
                        }
                    } else {
                        employeeStatusLabel.setForeground(new Color(180, 120, 0));
                        employeeStatusLabel.setText(
                                "انگشت ثبت‌شده‌ای روی دستگاه پیدا نشد؛ مقدار پیش‌فرض را می‌توانید عوض کنید");
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    employeeStatusLabel.setForeground(Color.RED);
                    employeeStatusLabel.setText("خواندن انگشت از دستگاه ناموفق — پیش‌فرض استفاده شد");
                    // keep default index; optional soft log
                    System.err.println("loadEnrolledFingerFromDevice: " + msg);
                } finally {
                    fingerCombo.setEnabled(true);
                    try {
                        if (fingerprintService != null && fingerprintService.isConnected()) {
                            fingerprintService.disconnect();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }.execute();
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

        if (model.isEmpty() || plate.isEmpty() || color.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "لطفاً تمام فیلدها را پر کنید!\n" +
                            "اگر پلاک خالی است، دوباره آن را با پنل پلاک وارد کنید.");
            return;
        }

        String oldPlate = car.getPlate();
        try {
            if (db.isPlateTaken(plate, oldPlate)) {
                JOptionPane.showMessageDialog(this, "این پلاک قبلاً برای ماشین دیگری ثبت شده است!");
                return;
            }

            car.setModel(model);
            car.setPlate(plate);
            car.setColor(color);
            db.updateCar(car, oldPlate);
            JOptionPane.showMessageDialog(this, "تغییرات ماشین ذخیره شد!");
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطا در ذخیره تغییرات: " + ex.getMessage());
        }
    }

    private void saveEmployeeChanges() {
        String name = nameField.getText().strip();
        String phone = phoneField.getText().strip();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "نام الزامی است!");
            return;
        }
        if (!name.matches(".*[A-Za-z].*")) {
            JOptionPane.showMessageDialog(this,
                    "نام باید انگلیسی باشد (حداقل یک حرف لاتین).\n" +
                            "دستگاه نام فارسی را درست ذخیره نمی‌کند.");
            return;
        }

        String deviceUserId = employee.getDeviceUserId();
        setEmployeeFormEnabled(false);
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("در حال به‌روزرسانی روی دستگاه...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                fingerprintService.connect();
                fingerprintService.updateUserName(deviceUserId, name);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    employee.setName(name);
                    employee.setPhone(phone);
                    db.updateEmployee(employee);

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
                            "به‌روزرسانی ناموفق بود (دیتابیس تغییر نکرد):\n" + msg,
                            "خطا",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setEmployeeFormEnabled(true);
                    try {
                        if (fingerprintService != null && fingerprintService.isConnected()) {
                            fingerprintService.disconnect();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }.execute();
    }

    private void reEnrollFingerprint() {
        String name = nameField.getText().strip();
        if (name.isEmpty() || !name.matches(".*[A-Za-z].*")) {
            JOptionPane.showMessageDialog(this, "ابتدا نام انگلیسی معتبر وارد کنید!");
            return;
        }

        int fingerIndex = fingerCombo.getSelectedIndex();
        String deviceUserId = employee.getDeviceUserId();

        setEmployeeFormEnabled(false);
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("انگشت را روی سنسور بگذارید...");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("اتصال به دستگاه...");
                fingerprintService.connect();
                publish("ثبت اثر انگشت — انگشت را چند بار روی سنسور بگذارید");
                fingerprintService.registerUserWithFingerprint(deviceUserId, name, fingerIndex);
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
                    employee.setName(name);
                    employee.setPhone(phoneField.getText().strip());
                    db.updateEmployee(employee);

                    employeeStatusLabel.setForeground(new Color(0, 128, 0));
                    employeeStatusLabel.setText("اثر انگشت با موفقیت ثبت شد — انگشت: "
                            + FINGER_LABELS[fingerIndex]);
                    JOptionPane.showMessageDialog(EditFrame.this, "اثر انگشت به‌روز شد ✅");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    employeeStatusLabel.setForeground(Color.RED);
                    employeeStatusLabel.setText("ثبت اثر انگشت ناموفق");
                    JOptionPane.showMessageDialog(EditFrame.this,
                            "ثبت مجدد اثر انگشت ناموفق بود:\n" + msg,
                            "خطا",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setEmployeeFormEnabled(true);
                    try {
                        if (fingerprintService != null && fingerprintService.isConnected()) {
                            fingerprintService.disconnect();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }.execute();
    }

    private void setEmployeeFormEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        fingerCombo.setEnabled(enabled);
        reEnrollButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }
}
