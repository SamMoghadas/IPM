package com.car.rental.util;

import java.util.Objects;
import java.util.Optional;

/**
 * Iranian license plate helpers.
 * Storage format (stable, no bidi issues): {@code 12|ب|345|11}
 * Display format (LTR-marked): {@code 12 ب 345 ایران 11}
 */
public final class IranianPlate {

    public static final String STORAGE_SEP = "|";
    private static final String LRM = "\u200E";

    public final String part1;   // 2 digits
    public final String letter; // Persian letter
    public final String part2;  // 3 digits
    public final String city;   // 2 digits

    public IranianPlate(String part1, String letter, String part2, String city) {
        this.part1 = normalizeDigits(part1);
        this.letter = normalizeLetter(letter);
        this.part2 = normalizeDigits(part2);
        this.city = normalizeDigits(city);
    }

    public boolean isValid() {
        return part1.matches("\\d{2}")
                && part2.matches("\\d{3}")
                && city.matches("\\d{2}")
                && letter != null && !letter.isBlank();
    }

    /** DB / internal form: 12|ب|345|11 */
    public String toStorage() {
        return part1 + STORAGE_SEP + letter + STORAGE_SEP + part2 + STORAGE_SEP + city;
    }

    /** Human-readable with LTR mark so Swing does not reorder digits/letters. */
    public String toDisplay() {
        return LRM + part1 + " " + letter + " " + part2 + " ایران " + city;
    }

    /** Same as display but without LRM (for search input matching). */
    public String toDisplayPlain() {
        return part1 + " " + letter + " " + part2 + " ایران " + city;
    }

    public static String normalizeDigits(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u06F0' && c <= '\u06F9') { // Persian digits
                sb.append((char) ('0' + (c - '\u06F0')));
            } else if (c >= '\u0660' && c <= '\u0669') { // Arabic-Indic digits
                sb.append((char) ('0' + (c - '\u0660')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    public static String normalizeLetter(String s) {
        if (s == null) return "";
        String t = s.trim();
        t = t.replace('\u064A', '\u06CC'); // Arabic Yeh -> Persian Yeh
        t = t.replace("هـ", "ه");
        return t;
    }

    /**
     * Parse storage {@code 12|ب|345|11} or legacy display {@code 12 ایران 345 ب 67}
     * (legacy getPlate order was: city ایران mid letter first).
     */
    public static Optional<IranianPlate> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String s = raw.trim();
        // strip LRM / bidi marks
        s = s.replace(LRM, "").replace("\u202A", "").replace("\u202C", "").trim();

        if (s.contains(STORAGE_SEP)) {
            String[] p = s.split("\\" + STORAGE_SEP, -1);
            if (p.length == 4) {
                IranianPlate plate = new IranianPlate(p[0], p[1], p[2], p[3]);
                return plate.isValid() ? Optional.of(plate) : Optional.empty();
            }
            return Optional.empty();
        }

        // Legacy display from old getPlate: city ایران mid letter first
        // e.g. "11 ایران 345 ب 12"
        String[] parts = s.split("\\s+");
        if (parts.length == 5 && parts[1].equals("ایران")) {
            IranianPlate plate = new IranianPlate(parts[4], parts[3], parts[2], parts[0]);
            return plate.isValid() ? Optional.of(plate) : Optional.empty();
        }

        // Alternate reading order sometimes used: first letter mid ایران city
        // e.g. "12 ب 345 ایران 11"
        if (parts.length == 5 && parts[3].equals("ایران")) {
            IranianPlate plate = new IranianPlate(parts[0], parts[1], parts[2], parts[4]);
            return plate.isValid() ? Optional.of(plate) : Optional.empty();
        }

        return Optional.empty();
    }

    /** Safe display string for tables; if unparsable returns original. */
    public static String formatForDisplay(String storedOrLegacy) {
        return parse(storedOrLegacy).map(IranianPlate::toDisplay).orElse(
                storedOrLegacy == null ? "" : LRM + storedOrLegacy.trim());
    }

    /** Normalize any input to storage form if possible. */
    public static String toStorageOrEmpty(String raw) {
        return parse(raw).map(IranianPlate::toStorage).orElse("");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IranianPlate)) return false;
        IranianPlate that = (IranianPlate) o;
        return Objects.equals(toStorage(), that.toStorage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toStorage());
    }

    @Override
    public String toString() {
        return toDisplayPlain();
    }
}
