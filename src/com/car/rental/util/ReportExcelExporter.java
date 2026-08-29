package com.car.rental.util;

import com.car.rental.model.RentalRecord;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes rental report rows to an .xlsx file (Apache POI).
 */
public final class ReportExcelExporter {

    private static final String[] HEADERS = {
            "شناسه کارمند", "نام کارمند", "ماشین", "رنگ", "پلاک",
            "تاریخ تحویل", "تاریخ برگشت", "مقصد"
    };

    private ReportExcelExporter() {
    }

    public static void export(List<RentalRecord> records, Path target) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("گزارش سفرها");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (RentalRecord r : records) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(r.deviceUserId));
                row.createCell(1).setCellValue(nullToEmpty(r.employeeName));
                row.createCell(2).setCellValue(nullToEmpty(r.carName));
                row.createCell(3).setCellValue(nullToEmpty(r.carColor));
                row.createCell(4).setCellValue(nullToEmpty(r.plate));
                row.createCell(5).setCellValue(nullToEmpty(r.pickupDate));
                row.createCell(6).setCellValue(nullToEmpty(r.returnDate));
                row.createCell(7).setCellValue(nullToEmpty(r.destination));
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            Files.createDirectories(target.getParent() != null ? target.getParent() : Path.of("."));
            try (OutputStream out = Files.newOutputStream(target)) {
                wb.write(out);
            }
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
