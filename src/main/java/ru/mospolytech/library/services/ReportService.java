package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.BookRepository;
import ru.mospolytech.library.repository.FacultyRepository;
import ru.mospolytech.library.repository.ReportRepository;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final ReportRepository reports;
    private final BookRepository books;
    private final FacultyRepository faculties;

    public ReportService(
            ReportRepository reports, BookRepository books, FacultyRepository faculties) {
        this.reports = reports;
        this.books = books;
        this.faculties = faculties;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Responses.BookReport book(long bookId, long branchId) {
        int copies = reports.countCopies(bookId, branchId);
        int facultyCount = reports.countFaculties(bookId, branchId);
        var bookFaculties = reports.findFaculties(bookId, branchId);
        long loaned = reports.countLoaned(bookId, branchId);
        return new Responses.BookReport(
                copies,
                copies - loaned,
                loaned,
                facultyCount,
                bookFaculties,
                reports.countStudents(bookId, branchId, null, null, null),
                copies == 0 ? "В выбранном филиале нет экземпляров этой книги." : "");
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public long studentCount(
            long bookId,
            Long branchId,
            Long facultyId,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new LibraryException("INVALID_DATA");
        }
        if (!books.exists(bookId)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        if (branchId != null) {
            reports.countCopies(bookId, branchId);
        }
        if (facultyId != null && !faculties.exists(facultyId)) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        return reports.countStudents(bookId, branchId, facultyId, from, to);
    }
}
