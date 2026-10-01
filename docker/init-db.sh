#!/bin/sh
set -eu
# Выполняется отдельным сервисом перед приложением, в том числе для существующей БД.
# Пароль существующей роли не меняем: повторный запуск безопасен.
psql -X -v ON_ERROR_STOP=1 --set=app_password="$APP_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE library_app LOGIN PASSWORD %L', :'app_password')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'library_app')
\gexec
SQL
