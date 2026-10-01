package ru.mospolytech.library.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Seeds the initial demo catalogue inside the bootstrap transaction. */
@Repository
public class DemoDataRepository {
    private final JdbcTemplate jdbc;

    public DemoDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void populate() {
        jdbc.update("insert into publisher(name) values ('Питер'),('Вильямс')");
        jdbc.update(
                "insert into author(full_name) values ('Роберт Мартин'),('Мартин Клеппман'),('Эрик"
                        + " Фримен'),('Элизабет Робсон')");
        jdbc.execute(
                """
                DO $$ DECLARE id bigint; BEGIN
                CALL library_api.add_book('{"title":"Чистый код","publisherId":1,"publicationYear":2024,"pagesCount":464,"illustrationsCount":12,"price":1490,"authorIds":[1]}',id);
                CALL library_api.add_book('{"title":"Высоконагруженные приложения","publisherId":1,"publicationYear":2023,"pagesCount":640,"illustrationsCount":85,"price":2190,"authorIds":[2]}',id);
                CALL library_api.add_book('{"title":"Паттерны проектирования","publisherId":2,"publicationYear":2024,"pagesCount":656,"illustrationsCount":120,"price":1890,"authorIds":[3,4]}',id);
                CALL library_api.add_branch('{"name":"Библиотека на Большой Семёновской","address":"Москва, ул. Большая Семёновская, 38","branchType":"BRANCH","phone":"+7 (495) 000-00-00","active":true}',id);
                CALL library_api.add_branch('{"name":"Центральное книгохранилище","address":"Москва","branchType":"DEPOSITORY","active":true}',id);
                END $$
                """);
        jdbc.update(
                "insert into faculty(name) values ('Факультет информационных"
                        + " технологий'),('Факультет машиностроения')");
        jdbc.update(
                "insert into book_stock(location_id,book_id,copies_count)"
                        + " values(1,1,12),(1,2,8),(1,3,5),(2,1,20)");
        jdbc.update(
                "insert into book_usage(branch_id,book_id,faculty_id)"
                        + " values(1,1,1),(1,1,2),(1,2,1),(1,3,1)");
        jdbc.update(
                "insert into student(student_card_number,full_name,faculty_id)"
                        + " values('DEMO-001','Иванов Иван (демо)',1),('DEMO-002','Петрова Анна"
                        + " (демо)',2)");
    }
}
