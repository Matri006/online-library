package ru.mospolytech.library.controllers;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.services.AuditService;

import java.util.*;

@RestControllerAdvice
public class Errors {
    private final AuditService audit;

    public Errors(AuditService audit) {
        this.audit = audit;
    }

    static final Map<String, String> MESSAGES =
            Map.ofEntries(
                    Map.entry(
                            "INVALID_PUBLICATION_YEAR",
                            "Год издания должен быть от 1450 до следующего года."),
                    Map.entry(
                            "INVALID_BOOK_DATA",
                            "Проверьте данные книги: страницы > 0, иллюстрации и стоимость ≥ 0."),
                    Map.entry("AUTHOR_REQUIRED", "Выберите хотя бы одного автора."),
                    Map.entry(
                            "NEGATIVE_STOCK",
                            "Количество экземпляров не может быть отрицательным."),
                    Map.entry(
                            "DUPLICATE_BOOK",
                            "Книга с таким названием, издательством, годом и набором авторов уже"
                                    + " существует."),
                    Map.entry("DUPLICATE_BRANCH", "Филиал с таким названием уже существует."),
                    Map.entry(
                            "ENTITY_NOT_FOUND",
                            "Объект не найден. Обновите список и повторите операцию."),
                    Map.entry("ACCESS_DENIED", "Недостаточно прав для этой операции."),
                    Map.entry(
                            "STOCK_BELOW_LOANS",
                            "Фонд не может быть меньше числа выданных экземпляров. Сначала оформите"
                                    + " возврат."),
                    Map.entry(
                            "STOCK_CONFLICT",
                            "Остаток уже изменён другим пользователем. Обновите данные и повторите"
                                    + " операцию."),
                    Map.entry(
                            "NO_AVAILABLE_COPIES",
                            "Нет свободных экземпляров в выбранном месте хранения."),
                    Map.entry("ALREADY_RETURNED", "Эта книга уже возвращена."),
                    Map.entry(
                            "INACTIVE_ENTITY",
                            "Нельзя оформить операцию для неактивного студента, факультета, филиала"
                                    + " или места хранения."),
                    Map.entry(
                            "LOCATION_HAS_HISTORY",
                            "У места хранения есть история выдач. Перенос в другой филиал"
                                    + " запрещён."),
                    Map.entry(
                            "DUPLICATE_ENTITY",
                            "Запись с таким названием, кодом или логином уже существует."),
                    Map.entry("INVALID_DATA", "Проверьте обязательные поля и допустимые значения."),
                    Map.entry("INVALID_PASSWORD", "Пароль должен содержать от 8 до 72 байт UTF-8."),
                    Map.entry(
                            "SELF_LOCKOUT",
                            "Нельзя отключить свою учётную запись или снять с себя роль"
                                    + " администратора."),
                    Map.entry("INTERNAL_ERROR", "Не удалось выполнить операцию. Повторите позже."));

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> error(Exception ex, HttpServletRequest request) {
        String code = "INTERNAL_ERROR";
        Map<String, String> fields = new LinkedHashMap<>();
        if (ex instanceof MethodArgumentNotValidException v) {
            code = "INVALID_DATA";
            v.getBindingResult()
                    .getFieldErrors()
                    .forEach(e -> fields.put(e.getField(), "Проверьте значение поля"));
            if (fields.containsKey("copiesCount")) code = "NEGATIVE_STOCK";
            if (fields.containsKey("publicationYear")) code = "INVALID_PUBLICATION_YEAR";
        } else if (ex instanceof AccessDeniedException) code = "ACCESS_DENIED";
        else if (ex instanceof HttpMessageNotReadableException
                || ex instanceof MethodArgumentTypeMismatchException) code = "INVALID_DATA";
        else if (ex instanceof LibraryException e) code = e.code;
        else {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                String msg = Objects.toString(cause.getMessage(), "");
                for (String known : MESSAGES.keySet()) if (msg.contains(known)) code = known;
                if (cause instanceof java.sql.SQLException sql && code.equals("INTERNAL_ERROR")) {
                    code =
                            switch (Objects.toString(sql.getSQLState(), "")) {
                                case "23505" -> "DUPLICATE_ENTITY";
                                case "23503" -> "ENTITY_NOT_FOUND";
                                case "23514", "23502", "22003", "22001", "22P02" -> "INVALID_DATA";
                                case "40001", "40P01" -> "STOCK_CONFLICT";
                                default -> code;
                            };
                }
            }
        }
        int status =
                switch (code) {
                    case "ACCESS_DENIED" -> 403;
                    case "ENTITY_NOT_FOUND" -> 404;
                    case "INTERNAL_ERROR" -> 500;
                    case "STOCK_CONFLICT",
                            "DUPLICATE_BOOK",
                            "DUPLICATE_BRANCH",
                            "DUPLICATE_ENTITY",
                            "ALREADY_RETURNED" ->
                            409;
                    default -> 400;
                };
        if (status == 500) LoggerFactory.getLogger(getClass()).error("Unhandled error", ex);
        audit.failure(code, request.getRequestURI());
        return ResponseEntity.status(status)
                .body(
                        Map.of(
                                "code",
                                code,
                                "message",
                                MESSAGES.getOrDefault(code, MESSAGES.get("INVALID_DATA")),
                                "fields",
                                fields));
    }
}
