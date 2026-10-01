package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.BookUsage;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.UsageRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UsageService {
    private final UsageRepository usages;
    private final OperationService operations;

    public UsageService(UsageRepository usages, OperationService operations) {
        this.usages = usages;
        this.operations = operations;
    }

    public List<Responses.Usage> findAll() {
        return usages.findAll();
    }

    @Transactional
    public void add(Requests.Usage request) {
        operations.begin("book_usage");
        usages.insert(new BookUsage(request.branchId(), request.bookId(), request.facultyId()));
        operations.complete("book_usage", request.bookId());
    }

    @Transactional
    public void delete(Requests.Usage request) {
        operations.begin("book_usage");
        if (!usages.delete(
                new BookUsage(request.branchId(), request.bookId(), request.facultyId()))) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        operations.complete("book_usage", request.bookId());
    }
}
