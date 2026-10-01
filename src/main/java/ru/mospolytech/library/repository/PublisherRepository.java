package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.entities.Publisher;

import java.util.List;

@Repository
public class PublisherRepository {
    private final JdbcTemplate jdbc;

    public PublisherRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Publisher> findAll(String query, int offset) {
        return jdbc.query(
                "select * from publisher where name ilike ? order by name limit 1000 offset ?",
                new DataClassRowMapper<>(Publisher.class),
                "%" + query + "%",
                offset);
    }

    public long insert(Publisher entity) {
        return jdbc.queryForObject(
                "insert into publisher(name) values(?) returning publisher_id",
                Long.class,
                entity.name());
    }

    public boolean update(Publisher entity) {
        return jdbc.update(
                        "update publisher set name = ? where publisher_id = ?",
                        entity.name(),
                        entity.publisherId())
                > 0;
    }
}
