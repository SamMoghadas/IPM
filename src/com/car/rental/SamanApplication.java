package com.car.rental;

import com.car.rental.db.DatabaseManager;
import com.car.rental.ui.frames.MainFrame;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.SwingUtilities;

/**
 * Spring Boot entry point. Starts the application context, initializes the DB,
 * then opens the existing Swing MainFrame on the EDT.
 *
 * Run: mvn spring-boot:run
 * Or run this class from IntelliJ (classpath = Maven).
 */
@SpringBootApplication
public class SamanApplication {

    public static void main(String[] args) {
        // Ensure AWT/Swing is allowed
        System.setProperty("java.awt.headless", "false");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(SamanApplication.class)
                .headless(false)
                .run(args);

        DatabaseManager db = context.getBean(DatabaseManager.class);
        db.initDatabase();

        SwingUtilities.invokeLater(() -> {
            // MainFrame resolves beans via SpringContext / constructors
            new MainFrame();
        });
    }
}
