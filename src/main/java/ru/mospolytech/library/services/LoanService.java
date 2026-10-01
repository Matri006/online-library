package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Loan;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.LoanRepository;
import ru.mospolytech.library.repository.LocationRepository;
import ru.mospolytech.library.repository.StockRepository;
import ru.mospolytech.library.repository.StudentRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class LoanService {
    private final LoanRepository loans;
    private final StockRepository stock;
    private final StudentRepository students;
    private final LocationRepository locations;
    private final OperationService operations;

    public LoanService(
            LoanRepository loans,
            StockRepository stock,
            StudentRepository students,
            LocationRepository locations,
            OperationService operations) {
        this.loans = loans;
        this.stock = stock;
        this.students = students;
        this.locations = locations;
        this.operations = operations;
    }

    public List<Responses.Loan> findAll(boolean open, int offset) {
        return loans.findAll(open, Math.max(0, offset));
    }

    @Transactional
    public long issue(Requests.Issue request) {
        long actor = operations.begin("issue");
        // All issue/return/stock changes serialize on the same stock row.
        var current =
                stock.lock(request.locationId(), request.bookId())
                        .orElseThrow(() -> new LibraryException("ENTITY_NOT_FOUND"));
        var student =
                students.lockEligibility(request.studentId())
                        .orElseThrow(() -> new LibraryException("ENTITY_NOT_FOUND"));
        boolean locationActive = locations.lockEligibility(request.locationId());
        if (!student.active() || !locationActive) {
            throw new LibraryException("INACTIVE_ENTITY");
        }
        if (current.copiesCount() <= loans.countOpen(request.locationId(), request.bookId())) {
            throw new LibraryException("NO_AVAILABLE_COPIES");
        }
        var loan =
                new Loan(
                        null,
                        request.locationId(),
                        request.bookId(),
                        request.studentId(),
                        student.facultyId(),
                        null,
                        null,
                        actor,
                        null);
        return operations.complete("issue", loans.insert(loan));
    }

    @Transactional
    public void returnLoan(long id) {
        long actor = operations.begin("return");
        var loan = loans.findById(id).orElseThrow(() -> new LibraryException("ENTITY_NOT_FOUND"));
        stock.lock(loan.locationId(), loan.bookId())
                .orElseThrow(() -> new LibraryException("ENTITY_NOT_FOUND"));
        if (!loans.markReturned(id, actor)) {
            throw new LibraryException("ALREADY_RETURNED");
        }
        operations.complete("return", id);
    }
}
