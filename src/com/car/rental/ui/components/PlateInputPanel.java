package com.car.rental.ui.components;

import com.car.rental.util.IranianPlate;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Optional;

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
        setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    }

    private void initComponents() {
        Dimension fieldSize = new Dimension(40, 24);

        firstTwo = new JTextField(2);
        firstTwo.setDocument(new LimitDigitsDocument(2));
        firstTwo.setPreferredSize(fieldSize);
        firstTwo.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        firstTwo.setHorizontalAlignment(JTextField.CENTER);

        String[] letters = {"الف", "ب", "پ", "ت", "ث", "ج", "د", "ز", "س", "ش",
                "ص", "ط", "ع", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و", "ه", "ی"};
        letterCombo = new JComboBox<>(letters);
        letterCombo.setPreferredSize(new Dimension(55, 24));

        middleThree = new JTextField(3);
        middleThree.setDocument(new LimitDigitsDocument(3));
        middleThree.setPreferredSize(new Dimension(50, 24));
        middleThree.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        middleThree.setHorizontalAlignment(JTextField.CENTER);

        cityCode = new JTextField(2);
        cityCode.setDocument(new LimitDigitsDocument(2));
        cityCode.setPreferredSize(fieldSize);
        cityCode.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        cityCode.setHorizontalAlignment(JTextField.CENTER);
    }

    private void initLayout() {
        // Visual LTR order matching physical plate reading: 12 | letter | 345 | ایران | city
        add(firstTwo);
        add(Box.createHorizontalStrut(5));
        add(letterCombo);
        add(Box.createHorizontalStrut(5));
        add(middleThree);
        add(Box.createHorizontalStrut(5));
        JLabel iran = new JLabel("ایران");
        iran.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        add(iran);
        add(Box.createHorizontalStrut(5));
        add(cityCode);
    }

    /**
     * Value stored in DB: {@code 12|ب|345|11}
     */
    public String getPlate() {
        if (!isComplete()) return "";
        IranianPlate plate = new IranianPlate(
                firstTwo.getText(),
                String.valueOf(letterCombo.getSelectedItem()),
                middleThree.getText(),
                cityCode.getText()
        );
        return plate.isValid() ? plate.toStorage() : "";
    }

    /** Human-readable form for labels (with LTR mark). */
    public String getPlateDisplay() {
        if (!isComplete()) return "";
        IranianPlate plate = new IranianPlate(
                firstTwo.getText(),
                String.valueOf(letterCombo.getSelectedItem()),
                middleThree.getText(),
                cityCode.getText()
        );
        return plate.isValid() ? plate.toDisplay() : "";
    }

    /**
     * Accepts storage form or legacy display strings. Never throws.
     */
    public void setPlate(String plate) {
        if (plate == null || plate.isBlank()) {
            clear();
            return;
        }
        Optional<IranianPlate> parsed = IranianPlate.parse(plate);
        if (parsed.isEmpty()) {
            clear();
            return;
        }
        IranianPlate p = parsed.get();
        firstTwo.setText(p.part1);
        middleThree.setText(p.part2);
        cityCode.setText(p.city);

        // Select letter if in list; otherwise add temporarily so value is visible
        boolean found = false;
        for (int i = 0; i < letterCombo.getItemCount(); i++) {
            if (letterCombo.getItemAt(i).equals(p.letter)) {
                letterCombo.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            letterCombo.addItem(p.letter);
            letterCombo.setSelectedItem(p.letter);
        }
    }

    public boolean isComplete() {
        return firstTwo.getText().length() == 2 &&
                middleThree.getText().length() == 3 &&
                cityCode.getText().length() == 2 &&
                letterCombo.getSelectedItem() != null;
    }

    public void clear() {
        firstTwo.setText("");
        middleThree.setText("");
        cityCode.setText("");
        if (letterCombo.getItemCount() > 0) {
            letterCombo.setSelectedIndex(0);
        }
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
            String normalized = IranianPlate.normalizeDigits(str);
            if ((getLength() + normalized.length()) <= limit && normalized.matches("\\d+")) {
                super.insertString(offset, normalized, attr);
            }
        }
    }
}
