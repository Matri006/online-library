package ru.mospolytech.library.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.ReportService;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/book")
    public Responses.BookReport book(@RequestParam long bookId, @RequestParam long branchId) {
        return service.book(bookId, branchId);
    }

    @GetMapping("/students")
    public Responses.Count students(
            @RequestParam long bookId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime to) {
        return new Responses.Count(service.studentCount(bookId, branchId, facultyId, from, to));
    }
}
