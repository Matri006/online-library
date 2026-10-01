package ru.mospolytech.library.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
class JsonColumns {
    private final ObjectMapper mapper;

    JsonColumns(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize database JSON", ex);
        }
    }

    JsonNode read(String value) {
        try {
            return value == null ? null : mapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot read database JSON", ex);
        }
    }

    <T> T read(String value, TypeReference<T> type) {
        try {
            return value == null ? null : mapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot read database JSON", ex);
        }
    }
}
