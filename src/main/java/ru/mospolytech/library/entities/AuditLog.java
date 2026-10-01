package ru.mospolytech.library.entities;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

public record AuditLog(
        Long eventId,
        OffsetDateTime eventTime,
        Long userId,
        String operation,
        String entityType,
        JsonNode entityKey,
        JsonNode details) {}
