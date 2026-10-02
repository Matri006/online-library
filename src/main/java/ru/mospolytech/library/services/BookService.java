package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Book;
import ru.mospolytech.library.repository.BookRepository;

@Service
@Transactional(readOnly = true)
public class BookService {
    private final BookRepository books;
    private final OperationService operations;

    public BookService(BookRepository books, OperationService operations) {
        this.books = books;
        this.operations = operations;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Responses.Page<Responses.Book> findAll(String query, int page, int size, String sort) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 1000));
        return new Responses.Page<>(
                books.findAll(query, safePage, safeSize, sort),
                books.count(query),
                safePage,
                safeSize);
    }

    @Transactional
    public long save(Long id, Requests.Book request) {
        operations.begin("save_book");
        var book =
                new Book(
                        id,
                        request.title(),
                        request.publisherId(),
                        request.publicationYear(),
                        request.pagesCount(),
                        request.illustrationsCount(),
                        request.price());
        return operations.complete("save_book", books.save(book, request.authorIds()));
    }

    @Transactional
    public void delete(long id) {
        operations.begin("delete_book");
        books.delete(id);
        operations.complete("delete_book", id);
    }
}
