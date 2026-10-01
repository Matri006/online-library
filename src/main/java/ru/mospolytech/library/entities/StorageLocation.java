package ru.mospolytech.library.entities;

public record StorageLocation(
        Long locationId, Long branchId, String code, String name, boolean isActive) {}
