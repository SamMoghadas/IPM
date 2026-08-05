package com.car.rental.ui.frames;

import com.car.rental.db.DatabaseManager;
import com.car.rental.service.ZkFingerprintService;
import com.car.rental.ui.components.PlateInputPanel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.SQLException;

public class AddFrame extends JFrame {

    private static final String[] FINGER_LABELS = {
            "0 — Left little",
            "1 — Left ring",
            "2 — Left middle",
            "3 — Left index",
            "4 — Left thumb",
            "5 — Right thumb",
            "6 — Right index",
            "7 — Right middle",
            "8 — Right ring",
            "9 — Right little"
    };

    private final DatabaseManager db = new DatabaseManager();
    private final ZkFingerprintService fingerprintService =
            new ZkFingerprintService("192.168.1.200", 4370);

    private JTextField deviceUserIdField;
    private JTextField nameField;
    private JTextField phoneField;
    private JComboBox<String> fingerCombo;
    private JButton enrollButton;
    private JLabel employeeStatusLabel;

    public AddFrame() {
        db.initDatabase();

        setTitle("اضافه کردن ماشین و کارمند");
        setSize(640, 640);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().setBackground(Color.WHITE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                try {
                    fingerprintService.disconnect();
                } catch (Exception ignored) {
                }
            }
        });

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        headerPanel.setBackground(Color.WHITE);
        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setBackground(new Color(230, 230, 230));
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> {
            dispose();
            new MainFrame();
        });
        headerPanel.add(backButton);
        add(headerPanel);

        // ---- Car form ----
        JPanel carFormPanel = createTitledPanel("اضافه کردن ماشین");
        JTextField modelField = createField(true);
        PlateInputPanel plateInputPanel = new PlateInputPanel();
        JTextField colorField = createField(true);
        JButton addCarButton = createActionButton("اضافه کردن ماشین", new Color(34, 139, 34));

        addFormRow(carFormPanel, "نام ماشین:", modelField);
        addFormRow(carFormPanel, "پلاک ماشین:", plateInputPanel);
        addFormRow(carFormPanel, "رنگ ماشین:", colorField);
        addFormRow(carFormPanel, "", addCarButton);
        add(carFormPanel);

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

        // ---- Employee form ----
        JPanel employeeFormPanel = createTitledPanel("اضافه کردن کارمند");

        deviceUserIdField = createField(false);
        deviceUserIdField.setEditable(false);
        deviceUserIdField.setBackground(new Color(240, 240, 240));
        deviceUserIdField.setToolTipText("شناسه به‌صورت خودکار از ۱۰۰۱ به بعد تولید می‌شود");

        // English name – LTR for device compatibility
        nameField = createField(false);
        nameField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        nameField.setHorizontalAlignment(JTextField.LEFT);
        nameField.setToolTipText("Enter name in English (device does not support Persian names)");

        phoneField = createField(false);
        phoneField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        phoneField.setHorizontalAlignment(JTextField.LEFT);

        fingerCombo = new JComboBox<>(FINGER_LABELS);
        fingerCombo.setSelectedIndex(6);
        fingerCombo.setPreferredSize(new Dimension(260, 28));

        enrollButton = createActionButton("ثبت اثر انگشت و ذخیره کارمند", new Color(0, 120, 215));
        employeeStatusLabel = new JLabel("نام را انگلیسی وارد کنید");
        employeeStatusLabel.setForeground(new Color(80, 80, 80));

        addFormRow(employeeFormPanel, "شناسه دستگاه:", deviceUserIdField);
        addFormRow(employeeFormPanel, "Name (English):", nameField);
        addFormRow(employeeFormPanel, "Phone:", phoneField);
        addFormRow(employeeFormPanel, "Finger:", fingerCombo);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        statusRow.setBackground(Color.WHITE);
        statusRow.add(employeeStatusLabel);
        employeeFormPanel.add(statusRow);

        addFormRow(employeeFormPanel, "", enrollButton);
        add(employeeFormPanel);

        enrollButton.addActionListener(e -> registerEmployeeWithFingerprint());

        refreshNextDeviceUserId();
        setVisible(true);
    }

    private void refreshNextDeviceUserId() {
        try {
            deviceUserIdField.setText(db.getNextDeviceUserId());
        } catch (SQLException ex) {
            deviceUserIdField.setText("");
            JOptionPane.showMessageDialog(this, "خطا در تولید شناسه: " + ex.getMessage());
        }
    }

    private void registerEmployeeWithFingerprint() {
        String deviceUserId = deviceUserIdField.getText().strip();
        String name = nameField.getText().strip();
        String phone = phoneField.getText().strip();
        int fingerIndex = fingerCombo.getSelectedIndex();

        if (deviceUserId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "شناسه دستگاه خالی است!");
            return;
        }
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter employee name in English!");
            return;
        }
        // Basic check: discourage pure Persian-only input without Latin letters
        if (!name.matches(".*[A-Za-z].*")) {
            JOptionPane.showMessageDialog(this,
                    "Name must be in English (at least one Latin letter).\n" +
                            "Device cannot store Persian names correctly.");
            return;
        }

        try {
            if (db.isDeviceUserIdExists(deviceUserId)) {
                JOptionPane.showMessageDialog(this, "این شناسه قبلاً ثبت شده؛ شناسه تازه تولید می‌شود.");
                refreshNextDeviceUserId();
                return;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "خطا در بررسی دیتابیس: " + ex.getMessage());
            return;
        }

        setEmployeeFormEnabled(false);
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("Connecting to device...");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Connecting to fingerprint device...");
                fingerprintService.connect();

                publish("Place finger on sensor when device asks (waiting for device event)...");
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

                    db.addEmployee(deviceUserId, name, phone.isEmpty() ? null : phone);

                    employeeStatusLabel.setForeground(new Color(0, 128, 0));
                    employeeStatusLabel.setText("Employee saved on device and in database");
                    JOptionPane.showMessageDialog(AddFrame.this,
                            "Employee registered ✅\nID: " + deviceUserId + "\nName: " + name);

                    nameField.setText("");
                    phoneField.setText("");
                    fingerCombo.setSelectedIndex(6);
                    refreshNextDeviceUserId();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    employeeStatusLabel.setForeground(Color.RED);
                    employeeStatusLabel.setText("Failed — device user rolled back if created");
                    JOptionPane.showMessageDialog(AddFrame.this,
                            "Registration failed:\n" + msg,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setEmployeeFormEnabled(true);
                    try {
                        if (fingerprintService.isConnected()) {
                            fingerprintService.disconnect();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }.execute();
    }

    private void setEmployeeFormEnabled(boolean enabled) {
        // deviceUserId stays non-editable always
        nameField.setEnabled(enabled);
        phoneField.setEnabled(enabled);
        fingerCombo.setEnabled(enabled);
        enrollButton.setEnabled(enabled);
    }

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
        label.setPreferredSize(new Dimension(140, 25));
        row.add(field);
        row.add(label);

        parent.add(row);
    }

    private JTextField createField(boolean RTL) {
        JTextField field = new JTextField(20);
        if (RTL) {
            field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        } else {
            field.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            field.setHorizontalAlignment(JTextField.LEFT);
        }
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
