package com.car.rental.ui.frames;

import com.car.rental.config.FingerprintProperties;
import com.car.rental.config.ServiceLookup;
import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;
import com.car.rental.service.CarService;
import com.car.rental.service.FingerprintException;
import com.car.rental.service.FingerprintService;
import com.car.rental.service.RentalService;
import com.car.rental.service.VerificationResult;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Logger;

public class MainFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(MainFrame.class.getName());

    private final CarService carService;
    private final RentalService rentalService;
    private final FingerprintService fingerprintService;
    private final int fpTimeoutSeconds;

    private JComboBox<String> carCombo;
    private JTextField destinationField;
    private JLabel pickupStatusLabel;
    private JButton pickupAuthButton;
    private JButton pickupCancelButton;
    private JButton pickupConfirmButton;

    private JLabel returnStatusLabel;
    private JButton returnAuthButton;
    private JButton returnCancelButton;
    private JButton returnConfirmButton;

    private String pickupDeviceUserId;
    private LocalDateTime pickupDeviceTime;
    private Employee pickupEmployee;

    private String returnDeviceUserId;
    private LocalDateTime returnDeviceTime;
    private RentalRecord returnActiveRental;

    public MainFrame() {
        this(
                ServiceLookup.get(CarService.class),
                ServiceLookup.get(RentalService.class),
                ServiceLookup.get(FingerprintService.class),
                resolveTimeoutSeconds()
        );
    }

    public MainFrame(CarService carService, RentalService rentalService,
                     FingerprintService fingerprintService, int fpTimeoutSeconds) {
        this.carService = carService;
        this.rentalService = rentalService;
        this.fingerprintService = fingerprintService;
        this.fpTimeoutSeconds = fpTimeoutSeconds > 0 ? fpTimeoutSeconds : 40;
        createMainUI();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                releaseDevice();
            }
        });
    }

    private static int resolveTimeoutSeconds() {
        try {
            return ServiceLookup.get(FingerprintProperties.class).getVerifyTimeoutSeconds();
        } catch (Exception e) {
            return 40;
        }
    }

    private void createMainUI() {
        setTitle("سیستم مدیریت ماشین شرکت");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(640, 560);
        setLayout(new BorderLayout(10, 10));
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        getContentPane().setBackground(Color.WHITE);

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(createPickupPanel());
        mainPanel.add(createReturnPanel());

        add(mainPanel, BorderLayout.CENTER);
        add(createButtonsPanel(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createPickupPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("ثبت تحویل ماشین"));
        panel.setBackground(Color.WHITE);
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 12, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(label("ماشین:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        carCombo = new JComboBox<>();
        carCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        carCombo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        loadCars();
        panel.add(carCombo, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.anchor = GridBagConstraints.EAST;
        panel.add(label("مقصد:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        destinationField = new JTextField(18);
        destinationField.setFont(new Font("Arial", Font.PLAIN, 14));
        destinationField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.add(destinationField, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        pickupStatusLabel = new JLabel(" ");
        pickupStatusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        pickupStatusLabel.setForeground(new Color(80, 80, 80));
        panel.add(pickupStatusLabel, gbc);
        gbc.gridwidth = 1;
        row++;

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        btns.setBackground(Color.WHITE);

        pickupAuthButton = actionButton("احراز هویت با اثر انگشت", new Color(0, 120, 215));
        pickupCancelButton = actionButton("انصراف", Color.GRAY);
        pickupCancelButton.setEnabled(false);
        pickupConfirmButton = actionButton("ثبت تحویل", new Color(34, 139, 34));
        pickupConfirmButton.setEnabled(false);

        pickupAuthButton.addActionListener(e -> startPickupAuth());
        pickupCancelButton.addActionListener(e -> cancelPickupAuth());
        pickupConfirmButton.addActionListener(e -> confirmPickup());

        btns.add(pickupAuthButton);
        btns.add(pickupCancelButton);
        btns.add(pickupConfirmButton);
        panel.add(btns, gbc);

        return panel;
    }

    private JPanel createReturnPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("ثبت بازگشت ماشین"));
        panel.setBackground(Color.WHITE);
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 12, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        returnStatusLabel = new JLabel("برای بازگشت، اثر انگشت کارمند را احراز کنید");
        returnStatusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        returnStatusLabel.setForeground(new Color(80, 80, 80));
        panel.add(returnStatusLabel, gbc);

        gbc.gridy = 1;
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        btns.setBackground(Color.WHITE);

        returnAuthButton = actionButton("احراز هویت با اثر انگشت", new Color(0, 120, 215));
        returnCancelButton = actionButton("انصراف", Color.GRAY);
        returnCancelButton.setEnabled(false);
        returnConfirmButton = actionButton("ثبت بازگشت", new Color(34, 139, 34));
        returnConfirmButton.setEnabled(false);

        returnAuthButton.addActionListener(e -> startReturnAuth());
        returnCancelButton.addActionListener(e -> cancelReturnAuth());
        returnConfirmButton.addActionListener(e -> confirmReturn());

        btns.add(returnAuthButton);
        btns.add(returnCancelButton);
        btns.add(returnConfirmButton);
        panel.add(btns, gbc);

        return panel;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        buttonsPanel.setBackground(Color.WHITE);

        JButton addItemsButton = actionButton("اضافه کردن ماشین / کارمند", new Color(34, 139, 34));
        addItemsButton.addActionListener(e -> {
            dispose();
            new AddFrame();
        });

        JButton manageButton = actionButton("مدیریت ماشین‌ها و کارمندها", new Color(55, 65, 81));
        manageButton.addActionListener(e -> {
            dispose();
            new ManageFrame();
        });

        JButton reportButton = actionButton("نمایش گزارش", new Color(139, 69, 19));
        reportButton.addActionListener(e -> {
            dispose();
            new ReportFrame();
        });

        buttonsPanel.add(reportButton);
        buttonsPanel.add(manageButton);
        buttonsPanel.add(addItemsButton);
        return buttonsPanel;
    }

    private void startPickupAuth() {
        if (carCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "ابتدا ماشین را انتخاب کنید!");
            return;
        }
        String dest = destinationField.getText().strip();
        if (dest.isEmpty()) {
            JOptionPane.showMessageDialog(this, "مقصد را وارد کنید!");
            return;
        }

        resetPickupVerifiedState();
        setPickupListeningUi(true);
        pickupStatusLabel.setText("در انتظار اثر انگشت... انگشت را روی دستگاه بگذارید");
        pickupStatusLabel.setForeground(new Color(180, 120, 0));

        fingerprintService.listenForVerification(
                fpTimeoutSeconds,
                result -> SwingUtilities.invokeLater(() -> onPickupVerified(result)),
                () -> SwingUtilities.invokeLater(this::onPickupTimeout),
                err -> SwingUtilities.invokeLater(() -> onPickupError(err))
        );
    }

    private void onPickupVerified(VerificationResult result) {
        try {
            Employee emp = rentalService.findEmployeeByDeviceUserId(result.getDeviceUserId());
            if (emp == null) {
                pickupStatusLabel.setText("کارمند با شناسه " + result.getDeviceUserId() + " در سیستم ثبت نشده است");
                pickupStatusLabel.setForeground(Color.RED);
                setPickupListeningUi(false);
                return;
            }
            if (emp.isRenting()) {
                pickupStatusLabel.setText(emp.getName() + " در حال حاضر در مأموریت است");
                pickupStatusLabel.setForeground(Color.RED);
                setPickupListeningUi(false);
                return;
            }

            pickupDeviceUserId = result.getDeviceUserId();
            pickupDeviceTime = result.getDeviceTime();
            pickupEmployee = emp;

            pickupStatusLabel.setText("تأیید شد: " + emp.getName()
                    + " | زمان دستگاه: " + formatDeviceTime(pickupDeviceTime));
            pickupStatusLabel.setForeground(new Color(0, 128, 0));
            setPickupListeningUi(false);
            pickupConfirmButton.setEnabled(true);
        } catch (SQLException e) {
            onPickupError(new FingerprintException(e.getMessage(), e));
        }
    }

    private void onPickupTimeout() {
        pickupStatusLabel.setText("زمان احراز هویت تمام شد. دوباره تلاش کنید.");
        pickupStatusLabel.setForeground(Color.RED);
        setPickupListeningUi(false);
    }

    private void onPickupError(FingerprintException err) {
        logger.severe("Pickup FP error: " + err.getMessage());
        pickupStatusLabel.setText("خطا: " + err.getMessage());
        pickupStatusLabel.setForeground(Color.RED);
        setPickupListeningUi(false);
    }

    private void cancelPickupAuth() {
        fingerprintService.cancelListen();
        releaseDevice();
        resetPickupVerifiedState();
        pickupStatusLabel.setText("احراز هویت لغو شد");
        pickupStatusLabel.setForeground(new Color(80, 80, 80));
        setPickupListeningUi(false);
    }

    private void confirmPickup() {
        if (pickupDeviceUserId == null || pickupEmployee == null || pickupDeviceTime == null) {
            JOptionPane.showMessageDialog(this, "ابتدا احراز هویت را انجام دهید!");
            return;
        }
        if (carCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "ماشین را انتخاب کنید!");
            return;
        }
        String dest = destinationField.getText().strip();
        if (dest.isEmpty()) {
            JOptionPane.showMessageDialog(this, "مقصد را وارد کنید!");
            return;
        }

        try {
            String[] selectedCar = Objects.requireNonNull(carCombo.getSelectedItem()).toString().split(" - ");
            String carPlate = selectedCar[selectedCar.length - 1];
            String pickupTime = formatDeviceTime(pickupDeviceTime);

            rentalService.pickup(pickupDeviceUserId, carPlate, pickupTime, dest);

            JOptionPane.showMessageDialog(this,
                    "ماشین تحویل داده شد ✅\n" +
                            "کارمند: " + pickupEmployee.getName() + "\n" +
                            "زمان: " + pickupTime);

            destinationField.setText("");
            resetPickupVerifiedState();
            pickupStatusLabel.setText(" ");
            pickupConfirmButton.setEnabled(false);
            loadCars();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در ثبت تحویل: " + e.getMessage());
        }
    }

    private void setPickupListeningUi(boolean listening) {
        pickupAuthButton.setEnabled(!listening);
        pickupCancelButton.setEnabled(listening);
        if (listening) {
            pickupConfirmButton.setEnabled(false);
        }
        carCombo.setEnabled(!listening);
        destinationField.setEnabled(!listening);
    }

    private void resetPickupVerifiedState() {
        pickupDeviceUserId = null;
        pickupDeviceTime = null;
        pickupEmployee = null;
        pickupConfirmButton.setEnabled(false);
    }

    private void startReturnAuth() {
        resetReturnVerifiedState();
        setReturnListeningUi(true);
        returnStatusLabel.setText("در انتظار اثر انگشت... انگشت را روی دستگاه بگذارید");
        returnStatusLabel.setForeground(new Color(180, 120, 0));

        fingerprintService.listenForVerification(
                fpTimeoutSeconds,
                result -> SwingUtilities.invokeLater(() -> onReturnVerified(result)),
                () -> SwingUtilities.invokeLater(this::onReturnTimeout),
                err -> SwingUtilities.invokeLater(() -> onReturnError(err))
        );
    }

    private void onReturnVerified(VerificationResult result) {
        try {
            Employee emp = rentalService.findEmployeeByDeviceUserId(result.getDeviceUserId());
            if (emp == null) {
                returnStatusLabel.setText("کارمند با شناسه " + result.getDeviceUserId() + " در سیستم ثبت نشده است");
                returnStatusLabel.setForeground(Color.RED);
                setReturnListeningUi(false);
                return;
            }

            RentalRecord active = rentalService.getActiveRentalByDeviceUserId(result.getDeviceUserId());
            if (active == null) {
                returnStatusLabel.setText(emp.getName() + " اجاره فعالی ندارد");
                returnStatusLabel.setForeground(Color.RED);
                setReturnListeningUi(false);
                return;
            }

            returnDeviceUserId = result.getDeviceUserId();
            returnDeviceTime = result.getDeviceTime();
            returnActiveRental = active;

            returnStatusLabel.setText("تأیید شد: " + emp.getName()
                    + " | ماشین: " + active.carName + " (" + active.plate + ")"
                    + " | زمان: " + formatDeviceTime(returnDeviceTime));
            returnStatusLabel.setForeground(new Color(0, 128, 0));
            setReturnListeningUi(false);
            returnConfirmButton.setEnabled(true);
        } catch (SQLException e) {
            onReturnError(new FingerprintException(e.getMessage(), e));
        }
    }

    private void onReturnTimeout() {
        returnStatusLabel.setText("زمان احراز هویت تمام شد. دوباره تلاش کنید.");
        returnStatusLabel.setForeground(Color.RED);
        setReturnListeningUi(false);
    }

    private void onReturnError(FingerprintException err) {
        logger.severe("Return FP error: " + err.getMessage());
        returnStatusLabel.setText("خطا: " + err.getMessage());
        returnStatusLabel.setForeground(Color.RED);
        setReturnListeningUi(false);
    }

    private void cancelReturnAuth() {
        fingerprintService.cancelListen();
        releaseDevice();
        resetReturnVerifiedState();
        returnStatusLabel.setText("احراز هویت لغو شد");
        returnStatusLabel.setForeground(new Color(80, 80, 80));
        setReturnListeningUi(false);
    }

    private void confirmReturn() {
        if (returnDeviceUserId == null || returnDeviceTime == null || returnActiveRental == null) {
            JOptionPane.showMessageDialog(this, "ابتدا احراز هویت را انجام دهید!");
            return;
        }

        try {
            String returnTime = formatDeviceTime(returnDeviceTime);
            boolean ok = rentalService.returnCar(returnDeviceUserId, returnTime);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "بازگشت ثبت شد ✅\n" +
                                "کارمند: " + returnActiveRental.employeeName + "\n" +
                                "ماشین: " + returnActiveRental.carName + "\n" +
                                "زمان: " + returnTime);
                loadCars();
            } else {
                JOptionPane.showMessageDialog(this, "اجاره فعالی برای ثبت بازگشت یافت نشد!");
            }
            resetReturnVerifiedState();
            returnStatusLabel.setText("برای بازگشت، اثر انگشت کارمند را احراز کنید");
            returnStatusLabel.setForeground(new Color(80, 80, 80));
            returnConfirmButton.setEnabled(false);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در ثبت بازگشت: " + e.getMessage());
        }
    }

    private void setReturnListeningUi(boolean listening) {
        returnAuthButton.setEnabled(!listening);
        returnCancelButton.setEnabled(listening);
        if (listening) {
            returnConfirmButton.setEnabled(false);
        }
    }

    private void resetReturnVerifiedState() {
        returnDeviceUserId = null;
        returnDeviceTime = null;
        returnActiveRental = null;
        returnConfirmButton.setEnabled(false);
    }

    /** Short-lived sessions: release device after each auth cycle or window close. */
    private void releaseDevice() {
        try {
            fingerprintService.cancelListen();
            fingerprintService.disconnect();
        } catch (Exception ignored) {
        }
    }

    public void loadCars() {
        carCombo.removeAllItems();
        try {
            for (Car car : carService.getAvailableCars()) {
                carCombo.addItem(car.getModel() + " - " + car.getColor() + " - " + car.getPlate());
            }
        } catch (SQLException e) {
            logger.severe("خطا در خواندن ماشین‌ها: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "خطا در خواندن ماشین‌ها: " + e.getMessage());
        }
    }

    private static String formatDeviceTime(LocalDateTime time) {
        if (time == null) return "";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 14));
        return l;
    }

    private static JButton actionButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    public static void main(String[] args) {
        com.car.rental.SamanApplication.main(args);
    }
}
