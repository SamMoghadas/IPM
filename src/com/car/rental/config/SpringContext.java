package com.car.rental.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Allows legacy Swing code (new Frame()) to obtain Spring beans
 * without rewriting every constructor at once.
 */
@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        SpringContext.context = applicationContext;
    }

    public static boolean isActive() {
        return context != null;
    }

    public static <T> T getBean(Class<T> type) {
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext is not initialized");
        }
        return context.getBean(type);
    }

    public static ApplicationContext getContext() {
        return context;
    }
}
