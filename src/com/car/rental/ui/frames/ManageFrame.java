package com.car.rental.ui.frames;

import com.car.rental.config.ServiceLookup;
import com.car.rental.model.Car;
import com.car.rental.model.Employee;
import com.car.rental.service.CarService;
import com.car.rental.service.EmployeeService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.List;

public class ManageFrame extends JFrame {

    private static final String STATUS_FREE = "آزاد";
    private static final String STATUS_ON_MISSION = "در مأموریت";

    private final CarService carService;
    private final EmployeeService employeeService;
    private JTable carTable, employeeTable;
    private DefaultTableModel carModel, employeeModel;
    private TableRowSorter<DefaultTableModel> carSorter, employeeSorter;
    private JButton editCarButton, deleteCarButton, editEmpButton, deleteEmpButton;
    private JTextField carSearchField, empSearchField;

    public ManageFrame() {
        carService = ServiceLookup.get(CarService.class);
        employeeService = ServiceLookup.get(EmployeeService.class);
        initializeUI();
        loadDataFromDB();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("مدیریت ماشین‌ها و کارمندها");
        setSize(720, 520);
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

        tabbedPane.addTab("ماشین‌ها", buildCarPanel());
        tabbedPane.addTab("کارمندها", buildEmployeePanel());

        add(tabbedPane, BorderLayout.CENTER);
        updateCarButtons();
        updateEmpButtons();
    }

