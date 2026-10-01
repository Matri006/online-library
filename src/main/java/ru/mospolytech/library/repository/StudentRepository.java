package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.Student;

import java.util.List;

@Repository
public class StudentRepository {
    private final JdbcTemplate jdbc;

    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Responses.Student> findAll(String query, int offset) {
        return jdbc.query(
                "select s.*, f.name as faculty_name from student s join faculty f using(faculty_id)"
                        + " where s.full_name ilike ? or s.student_card_number ilike ? order by"
                        + " s.full_name limit 1000 offset ?",
                new DataClassRowMapper<>(Responses.Student.class),
                "%" + query + "%",
                "%" + query + "%",
                offset);
    }

    public long insert(Student entity) {
        return jdbc.queryForObject(
                "insert into student(student_card_number, full_name, faculty_id, is_active)"
                        + " values(?, ?, ?, ?) returning student_id",
                Long.class,
                entity.studentCardNumber(),
                entity.fullName(),
                entity.facultyId(),
                entity.isActive());
    }

    public boolean update(Student entity) {
        return jdbc.update(
                        "update student set student_card_number = ?, full_name = ?, faculty_id = ?,"
                                + " is_active = ? where student_id = ?",
                        entity.studentCardNumber(),
                        entity.fullName(),
                        entity.facultyId(),
                        entity.isActive(),
                        entity.studentId())
                > 0;
    }

    public record Eligibility(long facultyId, boolean active) {}

    public java.util.Optional<Eligibility> lockEligibility(long id) {
        return jdbc
                .query(
                        """
                        select s.faculty_id, s.is_active and f.is_active as active
                        from student s join faculty f using(faculty_id)
                        where student_id = ? for share of s, f
                        """,
                        new DataClassRowMapper<>(Eligibility.class),
                        id)
                .stream()
                .findFirst();
    }
}
