package com.car.rental;

import javax.swing.*;
import java.awt.*;

public class AddFrame extends JFrame {
    public AddFrame() {
        setTitle("اضافه کردن ماشین و کارمند");
        setSize(600, 400);
        setLayout(new GridLayout(2, 1, 0, 10));
        setLocationRelativeTo(null);

        JPanel vehiclePanel = new JPanel(new GridLayout(4, 2, 10, 10));
        vehiclePanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن ماشین"));
        vehiclePanel.setBackground(new Color(255, 255, 255));

        JLabel modelLabel = new JLabel("نام ماشین:");
        JTextField modelField = new JTextField(20);
        JLabel plateLabel = new JLabel("پلاک:");
        JTextField plateField = new JTextField(20);
        JLabel colorLabel = new JLabel("رنگ:");
        JTextField colorField = new JTextField(20);
        JButton addVehicleButton = new JButton("اضافه کردن ماشین");

        vehiclePanel.add(modelLabel);
        vehiclePanel.add(modelField);
        vehiclePanel.add(plateLabel);
        vehiclePanel.add(plateField);
        vehiclePanel.add(colorLabel);
        vehiclePanel.add(colorField);
        vehiclePanel.add(new JLabel());
        vehiclePanel.add(addVehicleButton);


        JPanel employeePanel = new JPanel(new GridLayout(4, 2, 10, 10));
        employeePanel.setBorder(BorderFactory.createTitledBorder("اضافه کردن کارمند"));
        employeePanel.setBackground(new Color(255, 255, 255));

        JLabel nameLabel = new JLabel("نام:");
        JTextField nameField = new JTextField(20);
        JLabel phoneLabel = new JLabel("شماره تماس:");
        JTextField phoneField = new JTextField(20);
        JLabel nationalIdLabel = new JLabel("کد ملی:");
        JTextField nationalIdField = new JTextField(20);
        JButton addEmployeeButton = new JButton("اضافه کردن کارمند");

        employeePanel.add(nameLabel);
        employeePanel.add(nameField);
        employeePanel.add(phoneLabel);
        employeePanel.add(phoneField);
        employeePanel.add(nationalIdLabel);
        employeePanel.add(nationalIdField);
        employeePanel.add(new JLabel());
        employeePanel.add(addEmployeeButton);


        add(vehiclePanel);
        add(employeePanel);

        setVisible(true);
    }
}