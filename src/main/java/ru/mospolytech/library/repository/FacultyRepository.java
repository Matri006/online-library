package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.entities.Faculty;

import java.util.List;

@Repository
public class FacultyRepository {
    private final JdbcTemplate jdbc;

    public FacultyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Faculty> findAll(String query, int offset) {
        return jdbc.query(
                "select * from faculty where name ilike ? order by name limit 1000 offset ?",
                new DataClassRowMapper<>(Faculty.class),
                "%" + query + "%",
                offset);
    }

    public long insert(Faculty entity) {
        return jdbc.queryForObject(
                "insert into faculty(name, is_active) values(?, ?) returning faculty_id",
                Long.class,
                entity.name(),
                entity.isActive());
    }

    public boolean update(Faculty entity) {
        return jdbc.update(
                        "update faculty set name = ?, is_active = ? where faculty_id = ?",
                        entity.name(),
                        entity.isActive(),
                        entity.facultyId())
                > 0;
    }

    public boolean exists(long id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "select exists(select 1 from faculty where faculty_id = ?)",
                        Boolean.class,
                        id));
    }
}