    private JPanel buildCarPanel() {
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
        carTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        carSorter = new TableRowSorter<>(carModel);
        carTable.setRowSorter(carSorter);
        centerColumns(carTable);

        carTable.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                updateCarButtons();
            }
        });
        carTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && carTable.getSelectedRow() >= 0) {
                    openCarEditFrame();
                }
            }
        });

        JScrollPane carScroll = new JScrollPane(carTable);
        carScroll.getViewport().setBackground(Color.WHITE);

        carSearchField = new JTextField(18);
        carSearchField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        carSearchField.putClientProperty("JTextField.placeholderText", "جستجو: نام یا پلاک");
        carSearchField.getDocument().addDocumentListener(simpleListener(this::applyCarFilter));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        searchRow.setBackground(Color.WHITE);
        searchRow.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        searchRow.add(new JLabel("جستجو:"));
        searchRow.add(carSearchField);

        JPanel carPanel = new JPanel(new BorderLayout(10, 10));
        carPanel.setBackground(Color.WHITE);
        carPanel.add(searchRow, BorderLayout.NORTH);
        carPanel.add(carScroll, BorderLayout.CENTER);

        JPanel carButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        carButtons.setBackground(Color.WHITE);
        editCarButton = new JButton("ویرایش");
        editCarButton.setBackground(new Color(0, 120, 215));
        editCarButton.setForeground(Color.WHITE);
        deleteCarButton = new JButton("حذف");
        deleteCarButton.setBackground(new Color(165, 42, 42));
        deleteCarButton.setForeground(Color.WHITE);
        carButtons.add(editCarButton);
        carButtons.add(deleteCarButton);
        carPanel.add(carButtons, BorderLayout.SOUTH);

        editCarButton.addActionListener(e -> openCarEditFrame());
        deleteCarButton.addActionListener(e -> deleteCar());
        return carPanel;
    }

    private JPanel buildEmployeePanel() {
        String[] empColumns = {"شناسه کارمند", "نام", "شماره تماس", "وضعیت"};
        employeeModel = new DefaultTableModel(empColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(employeeModel);
        employeeTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        employeeTable.setRowHeight(25);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeSorter = new TableRowSorter<>(employeeModel);
        employeeTable.setRowSorter(employeeSorter);
        centerColumns(employeeTable);

        employeeTable.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                updateEmpButtons();
            }
        });
        employeeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && employeeTable.getSelectedRow() >= 0) {
                    openEmployeeEditFrame();
                }
            }
        });

        JScrollPane empScroll = new JScrollPane(employeeTable);
        empScroll.getViewport().setBackground(Color.WHITE);

        empSearchField = new JTextField(18);
        empSearchField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        empSearchField.setHorizontalAlignment(JTextField.LEFT);
        empSearchField.getDocument().addDocumentListener(simpleListener(this::applyEmpFilter));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        searchRow.setBackground(Color.WHITE);
        searchRow.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        searchRow.add(new JLabel("جستجو:"));
        searchRow.add(empSearchField);

        JPanel empPanel = new JPanel(new BorderLayout(10, 10));
        empPanel.setBackground(Color.WHITE);
        empPanel.add(searchRow, BorderLayout.NORTH);
        empPanel.add(empScroll, BorderLayout.CENTER);

        JPanel empButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        empButtons.setBackground(Color.WHITE);
        editEmpButton = new JButton("ویرایش");
        editEmpButton.setBackground(new Color(0, 120, 215));
        editEmpButton.setForeground(Color.WHITE);
        deleteEmpButton = new JButton("حذف");
        deleteEmpButton.setBackground(new Color(165, 42, 42));
        deleteEmpButton.setForeground(Color.WHITE);
        empButtons.add(editEmpButton);
        empButtons.add(deleteEmpButton);
        empPanel.add(empButtons, BorderLayout.SOUTH);

        editEmpButton.addActionListener(e -> openEmployeeEditFrame());
        deleteEmpButton.addActionListener(e -> deleteEmployee());
        return empPanel;
    }

    private static DocumentListener simpleListener(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    private void applyCarFilter() {
        String q = carSearchField.getText().trim();
        if (q.isEmpty()) {
            carSorter.setRowFilter(null);
        } else {
            carSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
        }
        updateCarButtons();
    }

    private void applyEmpFilter() {
        String q = empSearchField.getText().trim();
        if (q.isEmpty()) {
            employeeSorter.setRowFilter(null);
        } else {
            // name (col 1) or phone (col 2)
            employeeSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q), 1, 2));
        }
        updateEmpButtons();
    }

    private void updateCarButtons() {
        boolean has = carTable.getSelectedRow() >= 0;
        editCarButton.setEnabled(has);
        deleteCarButton.setEnabled(has);
    }

    private void updateEmpButtons() {
        boolean has = employeeTable.getSelectedRow() >= 0;
        editEmpButton.setEnabled(has);
        deleteEmpButton.setEnabled(has);
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
                String st = car.getStatus() != null ? car.getStatus() : "";
                // normalize legacy wording without hamza
                if (st.contains("ماموریت") && !st.contains("مأموریت")) {
                    st = STATUS_ON_MISSION;
                }
                carModel.addRow(new Object[]{
                        car.getModel(),
                        car.getColor(),
                        car.getPlate(),
                        st
                });
            }
            List<Employee> employees = employeeService.getAllEmployees();
            for (Employee emp : employees) {
                String status = emp.isRenting() ? STATUS_ON_MISSION : STATUS_FREE;
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
        updateCarButtons();
        updateEmpButtons();
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
        if (STATUS_ON_MISSION.equals(status) || status.contains("ماموریت")) {
            JOptionPane.showMessageDialog(this, "امکان ویرایش برای ماشین در مأموریت وجود ندارد!");
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
        if (STATUS_ON_MISSION.equals(status) || status.contains("ماموریت")) {
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
        String status = carModel.getValueAt(modelRow, 3).toString();
        if (STATUS_FREE.equals(status)) {
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
            JOptionPane.showMessageDialog(this, "ماشین در مأموریت را نمی‌توانید حذف کنید!");
        }
    }

    private void deleteEmployee() {
        int viewRow = employeeTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "لطفاً یک کارمند انتخاب کنید!");
            return;
        }
        int modelRow = employeeTable.convertRowIndexToModel(viewRow);
        String status = employeeModel.getValueAt(modelRow, 3).toString();
        if (STATUS_FREE.equals(status)) {
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
            JOptionPane.showMessageDialog(this, "کارمند در مأموریت را نمی‌توانید حذف کنید!");
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
