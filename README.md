# Онлайн-библиотека

Клиент-серверное приложение для учёта книг, фонда филиалов, студентов и выдач. Фронтенд: HTML/CSS/JavaScript. Бэкенд: Java 21 / Spring Boot 3.5.6. Все постоянные данные, включая сессии, хранятся в PostgreSQL 16. Взаимодействие — REST/JSON по HTTP/1.1.

Отчёт для задания: [Отчёт.md](Отчёт.md).

## Быстрый запуск

Нужны Docker Engine / Docker Desktop и Docker Compose v2. Для файла масштабирования нужна версия Compose не ниже 2.24.4. Java, Maven и Node.js на компьютере для запуска через Docker не нужны. Первой сборке необходим интернет для загрузки образов и Maven-зависимостей.

```sh
cp .env.example .env
docker compose build
docker compose up -d --wait
```

Открыть **http://localhost:8080**. По умолчанию `.env.example` включает учебные данные:

| Логин | Учебный пароль | Права |
| --- | --- | --- |
| admin | Admin123! | Все разделы, учётные записи, аудит |
| librarian | Library123! | Книги, фонд, студенты, выдачи, учебное использование |
| viewer | Viewer123! | Просмотр |

Пароли заданы в `.env`; перед публичным размещением их нужно заменить. При `DEMO_ENABLED=false` создаётся только `admin`, каталог пуст. Первичная инициализация выполняется один раз: изменение переменных паролей после создания пользователей **не меняет** их существующие пароли. Это делается в разделе «Пользователи». Аналогично смена переменных паролей PostgreSQL требует согласованного изменения паролей существующих ролей в БД.

При запуске последовательно работают `db` → `db-init` (роль БД) → `release` (миграции и начальные данные) → `app`. Завершение `db-init` и `release` с кодом 0 нормально: это разовые процессы. Веб-приложение не запускается, если подготовка базы не прошла.

```sh
docker compose ps -a
docker compose logs --tail=100 release app
docker compose stop
docker compose start db app
```

`stop` сохраняет контейнеры и данные. `docker compose down` удаляет контейнеры и сеть, но сохраняет том БД. Ключ `down -v` удаляет данные, поэтому для обычной остановки его не используют.

Если на 8080 уже работает другая версия, задайте другой `APP_PORT` и отдельное имя проекта, например `docker compose -p library-homework up -d --wait`. Том PostgreSQL будет отдельным. Для обновления существующей установки используйте те же пароли и имя проекта: Flyway применит только новые миграции. Файлы V1–V3 сохранены без изменений.

## Демонстрация CRUD

1. Войти как `admin` или `librarian`.
2. В разделе «Каталог книг» нажать «Добавить», заполнить поля, выбрать автора и издательство, сохранить.
3. Найти книгу поиском.
4. Нажать «Изменить», изменить название или цену, сохранить.
5. Нажать «Удалить» и подтвердить. Книга исчезнет, запись аудита сохранится.

На пустой базе сначала создать автора и издательство в «Справочниках» под `admin`. Удалять разрешено книги без связей с фондом и учебным использованием. Книга с такими связями получает ответ `409 BOOK_IN_USE`: история выдач и фонд защищены. Даже нулевой фонд является связью. У остальных сущностей, имеющих историю, используется деактивация, у выдач — возврат.

## API

| Метод и путь | Назначение |
| --- | --- |
| `GET /api/session` | Состояние входа и CSRF-токен |
| `POST /api/login` | Вход: form-urlencoded, поля `username`, `password` |
| `POST /api/logout` | Выход, `204` |
| `GET /api/books?q=...&page=0&size=20` | Чтение каталога, поиск и пагинация |
| `POST /api/books` | Создание книги, ответ `{"id": ...}` |
| `PUT /api/books/{id}` | Изменение книги |
| `DELETE /api/books/{id}` | Удаление книги, `204`; отсутствующая книга — `404` |
| `GET /api/references/authors` | Авторы |
| `GET /api/references/publishers` | Издательства |
| `GET /actuator/health` | Проверка состояния, включая соединение с БД |

Тело создания/обновления книги (идентификаторы должны существовать):

```json
{
  "title": "Новая книга",
  "publisherId": 1,
  "publicationYear": 2024,
  "pagesCount": 200,
  "illustrationsCount": 10,
  "price": 500.00,
  "authorIds": [1]
}
```

Для изменяющих запросов, включая вход и выход, нужно передавать CSRF-токен и cookie сессии. Сначала вызвать `GET /api/session`, взять `csrfHeader` и `csrfToken`, после входа запросить новый токен. Пример рабочего HTTP-клиента: [scripts/smoke.py](scripts/smoke.py). JSON каталога использует `book_id`, `author_ids` и другие snake_case поля; JSON запросов — camelCase, как в примере.

## Проверки

Полный набор тестов (реальная PostgreSQL через Testcontainers; Docker должен работать):

```sh
docker compose -f docker-compose.test.yml up --build --abort-on-container-exit --exit-code-from tests
docker compose -f docker-compose.test.yml down
```

На машине с JDK 21 и Maven 3.9 можно выполнить `mvn -B -ntp -Dapi.version=1.44 test`. Для macOS с Docker Desktop при необходимости задайте `DOCKER_HOST=unix://$HOME/.docker/run/docker.sock`. Docker нужен и при локальном запуске интеграционных тестов.

Проверка REST на запущенном учебном стенде (Python 3.9+, без внешних библиотек):

