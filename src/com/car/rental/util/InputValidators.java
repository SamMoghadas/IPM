package com.car.rental.util;

/**
 * Shared form validation helpers for Swing UI.
 */
public final class InputValidators {

    /** Latin letters, spaces, hyphen, apostrophe, period — suitable for ZK device names. */
    private static final String ENGLISH_NAME_PATTERN = "[A-Za-z][A-Za-z .'-]*";

    private InputValidators() {
    }

    /**
     * @return null if valid; otherwise a Persian error message
     */
    public static String validateEnglishFullName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "نام کامل را وارد کنید";
        }
        String name = raw.strip();
        if (!name.matches(ENGLISH_NAME_PATTERN)) {
            return "نام کامل باید انگلیسی باشد (فقط حروف لاتین، فاصله و - ' .)";
        }
        if (name.length() > 48) {
            return "نام کامل نباید بیشتر از ۴۸ کاراکتر باشد";
        }
        return null;
    }
}
