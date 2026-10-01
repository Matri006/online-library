-- Отдельная учётная запись приложения не владеет таблицами и не меняет схему.
GRANT USAGE ON SCHEMA public,library_api TO library_app;
GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO library_app;
REVOKE ALL ON flyway_schema_history FROM library_app;
REVOKE UPDATE,DELETE,TRUNCATE ON audit_log FROM library_app;
REVOKE INSERT,UPDATE,DELETE ON app_role FROM library_app;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO library_app;
REVOKE EXECUTE ON ALL ROUTINES IN SCHEMA library_api FROM PUBLIC;
GRANT EXECUTE ON ALL ROUTINES IN SCHEMA library_api TO library_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
