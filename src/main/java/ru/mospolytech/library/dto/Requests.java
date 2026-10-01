package ru.mospolytech.library.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class Requests {
    private Requests() {}

    public record Book(
            @NotBlank @Size(max = 500) String title,
            @NotNull @Positive Long publisherId,
            @NotNull @Min(1450) Integer publicationYear,
            @NotNull @Positive Integer pagesCount,
            @NotNull @PositiveOrZero Integer illustrationsCount,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotEmpty List<@NotNull @Positive Long> authorIds) {}

    public record Branch(
            @NotBlank @Size(max = 255) String name,
            String address,
            @NotBlank @Pattern(regexp = "BRANCH|DEPOSITORY") String branchType,
            @Size(max = 32) String phone,
            @NotNull Boolean active) {}

    public record Reference(@NotBlank @Size(max = 255) String name, @NotNull Boolean active) {}

    public record Location(
            @NotNull @Positive Long branchId,
            @NotBlank @Size(max = 50) String code,
            @NotBlank @Size(max = 255) String name,
            @NotNull Boolean active) {}

    public record Student(
            @NotBlank @Size(max = 50) String studentCardNumber,
            @NotBlank @Size(max = 255) String fullName,
            @NotNull @Positive Long facultyId,
            @NotNull Boolean active) {}

    public record Stock(
            @NotNull @Positive Long locationId,
            @NotNull @Positive Long bookId,
            @NotNull @PositiveOrZero Integer copiesCount,
            @NotNull @PositiveOrZero Integer expectedCopiesCount) {}

    public record Usage(
            @NotNull @Positive Long branchId,
            @NotNull @Positive Long bookId,
            @NotNull @Positive Long facultyId) {}

    public record Issue(
            @NotNull @Positive Long locationId,
            @NotNull @Positive Long bookId,
            @NotNull @Positive Long studentId) {}

    public record User(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,100}") String login,
            @Size(max = 72) String password,
            @NotBlank @Pattern(regexp = "ADMIN|LIBRARIAN|VIEWER") String role,
            @NotNull Boolean active) {}
}
