package ru.mospolytech.library.entities;

public record Student(
        Long studentId,
        String studentCardNumber,
        String fullName,
        Long facultyId,
        boolean isActive) {}
