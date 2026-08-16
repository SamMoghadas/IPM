package com.car.rental.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Gregorian ↔ Jalali (Persian / Shamsi) conversion.
 * <p>
 * Algorithm matches jalaali-js (https://github.com/jalaali/jalaali-js) / Borkowski.
 * Pure Java, no external dependencies.
 * <p>
 * Display format is always {@code yyyy/MM/dd} (zero-padded).
 */
public final class JalaliDate {

    private final int year;
    private final int month;
    private final int day;

    public JalaliDate(int year, int month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Invalid Jalali date: " + year + "/" + month + "/" + day);
        }
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public static JalaliDate fromGregorian(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date is null");
        }
        int[] j = toJalaali(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        return new JalaliDate(j[0], j[1], j[2]);
    }

    public static JalaliDate fromGregorian(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime is null");
        }
        return fromGregorian(dateTime.toLocalDate());
    }

    public LocalDate toGregorian() {
        int[] g = toGregorianParts(year, month, day);
        return LocalDate.of(g[0], g[1], g[2]);
    }

    /** Always {@code yyyy/MM/dd} with zero-padding. */
    public String formatDate() {
        return String.format("%04d/%02d/%02d", year, month, day);
    }

    /** {@code yyyy/MM/dd HH:mm:ss} */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        JalaliDate j = fromGregorian(dateTime);
        LocalTime t = dateTime.toLocalTime();
        return String.format("%s %02d:%02d:%02d",
                j.formatDate(), t.getHour(), t.getMinute(), t.getSecond());
    }

    /** {@code yyyy/MM/dd HH:mm} */
    public static String formatDateTimeMinutes(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        JalaliDate j = fromGregorian(dateTime);
        LocalTime t = dateTime.toLocalTime();
        return String.format("%s %02d:%02d",
                j.formatDate(), t.getHour(), t.getMinute());
    }

    // --- jalaali-js compatible core ---

    private static int[] toJalaali(int gy, int gm, int gd) {
        return d2j(g2d(gy, gm, gd));
    }

    private static int[] toGregorianParts(int jy, int jm, int jd) {
        return d2g(j2d(jy, jm, jd));
    }

    private static int[] d2j(int jdn) {
        int gy = d2g(jdn)[0];
        int jy = gy - 621;
        int[] r = jalCal(jy);
        int jdn1f = g2d(gy, 3, r[1]);
        int k = jdn - jdn1f;
        int jm;
        int jd;
        if (k >= 0) {
            if (k <= 185) {
                jm = 1 + div(k, 31);
                jd = mod(k, 31) + 1;
                return new int[]{jy, jm, jd};
            } else {
                k -= 186;
            }
        } else {
            jy -= 1;
            k += 179;
            if (r[0] == 1) {
                k += 1;
            }
        }
        jm = 7 + div(k, 30);
        jd = mod(k, 30) + 1;
        return new int[]{jy, jm, jd};
    }

    private static int j2d(int jy, int jm, int jd) {
        int[] r = jalCal(jy);
        return g2d(r[2], 3, r[1]) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1;
    }

    /**
     * Same as jalaali-js {@code jalCal}.
     *
     * @return {leap, march (day of Farvardin 1 in Gregorian March), gy}
     */
    private static int[] jalCal(int jy) {
        int[] breaks = {
                -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
                1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        };
        int bl = breaks.length;
        if (jy < breaks[0] || jy >= breaks[bl - 1]) {
            throw new IllegalArgumentException("Invalid Jalaali year " + jy);
        }

        int gy = jy + 621;
        int leapJ = -14;
        int jp = breaks[0];
        int jump = 0;

        for (int i = 1; i < bl; i++) {
            int jm = breaks[i];
            jump = jm - jp;
            if (jy < jm) {
                break;
            }
            // jalaali-js: leapJ + div(jump, 33) * 8 + div(mod(jump, 33), 4)
            leapJ = leapJ + div(jump, 33) * 8 + div(mod(jump, 33), 4);
            jp = jm;
        }

        int n = jy - jp;
        // jalaali-js: leapJ + div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        leapJ = leapJ + div(n, 33) * 8 + div(mod(n, 33) + 3, 4);

        // jalaali-js: if (mod(jump, 33) === 4 && jump - n === 4)
        if (mod(jump, 33) == 4 && jump - n == 4) {
            leapJ += 1;
        }

        int leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150;
        int march = 20 + leapJ - leapG;

        if (jump - n < 6) {
            n = n - jump + div(jump + 4, 33) * 33;
        }

        int leap = mod(mod(n + 1, 33) - 1, 4);
        if (leap == -1) {
            leap = 4;
        }

        return new int[]{leap, march, gy};
    }

    private static int g2d(int gy, int gm, int gd) {
        int d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4)
                + div(153 * mod(gm + 12 * div(9 - gm, 6) - 3, 12) + 2, 5)
                + gd - 34840408;
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752;
        return d;
    }

    private static int[] d2g(int jdn) {
        int j = 4 * jdn + 139361631
                + div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4
                - 3908;
        int i = div(mod(j, 1461), 4) * 5 + 308;
        int gd = div(mod(i, 153), 5) + 1;
        int gm = mod(div(i, 153), 12) + 1;
        int gy = div(j, 1461) - 100100 + div(8 - gm, 6);
        return new int[]{gy, gm, gd};
    }

    /** Truncating division toward zero (same as JS ~~(a/b) for these ranges). */
    private static int div(int a, int b) {
        return a / b;
    }

    private static int mod(int a, int b) {
        return a - div(a, b) * b;
    }

    @Override
    public String toString() {
        return formatDate();
    }
}
