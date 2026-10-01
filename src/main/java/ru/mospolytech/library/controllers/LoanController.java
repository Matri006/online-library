package ru.mospolytech.library.controllers;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.LoanService;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @GetMapping
    public List<Responses.Loan> list(
            @RequestParam(defaultValue = "false") boolean open,
            @RequestParam(defaultValue = "0") int offset) {
        return service.findAll(open, offset);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Id issue(@Valid @RequestBody Requests.Issue request) {
        return new Responses.Id(service.issue(request));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Success returnLoan(@PathVariable long id) {
        service.returnLoan(id);
        return Responses.Success.OK;
    }
}
