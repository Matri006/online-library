package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.StorageLocation;

import java.util.List;

@Repository
public class LocationRepository {
    private final JdbcTemplate jdbc;

    public LocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Responses.Location> findAll() {
        return jdbc.query(
                "select l.*, b.name as branch_name, b.is_active as branch_active from"
                    + " storage_location l join branch b using(branch_id) order by b.name, l.name",
                new DataClassRowMapper<>(Responses.Location.class));
    }

    public long insert(StorageLocation entity) {
        return jdbc.queryForObject(
                "insert into storage_location(branch_id, code, name, is_active) values(?, ?, ?, ?)"
                        + " returning location_id",
                Long.class,
                entity.branchId(),
                entity.code(),
                entity.name(),
                entity.isActive());
    }

    public boolean update(StorageLocation entity) {
        return jdbc.update(
                        "update storage_location set branch_id = ?, code = ?, name = ?, is_active ="
                                + " ? where location_id = ?",
                        entity.branchId(),
                        entity.code(),
                        entity.name(),
                        entity.isActive(),
                        entity.locationId())
                > 0;
    }

    public boolean lockEligibility(long id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        """
                        select l.is_active and b.is_active
                        from storage_location l join branch b using(branch_id)
                        where l.location_id = ? for share of l, b
                        """,
                        Boolean.class,
                        id));
    }
}
