package ru.mospolytech.library.repository;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Book;
import ru.mospolytech.library.exceptions.LibraryException;

import java.util.List;
import java.util.Map;

@Repository
public class BookRepository {
    private static final String SEARCH =
            """
            where (b.title ilike ? or exists (
                select 1 from book_author ba join author a using(author_id)
                where ba.book_id = b.book_id and a.full_name ilike ?))
            """;
    private final JdbcTemplate jdbc;
    private final JsonColumns json;

    public BookRepository(JdbcTemplate jdbc, JsonColumns json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Responses.Book> findAll(String query, int page, int size, String sort) {
        String order =
                switch (sort) {
                    case "year" -> "b.publication_year desc, b.book_id";
                    case "price" -> "b.price, b.book_id";
                    default -> "lower(b.title), b.book_id";
                };
        String term = "%" + query + "%";
        return jdbc.query(
                """
                select b.*, p.name as publisher_name,
                    (select jsonb_agg(a.author_id order by a.author_id)
                     from book_author ba join author a using(author_id)
                     where ba.book_id = b.book_id) as author_ids,
                    (select string_agg(a.full_name, ', ' order by a.full_name)
                     from book_author ba join author a using(author_id)
                     where ba.book_id = b.book_id) as authors
                from book b join publisher p using(publisher_id)
                """
                        + SEARCH
                        + " order by "
                        + order
                        + " limit ? offset ?",
                (rs, row) ->
                        new Responses.Book(
                                rs.getLong("book_id"),
                                rs.getString("title"),
                                rs.getLong("publisher_id"),
                                rs.getInt("publication_year"),
                                rs.getInt("pages_count"),
                                rs.getInt("illustrations_count"),
                                rs.getBigDecimal("price"),
                                rs.getString("publisher_name"),
                                json.read(
                                        rs.getString("author_ids"),
                                        new TypeReference<List<Long>>() {}),
                                rs.getString("authors")),
                term,
                term,
                size,
                (long) page * size);
    }

    public long count(String query) {
        String term = "%" + query + "%";
        return jdbc.queryForObject("select count(*) from book b " + SEARCH, Long.class, term, term);
    }

    public boolean exists(long id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "select exists(select 1 from book where book_id = ?)", Boolean.class, id));
    }

    public long save(Book book, List<Long> authorIds) {
        // The catalogue lock and deferred checks must run inside the service transaction.
        jdbc.execute("select pg_advisory_xact_lock(817001)");
        String data =
                json.write(
                        Map.of(
                                "title",
                                book.title(),
                                "publisherId",
                                book.publisherId(),
                                "publicationYear",
                                book.publicationYear(),
                                "pagesCount",
                                book.pagesCount(),
                                "illustrationsCount",
                                book.illustrationsCount(),
                                "price",
                                book.price(),
                                "authorIds",
                                authorIds));
        Long id = book.bookId();
        if (id == null) {
            id =
                    ((Number)
                                    jdbc.queryForMap(
                                                    "call library_api.add_book(?::jsonb, null)",
                                                    data)
                                            .get("id"))
                            .longValue();
        } else {
            jdbc.update("call library_api.update_book(?, ?::jsonb)", id, data);
        }
        jdbc.execute("set constraints all immediate");
        return id;
    }

    public void delete(long id) {
        jdbc.execute("select pg_advisory_xact_lock(817001)");
        if (jdbc.queryForList("select book_id from book where book_id = ? for update", id).isEmpty()) {
            throw new LibraryException("ENTITY_NOT_FOUND");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from book_stock where book_id = ?)
                    or exists(select 1 from book_usage where book_id = ?)
                """, Boolean.class, id, id))) {
            throw new LibraryException("BOOK_IN_USE");
        }
        jdbc.update("delete from book_author where book_id = ?", id);
        jdbc.update("delete from book where book_id = ?", id);
    }
}
