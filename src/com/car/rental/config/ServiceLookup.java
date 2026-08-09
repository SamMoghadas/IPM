package com.car.rental.config;

/**
 * Resolve Spring beans from Swing frames. Application must be started via {@code SamanApplication}.
 */
public final class ServiceLookup {

    private ServiceLookup() {
    }

    public static <T> T get(Class<T> type) {
        if (!SpringContext.isActive()) {
            throw new IllegalStateException(
                    "Spring context is not active. Run the app via com.car.rental.SamanApplication.");
        }
        return SpringContext.getBean(type);
    }
}
