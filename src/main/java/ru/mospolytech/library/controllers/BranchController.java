package ru.mospolytech.library.controllers;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.BranchService;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {
    private final BranchService service;

    public BranchController(BranchService service) {
        this.service = service;
    }

    @GetMapping
    public List<Responses.Branch> list() {
        return service.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Responses.Id create(@Valid @RequestBody Requests.Branch request) {
        return new Responses.Id(service.save(null, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Responses.Id update(@PathVariable long id, @Valid @RequestBody Requests.Branch request) {
        return new Responses.Id(service.save(id, request));
    }
}
