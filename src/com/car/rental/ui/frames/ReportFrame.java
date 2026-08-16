package com.car.rental.ui.frames;

import com.car.rental.config.ServiceLookup;
import com.car.rental.model.RentalRecord;
import com.car.rental.service.RentalService;
import com.car.rental.ui.components.PlateInputPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;

public class ReportFrame extends JFrame {

    private static final String[] COLUMNS = {
            "شناسه کاربر", "نام کارمند", "ماشین", "رنگ", "پلاک", "تاریخ تحویل", "تاریخ برگشت", "مقصد"
    };

    /** Jalali timestamps stored as yyyy/MM/dd HH:mm[:ss] */
    private static final DateTimeFormatter JALALI_DT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm[:ss]");
    /** Legacy Gregorian rows may still use dashes. */
    private static final DateTimeFormatter LEGACY_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");

    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private JFormattedTextField searchDateField;
    private static final Logger logger = Logger.getLogger(ReportFrame.class.getName());

    public ReportFrame() {
        setTitle("گزارش سفرهای ماشین");
        setSize(900, 450);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(new Font("Arial", Font.PLAIN, 12));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setBackground(new Color(230, 230, 230));
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> {
            dispose();
            new MainFrame();
        });
        topPanel.add(backButton, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        PlateInputPanel plateSearchPanel = new PlateInputPanel();
        JTextField searchEmployeeField = new JTextField(10);
        searchEmployeeField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        try {
            // Jalali: 1403/05/25 14:30
            MaskFormatter mask = new MaskFormatter("####/##/## ##:##");
            mask.setPlaceholderCharacter('_');
            searchDateField = new JFormattedTextField(mask);
            searchDateField.setColumns(12);
            searchDateField.setToolTipText("تاریخ شمسی، مثلاً 1403/05/25 14:30");
        } catch (Exception ex) {
            logger.severe("خطا در ساخت ماسک تاریخ");
            searchDateField = new JFormattedTextField();
            searchDateField.setColumns(12);
        }

        JButton searchButton = new JButton("جستجو");
        searchButton.setBackground(new Color(0, 120, 215));
        searchButton.setForeground(Color.WHITE);
        searchButton.addActionListener(e -> applyFilters(plateSearchPanel, searchEmployeeField));

        searchPanel.add(new JLabel("پلاک:"));
        searchPanel.add(plateSearchPanel);
        searchPanel.add(new JLabel("شناسه کاربر:"));
        searchPanel.add(searchEmployeeField);
        searchPanel.add(new JLabel("تاریخ (شمسی):"));
        searchPanel.add(searchDateField);
        searchPanel.add(searchButton);

        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        reloadTableData();
        setVisible(true);
    }

    private void reloadTableData() {
        tableModel.setRowCount(0);
        RentalService rentalService = ServiceLookup.get(RentalService.class);
        try {
            List<RentalRecord> rentalRecords = rentalService.getRentalReport();
            for (RentalRecord r : rentalRecords) {
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
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در دریافت گزارش: " + e.getMessage());
        }
    }

    private void applyFilters(PlateInputPanel platePanel, JTextField empField) {
        final String plateText = platePanel.getPlate().trim();
        final String empText = empField.getText().trim();
        final String dateText = searchDateField.getText().trim();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean plateMatch = plateText.isEmpty() || entry.getStringValue(4).contains(plateText);
                boolean empMatch = empText.isEmpty() || entry.getStringValue(0).contains(empText);
                boolean dateMatch = true;

                if (!dateText.contains("_")) {
                    try {
                        LocalDateTime filterDate = parseFlexible(dateText);
                        LocalDateTime pickupDate = parseFlexible(entry.getStringValue(5));
                        String returnStr = entry.getStringValue(6);
                        LocalDateTime returnDate = (returnStr == null || returnStr.isEmpty() || returnStr.contains("منتظر"))
                                ? null
                                : parseFlexible(returnStr);
                        boolean pickupCondition = !pickupDate.isAfter(filterDate);
                        boolean returnCondition = (returnDate == null) || !returnDate.isBefore(filterDate);
                        dateMatch = pickupCondition && returnCondition;
                    } catch (Exception ex) {
                        // incomplete / mixed legacy rows
                    }
                }
                return plateMatch && empMatch && dateMatch;
            }
        });
    }

    /**
     * Accepts Jalali {@code yyyy/MM/dd HH:mm[:ss]} and legacy Gregorian dash form.
     * Component values are compared as-is (same calendar system within one dataset).
     */
    private static LocalDateTime parseFlexible(String raw) {
        if (raw == null) {
            throw new DateTimeParseException("null", "", 0);
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, JALALI_DT);
        } catch (DateTimeParseException ignored) {
        }
        return LocalDateTime.parse(s, LEGACY_DT);
    }
}
