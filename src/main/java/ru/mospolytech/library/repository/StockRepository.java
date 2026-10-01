package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.BookStock;

import java.util.List;
import java.util.Optional;

@Repository
public class StockRepository {
    private final JdbcTemplate jdbc;

    public StockRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Responses.Stock> findAll(Long bookId, Long branchId) {
        return jdbc.query(
                """
                select s.*, b.title, l.name as location_name, l.branch_id, br.name as branch_name,
                    (select count(*) from loan x where x.location_id = s.location_id
                     and x.book_id = s.book_id and x.returned_at is null) as loaned,
                    s.copies_count - (select count(*) from loan x where x.location_id = s.location_id
                     and x.book_id = s.book_id and x.returned_at is null) as available
                from book_stock s join book b using(book_id) join storage_location l using(location_id)
                join branch br on br.branch_id = l.branch_id
                where (?::bigint is null or s.book_id = ?) and (?::bigint is null or l.branch_id = ?)
                order by b.title, br.name, l.name limit 1000
                """,
                new DataClassRowMapper<>(Responses.Stock.class),
                bookId,
                bookId,
                branchId,
                branchId);
    }

    public void createIfAbsent(long locationId, long bookId) {
        jdbc.update(
                "insert into book_stock(location_id, book_id, copies_count) values(?, ?, 0) on"
                        + " conflict do nothing",
                locationId,
                bookId);
    }

    public Optional<BookStock> lock(long locationId, long bookId) {
        return jdbc
                .query(
                        "select * from book_stock where location_id = ? and book_id = ? for update",
                        new DataClassRowMapper<>(BookStock.class),
                        locationId,
                        bookId)
                .stream()
                .findFirst();
    }

    public void update(BookStock stock) {
        jdbc.update(
                "update book_stock set copies_count = ? where location_id = ? and book_id = ?",
                stock.copiesCount(),
                stock.locationId(),
                stock.bookId());
    }
}
