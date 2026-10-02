package ru.mospolytech.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibraryApplication {
    public static void main(String[] args) {
        var context = SpringApplication.run(LibraryApplication.class, args);
        // A release is a one-off process using the same artifact as the web server.
        if (context.getEnvironment().matchesProfiles("release")
                && "none".equals(context.getEnvironment().getProperty("spring.main.web-application-type"))) {
            context.close();
        }
    }
}
