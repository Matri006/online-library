package ru.mospolytech.library;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.services.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(username = "admin", roles = "ADMIN")
class LibraryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> db =
            new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init-test.sql");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", () -> "library_app");
        r.add("spring.datasource.password", () -> "test-app");
        r.add("spring.flyway.url", db::getJdbcUrl);
        r.add("spring.flyway.user", db::getUsername);
        r.add("spring.flyway.password", db::getPassword);
        r.add("library.demo-enabled", () -> false);
        r.add("library.admin-password", () -> "TestAdmin123!");
    }

    @Autowired BookService books;
    @Autowired BranchService branches;
    @Autowired ReferenceService references;
    @Autowired LocationService locations;
    @Autowired StudentService students;
    @Autowired StockService stocks;
    @Autowired UsageService usages;
    @Autowired LoanService loans;
    @Autowired ReportService reports;
    @Autowired UserService users;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    long book, branch, location, author, publisher, faculty, student;

    Requests.Book bookData(String title, int year, List<Long> authors) {
        return new Requests.Book(title, publisher, year, 200, 0, new BigDecimal("500.00"), authors);
    }

    @BeforeEach
    void prepare() {
        var owner =
                new JdbcTemplate(
                        new DriverManagerDataSource(
                                db.getJdbcUrl(), db.getUsername(), db.getPassword()));
        owner.execute(
                "truncate"
                    + " loan,book_usage,book_stock,book_author,book,author,publisher,student,faculty,storage_location,branch,audit_log"
                    + " restart identity cascade");
        publisher =
                references.save("publishers", null, new Requests.Reference("Издательство", true));
        author = references.save("authors", null, new Requests.Reference("Автор", true));
        faculty = references.save("faculties", null, new Requests.Reference("Факультет", true));
        book = books.save(null, bookData("Книга", 2024, List.of(author)));
        branch = branches.save(null, new Requests.Branch("Филиал", "Адрес", "BRANCH", null, true));
        location =
                jdbc.queryForObject(
                        "select location_id from storage_location where branch_id=?",
                        Long.class,
                        branch);
        student = students.save(null, new Requests.Student("001", "Студент", faculty, true));
    }

    @Test
    void stockAndIndependentUsage() {
        assertEquals(0, reports.book(book, branch).copies());
        usages.add(new Requests.Usage(branch, book, faculty));
        long other =
                references.save("faculties", null, new Requests.Reference("А факультет", true));
        usages.add(new Requests.Usage(branch, book, other));
        assertEquals(2, reports.book(book, branch).facultyCount());
        var names = reports.book(book, branch).faculties();
        assertEquals("А факультет", names.getFirst().name());
        stocks.setStock(new Requests.Stock(location, book, 4, 0));
        long second = locations.save(null, new Requests.Location(branch, "second", "Второе", true));
        stocks.setStock(new Requests.Stock(second, book, 3, 0));
        assertEquals(7, reports.book(book, branch).copies());
    }

    @Test
    void duplicateBookRollsBackIncludingAuditAndAuthors() {
        long before = jdbc.queryForObject("select count(*) from audit_log", Long.class);
        assertThrows(
                RuntimeException.class,
                () -> books.save(null, bookData(" КНИГА ", 2024, List.of(author))));
        assertEquals(1, jdbc.queryForObject("select count(*) from book", Integer.class));
        assertEquals(before, jdbc.queryForObject("select count(*) from audit_log", Long.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from book_author", Integer.class));
    }

    @Test
    void updatePreservesAuthorsAndBranchLinks() {
        long second =
                references.save("authors", null, new Requests.Reference("Второй автор", true));
        books.save(book, bookData("Новое название", 2025, List.of(author, second)));
        stocks.setStock(new Requests.Stock(location, book, 2, 0));
        branches.save(branch, new Requests.Branch("Новое имя", null, "DEPOSITORY", null, true));
        assertEquals(2, reports.book(book, branch).copies());
        assertEquals(
                2,
                jdbc.queryForObject(
                        "select count(*) from book_author where book_id=?", Integer.class, book));
        assertThrows(
                RuntimeException.class,
                () ->
                        branches.save(
                                null,
                                new Requests.Branch(" новое ИМЯ ", null, "BRANCH", null, true)));
    }

    @Test
    void authorSetOrderDoesNotAllowDuplicate() {
        long second = references.save("authors", null, new Requests.Reference("Второй", true));
        books.save(book, bookData("Книга", 2024, List.of(author, second)));
        assertThrows(
                RuntimeException.class,
                () -> books.save(null, bookData("Книга", 2024, List.of(second, author))));
    }

    @Test
    void invalidYearAndAuthorRemovalRejected() throws Exception {
        assertThrows(
                RuntimeException.class,
                () -> books.save(book, bookData("Книга", 3000, List.of(author))));
        assertThrows(
                RuntimeException.class,
                () -> jdbc.update("delete from book_author where book_id=?", book));
        mvc.perform(
                        put("/api/stock")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"locationId\":"
                                                + location
                                                + ",\"bookId\":"
                                                + book
                                                + ",\"copiesCount\":-1,\"expectedCopiesCount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NEGATIVE_STOCK"));
        assertEquals(0, reports.book(book, branch).copies());
        assertTrue(
                jdbc.queryForObject(
                        "select exists(select 1 from audit_log where"
                                + " details->>'code'='NEGATIVE_STOCK')",
                        Boolean.class));
    }

    @Test
    void loansAndDistinctStudentsKeepHistoricalFaculty() {
        stocks.setStock(new Requests.Stock(location, book, 1, 0));
        long loan = loans.issue(new Requests.Issue(location, book, student));
        assertEquals(0L, reports.book(book, branch).available());
        assertThrows(
                LibraryException.class,
                () -> loans.issue(new Requests.Issue(location, book, student)));
        assertThrows(
                RuntimeException.class,
                () -> stocks.setStock(new Requests.Stock(location, book, 0, 1)));
        long second = references.save("faculties", null, new Requests.Reference("Другой", true));
        students.save(student, new Requests.Student("001", "Студент", second, true));
        assertEquals(1, reports.studentCount(book, null, faculty, null, null));
        assertEquals(0, reports.studentCount(book, null, second, null, null));
        loans.returnLoan(loan);
        assertThrows(LibraryException.class, () -> loans.returnLoan(loan));
        loans.issue(new Requests.Issue(location, book, student));
        assertEquals(1, reports.studentCount(book, null, null, null, null));
        assertEquals(
                0, reports.studentCount(book, null, null, OffsetDateTime.now().plusDays(1), null));
    }

    @Test
    void locationWithHistoryCannotMoveAndDeactivationDoesNotRemoveStock() {
        stocks.setStock(new Requests.Stock(location, book, 2, 0));
        loans.issue(new Requests.Issue(location, book, student));
        long other = branches.save(null, new Requests.Branch("Другой", null, "BRANCH", null, true));
        assertThrows(
                RuntimeException.class,
                () -> locations.save(location, new Requests.Location(other, "main", "Зал", true)));
        locations.save(location, new Requests.Location(branch, "main", "Зал", false));
        assertEquals(2, reports.book(book, branch).copies());
        assertThrows(
                LibraryException.class,
                () -> loans.issue(new Requests.Issue(location, book, student)));
    }

    @Test
    void optimisticStockUpdateRejectsStaleValue() {
        stocks.setStock(new Requests.Stock(location, book, 3, 0));
        assertThrows(
                LibraryException.class,
                () -> stocks.setStock(new Requests.Stock(location, book, 8, 0)));
        assertEquals(3, reports.book(book, branch).copies());
    }

    @Test
    void concurrentIssueCannotOversell() throws Exception {
        stocks.setStock(new Requests.Stock(location, book, 1, 0));
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> issue =
                    () -> {
                        SecurityContextHolder.getContext()
                                .setAuthentication(
                                        UsernamePasswordAuthenticationToken.authenticated(
                                                "admin", null, List.of()));
                        try {
                            barrier.await(10, TimeUnit.SECONDS);
                            loans.issue(new Requests.Issue(location, book, student));
                            return true;
                        } catch (LibraryException e) {
                            assertEquals("NO_AVAILABLE_COPIES", e.code);
                            return false;
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    };
            var a = executor.submit(issue);
            var b = executor.submit(issue);
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
        assertEquals(
                1,
                jdbc.queryForObject(
                        "select count(*) from loan where returned_at is null", Integer.class));
    }

    @Test
    void unknownObjectsAreErrors() throws Exception {
        mvc.perform(
                        get("/api/reports/book")
                                .param("bookId", "999999")
                                .param("branchId", Long.toString(branch)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"));
    }

    @Test
    void viewerCannotWriteAndDenialIsAudited() throws Exception {
        if (jdbc.queryForObject("select count(*) from app_user where login='reader'", Integer.class)
                == 0) users.save(null, new Requests.User("reader", "Reader123!", "VIEWER", true));
        mvc.perform(
                        post("/api/branches")
                                .with(user("reader").roles("VIEWER"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"name\":\"X\",\"branchType\":\"BRANCH\",\"active\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        assertTrue(
                jdbc.queryForObject(
                        "select exists(select 1 from audit_log where"
                                + " details->>'code'='ACCESS_DENIED')",
                        Boolean.class));
    }

    @Test
    void csrfIsRequiredAndAuditIsAppendOnly() throws Exception {
        mvc.perform(post("/api/books").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        assertThrows(RuntimeException.class, () -> jdbc.update("delete from audit_log"));
        assertThrows(
                RuntimeException.class,
                () -> jdbc.update("update audit_log set operation='forged'"));
    }

    @Test
    void catalogueResponsesKeepExistingJsonFields() throws Exception {
        mvc.perform(get("/api/books").param("q", "Автор").param("page", "-1").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].book_id").value(book))
                .andExpect(jsonPath("$.items[0].publisher_name").value("Издательство"))
                .andExpect(jsonPath("$.items[0].author_ids[0]").value(author))
                .andExpect(jsonPath("$.items[0].authors").value("Автор"))
                .andExpect(jsonPath("$.items[0].publication_year").value(2024));
        mvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].branch_id").value(branch))
                .andExpect(jsonPath("$[0].is_active").value(true));
        mvc.perform(get("/api/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].location_id").value(location))
                .andExpect(jsonPath("$[0].branch_name").value("Филиал"))
                .andExpect(jsonPath("$[0].branch_active").value(true));
        mvc.perform(get("/api/students").param("q", "001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].student_id").value(student))
                .andExpect(jsonPath("$[0].student_card_number").value("001"))
                .andExpect(jsonPath("$[0].faculty_name").value("Факультет"));
    }

    @Test
    void referenceResponsesAndUpdatesKeepTheirContract() throws Exception {
        references.save("authors", author, new Requests.Reference("Новый автор", true));
        references.save(
                "publishers", publisher, new Requests.Reference("Новое издательство", true));
        references.save("faculties", faculty, new Requests.Reference("Новый факультет", false));
        mvc.perform(get("/api/references/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].author_id").value(author))
                .andExpect(jsonPath("$[0].full_name").value("Новый автор"))
                .andExpect(jsonPath("$[0].name").value("Новый автор"))
                .andExpect(jsonPath("$[0].faculty_id").doesNotExist());
        mvc.perform(get("/api/references/publishers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publisher_id").value(publisher))
                .andExpect(jsonPath("$[0].name").value("Новое издательство"));
        mvc.perform(get("/api/references/faculties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].faculty_id").value(faculty))
                .andExpect(jsonPath("$[0].is_active").value(false));
        mvc.perform(get("/api/references/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENTITY_NOT_FOUND"));
    }

    @Test
    void loanStockUsageAndReportResponsesKeepTheirContract() throws Exception {
        stocks.setStock(new Requests.Stock(location, book, 3, 0));
        usages.add(new Requests.Usage(branch, book, faculty));
        long loan = loans.issue(new Requests.Issue(location, book, student));
        mvc.perform(
                        get("/api/stock")
                                .param("bookId", Long.toString(book))
                                .param("branchId", Long.toString(branch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].copies_count").value(3))
                .andExpect(jsonPath("$[0].loaned").value(1))
                .andExpect(jsonPath("$[0].available").value(2));
        mvc.perform(get("/api/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].faculty_id").value(faculty))
                .andExpect(jsonPath("$[0].faculty_name").value("Факультет"));
        mvc.perform(get("/api/loans").param("open", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].loan_id").value(loan))
                .andExpect(jsonPath("$[0].issued_at").isString())
                .andExpect(jsonPath("$[0].returned_at").isEmpty())
                .andExpect(jsonPath("$[0].faculty_at_issue_id").value(faculty));
        mvc.perform(
                        get("/api/reports/book")
                                .param("bookId", Long.toString(book))
                                .param("branchId", Long.toString(branch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facultyCount").value(1))
                .andExpect(jsonPath("$.studentCount").value(1))
                .andExpect(jsonPath("$.faculties[0].faculty_id").value(faculty));
        mvc.perform(post("/api/loans/{id}/return", loan).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].returned_at").isString())
                .andExpect(jsonPath("$[0].returned_by_user_id").isNumber());
        mvc.perform(get("/api/loans").param("open", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(
                        delete("/api/usage")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        "{\"branchId\":"
                                                + branch
                                                + ",\"bookId\":"
                                                + book
                                                + ",\"facultyId\":"
                                                + faculty
                                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
        assertTrue(usages.findAll().isEmpty());
    }

    @Test
    void usersAndAuditNeverExposePasswordHashes() throws Exception {
        long id =
                users.save(
                        null,
                        new Requests.User("contract-user", "Password123!", "LIBRARIAN", true));
        String hash =
                jdbc.queryForObject(
                        "select password_hash from app_user where user_id = ?", String.class, id);
        users.save(id, new Requests.User("contract-user", "", "VIEWER", false));
        assertEquals(
                hash,
                jdbc.queryForObject(
                        "select password_hash from app_user where user_id = ?", String.class, id));
        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.login == 'contract-user')].role").value("VIEWER"))
                .andExpect(jsonPath("$[?(@.login == 'contract-user')].is_active").value(false))
                .andExpect(jsonPath("$[*].password_hash").isEmpty());
        mvc.perform(get("/api/audit").param("q", "app_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].details.new.login").value("contract-user"))
                .andExpect(jsonPath("$[0].entity_key.user_id").value(id))
                .andExpect(jsonPath("$[0].login").value("admin"))
                .andExpect(jsonPath("$[0].details.new.password_hash").doesNotExist());
    }

    @Test
    void authenticationUsesRepositoryAndRefreshesAccountRole() throws Exception {
        long id =
                users.save(null, new Requests.User("refresh-user", "Password123!", "ADMIN", true));
        users.save(id, new Requests.User("refresh-user", "", "VIEWER", true));
        mvc.perform(
                        org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestBuilders.formLogin("/api/login")
                                .user(" AdMiN ")
                                .password("TestAdmin123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
        mvc.perform(get("/api/users").with(user("refresh-user").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/users/{id}", id)
                                .with(user("admin").roles("ADMIN"))
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        """
                                        {"login":"refresh-user","password":"","role":"VIEWER","active":false}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/books").with(user("refresh-user").roles("ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingUpdatesAndSelfLockoutRemainDomainErrors() {
        assertEquals(
                "ENTITY_NOT_FOUND",
                assertThrows(
                                LibraryException.class,
                                () ->
                                        students.save(
                                                999999L,
                                                new Requests.Student("999", "Нет", faculty, true)))
                        .code);
        assertEquals(
                "ENTITY_NOT_FOUND",
                assertThrows(
                                LibraryException.class,
                                () ->
                                        references.save(
                                                "authors",
                                                999999L,
                                                new Requests.Reference("Нет", true)))
                        .code);
        long admin =
                jdbc.queryForObject(
                        "select user_id from app_user where login = 'admin'", Long.class);
        assertEquals(
                "SELF_LOCKOUT",
                assertThrows(
                                LibraryException.class,
                                () ->
                                        users.save(
                                                admin,
                                                new Requests.User("admin", "", "VIEWER", true)))
                        .code);
        assertEquals(
                "INVALID_DATA",
                assertThrows(
                                LibraryException.class,
                                () ->
                                        reports.studentCount(
                                                book,
                                                null,
                                                null,
                                                OffsetDateTime.now(),
                                                OffsetDateTime.now().minusDays(1)))
                        .code);
    }
}
