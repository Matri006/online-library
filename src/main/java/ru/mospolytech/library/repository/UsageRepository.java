package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.BookUsage;

import java.util.List;

@Repository
public class UsageRepository {
    private final JdbcTemplate jdbc;

    public UsageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Responses.Usage> findAll() {
        return jdbc.query(
                """
                select u.*, b.title, br.name as branch_name, f.name as faculty_name
                from book_usage u join book b using(book_id) join branch br using(branch_id)
                join faculty f using(faculty_id) order by b.title, br.name, f.name limit 1000
                """,
                new DataClassRowMapper<>(Responses.Usage.class));
    }

    public void insert(BookUsage usage) {
        jdbc.update(
                "insert into book_usage(branch_id, book_id, faculty_id) values(?, ?, ?)",
                usage.branchId(),
                usage.bookId(),
                usage.facultyId());
    }

    public boolean delete(BookUsage usage) {
        return jdbc.update(
                        "delete from book_usage where branch_id = ? and book_id = ? and faculty_id"
                                + " = ?",
                        usage.branchId(),
                        usage.bookId(),
                        usage.facultyId())
                > 0;
    }
}
