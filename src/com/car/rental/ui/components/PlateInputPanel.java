package com.car.rental.ui.components;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class PlateInputPanel extends JPanel {

    private JTextField firstTwo;
    private JComboBox<String> letterCombo;
    private JTextField middleThree;
    private JTextField cityCode;

    public PlateInputPanel() {
        initPanel();
        initComponents();
        initLayout();
    }

    public PlateInputPanel(String plate) {
        this();
        setPlate(plate);
    }

    private void initPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBackground(Color.WHITE);
    }

    private void initComponents() {
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
    }

    private void initLayout() {
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

    public String getPlate() {
        if (!isComplete()) return "";
        return cityCode.getText() + " ایران " +
                middleThree.getText() + " " +
                letterCombo.getSelectedItem() + " " +
                firstTwo.getText();
    }

    /**
     * Expected format from getPlate(): {@code city ایران mid letter first}
     * e.g. {@code 11 ایران 345 ب 12}
     * Never throws — incomplete/unknown plates leave fields empty.
     */
    public void setPlate(String plate) {
        if (plate == null || plate.isBlank()) {
            return;
        }

        String[] parts = plate.trim().split("\\s+");
        if (parts.length < 5) {
            // Cannot safely map — avoid ArrayIndexOutOfBoundsException
            return;
        }

        // Preferred: city ایران mid letter first (matches getPlate)
        String city;
        String mid;
        String letter;
        String first;
        if ("ایران".equals(parts[1]) && parts.length >= 5) {
            city = parts[0];
            mid = parts[2];
            letter = parts[3];
            first = parts[4];
        } else {
            // Unknown layout — do not crash
            return;
        }

        firstTwo.setText(first);
        letterCombo.setSelectedItem(letter);
        middleThree.setText(mid);
        cityCode.setText(city);
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
