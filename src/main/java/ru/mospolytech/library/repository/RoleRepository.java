package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.entities.AppRole;

import java.util.Optional;

@Repository
public class RoleRepository {
    private final JdbcTemplate jdbc;

    public RoleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppRole> findById(long id) {
        return jdbc
                .query(
                        "select * from app_role where role_id = ?",
                        new DataClassRowMapper<>(AppRole.class),
                        id)
                .stream()
                .findFirst();
    }

    public Optional<AppRole> findByCode(String code) {
        return jdbc
                .query(
                        "select * from app_role where code = ?",
                        new DataClassRowMapper<>(AppRole.class),
                        code)
                .stream()
                .findFirst();
    }
}
