package ru.mospolytech.library.repository;

import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class ReportRepository {
    private final JdbcTemplate jdbc;
    private final JsonColumns json;

    public ReportRepository(JdbcTemplate jdbc, JsonColumns json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public int countCopies(long bookId, long branchId) {
        return jdbc.queryForObject(
                "select library_api.get_book_copies(?, ?)", Integer.class, branchId, bookId);
    }

    public int countFaculties(long bookId, long branchId) {
        return jdbc.queryForObject(
                "select library_api.get_faculty_count(?, ?)", Integer.class, branchId, bookId);
    }

    public List<Responses.FacultySummary> findFaculties(long bookId, long branchId) {
        Object result =
                jdbc.queryForMap(
                                "call library_api.list_book_faculties(?, ?, null)",
                                branchId,
                                bookId)
                        .get("result");
        return json.read(result.toString(), new TypeReference<List<Responses.FacultySummary>>() {});
    }

    public long countLoaned(long bookId, long branchId) {
        return jdbc.queryForObject(
                """
                select count(*) from loan x join storage_location l using(location_id)
                where x.book_id = ? and l.branch_id = ? and x.returned_at is null
                """,
                Long.class,
                bookId,
                branchId);
    }

    public long countStudents(
            long bookId, Long branchId, Long facultyId, OffsetDateTime from, OffsetDateTime to) {
        return jdbc.queryForObject(
                """
                select count(distinct x.student_id) from loan x join storage_location l using(location_id)
                where x.book_id = ? and (?::bigint is null or l.branch_id = ?)
                    and (?::bigint is null or x.faculty_at_issue_id = ?)
                    and (?::timestamptz is null or x.issued_at >= ?)
                    and (?::timestamptz is null or x.issued_at < ?)
                """,
                Long.class,
                bookId,
                branchId,
                branchId,
                facultyId,
                facultyId,
                from,
                from,
                to,
                to);
    }
}
