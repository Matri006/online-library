package ru.mospolytech.library.controllers;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.ReferenceService;

import java.util.List;

@RestController
@RequestMapping("/api/references")
public class ReferenceController {
    private final ReferenceService service;

    public ReferenceController(ReferenceService service) {
        this.service = service;
    }

    @GetMapping("/{type}")
    public List<Responses.Reference> list(
            @PathVariable String type,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int offset) {
        return service.findAll(type, q, offset);
    }

    @PostMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public Responses.Id create(
            @PathVariable String type, @Valid @RequestBody Requests.Reference request) {
        return new Responses.Id(service.save(type, null, request));
    }

    @PutMapping("/{type}/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Responses.Id update(
            @PathVariable String type,
            @PathVariable long id,
            @Valid @RequestBody Requests.Reference request) {
        return new Responses.Id(service.save(type, id, request));
    }
}
