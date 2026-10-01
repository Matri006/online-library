package ru.mospolytech.library.entities;

import java.time.OffsetDateTime;

public record Loan(
        Long loanId,
        Long locationId,
        Long bookId,
        Long studentId,
        Long facultyAtIssueId,
        OffsetDateTime issuedAt,
        OffsetDateTime returnedAt,
        Long issuedByUserId,
        Long returnedByUserId) {}
