package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.entities.Author;

import java.util.List;

@Repository
public class AuthorRepository {
    private final JdbcTemplate jdbc;

    public AuthorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Author> findAll(String query, int offset) {
        return jdbc.query(
                "select * from author where full_name ilike ? order by full_name limit 1000 offset"
                        + " ?",
                new DataClassRowMapper<>(Author.class),
                "%" + query + "%",
                offset);
    }

    public long insert(Author entity) {
        return jdbc.queryForObject(
                "insert into author(full_name) values(?) returning author_id",
                Long.class,
                entity.fullName());
    }

    public boolean update(Author entity) {
        return jdbc.update(
                        "update author set full_name = ? where author_id = ?",
                        entity.fullName(),
                        entity.authorId())
                > 0;
    }
}
