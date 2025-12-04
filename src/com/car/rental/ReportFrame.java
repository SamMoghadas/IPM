package com.car.rental;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.sql.SQLException;
import java.text.ParseException;
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
        setSize(900, 400);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.WHITE);

        // ----------------- پنل بالایی با دکمه بازگشت -----------------
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JButton backButton = new JButton("برگشت به صفحه اصلی");
        backButton.setFocusPainted(false);
        backButton.setBackground(new Color(230, 230, 230));
        backButton.addActionListener(e -> {
            dispose();
            new AdminUI();
        });
        topPanel.add(backButton, BorderLayout.WEST);

        // ----------------- پنل جستجو -----------------
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel plateSearchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        plateSearchPanel.setBackground(Color.WHITE);

        JTextField firstTwoSearch = new JTextField(2);
        firstTwoSearch.setDocument(new AddFrame.LimitDigitsDocument(2));

        String[] letters = {"الف", "ب", "ج", "د", "س", "ص", "ط", "ق", "گ", "ل", "م", "ن", "و", "هـ", "ی"};
        JComboBox<String> letterCombo = new JComboBox<>(letters);
        letterCombo.setBackground(Color.WHITE);

        JTextField middleThreeSearch = new JTextField(3);
        middleThreeSearch.setDocument(new AddFrame.LimitDigitsDocument(3));

        JTextField cityCodeSearch = new JTextField(2);
        cityCodeSearch.setDocument(new AddFrame.LimitDigitsDocument(2));

        plateSearchPanel.add(firstTwoSearch);
        plateSearchPanel.add(letterCombo);
        plateSearchPanel.add(middleThreeSearch);
        plateSearchPanel.add(new JLabel("ایران"));
        plateSearchPanel.add(cityCodeSearch);

        JTextField searchEmployeeField = new JTextField(10);
        searchEmployeeField.setToolTipText("جستجو بر اساس کارمند");
        searchEmployeeField.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        searchDateField = null;
        try {
            MaskFormatter dateMask = new MaskFormatter("####/##/## ##:##");
            dateMask.setPlaceholderCharacter('_');
            searchDateField = new JFormattedTextField(dateMask);
            searchDateField.setColumns(9);
            searchDateField.setToolTipText("فرمت تاریخ: yyyy/MM/dd HH:mm");
        } catch (ParseException e) {
            logger.severe("خطا در ساخت ماسک تاریخ: " + e.getMessage());
        }

        JButton searchButton = search(firstTwoSearch, String.valueOf(letterCombo.getSelectedItem()), middleThreeSearch, cityCodeSearch, searchEmployeeField);
        searchPanel.add(searchButton);
        searchButton.setBackground(new Color(0, 120, 215));
        searchButton.setForeground(Color.WHITE);

        JPanel carPanel = new JPanel(new FlowLayout());
        carPanel.setBackground(Color.WHITE);
        carPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel empPanel = new JPanel(new FlowLayout());
        empPanel.setBackground(Color.WHITE);
        empPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel datePanel = new JPanel(new FlowLayout());
        datePanel.setBackground(Color.WHITE);
        datePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        carPanel.add(new JLabel("پلاک ماشین:"));
        carPanel.add(plateSearchPanel);
        empPanel.add(new JLabel("شماره پرسنلی:"));
        empPanel.add(searchEmployeeField);
        datePanel.add(new JLabel("تاریخ:"));
        datePanel.add(searchDateField);
        searchPanel.add(carPanel);
        searchPanel.add(empPanel);
        searchPanel.add(datePanel);
        searchPanel.add(searchButton);
        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        DatabaseManager db = new DatabaseManager();
        try {
            List<RentalRecord> rentalRecords = db.getRentalReport();

            // ستون‌های جدول
            String[] columns = {"شماره پرسنلی", "نام کارمند", "ماشین", "رنگ", "پلاک", "تاریخ تحویل", "تاریخ برگشت", "مقصد"};
            Object[][] data = new Object[rentalRecords.size()][columns.length];

            for (int i = 0; i < rentalRecords.size(); i++) {
                RentalRecord r = rentalRecords.get(i);
                data[i][0] = r.personnelId;
                data[i][1] = r.employeeName;
                data[i][2] = r.carName;
                data[i][3] = r.carColor;
                data[i][4] = r.plate;
                data[i][5] = r.pickupDate;
                data[i][6] = r.returnDate;
                data[i][7] = r.destination;
            }

            // مدل جدول فقط خواندنی
            DefaultTableModel model = new DefaultTableModel(data, columns) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false; // همه سلول‌ها غیرقابل ویرایش
                }
            };

            JTable table = new JTable(model);
            table.setFont(new Font("Arial", Font.PLAIN, 12));
            table.setRowHeight(24);

            // وسط‌چین کردن سلول‌ها
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            for (int i = 0; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }

            // مرتب‌سازی پیش‌فرض بر اساس ستون تاریخ تحویل (ستون 5) نزولی
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.setSortKeys(List.of(new RowSorter.SortKey(5, SortOrder.DESCENDING)));

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.getViewport().setBackground(Color.WHITE);
            add(scrollPane, BorderLayout.CENTER);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در دریافت گزارش: " + e.getMessage());
        }

        setVisible(true);
    }

    private JButton search(JTextField firstTwoSearch, String letterSearch,
                           JTextField middleThreeSearch, JTextField cityCodeSearch,
                           JTextField searchEmployeeField) {
        JButton searchButton = new JButton("جستجو");
        searchButton.setBackground(new Color(0, 120, 215));
        searchButton.setForeground(Color.WHITE);

        searchButton.addActionListener(e -> {
            String plateSearchText = (!firstTwoSearch.getText().isEmpty() && !letterSearch.isEmpty()
                    && !middleThreeSearch.getText().isEmpty() && !cityCodeSearch.getText().isEmpty()) ?
                    firstTwoSearch.getText().trim() + " " +
                    letterSearch.trim() + " " +
                    middleThreeSearch.getText().trim() + " | " +
                    cityCodeSearch.getText().trim()
                    : "";
            String empText = searchEmployeeField.getText().trim();
            String dateText = searchDateField.getText().trim();

            RowFilter<DefaultTableModel, Integer> rf = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    boolean carMatch = plateSearchText.isEmpty() || entry.getStringValue(4).contains(plateSearchText);
                    boolean empMatch = empText.isEmpty() || entry.getStringValue(0).contains(empText);

                    boolean dateMatch = true;
                    if (!dateText.isEmpty() && !dateText.contains("_")) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
                            LocalDateTime filterDate = LocalDateTime.parse(dateText, formatter);

                            LocalDateTime pickupDate = LocalDateTime.parse(entry.getStringValue(5), formatter);

                            String returnStr = entry.getStringValue(6);
                            LocalDateTime returnDate = null;
                            if (returnStr != null && !returnStr.isEmpty()) {
                                returnDate = LocalDateTime.parse(returnStr, formatter);
                            }

                            boolean pickupCondition = pickupDate.isBefore(filterDate);
                            boolean returnCondition = (returnDate == null) || returnDate.isAfter(filterDate);

                            dateMatch = pickupCondition && returnCondition;
                        } catch (Exception ex) {
                            System.out.println("فرمت تاریخ اشتباه بود: " + ex.getMessage());
                        }
                    }

                    return carMatch && empMatch && dateMatch;
                }
            };

            sorter.setRowFilter(rf);
        });

        return searchButton;
    }
}
