package com.car.rental.ui.frames;

import com.car.rental.db.DatabaseManager;
import com.car.rental.model.Employee;
import com.car.rental.model.RentalRecord;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Logger;

public class MainFrame extends JFrame {
    private final DatabaseManager db;
    private JComboBox<String> carCombo;
    private JComboBox<String> employeeCombo;
    private JTextField pickupTimeField, destinationField;
    private JTextField confirmCodeField, returnTimeField;
    private static final Logger logger = Logger.getLogger(MainFrame.class.getName());

    public MainFrame() {
        db = new DatabaseManager();
        db.initDatabase();
        createMainUI();
    }

    private void createMainUI() {
        setTitle("سیستم مدیریت ماشین شرکت");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 500);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(245, 245, 250));
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        getContentPane().setBackground(Color.WHITE);

        JPanel pickupPanel = createPickupPanel();
        JPanel returnPanel = createReturnPanel();
        JPanel buttonsPanel = createButtonsPanel();

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(pickupPanel);
        mainPanel.add(returnPanel);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createPickupPanel() {
        JPanel pickupPanel = new JPanel(new GridBagLayout());
        pickupPanel.setBorder(BorderFactory.createTitledBorder("ثبت تحویل ماشین"));
        pickupPanel.setBackground(Color.WHITE);
        pickupPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel carLabel = new JLabel("ماشین:");
        carLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(carLabel, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        carCombo = new JComboBox<>();
        carCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        carCombo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        carCombo.setRenderer(renderer);
        loadCars();
        pickupPanel.add(carCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel employeeLabel = new JLabel("کارمند:");
        employeeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(employeeLabel, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        employeeCombo = new JComboBox<>();
        employeeCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        employeeCombo.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        employeeCombo.setRenderer(renderer);
        loadEmployees();
        pickupPanel.add(employeeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel pickupTimeLabel = new JLabel("زمان تحویل:");
        pickupTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(pickupTimeLabel, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel pickupTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pickupTimePanel.setBackground(Color.WHITE);
        pickupTimePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pickupTimeField = new JTextField(15);
        pickupTimeField.setFont(new Font("Arial", Font.PLAIN, 14));
        pickupTimeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton nowPickupButton = new JButton("الان");
        nowPickupButton.setBackground(new Color(230, 230, 230));
        nowPickupButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nowPickupButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        nowPickupButton.addActionListener(e -> pickupTimeField.setText(getDate()));
        pickupTimePanel.add(pickupTimeField);
        pickupTimePanel.add(nowPickupButton);
        pickupPanel.add(pickupTimePanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel destinationLabel = new JLabel("مقصد:");
        destinationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        pickupPanel.add(destinationLabel, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        destinationField = new JTextField(15);
        destinationField.setFont(new Font("Arial", Font.PLAIN, 14));
        destinationField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pickupPanel.add(destinationField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton pickupButton = new JButton("ثبت تحویل");
        pickupButton.setFont(new Font("Arial", Font.BOLD, 14));
        pickupButton.setBackground(new Color(0, 120, 215));
        pickupButton.setForeground(Color.WHITE);
        pickupButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pickupButton.addActionListener(e -> {
            pickupCar();
            pickupTimeField.setText("");
            destinationField.setText("");
        });
        pickupPanel.add(pickupButton, gbc);

        return pickupPanel;
    }

    private JPanel createReturnPanel() {
        JPanel returnPanel = new JPanel(new GridBagLayout());
        returnPanel.setBorder(BorderFactory.createTitledBorder("ثبت بازگشت ماشین"));
        returnPanel.setBackground(Color.WHITE);
        returnPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 15, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel confirmCodeLabel = new JLabel("کد تحویل:");
        confirmCodeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        returnPanel.add(confirmCodeLabel, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        confirmCodeField = new JTextField(15);
        confirmCodeField.setFont(new Font("Arial", Font.PLAIN, 14));
        confirmCodeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        returnPanel.add(confirmCodeField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel returnTimeLabel = new JLabel("زمان بازگشت:");
        returnTimeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        returnPanel.add(returnTimeLabel, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel returnTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        returnTimePanel.setBackground(Color.WHITE);
        returnTimePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        returnTimeField = new JTextField(15);
        returnTimeField.setFont(new Font("Arial", Font.PLAIN, 14));
        returnTimeField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JButton nowReturnButton = new JButton("الان");
        nowReturnButton.setBackground(new Color(230, 230, 230));
        nowReturnButton.setFont(new Font("Arial", Font.PLAIN, 12));
        nowReturnButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        nowReturnButton.addActionListener(e -> returnTimeField.setText(getDate()));
        returnTimePanel.add(returnTimeField);
        returnTimePanel.add(nowReturnButton);
        returnPanel.add(returnTimePanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton returnButton = new JButton("ثبت بازگشت");
        returnButton.setFont(new Font("Arial", Font.BOLD, 14));
        returnButton.setBackground(new Color(0, 120, 215));
        returnButton.setForeground(Color.WHITE);
        returnButton.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        returnButton.addActionListener(e -> {
            returnCar();
            confirmCodeField.setText("");
            returnTimeField.setText("");
        });
        returnPanel.add(returnButton, gbc);

        return returnPanel;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        buttonsPanel.setBackground(Color.WHITE);

        JButton addItemsButton = new JButton("اضافه کردن ماشین / کارمند");
        addItemsButton.setFont(new Font("Arial", Font.BOLD, 14));
        addItemsButton.setBackground(new Color(34, 139, 34));
        addItemsButton.setForeground(Color.WHITE);
        addItemsButton.addActionListener(e -> {
            dispose();
            new AddFrame();
        });

        JButton manageButton = new JButton("مدیریت ماشین‌ها و کارمندها");
        manageButton.setFont(new Font("Arial", Font.BOLD, 14));
        manageButton.setBackground(new Color(55, 65, 81));
        manageButton.setForeground(Color.WHITE);
        manageButton.addActionListener(e -> {
            dispose();
            new ManageFrame();
        });

        JButton reportButton = new JButton("نمایش گزارش");
        reportButton.setFont(new Font("Arial", Font.BOLD, 14));
        reportButton.setBackground(new Color(139, 69, 19));
        reportButton.setForeground(Color.WHITE);
        reportButton.addActionListener(e -> {
            dispose();
            new ReportFrame();
        });

        buttonsPanel.add(reportButton);
        buttonsPanel.add(manageButton);
        buttonsPanel.add(addItemsButton);

        return buttonsPanel;
    }

    public void loadCars() {
        carCombo.removeAllItems();
        try {
            for (String car : db.getAvailableCars()) {
                carCombo.addItem(car);
            }
        } catch (SQLException e) {
            logger.severe("خطا در خواندن ماشین‌ها: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "خطا در خواندن ماشین‌ها: " + e.getMessage());
        }
    }

    public void loadEmployees() {
        employeeCombo.removeAllItems();
        try {
            List<Employee> employees = db.getAvailableEmployees();
            for (Employee emp : employees) {
                // Format: "Name (deviceUserId)" — deviceUserId is parsed on pickup
                employeeCombo.addItem(emp.getName() + " (" + emp.getDeviceUserId() + ")");
            }
        } catch (SQLException e) {
            logger.severe("خطا در خواندن کارمندها: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "خطا در خواندن کارمندها: " + e.getMessage());
        }
    }

    private int generateConfirmCode() {
        Random rand = new Random();
        return 10000 + rand.nextInt(90000);
    }

    private void pickupCar() {
        try {
            if (employeeCombo.getSelectedItem() == null || carCombo.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(null, "ماشین و کارمند را انتخاب کنید!");
                return;
            }

            int confirmCode = generateConfirmCode();
            while (db.isCCDuplicated(confirmCode)) {
                confirmCode = generateConfirmCode();
            }

            // Parse deviceUserId from "Name (deviceUserId)"
            String empItem = (String) employeeCombo.getSelectedItem();
            int open = empItem.lastIndexOf('(');
            int close = empItem.lastIndexOf(')');
            if (open < 0 || close < 0 || close <= open) {
                JOptionPane.showMessageDialog(null, "فرمت کارمند نامعتبر است!");
                return;
            }
            String deviceUserId = empItem.substring(open + 1, close).trim();

            String[] selectedCar = ((String) Objects.requireNonNull(carCombo.getSelectedItem())).split(" - ");
            String carPlate = selectedCar[2];

            String pickupTime = pickupTimeField.getText().strip();
            String destination = destinationField.getText().strip();

            if (pickupTime.isEmpty() || destination.isEmpty()) {
                JOptionPane.showMessageDialog(null, "تمام فیلدها باید پر شوند!");
                return;
            }

            db.insertRental(deviceUserId, carPlate, pickupTime, destination, confirmCode);

            JOptionPane.showMessageDialog(null, "ماشین تحویل داده شد ✅ \nکد تحویل: " + confirmCode);
            loadEmployees();
            loadCars();

            pickupTimeField.setText("");
            destinationField.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا: " + e.getMessage());
        }
    }

    private void returnCar() {
        String returnTime = returnTimeField.getText().strip();
        String confirmCodeText = confirmCodeField.getText().strip();

        if (returnTime.isEmpty() || confirmCodeText.isEmpty()) {
            JOptionPane.showMessageDialog(null, "تمام فیلدها باید پر شوند!");
            return;
        }

        int confirmCode;
        try {
            confirmCode = Integer.parseInt(confirmCodeText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "کد تحویل معتبر نیست!");
            return;
        }

        try {
            RentalRecord record = db.getRentalRecord(confirmCode);
            if (record == null) {
                JOptionPane.showMessageDialog(null, "کد تحویل وجود ندارد!");
                return;
            }

            boolean success = db.returnCar(confirmCode, returnTime);
            if (success) {
                JOptionPane.showMessageDialog(null,
                        "ماشین '" + record.carName + "' توسط کارمند '" + record.employeeName + "' برگشت داده شد ✅"
                );
                loadEmployees();
                loadCars();
            } else {
                JOptionPane.showMessageDialog(null, "قبلاً بازگشت ثبت شده است!");
            }

            returnTimeField.setText("");
            confirmCodeField.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "خطا: " + e.getMessage());
        }
    }

    private static String getDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String[] date_time = now.format(formatter).split(" ");
        String[] date = date_time[0].split("-");
        int[] shams_date = DateConverter.gregorian_to_jalali(
                Integer.parseInt(date[0]), Integer.parseInt(date[1]), Integer.parseInt(date[2]));

        return shams_date[0] + "/" +
                ((shams_date[1] < 10) ? "0" + shams_date[1] : shams_date[1]) + "/" +
                ((shams_date[2] < 10) ? "0" + shams_date[2] : shams_date[2]) +
                " " + date_time[1];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }

    static class DateConverter {

        public static int[] gregorian_to_jalali(int gy, int gm, int gd) {
            int[] out = {
                    (gm > 2) ? (gy + 1) : gy,
                    0,
                    0
            };
            {
                int[] g_d_m = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
                out[2] = 355666 + (365 * gy) + (((out[0] + 3) / 4)) - (((out[0] + 99) / 100)) + (((out[0] + 399) / 400)) + gd + g_d_m[gm - 1];
            }
            out[0] = -1595 + (33 * ((out[2] / 12053)));
            out[2] %= 12053;
            out[0] += 4 * ((out[2] / 1461));
            out[2] %= 1461;
            if (out[2] > 365) {
                out[0] += ((out[2] - 1) / 365);
                out[2] = (out[2] - 1) % 365;
            }
            if (out[2] < 186) {
                out[1] = 1 + (out[2] / 31);
                out[2] = 1 + (out[2] % 31);
            } else {
                out[1] = 7 + ((out[2] - 186) / 30);
                out[2] = 1 + ((out[2] - 186) % 30);
            }
            return out;
        }
    }
}
