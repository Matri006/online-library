package ru.mospolytech.library.entities;

public record BookStock(Long locationId, Long bookId, int copiesCount) {}
