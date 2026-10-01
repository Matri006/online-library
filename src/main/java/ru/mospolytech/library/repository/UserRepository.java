package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.mospolytech.library.dto.Responses;
import ru.mospolytech.library.entities.AppUser;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByLoginIgnoreCase(String login) {
        return jdbc
                .query(
                        "select * from app_user where lower(login) = lower(?)",
                        new DataClassRowMapper<>(AppUser.class),
                        login)
                .stream()
                .findFirst();
    }

    public List<Responses.User> findAll() {
        return jdbc.query(
                """
                select u.user_id, u.login, u.is_active, r.code as role
                from app_user u join app_role r using(role_id) order by u.login
                """,
                new DataClassRowMapper<>(Responses.User.class));
    }

    public long insert(AppUser user) {
        return jdbc.queryForObject(
                """
                insert into app_user(login, password_hash, role_id, is_active) values(?, ?, ?, ?) returning user_id
                """,
                Long.class,
                user.login(),
                user.passwordHash(),
                user.roleId(),
                user.isActive());
    }

    public boolean update(AppUser user) {
        return jdbc.update(
                        """
                        update app_user set login = ?, password_hash = coalesce(?, password_hash), role_id = ?, is_active = ?
                        where user_id = ?
                        """,
                        user.login(),
                        user.passwordHash(),
                        user.roleId(),
                        user.isActive(),
                        user.userId())
                > 0;
    }

    public void lockBootstrap() {
        jdbc.execute("select pg_advisory_xact_lock(817002)");
    }

    public boolean existsAny() {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("select exists(select 1 from app_user)", Boolean.class));
    }
}