```sh
SMOKE_PASSWORD='Admin123!' python3 scripts/smoke.py http://127.0.0.1:8080
```

Скрипт создаёт и удаляет отдельную тестовую книгу; события остаются в аудите. Нужны хотя бы один автор и издательство. Для другой учётной записи можно задать `SMOKE_USERNAME` и `SMOKE_PASSWORD`.

## Два экземпляра и перезапуск

```sh
docker compose -f docker-compose.yml -f docker-compose.scale.yml up -d --scale app=2 --wait
docker compose -f docker-compose.yml -f docker-compose.scale.yml port --index 1 app 8080
docker compose -f docker-compose.yml -f docker-compose.scale.yml port --index 2 app 8080
```

Подставить выведенные адреса в следующую команду:

```sh
SMOKE_PASSWORD='Admin123!' python3 scripts/smoke.py http://127.0.0.1:PORT1 --second-url http://127.0.0.1:PORT2 --session-file /tmp/library-session.cookies
docker compose -f docker-compose.yml -f docker-compose.scale.yml restart app
```

Когда `docker compose ps` покажет `healthy`, снова узнать порты командами `port --index`: Docker может назначить новые порты при перезапуске. Затем повторить команду `smoke.py` с тем же `--session-file`. При наличии файла скрипт **не выполняет повторный вход**, а проверяет сохранённую сессию. Для обычной проверки выхода запускать без `--session-file`. Файл с cookie даёт доступ к аккаунту, хранить его как секрет и удалить после проверки. Все тесты проводить на отдельном учебном стенде.

Общая сессия хранится в PostgreSQL, поэтому привязка клиента к одному процессу (sticky sessions) не нужна. Этот пример публикует два отдельных порта; единый адрес через балансировщик можно добавить в следующем задании. При увеличении числа экземпляров нужно учитывать суммарное число соединений: число процессов × `DB_POOL_SIZE`, плюс резерв для release и администрирования.

## Build / release / run

Для отдельного выпуска использовать новый тег образа, например `APP_VERSION=hw1-001` в `.env`. `local` предназначен для разработки. Публикуемый тег не переиспользуют: образ собирают один раз и запускают этот же образ во всех окружениях.

```sh
# Build: исходники -> исполняемый образ
docker compose build
# Подготовка внешней зависимости
docker compose up -d --wait db
docker compose run --rm db-init
# Release: миграции и начальные данные из того же образа
docker compose run --rm --no-deps release
# Run: только HTTP-сервер, без миграций и без пароля владельца БД
docker compose up -d --no-deps --wait app
```

Обычный `up -d --wait` выполняет эту последовательность автоматически. Повторный `release` идемпотентен. Несовместимые изменения схемы при будущих обновлениях потребуют отдельного плана миграции; откат одного образа не откатывает БД.

## Конфигурация

| Переменная | Назначение |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Подключение Java к PostgreSQL; Compose задаёт URL и имя роли для своего сервиса `db` |
| `DB_OWNER_USERNAME`, `DB_OWNER_PASSWORD` | Роль миграций; пароль не передаётся сервису `app` |
| `ADMIN_PASSWORD`, `LIBRARIAN_PASSWORD`, `VIEWER_PASSWORD`, `DEMO_ENABLED` | Первичная инициализация в процессе `release` |
| `PORT`, `HOST` | Адрес и порт Java-процесса; по умолчанию `0.0.0.0:8080` |
| `APP_PORT`, `BIND_ADDRESS` | Публикуемый порт/адрес Docker Compose |
| `DB_POOL_SIZE`, `DB_POOL_MIN_IDLE` | Пул соединений Java; по умолчанию 10 и 2 |
| `SESSION_TIMEOUT`, `COOKIE_SECURE` | Срок сессии (30 минут) и Secure cookie; при HTTPS задать `true` |
| `LOG_LEVEL` | Уровень консольных логов, по умолчанию INFO |
| `SHUTDOWN_TIMEOUT` | Ожидание завершения запросов при SIGTERM, по умолчанию 20 секунд |
| `APP_MEMORY_LIMIT`, `JAVA_TOOL_OPTIONS` | Лимит контейнера (512 MiB) и параметры JVM |
| `APP_VERSION` | Тег образа конкретного выпуска |

Spring Boot также принимает стандартные переменные окружения. `.env` автоматически читает Compose; при `java -jar` переменные нужно экспортировать в окружение процесса. На внешней PostgreSQL администратор предварительно создаёт базу и роль `library_app` (как в `docker/init-db.sh`), после чего передаёт JDBC URL/учётные данные процессам release и web. PostgreSQL-специфические SQL-функции являются частью приложения: заменить PostgreSQL на другую СУБД без изменения кода нельзя.

## Администрирование

Логи и разовая резервная копия:

```sh
docker compose logs -f app
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > library.dump
```

Для ежедневных резервных копий в отдельном Docker-томе:

```sh
docker compose --profile ops up -d backup
```

Этот дополнительный процесс хранит копии за последние 30 дней и пишет результат в stdout/stderr. В минимальном стенде он выключен. Для восстановления дампа нужна отдельная администраторская операция с `pg_restore`; её не выполняют автоматически поверх рабочей базы.

## Архив для сдачи

```sh
python3 scripts/package.py
```

Результат: `artifacts/online-library-homework.zip`. Внутри папка `online-library` с полным исходным кодом, тестами, конфигурацией, инструкциями и `Отчёт.md`. В архив не включаются `.env`, дампы, зависимости, `target`, файлы IDE и служебная директория Git.
