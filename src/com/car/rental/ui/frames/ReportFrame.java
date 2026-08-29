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
        setSize(1100, 560);
        setLayout(new BorderLayout(8, 8));
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

    /**
     * Nav row (fixed) separate from compact filter rows (FlowLayout = no field stretch).
     */
    private JPanel createTopBar() {
        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBackground(Color.WHITE);
        outer.setBorder(BorderFactory.createEmptyBorder(6, 10, 4, 10));

        // 1) Navigation only — never shares height/width with filters
        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        navRow.setBackground(Color.WHITE);
        navRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        navRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setBackground(new Color(230, 230, 230));
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> {
            dispose();
            new MainFrame();
        });
        navRow.add(backButton);
        outer.add(navRow);

        KeyAdapter enterSearch = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    runSearch();
                }
            }
        };

        employeeNameField = fixedTextField(12, false);
        employeeNameField.addKeyListener(enterSearch);

        plateSearchPanel = new PlateInputPanel();

        carNameField = fixedTextField(10, true);
        carNameField.addKeyListener(enterSearch);

        destinationField = fixedTextField(10, true);
        destinationField.addKeyListener(enterSearch);

        dateFromField = jalaliDateField();
        dateFromField.addKeyListener(enterSearch);
        dateToField = jalaliDateField();
        dateToField.addKeyListener(enterSearch);

        statusCombo = new JComboBox<>(new String[]{"همه", "فقط باز (منتظر برگشت)", "فقط بسته‌شده"});
        statusCombo.setPreferredSize(new Dimension(160, 26));
        statusCombo.setMaximumSize(new Dimension(160, 26));

        // 2) Filter row A — identity / vehicle
        JPanel rowA = filterFlowRow();
        rowA.add(label("نام کارمند:"));
        rowA.add(employeeNameField);
        rowA.add(Box.createHorizontalStrut(10));
        rowA.add(label("پلاک:"));
        rowA.add(plateSearchPanel);
        rowA.add(Box.createHorizontalStrut(10));
        rowA.add(label("ماشین:"));
        rowA.add(carNameField);
        rowA.add(Box.createHorizontalStrut(10));
        rowA.add(label("مقصد:"));
        rowA.add(destinationField);
        outer.add(rowA);

        // 3) Filter row B — dates, status, actions (compact)
        JPanel rowB = filterFlowRow();
        rowB.add(label("از تاریخ:"));
        rowB.add(dateFromField);
        rowB.add(Box.createHorizontalStrut(8));
        rowB.add(label("تا تاریخ:"));
        rowB.add(dateToField);
        rowB.add(Box.createHorizontalStrut(8));
        rowB.add(label("وضعیت:"));
        rowB.add(statusCombo);
        rowB.add(Box.createHorizontalStrut(12));

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

        rowB.add(searchButton);
        rowB.add(clearButton);
        rowB.add(excelButton);
        outer.add(rowB);

        JLabel tip = new JLabel(
                "تاریخ شمسی yyyy/MM/dd — برای یک روز خاص همان تاریخ را در «از» و «تا» بگذارید.");
        tip.setFont(new Font("Arial", Font.PLAIN, 11));
        tip.setForeground(new Color(90, 90, 90));
        tip.setAlignmentX(Component.RIGHT_ALIGNMENT);
        tip.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        JPanel tipRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        tipRow.setBackground(Color.WHITE);
        tipRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        tipRow.add(tip);
        outer.add(tipRow);

        return outer;
    }

    private static JPanel filterFlowRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        row.setBackground(Color.WHITE);
        row.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return row;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.PLAIN, 12));
        return l;
    }

    /** Fixed preferred width so FlowLayout does not stretch fields. */
    private static JTextField fixedTextField(int columns, boolean rtl) {
        JTextField f = new JTextField(columns);
        f.setPreferredSize(new Dimension(columns * 9 + 24, 26));
        f.setMaximumSize(f.getPreferredSize());
        if (rtl) {
            f.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            f.setHorizontalAlignment(JTextField.RIGHT);
        } else {
            f.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            f.setHorizontalAlignment(JTextField.LEFT);
        }
        return f;
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

    private static JFormattedTextField jalaliDateField() {
        try {
            MaskFormatter mask = new MaskFormatter("####/##/##");
            mask.setPlaceholderCharacter('_');
            JFormattedTextField f = new JFormattedTextField(mask);
            f.setColumns(7);
            f.setPreferredSize(new Dimension(92, 26));
            f.setMaximumSize(new Dimension(92, 26));
            f.setToolTipText("مثال: 1405/05/25");
            return f;
        } catch (Exception e) {
            JFormattedTextField f = new JFormattedTextField();
            f.setColumns(7);
            f.setPreferredSize(new Dimension(92, 26));
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
