package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.AuditLog;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class AuditRepository {
    private final JdbcTemplate jdbc;
    private final JsonColumns json;

    public AuditRepository(JdbcTemplate jdbc, JsonColumns json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void setActor(long id) {
        jdbc.queryForObject(
                "select set_config('library.user_id', ?, true)", String.class, Long.toString(id));
    }

    public void logFailure(Long actor, String code, String path) {
        jdbc.update(
                "call library_api.log_user_exception(?, ?, jsonb_build_object('path', cast(? as"
                        + " text)))",
                actor,
                code,
                path);
    }

    public List<Responses.Audit> findAll(int offset, String query) {
        String term = "%" + query + "%";
        return jdbc.query(
                """
                select a.*, u.login from audit_log a left join app_user u using(user_id)
                where coalesce(a.entity_type, '') ilike ? or a.operation ilike ? or a.details->>'code' ilike ?
                order by event_id desc limit 100 offset ?
                """,
                (rs, row) -> {
                    var event =
                            new AuditLog(
                                    rs.getLong("event_id"),
                                    rs.getObject("event_time", OffsetDateTime.class),
                                    rs.getObject("user_id", Long.class),
                                    rs.getString("operation"),
                                    rs.getString("entity_type"),
                                    json.read(rs.getString("entity_key")),
                                    json.read(rs.getString("details")));
                    return new Responses.Audit(
                            event.eventId(),
                            event.eventTime(),
                            event.userId(),
                            event.operation(),
                            event.entityType(),
                            event.entityKey(),
                            event.details(),
                            rs.getString("login"));
                },
                term,
                term,
                term,
                offset);
    }
}
