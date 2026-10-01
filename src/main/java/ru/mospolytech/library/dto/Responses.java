package ru.mospolytech.library.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class Responses {
    private Responses() {}

    public record Id(long id) {}

    public record Success(boolean ok) {
        public static final Success OK = new Success(true);
    }

    public record Count(long count) {}

    public record Page<T>(List<T> items, long total, int page, int size) {}

    public record Session(
            boolean authenticated,
            String login,
            String role,
            String csrfToken,
            String csrfHeader) {}

    public record BookReport(
            int copies,
            long available,
            long loaned,
            int facultyCount,
            List<FacultySummary> faculties,
            long studentCount,
            String message) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Book(
            Long bookId,
            String title,
            Long publisherId,
            Integer publicationYear,
            Integer pagesCount,
            Integer illustrationsCount,
            BigDecimal price,
            String publisherName,
            List<Long> authorIds,
            String authors) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Branch(
            Long branchId,
            String name,
            String address,
            String branchType,
            String phone,
            boolean isActive) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Location(
            Long locationId,
            Long branchId,
            String code,
            String name,
            boolean isActive,
            String branchName,
            boolean branchActive) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Student(
            Long studentId,
            String studentCardNumber,
            String fullName,
            Long facultyId,
            boolean isActive,
            String facultyName) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Stock(
            Long locationId,
            Long bookId,
            int copiesCount,
            String title,
            String locationName,
            Long branchId,
            String branchName,
            long loaned,
            long available) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Usage(
            Long branchId,
            Long bookId,
            Long facultyId,
            String title,
            String branchName,
            String facultyName) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Loan(
            Long loanId,
            Long locationId,
            Long bookId,
            Long studentId,
            Long facultyAtIssueId,
            OffsetDateTime issuedAt,
            OffsetDateTime returnedAt,
            Long issuedByUserId,
            Long returnedByUserId,
            String title,
            String fullName,
            String studentCardNumber,
            String facultyName,
            String locationName,
            String branchName) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record User(Long userId, String login, boolean isActive, String role) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Audit(
            Long eventId,
            OffsetDateTime eventTime,
            Long userId,
            String operation,
            String entityType,
            JsonNode entityKey,
            JsonNode details,
            String login) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record FacultySummary(Long facultyId, String name) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Reference(
            Long authorId,
            Long publisherId,
            Long facultyId,
            String fullName,
            String name,
            Boolean isActive) {}
}
