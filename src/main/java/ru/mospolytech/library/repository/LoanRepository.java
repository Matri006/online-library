package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Loan;

import java.util.List;
import java.util.Optional;

@Repository
public class LoanRepository {
    private final JdbcTemplate jdbc;

    public LoanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Responses.Loan> findAll(boolean open, int offset) {
        return jdbc.query(
                """
                select x.*, b.title, s.full_name, s.student_card_number, f.name as faculty_name,
                    l.name as location_name, br.name as branch_name
                from loan x join book b using(book_id) join student s using(student_id)
                join faculty f on f.faculty_id = x.faculty_at_issue_id
                join storage_location l using(location_id) join branch br on br.branch_id = l.branch_id
                where (? = false or x.returned_at is null) order by x.issued_at desc limit 1000 offset ?
                """,
                new DataClassRowMapper<>(Responses.Loan.class),
                open,
                offset);
    }

    public Optional<Loan> findById(long id) {
        return jdbc
                .query(
                        "select * from loan where loan_id = ?",
                        new DataClassRowMapper<>(Loan.class),
                        id)
                .stream()
                .findFirst();
    }

    public long countOpen(long locationId, long bookId) {
        return jdbc.queryForObject(
                """
                select count(*) from loan where location_id = ? and book_id = ? and returned_at is null
                """,
                Long.class,
                locationId,
                bookId);
    }

    public long insert(Loan loan) {
        return jdbc.queryForObject(
                """
                insert into loan(location_id, book_id, student_id, faculty_at_issue_id, issued_by_user_id)
                values(?, ?, ?, ?, ?) returning loan_id
                """,
                Long.class,
                loan.locationId(),
                loan.bookId(),
                loan.studentId(),
                loan.facultyAtIssueId(),
                loan.issuedByUserId());
    }

    public boolean markReturned(long id, long actor) {
        return jdbc.update(
                        """
                        update loan set returned_at = clock_timestamp(), returned_by_user_id = ?
                        where loan_id = ? and returned_at is null
                        """,
                        actor,
                        id)
                > 0;
    }
}
