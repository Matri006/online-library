package ru.mospolytech.library.controllers;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.services.StockService;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
public class StockController {
    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping
    public List<Responses.Stock> list(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long branchId) {
        return service.findAll(bookId, branchId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public Responses.Success update(@Valid @RequestBody Requests.Stock request) {
        service.setStock(request);
        return Responses.Success.OK;
    }
}
