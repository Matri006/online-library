package ru.mospolytech.library.entities;

public record AppUser(
        Long userId, String login, String passwordHash, Long roleId, boolean isActive) {}
