package com.car.rental;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class ReportFrame extends JFrame {

    public ReportFrame() {
        setTitle("گزارش سفرهای ماشین");
        setSize(900, 400);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            sorter.setSortKeys(List.of(new RowSorter.SortKey(5, SortOrder.DESCENDING)));

            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane, BorderLayout.CENTER);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در دریافت گزارش: " + e.getMessage());
        }

        setVisible(true);
    }
}
