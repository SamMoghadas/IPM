package com.car.rental;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
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

            JTable table = new JTable(data, columns);
            table.setAutoCreateRowSorter(true); // قابلیت مرتب سازی
            table.setFont(new Font("Arial", Font.PLAIN, 12));
            table.setRowHeight(24);
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

            for (int i = 0; i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }


            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane, BorderLayout.CENTER);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "خطا در دریافت گزارش: " + e.getMessage());
        }

        setVisible(true);
    }

    // ----------------- کلاس داخلی رکورد -----------------
    public static class RentalRecord {
        int personnelId;
        String employeeName;
        String carName;
        String carColor;
        String plate;
        String pickupDate;
        String returnDate;
        String destination;

        public RentalRecord(int personnelId, String employeeName, String carName, String carColor,
                            String plate, String pickupDate, String returnDate, String destination) {
            this.personnelId = personnelId;
            this.employeeName = employeeName;
            this.carName = carName;
            this.carColor = carColor;
            this.plate = plate;
            this.pickupDate = pickupDate;
            this.returnDate = returnDate == null ? "منتظر بازگشت" : returnDate;
            this.destination = destination;
        }
    }
}
