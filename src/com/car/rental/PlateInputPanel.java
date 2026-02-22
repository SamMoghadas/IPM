package com.car.rental;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class PlateInputPanel extends JPanel {

    private final JTextField firstTwo;
    private final JComboBox<String> letterCombo;
    private final JTextField middleThree;
    private final JTextField cityCode;

    public PlateInputPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(Color.WHITE);

        Dimension fieldSize = new Dimension(40, 24);

        firstTwo = new JTextField(2);
        firstTwo.setDocument(new LimitDigitsDocument(2));
        firstTwo.setPreferredSize(fieldSize);

        String[] letters = {"الف", "ب", "ج", "د", "س", "ص", "ط", "ق", "گ", "ل", "م", "ن", "و", "هـ", "ی"};
        letterCombo = new JComboBox<>(letters);
        letterCombo.setPreferredSize(new Dimension(50, 24));

        middleThree = new JTextField(3);
        middleThree.setDocument(new LimitDigitsDocument(3));
        middleThree.setPreferredSize(new Dimension(50, 24));

        cityCode = new JTextField(2);
        cityCode.setDocument(new LimitDigitsDocument(2));
        cityCode.setPreferredSize(fieldSize);

        add(firstTwo);
        add(Box.createHorizontalStrut(5));
        add(letterCombo);
        add(Box.createHorizontalStrut(5));
        add(middleThree);
        add(Box.createHorizontalStrut(5));
        add(new JLabel("ایران"));
        add(Box.createHorizontalStrut(5));
        add(cityCode);
    }

    // ----------------- متدهای کاربردی -----------------

    public String getPlate() {
        if (!isComplete()) return "";
        return cityCode.getText() + " ایران " +
                middleThree.getText() + " " +
                letterCombo.getSelectedItem() + " " +
                firstTwo.getText();
    }

    public boolean isComplete() {
        return !firstTwo.getText().isEmpty() &&
                !middleThree.getText().isEmpty() &&
                !cityCode.getText().isEmpty();
    }

    public void clear() {
        firstTwo.setText("");
        middleThree.setText("");
        cityCode.setText("");
        letterCombo.setSelectedIndex(0);
    }

    // ----------------- کنترل ارقام -----------------
    static class LimitDigitsDocument extends PlainDocument {
        private final int limit;

        public LimitDigitsDocument(int limit) {
            this.limit = limit;
        }

        @Override
        public void insertString(int offset, String str, AttributeSet attr)
                throws BadLocationException {
            if (str == null) return;

            if ((getLength() + str.length()) <= limit && str.matches("\\d+")) {
                super.insertString(offset, str, attr);
            }
        }
    }
}
