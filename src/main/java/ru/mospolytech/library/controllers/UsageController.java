package ru.mospolytech.library.controllers;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.UsageService;

import java.util.List;

@RestController
@RequestMapping("/api/usage")
public class UsageController {
    private final UsageService service;

    public UsageController(UsageService service) {
        this.service = service;
    }

    @GetMapping
    public List<Responses.Usage> list() {
        return service.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Success add(@Valid @RequestBody Requests.Usage request) {
        service.add(request);
        return Responses.Success.OK;
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Success delete(@Valid @RequestBody Requests.Usage request) {
        service.delete(request);
        return Responses.Success.OK;
    }
}
