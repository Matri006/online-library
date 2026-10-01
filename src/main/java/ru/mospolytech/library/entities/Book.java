package ru.mospolytech.library.entities;

import java.math.BigDecimal;

public record Book(
        Long bookId,
        String title,
        Long publisherId,
        Integer publicationYear,
        Integer pagesCount,
        Integer illustrationsCount,
        BigDecimal price) {}
