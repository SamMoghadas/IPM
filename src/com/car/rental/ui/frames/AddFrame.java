package com.car.rental.ui.frames;

import com.car.rental.config.ServiceLookup;
import com.car.rental.service.CarService;
import com.car.rental.service.EmployeeService;
import com.car.rental.ui.components.PlateInputPanel;
import com.car.rental.util.InputValidators;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class AddFrame extends JFrame {

    /** Shared primary action color for "create/save" buttons on this form. */
    private static final Color ACTION_GREEN = new Color(34, 139, 34);

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

    private final CarService carService;
    private final EmployeeService employeeService;

    private JTextField nameField;
    private JTextField phoneField;
    private JComboBox<String> fingerCombo;
    private JButton enrollButton;
    private JLabel employeeStatusLabel;

    public AddFrame() {
        this.carService = ServiceLookup.get(CarService.class);
        this.employeeService = ServiceLookup.get(EmployeeService.class);

        setTitle("اضافه کردن ماشین و کارمند");
        setSize(640, 600);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        getContentPane().setBackground(Color.WHITE);

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

        JPanel carFormPanel = createTitledPanel("اضافه کردن ماشین");
        JTextField modelField = createField(true);
        PlateInputPanel plateInputPanel = new PlateInputPanel();
        JTextField colorField = createField(true);
        JButton addCarButton = createActionButton("ذخیره ماشین", ACTION_GREEN);

        addFormRow(carFormPanel, "نام ماشین:", modelField);
        addFormRow(carFormPanel, "پلاک ماشین:", plateInputPanel);
        addFormRow(carFormPanel, "رنگ ماشین:", colorField);
        addFormRow(carFormPanel, "", addCarButton);
        add(carFormPanel);

        addCarButton.addActionListener(e -> {
            String name = modelField.getText().strip();
            String plate = plateInputPanel.getPlate();
            String color = colorField.getText().strip();
            try {
                carService.addCar(name, plate, color);
                JOptionPane.showMessageDialog(this,
                        "ماشین ثبت شد ✅\n" +
                                "نام: " + name + "\n" +
                                "رنگ: " + color + "\n" +
                                "پلاک: " + plate);
                modelField.setText("");
                plateInputPanel.clear();
                colorField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "خطا در اضافه کردن ماشین: " + ex.getMessage());
            }
        });

        JPanel employeeFormPanel = createTitledPanel("اضافه کردن کارمند");

        nameField = createField(false);
        nameField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        nameField.setHorizontalAlignment(JTextField.LEFT);
        nameField.setToolTipText("نام کامل را انگلیسی وارد کنید (دستگاه از فارسی پشتیبانی نمی‌کند)");

        phoneField = createField(false);
        phoneField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        phoneField.setHorizontalAlignment(JTextField.LEFT);

        fingerCombo = new JComboBox<>(FINGER_LABELS);
        fingerCombo.setSelectedIndex(6);
        fingerCombo.setPreferredSize(new Dimension(280, 28));

        enrollButton = createActionButton("ثبت اثر انگشت و ذخیره کارمند", ACTION_GREEN);
        employeeStatusLabel = new JLabel(" ");
        employeeStatusLabel.setForeground(new Color(80, 80, 80));

        addFormRow(employeeFormPanel, "نام کامل (انگلیسی):", nameField);
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
        String name = nameField.getText().strip();
        String phone = phoneField.getText().strip();
        int fingerIndex = fingerCombo.getSelectedIndex();

        String nameError = InputValidators.validateEnglishFullName(name);
        if (nameError != null) {
            JOptionPane.showMessageDialog(this, nameError);
            return;
        }

        setEmployeeFormEnabled(false);
        employeeStatusLabel.setForeground(new Color(180, 120, 0));
        employeeStatusLabel.setText("در حال ثبت روی دستگاه و دیتابیس...");

        new SwingWorker<String, String>() {
            @Override
            protected String doInBackground() throws Exception {
                publish("انگشت را روی سنسور بگذارید...");
                String deviceUserId = employeeService.getNextDeviceUserId();
                employeeService.registerWithFingerprint(
                        deviceUserId, name, phone.isEmpty() ? null : phone, fingerIndex);
                return deviceUserId;
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
                    String deviceUserId = get();
                    employeeStatusLabel.setForeground(new Color(0, 128, 0));
                    employeeStatusLabel.setText("کارمند روی دستگاه و در سیستم ثبت شد");
                    JOptionPane.showMessageDialog(AddFrame.this,
                            "کارمند ثبت شد ✅\n" +
                                    "نام: " + name + "\n" +
                                    "شناسه کارمند: " + deviceUserId);
                    nameField.setText("");
                    phoneField.setText("");
                    fingerCombo.setSelectedIndex(6);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage() != null ? cause.getMessage() : cause.toString();
                    employeeStatusLabel.setForeground(Color.RED);
                    employeeStatusLabel.setText("ثبت ناموفق");
                    JOptionPane.showMessageDialog(AddFrame.this,
                            "ثبت کارمند ناموفق بود:\n" + msg,
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
        label.setPreferredSize(new Dimension(160, 25));
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
