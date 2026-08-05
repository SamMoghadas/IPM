package com.car.rental.ui.frames;

import com.car.rental.db.DatabaseManager;
import com.car.rental.service.FingerprintException;
import com.car.rental.service.ZkFingerprintService;
import com.car.rental.ui.components.PlateInputPanel;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.SQLException;

public class AddFrame extends JFrame {

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
        setTitle("اضافه کردن ماشین و کارمند");
        setSize(640, 620);
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

        // Header
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
        nameField = createField(true);
        phoneField = createField(false);
        fingerCombo = new JComboBox<>(FINGER_LABELS);
        fingerCombo.setSelectedIndex(6); // default: right index finger
        fingerCombo.setPreferredSize(new Dimension(260, 28));

        enrollButton = createActionButton("ثبت اثر انگشت و ذخیره کارمند", new Color(0, 120, 215));
        employeeStatusLabel = new JLabel(" ");
        employeeStatusLabel.setForeground(new Color(80, 80, 80));

        addFormRow(employeeFormPanel, "شناسه دستگاه:", deviceUserIdField);
        addFormRow(employeeFormPanel, "نام و نام خانوادگی:", nameField);
        addFormRow(employeeFormPanel, "شماره تماس:", phoneField);
        addFormRow(employeeFormPanel, "انگشت:", fingerCombo);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        statusRow.setBackground(Color.WHITE);
        statusRow.add(employeeStatusLabel);
        employeeFormPanel.add(statusRow);

        addFormRow(employeeFormPanel, "", enrollButton);
        add(employeeFormPanel);

        enrollButton.addActionListener(e -> registerEmployeeWithFingerprint());

        setVisible(true);
    }

    private void registerEmployeeWithFingerprint() {
        String deviceUserId = deviceUserIdField.getText().strip();
        String name = nameField.getText().strip();
        String phone = phoneField.getText().strip();
        int fingerIndex = fingerCombo.getSelectedIndex();

        if (deviceUserId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "شناسه دستگاه را وارد کنید!");
            return;
        }
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "نام را وارد کنید!");
            return;
        }

        try {
            if (db.isDeviceUserIdExists(deviceUserId)) {
                JOptionPane.showMessageDialog(this, "این شناسه دستگاه قبلاً در نرم‌افزار ثبت شده است!");
                return;
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "خطا در بررسی دیتابیس: " + ex.getMessage());
            return;
        }

        setEmployeeFormEnabled(false);
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("اتصال به دستگاه و ثبت کاربر... سپس انگشت را روی سنسور بگذارید");

        // Heavy device work off the EDT
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("در حال اتصال به دستگاه اثر انگشت...");
                fingerprintService.connect();

                publish("ثبت کاربر روی دستگاه و دریافت اثر انگشت (حدود ۲۵ ثانیه)...");
                // createUser + enroll; on failure rolls back device user
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
                    get(); // rethrow device errors

                    // Only after successful device registration
                    db.addEmployee(deviceUserId, name, phone.isEmpty() ? null : phone);

                    employeeStatusLabel.setForeground(new Color(0, 128, 0));
                    employeeStatusLabel.setText("کارمند با موفقیت روی دستگاه و در سیستم ثبت شد");
                    JOptionPane.showMessageDialog(AddFrame.this,
                            "کارمند ثبت شد ✅\nشناسه: " + deviceUserId + "\nنام: " + name);

                    deviceUserIdField.setText("");
                    nameField.setText("");
                    phoneField.setText("");
                    fingerCombo.setSelectedIndex(6);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    employeeStatusLabel.setForeground(Color.RED);
                    employeeStatusLabel.setText("ثبت ناموفق — کاربر روی دستگاه در صورت ایجاد حذف شد");
                    JOptionPane.showMessageDialog(AddFrame.this,
                            "ثبت کارمند ناموفق بود:\n" + msg +
                                    "\n\nاگر کاربر روی دستگاه ساخته شده بود، تلاش شد حذف شود.",
                            "خطا",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    setEmployeeFormEnabled(true);
                    try {
                        // keep session reusable; disconnect on window close
                        if (fingerprintService.isConnected()) {
                            // leave connected for next add, or disconnect to free device:
                            fingerprintService.disconnect();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }.execute();
    }

    private void setEmployeeFormEnabled(boolean enabled) {
        deviceUserIdField.setEnabled(enabled);
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
        label.setPreferredSize(new Dimension(130, 25));
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
