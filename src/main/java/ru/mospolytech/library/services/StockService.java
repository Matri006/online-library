package ru.mospolytech.library.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.mospolytech.library.dto.Requests;
import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.BookStock;
import ru.mospolytech.library.exceptions.LibraryException;
import ru.mospolytech.library.repository.StockRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StockService {
    private final StockRepository stock;
    private final OperationService operations;

    public StockService(StockRepository stock, OperationService operations) {
        this.stock = stock;
        this.operations = operations;
    }

    public List<Responses.Stock> findAll(Long bookId, Long branchId) {
        return stock.findAll(bookId, branchId);
    }

    @Transactional
    public void setStock(Requests.Stock request) {
        operations.begin("set_stock");
        stock.createIfAbsent(request.locationId(), request.bookId());
        var current =
                stock.lock(request.locationId(), request.bookId())
                        .orElseThrow(() -> new LibraryException("ENTITY_NOT_FOUND"));
        if (current.copiesCount() != request.expectedCopiesCount()) {
            throw new LibraryException("STOCK_CONFLICT");
        }
        stock.update(new BookStock(request.locationId(), request.bookId(), request.copiesCount()));
        operations.complete("set_stock", request.bookId());
    }
}
