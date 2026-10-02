package ru.mospolytech.library.controllers;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.BookService;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @GetMapping
    public Responses.Page<Responses.Book> list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sort) {
        return service.findAll(q, page, size, sort);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Id create(@Valid @RequestBody Requests.Book request) {
        return new Responses.Id(service.save(null, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Id update(@PathVariable long id, @Valid @RequestBody Requests.Book request) {
        return new Responses.Id(service.save(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
