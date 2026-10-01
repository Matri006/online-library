package ru.mospolytech.library.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import ru.mospolytech.library.services.BootstrapService;

@Component
public class Bootstrap implements CommandLineRunner {
    private final BootstrapService service;
    private final boolean demo;
    private final String adminPassword;
    private final String librarianPassword;
    private final String viewerPassword;

    public Bootstrap(
            BootstrapService service,
            @Value("${library.demo-enabled}") boolean demo,
            @Value("${library.admin-password}") String adminPassword,
            @Value("${library.librarian-password}") String librarianPassword,
            @Value("${library.viewer-password}") String viewerPassword) {
        this.service = service;
        this.demo = demo;
        this.adminPassword = adminPassword;
        this.librarianPassword = librarianPassword;
        this.viewerPassword = viewerPassword;
    }

    @Override
    public void run(String... args) {
        service.initialize(demo, adminPassword, librarianPassword, viewerPassword);
    }
}
