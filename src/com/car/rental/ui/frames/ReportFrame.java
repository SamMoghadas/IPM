package com.car.rental.ui.frames;

import com.car.rental.config.ServiceLookup;
import com.car.rental.model.RentalRecord;
import com.car.rental.model.RentalReportFilter;
import com.car.rental.service.RentalService;
import com.car.rental.ui.components.PlateInputPanel;
import com.car.rental.util.ReportExcelExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ReportFrame extends JFrame {

    private static final String[] COLUMNS = {
            "شناسه کارمند", "نام کارمند", "ماشین", "رنگ", "پلاک", "تاریخ تحویل", "تاریخ برگشت", "مقصد"
    };

    private static final Logger logger = Logger.getLogger(ReportFrame.class.getName());

    private final DefaultTableModel tableModel;
    private final List<RentalRecord> currentRows = new ArrayList<>();

    private JTextField employeeNameField;
    private PlateInputPanel plateSearchPanel;
    private JTextField carNameField;
    private JTextField destinationField;
    private JFormattedTextField dateFromField;
    private JFormattedTextField dateToField;
    private JComboBox<String> statusCombo;
    private JLabel resultCountLabel;

    public ReportFrame() {
        setTitle("گزارش سفرهای ماشین");
        setSize(1120, 580);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        add(createTopBar(), BorderLayout.NORTH);
        add(createBottomBar(), BorderLayout.SOUTH);

        runSearch();
        setVisible(true);
    }

    private JPanel createTopBar() {
        JPanel outer = new JPanel(new BorderLayout(8, 8));
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 12));
        outer.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setBackground(new Color(230, 230, 230));
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> {
            dispose();
            new MainFrame();
        });
        outer.add(backButton, BorderLayout.WEST);

        // Two content rows + actions row for clearer spacing on smaller screens
        JPanel filters = new JPanel(new GridBagLayout());
        filters.setBackground(Color.WHITE);
        filters.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        employeeNameField = new JTextField(10);
        employeeNameField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        employeeNameField.setHorizontalAlignment(JTextField.LEFT);

        plateSearchPanel = new PlateInputPanel();

        carNameField = new JTextField(8);
        carNameField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        carNameField.setHorizontalAlignment(JTextField.RIGHT);

        destinationField = new JTextField(8);
        destinationField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        destinationField.setHorizontalAlignment(JTextField.RIGHT);

        dateFromField = jalaliDateField();
        dateToField = jalaliDateField();
        statusCombo = new JComboBox<>(new String[]{"همه", "فقط باز (منتظر برگشت)", "فقط بسته‌شده"});

        KeyAdapter enterSearch = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    runSearch();
                }
            }
        };
        employeeNameField.addKeyListener(enterSearch);
        carNameField.addKeyListener(enterSearch);
        destinationField.addKeyListener(enterSearch);
        dateFromField.addKeyListener(enterSearch);
        dateToField.addKeyListener(enterSearch);

        int col = 0;
        int row = 0;
        col = addFilter(filters, gbc, col, row, "نام کارمند:", employeeNameField);
        col = addFilter(filters, gbc, col, row, "پلاک:", plateSearchPanel);
        col = addFilter(filters, gbc, col, row, "ماشین:", carNameField);
        col = addFilter(filters, gbc, col, row, "مقصد:", destinationField);

        row = 1;
        col = 0;
        col = addFilter(filters, gbc, col, row, "از تاریخ:", dateFromField);
        col = addFilter(filters, gbc, col, row, "تا تاریخ:", dateToField);
        col = addFilter(filters, gbc, col, row, "وضعیت:", statusCombo);

        row = 2;
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setBackground(Color.WHITE);
        JButton searchButton = new JButton("جستجو");
        searchButton.setBackground(new Color(0, 120, 215));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> runSearch());

        JButton clearButton = new JButton("پاک کردن فیلتر");
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> clearFilters());

        JButton excelButton = new JButton("خروجی Excel");
        excelButton.setBackground(new Color(34, 139, 34));
        excelButton.setForeground(Color.WHITE);
        excelButton.setFocusPainted(false);
        excelButton.addActionListener(e -> exportExcel());

        actions.add(excelButton);
        actions.add(clearButton);
        actions.add(searchButton);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 8;
        gbc.anchor = GridBagConstraints.EAST;
        filters.add(actions, gbc);

        outer.add(filters, BorderLayout.CENTER);

        JLabel tip = new JLabel(
                "تاریخ شمسی yyyy/MM/dd — برای یک روز خاص همان تاریخ را در «از» و «تا» بگذارید.");
        tip.setFont(new Font("Arial", Font.PLAIN, 11));
        tip.setForeground(new Color(90, 90, 90));
        outer.add(tip, BorderLayout.SOUTH);

        return outer;
    }

    private JPanel createBottomBar() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        bottom.setBackground(Color.WHITE);
        bottom.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        resultCountLabel = new JLabel(" ");
        resultCountLabel.setFont(new Font("Arial", Font.BOLD, 13));
        bottom.add(resultCountLabel);
        return bottom;
    }

    private static int addFilter(JPanel panel, GridBagConstraints gbc, int col, int row,
                                 String label, JComponent field) {
        gbc.gridx = col++;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = col++;
        gbc.weightx = 0.2;
        panel.add(field, gbc);
        return col;
    }

    private static JFormattedTextField jalaliDateField() {
        try {
            MaskFormatter mask = new MaskFormatter("####/##/##");
            mask.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField(mask);
            f.setColumns(8);
            f.setToolTipText("مثال: 1405/05/25");
            return f;
        } catch (Exception e) {
            JFormattedTextField f = new JFormattedTextField();
            f.setColumns(8);
            return f;
        }
    }

    private void clearFilters() {
        employeeNameField.setText("");
        plateSearchPanel.clear();
        carNameField.setText("");
        destinationField.setText("");
        dateFromField.setValue(null);
        dateFromField.setText("____/__/__");
        dateToField.setValue(null);
        dateToField.setText("____/__/__");
        statusCombo.setSelectedIndex(0);
        runSearch();
    }

    private RentalReportFilter buildFilter() {
        RentalReportFilter f = new RentalReportFilter();
        f.setEmployeeName(employeeNameField.getText());
        f.setPlate(safePlate());
        f.setCarName(carNameField.getText());
        f.setDestination(destinationField.getText());
        f.setDateFrom(normalizeJalaliDay(dateFromField.getText()));
        f.setDateTo(normalizeJalaliDay(dateToField.getText()));

        int statusIdx = statusCombo.getSelectedIndex();
        if (statusIdx == 1) {
            f.setStatus(RentalReportFilter.Status.OPEN);
        } else if (statusIdx == 2) {
            f.setStatus(RentalReportFilter.Status.CLOSED);
        } else {
            f.setStatus(RentalReportFilter.Status.ALL);
        }
        return f;
    }

    private String safePlate() {
        try {
            String p = plateSearchPanel.getPlate();
            return p == null ? "" : p.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String normalizeJalaliDay(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.contains("_")) {
            return null;
        }
        if (s.matches("\\d{4}/\\d{2}/\\d{2}")) {
            return s;
        }
        return null;
    }

    private void runSearch() {
        RentalService rentalService = ServiceLookup.get(RentalService.class);
        RentalReportFilter filter = buildFilter();
        try {
            List<RentalRecord> rows = rentalService.getRentalReport(filter);
            currentRows.clear();
            currentRows.addAll(rows);
            tableModel.setRowCount(0);
            for (RentalRecord r : rows) {
                tableModel.addRow(new Object[]{
                        r.deviceUserId,
                        r.employeeName,
                        r.carName,
                        r.carColor,
                        r.plate,
                        r.pickupDate,
                        r.returnDate,
                        r.destination
                });
            }
            resultCountLabel.setText(rows.size() + " مورد یافت شد");
        } catch (SQLException e) {
            logger.severe("Report search failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "خطا در دریافت گزارش: " + e.getMessage());
        }
    }

    private void exportExcel() {
        if (currentRows.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ردیفی برای خروجی وجود ندارد.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("ذخیره گزارش Excel");
        chooser.setSelectedFile(new java.io.File("gozaresh-safarha.xlsx"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        if (!path.toString().toLowerCase().endsWith(".xlsx")) {
            path = Path.of(path + ".xlsx");
        }
        try {
            ReportExcelExporter.export(currentRows, path);
            JOptionPane.showMessageDialog(this, "فایل ذخیره شد:\n" + path);
        } catch (Exception e) {
            logger.severe("Excel export failed: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "خطا در ذخیره Excel: " + e.getMessage());
        }
    }
}
