package ru.mospolytech.library.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.AuditService;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public List<Responses.Audit> list(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "") String q) {
        return service.findAll(offset, q);
    }
}
