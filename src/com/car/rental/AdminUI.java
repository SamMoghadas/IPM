package com.car.rental;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminUI {
    public AdminUI() {
        createMainUI();
    }

    private void createMainUI() {
        JFrame frame = new JFrame("سیستم مدیریت ماشین شرکت - فریم اصلی");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(240, 240, 240));


        JPanel assignmentPanel = new JPanel(new GridLayout(7, 2, 10, 10));
        assignmentPanel.setBorder(BorderFactory.createTitledBorder("ثبت تحویل ماشین"));
        assignmentPanel.setBackground(new Color(255, 255, 255));


        JLabel vehicleLabel = new JLabel("ماشین:");
        vehicleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JComboBox<String> vehicleCombo = new JComboBox<>(new String[]{"تویوتا کمری - قرمز", "هیوندای سوناتا - آبی"});

        JLabel employeeLabel = new JLabel("کارمند:");
        employeeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JComboBox<String> employeeCombo = new JComboBox<>(new String[]{"علی محمدی", "رضا احمدی", "حسین حسینی"});

        JLabel pickupLabel = new JLabel("زمان تحویل:");
        pickupLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JTextField pickupField = new JTextField(20);
        JButton nowButton = new JButton("الان");
        nowButton.setBackground(new Color(70, 130, 180));
        nowButton.setForeground(Color.WHITE);
        nowButton.setFocusPainted(false);
        nowButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            pickupField.setText(now.format(formatter));
        });

        JLabel destinationLabel = new JLabel("مقصد:");
        destinationLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JTextField destinationField = new JTextField(20);

        JButton assignButton = new JButton("ثبت تحویل");
        assignButton.setBackground(new Color(70, 130, 180));
        assignButton.setForeground(Color.WHITE);
        assignButton.setFocusPainted(false);

        assignmentPanel.add(vehicleLabel);
        assignmentPanel.add(vehicleCombo);
        assignmentPanel.add(employeeLabel);
        assignmentPanel.add(employeeCombo);
        assignmentPanel.add(pickupLabel);
        assignmentPanel.add(pickupField);
        assignmentPanel.add(new JLabel(""));
        assignmentPanel.add(nowButton);
        assignmentPanel.add(destinationLabel);
        assignmentPanel.add(destinationField);
        assignmentPanel.add(new JLabel());
        assignmentPanel.add(assignButton);


        JPanel returnPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        returnPanel.setBorder(BorderFactory.createTitledBorder("ثبت بازگشت ماشین"));
        returnPanel.setBackground(new Color(255, 255, 255));

        JLabel assignmentIdLabel = new JLabel("آیدی تحویل:");
        assignmentIdLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JTextField assignmentIdField = new JTextField(20);
        JLabel returnTimeLabel = new JLabel("زمان بازگشت:");
        returnTimeLabel.setFont(new Font("Arial", Font.BOLD, 12));
        JTextField returnTimeField = new JTextField(20);
        JButton nowReturnButton = new JButton("الان");
        nowReturnButton.setBackground(new Color(70, 130, 180));
        nowReturnButton.setForeground(Color.WHITE);
        nowReturnButton.setFocusPainted(false);
        nowReturnButton.addActionListener(e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            returnTimeField.setText(now.format(formatter));
        });

        JButton returnButton = new JButton("ثبت بازگشت");
        returnButton.setBackground(new Color(70, 130, 180));
        returnButton.setForeground(Color.WHITE);
        returnButton.setFocusPainted(false);

        returnPanel.add(assignmentIdLabel);
        returnPanel.add(assignmentIdField);
        returnPanel.add(returnTimeLabel);
        returnPanel.add(returnTimeField);
        returnPanel.add(new JLabel(""));
        returnPanel.add(nowReturnButton);
        returnPanel.add(new JLabel());
        returnPanel.add(returnButton);


        JButton addItemsButton = new JButton("اضافه کردن ماشین/کارمند");
        addItemsButton.setBackground(new Color(70, 130, 180));
        addItemsButton.setForeground(Color.WHITE);
        addItemsButton.setFocusPainted(false);
        addItemsButton.addActionListener(e -> new AddFrame());

        JButton reportButton = new JButton("نمایش گزارش");
        reportButton.setBackground(new Color(70, 130, 180));
        reportButton.setForeground(Color.WHITE);
        reportButton.setFocusPainted(false);
        reportButton.addActionListener(e -> new ReportFrame());

        // چیدمان اصلی
        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(assignmentPanel);
        mainPanel.add(returnPanel);
        frame.add(mainPanel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonsPanel.add(addItemsButton);
        buttonsPanel.add(reportButton);
        frame.add(buttonsPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminUI());
    }
}