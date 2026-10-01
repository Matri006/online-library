package ru.mospolytech.library.entities;

public record Branch(
        Long branchId,
        String name,
        String address,
        String branchType,
        String phone,
        boolean isActive) {}
