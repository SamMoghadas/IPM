package com.car.rental.ui.frames;

import com.car.rental.config.ServiceLookup;
import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.service.CarService;
import com.car.rental.service.EmployeeService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;

public class ManageFrame extends JFrame {
    private final CarService carService;
    private final EmployeeService employeeService;
    private JTable carTable, employeeTable;
    private DefaultTableModel carModel, employeeModel;

    public ManageFrame() {
        carService = ServiceLookup.get(CarService.class);
        employeeService = ServiceLookup.get(EmployeeService.class);
        initializeUI();
        loadDataFromDB();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("مدیریت ماشین‌ها و کارمندها");
        setSize(700, 500);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);
        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setFocusPainted(false);
        backButton.setBackground(new Color(230, 230, 230));
        backButton.addActionListener(e -> {
            dispose();
            new MainFrame();
        });
        topPanel.add(backButton);
        add(topPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        String[] carColumns = {"نام", "رنگ", "پلاک", "وضعیت"};
        carModel = new DefaultTableModel(carColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        carTable = new JTable(carModel);
        carTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        carTable.setRowHeight(25);
        carTable.setRowSorter(new TableRowSorter<>(carModel));
        centerColumns(carTable);

        JScrollPane carScroll = new JScrollPane(carTable);
        carScroll.getViewport().setBackground(Color.WHITE);

        JPanel carPanel = new JPanel(new BorderLayout(10, 10));
        carPanel.add(carScroll, BorderLayout.CENTER);
        carPanel.setBackground(Color.WHITE);

        JPanel carButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        carButtons.setBackground(Color.WHITE);
        JButton editCarButton = new JButton("ویرایش");
        editCarButton.setBackground(new Color(0, 120, 215));
        editCarButton.setForeground(Color.WHITE);
        JButton deleteCarButton = new JButton("حذف");
        deleteCarButton.setBackground(new Color(165, 42, 42));
        deleteCarButton.setForeground(Color.WHITE);
        carButtons.add(editCarButton);
        carButtons.add(deleteCarButton);
        carPanel.add(carButtons, BorderLayout.SOUTH);

        editCarButton.addActionListener(e -> openCarEditFrame());
        deleteCarButton.addActionListener(e -> deleteCar());

        String[] empColumns = {"شناسه کاربر", "نام", "شماره تماس", "وضعیت"};
        employeeModel = new DefaultTableModel(empColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(employeeModel);
        employeeTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        employeeTable.setRowHeight(25);
        employeeTable.setRowSorter(new TableRowSorter<>(employeeModel));
        centerColumns(employeeTable);

        JScrollPane empScroll = new JScrollPane(employeeTable);
        empScroll.getViewport().setBackground(Color.WHITE);

        JPanel empPanel = new JPanel(new BorderLayout(10, 10));
        empPanel.add(empScroll, BorderLayout.CENTER);
        empPanel.setBackground(Color.WHITE);

        JPanel empButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        empButtons.setBackground(Color.WHITE);
        JButton editEmpButton = new JButton("ویرایش");
        editEmpButton.setBackground(new Color(0, 120, 215));
        editEmpButton.setForeground(Color.WHITE);
        JButton deleteEmpButton = new JButton("حذف");
        deleteEmpButton.setBackground(new Color(165, 42, 42));
        deleteEmpButton.setForeground(Color.WHITE);
        empButtons.add(editEmpButton);
        empButtons.add(deleteEmpButton);
        empPanel.add(empButtons, BorderLayout.SOUTH);

        editEmpButton.addActionListener(e -> openEmployeeEditFrame());
        deleteEmpButton.addActionListener(e -> deleteEmployee());

        tabbedPane.addTab("ماشین‌ها", carPanel);
        tabbedPane.addTab("کارمندها", empPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void centerColumns(JTable table) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void loadDataFromDB() {
        carModel.setRowCount(0);
        employeeModel.setRowCount(0);
        try {
            for (Car car : carService.getAllCars()) {
                carModel.addRow(new Object[]{
                        car.getModel(),
                        car.getColor(),
                        car.getPlate(),
                        car.getStatus() != null ? car.getStatus() : ""
                });
            }
            List<Employee> employees = employeeService.getAllEmployees();
            for (Employee emp : employees) {
                String status = emp.isRenting() ? "در ماموریت" : "آزاد";
                employeeModel.addRow(new Object[]{
                        emp.getDeviceUserId(),
                        emp.getName(),
                        emp.getPhone() != null ? emp.getPhone() : "",
                        status
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در بارگذاری داده‌ها: " + e.getMessage());
        }
    }

    private void openCarEditFrame() {
        int viewRow = carTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "لطفاً یک ماشین انتخاب کنید!");
            return;
        }
        int modelRow = carTable.convertRowIndexToModel(viewRow);
        String model = carModel.getValueAt(modelRow, 0).toString();
        String color = carModel.getValueAt(modelRow, 1).toString();
        String plate = carModel.getValueAt(modelRow, 2).toString();
        String status = carModel.getValueAt(modelRow, 3).toString();
        if (status.equals("در ماموریت")) {
            JOptionPane.showMessageDialog(this, "امکان ویرایش برای ماشین در ماموریت وجود ندارد!");
            return;
        }
        Car car = new Car(model, plate, color);
        dispose();
        EditFrame editFrame = new EditFrame(car);
        editFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                new ManageFrame();
            }
        });
        editFrame.setVisible(true);
    }

    private void openEmployeeEditFrame() {
        int viewRow = employeeTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "لطفاً یک کارمند انتخاب کنید!");
            return;
        }
        int modelRow = employeeTable.convertRowIndexToModel(viewRow);
        String deviceUserId = employeeModel.getValueAt(modelRow, 0).toString();
        String status = employeeModel.getValueAt(modelRow, 3).toString();
        if (status.equals("در ماموریت")) {
            JOptionPane.showMessageDialog(this,
                    "این کارمند در مأموریت است و امکان ویرایش آن وجود ندارد!");
            return;
        }
        try {
            Employee emp = employeeService.findByDeviceUserId(deviceUserId);
            if (emp == null) {
                JOptionPane.showMessageDialog(this, "کارمند پیدا نشد!");
                return;
            }
            dispose();
            EditFrame editFrame = new EditFrame(emp);
            editFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    new ManageFrame();
                }
            });
            editFrame.setVisible(true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا: " + e.getMessage());
        }
    }

    private void deleteCar() {
        int viewRow = carTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "لطفاً یک ماشین انتخاب کنید!");
            return;
        }
        int modelRow = carTable.convertRowIndexToModel(viewRow);
        if (carModel.getValueAt(modelRow, 3).equals("آزاد")) {
            boolean confirmed = deleteConfirmDialog("آیا مطمئن هستید که این ماشین را حذف کنید؟");
            if (confirmed) {
                try {
                    String plate = carModel.getValueAt(modelRow, 2).toString();
                    carService.deleteCar(plate);
                    loadDataFromDB();
                    JOptionPane.showMessageDialog(this, "ماشین حذف شد!");
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "خطا در حذف ماشین: " + e.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "ماشین در ماموریت را نمیتوانید حذف کنید!");
        }
    }

    private void deleteEmployee() {
        int viewRow = employeeTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "لطفاً یک کارمند انتخاب کنید!");
            return;
        }
        int modelRow = employeeTable.convertRowIndexToModel(viewRow);
        if (employeeModel.getValueAt(modelRow, 3).equals("آزاد")) {
            boolean confirmed = deleteConfirmDialog("آیا مطمئن هستید که این کارمند را حذف کنید؟");
            if (confirmed) {
                try {
                    String deviceUserId = employeeModel.getValueAt(modelRow, 0).toString();
                    employeeService.deleteEmployee(deviceUserId);
                    loadDataFromDB();
                    JOptionPane.showMessageDialog(this, "کارمند حذف شد!");
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(this, "خطا در حذف کارمند: " + e.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "کارمند در ماموریت را نمیتوانید حذف کنید!");
        }
    }

    private boolean deleteConfirmDialog(String message) {
        JDialog dialog = new JDialog(this, "تأیید حذف", true);
        dialog.setLayout(new BorderLayout(10, 10));
        JLabel msgLabel = new JLabel(message, SwingConstants.CENTER);
        dialog.add(msgLabel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton yesButton = new JButton("بله، حذف شود");
        yesButton.setBackground(Color.RED);
        yesButton.setForeground(Color.WHITE);
        JButton noButton = new JButton("خیر، منصرف شدم");
        noButton.setBackground(Color.GRAY);
        noButton.setForeground(Color.WHITE);
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        final boolean[] confirmed = {false};
        yesButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        noButton.addActionListener(e -> dialog.dispose());
        dialog.setSize(350, 150);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return confirmed[0];
    }
}
