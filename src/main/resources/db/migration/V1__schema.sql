CREATE SCHEMA library_api;
CREATE TABLE publisher (
 publisher_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(255) NOT NULL CHECK (btrim(name) <> '')
);
CREATE UNIQUE INDEX publisher_name_uq ON publisher(lower(btrim(name)));
CREATE TABLE author (
 author_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 full_name varchar(255) NOT NULL CHECK (btrim(full_name) <> '')
);
CREATE TABLE book (
 book_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 title varchar(500) NOT NULL CHECK (btrim(title) <> ''),
 publisher_id bigint NOT NULL REFERENCES publisher ON DELETE RESTRICT,
 publication_year smallint NOT NULL CHECK (publication_year >= 1450),
 pages_count integer NOT NULL CHECK (pages_count > 0),
 illustrations_count integer NOT NULL DEFAULT 0 CHECK (illustrations_count >= 0),
 price numeric(12,2) NOT NULL CHECK (price >= 0)
);
CREATE INDEX book_publisher_idx ON book(publisher_id);
CREATE INDEX book_title_idx ON book(lower(title));
CREATE TABLE book_author (
 book_id bigint NOT NULL REFERENCES book ON DELETE RESTRICT,
 author_id bigint NOT NULL REFERENCES author ON DELETE RESTRICT,
 PRIMARY KEY(book_id, author_id)
);
CREATE INDEX book_author_author_idx ON book_author(author_id);
CREATE TABLE branch (
 branch_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(255) NOT NULL CHECK (btrim(name) <> ''),
 address text,
 branch_type varchar(20) NOT NULL CHECK (branch_type IN ('BRANCH','DEPOSITORY')),
 phone varchar(32),
 is_active boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX branch_name_uq ON branch(lower(btrim(name)));
CREATE TABLE storage_location (
 location_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 branch_id bigint NOT NULL REFERENCES branch ON DELETE RESTRICT,
 code varchar(50) NOT NULL CHECK (btrim(code) <> ''),
 name varchar(255) NOT NULL CHECK (btrim(name) <> ''),
 is_active boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX location_code_uq ON storage_location(branch_id, lower(btrim(code)));
CREATE TABLE book_stock (
 location_id bigint NOT NULL REFERENCES storage_location ON DELETE RESTRICT,
 book_id bigint NOT NULL REFERENCES book ON DELETE RESTRICT,
 copies_count integer NOT NULL DEFAULT 0 CHECK (copies_count >= 0),
 PRIMARY KEY(location_id, book_id)
);
CREATE INDEX stock_book_idx ON book_stock(book_id);
CREATE TABLE faculty (
 faculty_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(255) NOT NULL CHECK (btrim(name) <> ''),
 is_active boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX faculty_name_uq ON faculty(lower(btrim(name)));
CREATE TABLE book_usage (
 branch_id bigint NOT NULL REFERENCES branch ON DELETE RESTRICT,
 book_id bigint NOT NULL REFERENCES book ON DELETE RESTRICT,
 faculty_id bigint NOT NULL REFERENCES faculty ON DELETE RESTRICT,
 PRIMARY KEY(branch_id, book_id, faculty_id)
);
CREATE INDEX usage_book_idx ON book_usage(book_id);
CREATE INDEX usage_faculty_idx ON book_usage(faculty_id);
CREATE TABLE student (
 student_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 student_card_number varchar(50) NOT NULL CHECK (btrim(student_card_number) <> ''),
 full_name varchar(255) NOT NULL CHECK (btrim(full_name) <> ''),
 faculty_id bigint NOT NULL REFERENCES faculty ON DELETE RESTRICT,
 is_active boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX student_card_uq ON student(lower(btrim(student_card_number)));
CREATE INDEX student_faculty_idx ON student(faculty_id);
CREATE TABLE app_role (
 role_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 code varchar(30) NOT NULL UNIQUE CHECK (code IN ('ADMIN','LIBRARIAN','VIEWER')),
 name varchar(100) NOT NULL CHECK (btrim(name) <> '')
);
INSERT INTO app_role(code,name) VALUES ('ADMIN','Администратор'),('LIBRARIAN','Библиотекарь'),('VIEWER','Просмотр');
CREATE TABLE app_user (
 user_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 login varchar(100) NOT NULL CHECK (btrim(login) <> ''),
 password_hash text NOT NULL,
 role_id bigint NOT NULL REFERENCES app_role ON DELETE RESTRICT,
 is_active boolean NOT NULL DEFAULT true
);
CREATE UNIQUE INDEX app_user_login_uq ON app_user(lower(btrim(login)));
CREATE INDEX user_role_idx ON app_user(role_id);
CREATE TABLE loan (
 loan_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 location_id bigint NOT NULL,
 book_id bigint NOT NULL,
 student_id bigint NOT NULL REFERENCES student ON DELETE RESTRICT,
 faculty_at_issue_id bigint NOT NULL REFERENCES faculty ON DELETE RESTRICT,
 issued_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 returned_at timestamptz,
 issued_by_user_id bigint NOT NULL REFERENCES app_user ON DELETE RESTRICT,
 returned_by_user_id bigint REFERENCES app_user ON DELETE RESTRICT,
 FOREIGN KEY(location_id,book_id) REFERENCES book_stock ON DELETE RESTRICT,
 CHECK ((returned_at IS NULL) = (returned_by_user_id IS NULL)),
 CHECK (returned_at >= issued_at)
);
CREATE INDEX loan_open_idx ON loan(location_id, book_id) WHERE returned_at IS NULL;
CREATE INDEX loan_stock_idx ON loan(location_id,book_id);
CREATE INDEX loan_book_date_idx ON loan(book_id,issued_at);
CREATE INDEX loan_student_idx ON loan(student_id);
CREATE INDEX loan_faculty_idx ON loan(faculty_at_issue_id);
CREATE INDEX loan_issuer_idx ON loan(issued_by_user_id);
CREATE INDEX loan_returner_idx ON loan(returned_by_user_id);
CREATE TABLE audit_log (
 event_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 event_time timestamptz NOT NULL DEFAULT clock_timestamp(),
 user_id bigint REFERENCES app_user ON DELETE RESTRICT,
 operation varchar(50) NOT NULL,
 entity_type varchar(100),
 entity_key jsonb,
 details jsonb NOT NULL
);
CREATE INDEX audit_user_idx ON audit_log(user_id);
CREATE INDEX audit_time_idx ON audit_log(event_time DESC);

-- Успешные изменения и аудит откатываются вместе. Пароли не попадают в журнал.
CREATE FUNCTION library_api.audit_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE old_data jsonb; new_data jsonb; keys jsonb := '{}'; key_name text;
BEGIN
 old_data := CASE WHEN TG_OP <> 'INSERT' THEN to_jsonb(OLD) - 'password_hash' END;
 new_data := CASE WHEN TG_OP <> 'DELETE' THEN to_jsonb(NEW) - 'password_hash' END;
 FOREACH key_name IN ARRAY TG_ARGV LOOP
   keys := keys || jsonb_build_object(key_name, coalesce(new_data,old_data)->key_name);
 END LOOP;
 INSERT INTO audit_log(user_id,operation,entity_type,entity_key,details)
 VALUES (nullif(current_setting('library.user_id',true),'')::bigint, TG_OP, TG_TABLE_NAME, keys,
 jsonb_build_object('old',old_data,'new',new_data));
 RETURN coalesce(NEW,OLD);
END $$;
DO $$
DECLARE t text; k text;
BEGIN
 FOR t,k IN SELECT * FROM (VALUES ('book','book_id'),('author','author_id'),('publisher','publisher_id'),
 ('branch','branch_id'),('faculty','faculty_id'),('storage_location','location_id'),('student','student_id'),
 ('app_user','user_id'),('loan','loan_id')) AS x(t,k) LOOP
 EXECUTE format('CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION library_api.audit_change(%L)',t,k);
 END LOOP;
END $$;
CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON book_stock FOR EACH ROW EXECUTE FUNCTION library_api.audit_change('location_id','book_id');
CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON book_usage FOR EACH ROW EXECUTE FUNCTION library_api.audit_change('branch_id','book_id','faculty_id');
CREATE TRIGGER audit_change AFTER INSERT OR UPDATE OR DELETE ON book_author FOR EACH ROW EXECUTE FUNCTION library_api.audit_change('book_id','author_id');

CREATE FUNCTION library_api.validate_book_trigger() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.publication_year > extract(year FROM current_date)+1 THEN RAISE EXCEPTION 'INVALID_PUBLICATION_YEAR'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER book_year BEFORE INSERT OR UPDATE ON book FOR EACH ROW EXECUTE FUNCTION library_api.validate_book_trigger();

-- Отложенная проверка позволяет заменить полный набор авторов в одной транзакции.
CREATE FUNCTION library_api.check_book_authors() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE id bigint; ids bigint[]; b book;
BEGIN
 id := CASE WHEN TG_OP='DELETE' THEN OLD.book_id ELSE NEW.book_id END;
 SELECT * INTO b FROM book WHERE book_id=id;
 IF NOT FOUND THEN RETURN NULL; END IF;
 SELECT array_agg(author_id ORDER BY author_id) INTO ids FROM book_author WHERE book_id=id;
 IF ids IS NULL THEN RAISE EXCEPTION 'AUTHOR_REQUIRED'; END IF;
 IF EXISTS (SELECT 1 FROM book other WHERE other.book_id<>id AND lower(btrim(other.title))=lower(btrim(b.title))
   AND other.publisher_id=b.publisher_id AND other.publication_year=b.publication_year
   AND (SELECT array_agg(author_id ORDER BY author_id) FROM book_author WHERE book_id=other.book_id)=ids)
 THEN RAISE EXCEPTION 'DUPLICATE_BOOK'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER book_authors_check AFTER INSERT OR UPDATE ON book DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION library_api.check_book_authors();
CREATE CONSTRAINT TRIGGER book_authors_check AFTER INSERT OR UPDATE OR DELETE ON book_author DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION library_api.check_book_authors();

CREATE FUNCTION library_api.protect_stock() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.copies_count < 0 THEN RAISE EXCEPTION 'NEGATIVE_STOCK'; END IF;
 IF NEW.copies_count < (SELECT count(*) FROM loan WHERE location_id=NEW.location_id AND book_id=NEW.book_id AND returned_at IS NULL)
 THEN RAISE EXCEPTION 'STOCK_BELOW_LOANS'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER protect_stock BEFORE INSERT OR UPDATE ON book_stock FOR EACH ROW EXECUTE FUNCTION library_api.protect_stock();
CREATE FUNCTION library_api.protect_location() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.branch_id <> OLD.branch_id AND EXISTS (SELECT 1 FROM loan WHERE location_id=OLD.location_id)
 THEN RAISE EXCEPTION 'LOCATION_HAS_HISTORY'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER protect_location BEFORE UPDATE ON storage_location FOR EACH ROW EXECUTE FUNCTION library_api.protect_location();
