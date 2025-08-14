package com.car.rental;

import javax.swing.*;
import java.awt.*;

public class ReportFrame extends JFrame {
    public ReportFrame() {
        setTitle("گزارش سفرهای ماشین");
        setSize(600, 400);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);


        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Arial", Font.PLAIN, 12));
        reportArea.setText("سفر 1: ماشین تویوتا کمری - کارمند علی محمدی - زمان تحویل: 2025-08-05 10:00 - مقصد: تهران - زمان بازگشت: 2025-08-07 10:00\n\nسفر 2: ماشین هیوندای سوناتا - کارمند رضا احمدی - زمان تحویل: 2025-08-06 14:00 - مقصد: اصفهان - زمان بازگشت: 2025-08-08 14:00");
        JScrollPane scrollPane = new JScrollPane(reportArea);

        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }
}