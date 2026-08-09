package com.car.rental.ui.frames;

import com.car.rental.config.ServiceLookup;
import com.car.rental.model.RentalRecord;
import com.car.rental.service.RentalService;
import com.car.rental.ui.components.PlateInputPanel;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

public class ReportFrame extends JFrame {

    private TableRowSorter<DefaultTableModel> sorter;
    private JFormattedTextField searchDateField;
    private static final Logger logger = Logger.getLogger(ReportFrame.class.getName());

    public ReportFrame() {
        setTitle("گزارش سفرهای ماشین");
        setSize(900, 450);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);

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
            MaskFormatter mask = new MaskFormatter("####/##/## ##:##");
            mask.setPlaceholderCharacter('_');
            searchDateField = new JFormattedTextField(mask);
            searchDateField.setColumns(10);
        } catch (Exception ex) {
            logger.severe("خطا در ساخت ماسک تاریخ");
        }

        JButton searchButton = new JButton("جستجو");
        searchButton.setBackground(new Color(0, 120, 215));
        searchButton.setForeground(Color.WHITE);
        searchButton.addActionListener(e -> applyFilters(plateSearchPanel, searchEmployeeField));

        searchPanel.add(new JLabel("پلاک:"));
        searchPanel.add(plateSearchPanel);
        searchPanel.add(new JLabel("شناسه کاربر:"));
        searchPanel.add(searchEmployeeField);
        searchPanel.add(new JLabel("تاریخ:"));
        searchPanel.add(searchDateField);
        searchPanel.add(searchButton);

        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        loadTable();
        setVisible(true);
    }

    private void loadTable() {
        RentalService rentalService = ServiceLookup.get(RentalService.class);
        try {
            List<RentalRecord> rentalRecords = rentalService.getRentalReport();

            String[] columns = {"شناسه کاربر", "نام کارمند", "ماشین", "رنگ", "پلاک", "تاریخ تحویل", "تاریخ برگشت", "مقصد"};
            Object[][] data = new Object[rentalRecords.size()][columns.length];

            for (int i = 0; i < rentalRecords.size(); i++) {
                RentalRecord r = rentalRecords.get(i);
                data[i][0] = r.deviceUserId;
                data[i][1] = r.employeeName;
                data[i][2] = r.carName;
                data[i][3] = r.carColor;
                data[i][4] = r.plate;
                data[i][5] = r.pickupDate;
                data[i][6] = r.returnDate;
                data[i][7] = r.destination;
            }

            DefaultTableModel model = new DefaultTableModel(data, columns) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable table = new JTable(model);
            table.setRowHeight(24);
            table.setFont(new Font("Arial", Font.PLAIN, 12));

            DefaultTableCellRenderer center = new DefaultTableCellRenderer();
            center.setHorizontalAlignment(SwingConstants.CENTER);
            for (int i = 0; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(center);
            }

            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.getViewport().setBackground(Color.WHITE);
            add(scrollPane, BorderLayout.CENTER);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در دریافت گزارش: " + e.getMessage());
        }
    }

    private void applyFilters(PlateInputPanel platePanel, JTextField empField) {
        final String plateText = platePanel.getPlate().trim();
        final String empText = empField.getText().trim();
        final String dateText = searchDateField.getText().trim();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean plateMatch = plateText.isEmpty() || entry.getStringValue(4).contains(plateText);
                boolean empMatch = empText.isEmpty() || entry.getStringValue(0).contains(empText);
                boolean dateMatch = true;

                if (!dateText.contains("_")) {
                    try {
                        LocalDateTime filterDate = LocalDateTime.parse(dateText, formatter);
                        LocalDateTime pickupDate = LocalDateTime.parse(entry.getStringValue(5), formatter);
                        String returnStr = entry.getStringValue(6);
                        LocalDateTime returnDate = (returnStr == null || returnStr.isEmpty() || returnStr.contains("منتظر"))
                                ? null
                                : LocalDateTime.parse(returnStr, formatter);
                        boolean pickupCondition = pickupDate.isBefore(filterDate);
                        boolean returnCondition = (returnDate == null) || returnDate.isAfter(filterDate);
                        dateMatch = pickupCondition && returnCondition;
                    } catch (Exception ex) {
                        // ignore
                    }
                }
                return plateMatch && empMatch && dateMatch;
            }
        });
    }
}
