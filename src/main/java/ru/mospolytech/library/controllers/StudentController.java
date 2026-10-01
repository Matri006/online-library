package ru.mospolytech.library.controllers;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.StudentService;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Responses.Student> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int offset) {
        return service.findAll(q, offset);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Id create(@Valid @RequestBody Requests.Student request) {
        return new Responses.Id(service.save(null, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Id update(
            @PathVariable long id, @Valid @RequestBody Requests.Student request) {
        return new Responses.Id(service.save(id, request));
    }
}
