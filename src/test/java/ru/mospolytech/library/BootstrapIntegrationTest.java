package ru.mospolytech.library;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import ru.mospolytech.library.services.AccountService;
import ru.mospolytech.library.services.BookService;
import ru.mospolytech.library.services.BootstrapService;
import ru.mospolytech.library.services.ReportService;

@org.springframework.test.context.ActiveProfiles("release")
@SpringBootTest(properties = "spring.flyway.enabled=true")
@Testcontainers
class BootstrapIntegrationTest {
    @Container
    static PostgreSQLContainer<?> db =
            new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init-test.sql");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", db::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "library_app");
        registry.add("spring.datasource.password", () -> "test-app");
        registry.add("spring.flyway.url", db::getJdbcUrl);
        registry.add("spring.flyway.user", db::getUsername);
        registry.add("spring.flyway.password", db::getPassword);
        registry.add("library.demo-enabled", () -> true);
        registry.add("library.admin-password", () -> "TestAdmin123!");
        registry.add("library.librarian-password", () -> "TestLibrarian123!");
        registry.add("library.viewer-password", () -> "TestViewer123!");
    }

    @Autowired BootstrapService bootstrap;
    @Autowired BookService books;
    @Autowired ReportService reports;
    @Autowired AccountService accounts;
    @Autowired JdbcTemplate jdbc;

    @Test
    void demoCatalogueAndAccountsAreCreatedTogether() {
        assertEquals(3, books.findAll("", 0, 20, "title").total());
        assertEquals(12, reports.book(1, 1).copies());
        assertEquals(2, reports.book(1, 1).facultyCount());
        var librarian = accounts.loadUserByUsername("LIBRARIAN");
        assertTrue(librarian.isEnabled());
        assertTrue(
                librarian.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN")));
        assertEquals(3, jdbc.queryForObject("select count(*) from app_user", Integer.class));
    }

    @Test
    void restartingBootstrapDoesNotDuplicateOrReplaceData() {
        String hash = accounts.loadUserByUsername("admin").getPassword();
        bootstrap.initialize(
                true, "DifferentAdmin123!", "DifferentLibrarian123!", "DifferentViewer123!");
        assertEquals(hash, accounts.loadUserByUsername("admin").getPassword());
        assertEquals(3, books.findAll("", 0, 20, "title").total());
        assertEquals(3, jdbc.queryForObject("select count(*) from app_user", Integer.class));
    }
}
