CREATE FUNCTION library_api.assert_book_branch(p_branch_id bigint,p_book_id bigint) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM book WHERE book_id=p_book_id) OR NOT EXISTS (SELECT 1 FROM branch WHERE branch_id=p_branch_id)
 THEN RAISE EXCEPTION 'ENTITY_NOT_FOUND'; END IF;
END $$;
CREATE FUNCTION library_api.get_book_copies(p_branch_id bigint,p_book_id bigint) RETURNS integer LANGUAGE plpgsql AS $$
BEGIN
 PERFORM library_api.assert_book_branch(p_branch_id,p_book_id);
 RETURN (SELECT coalesce(sum(s.copies_count),0)::integer FROM book_stock s JOIN storage_location l USING(location_id) WHERE l.branch_id=p_branch_id AND s.book_id=p_book_id);
END $$;
CREATE FUNCTION library_api.get_faculty_count(p_branch_id bigint,p_book_id bigint) RETURNS integer LANGUAGE plpgsql AS $$
BEGIN
 PERFORM library_api.assert_book_branch(p_branch_id,p_book_id);
 RETURN (SELECT count(*)::integer FROM book_usage WHERE branch_id=p_branch_id AND book_id=p_book_id);
END $$;
CREATE PROCEDURE library_api.list_book_faculties(IN p_branch_id bigint,IN p_book_id bigint,INOUT result jsonb) LANGUAGE plpgsql AS $$
BEGIN
 PERFORM library_api.assert_book_branch(p_branch_id,p_book_id);
 SELECT coalesce(jsonb_agg(jsonb_build_object('faculty_id',f.faculty_id,'name',f.name) ORDER BY f.name),'[]') INTO result
 FROM book_usage u JOIN faculty f USING(faculty_id) WHERE u.branch_id=p_branch_id AND u.book_id=p_book_id;
END $$;
CREATE PROCEDURE library_api.validate_book(IN data jsonb) LANGUAGE plpgsql AS $$
BEGIN
 IF nullif(btrim(data->>'title'),'') IS NULL OR (data->>'publisherId') IS NULL THEN RAISE EXCEPTION 'INVALID_BOOK_DATA'; END IF;
 IF (data->>'publicationYear')::integer NOT BETWEEN 1450 AND extract(year FROM current_date)+1 OR data->>'publicationYear' IS NULL THEN RAISE EXCEPTION 'INVALID_PUBLICATION_YEAR'; END IF;
 IF coalesce((data->>'pagesCount')::integer,0)<=0 OR coalesce((data->>'illustrationsCount')::integer,-1)<0 OR coalesce((data->>'price')::numeric,-1)<0 THEN RAISE EXCEPTION 'INVALID_BOOK_DATA'; END IF;
 IF jsonb_array_length(coalesce(data->'authorIds','[]'))=0 THEN RAISE EXCEPTION 'AUTHOR_REQUIRED'; END IF;
END $$;
CREATE PROCEDURE library_api.add_book(IN data jsonb, INOUT id bigint) LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(817001);
 CALL library_api.validate_book(data);
 INSERT INTO book(title,publisher_id,publication_year,pages_count,illustrations_count,price)
 VALUES (btrim(data->>'title'),(data->>'publisherId')::bigint,(data->>'publicationYear')::smallint,
 (data->>'pagesCount')::integer,(data->>'illustrationsCount')::integer,(data->>'price')::numeric) RETURNING book_id INTO id;
 INSERT INTO book_author SELECT id, value::bigint FROM jsonb_array_elements_text(data->'authorIds');
END $$;
CREATE PROCEDURE library_api.update_book(IN id bigint,IN data jsonb) LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(817001);
 CALL library_api.validate_book(data);
 UPDATE book SET title=btrim(data->>'title'),publisher_id=(data->>'publisherId')::bigint,
 publication_year=(data->>'publicationYear')::smallint,pages_count=(data->>'pagesCount')::integer,
 illustrations_count=(data->>'illustrationsCount')::integer,price=(data->>'price')::numeric WHERE book_id=id;
 IF NOT FOUND THEN RAISE EXCEPTION 'ENTITY_NOT_FOUND'; END IF;
 DELETE FROM book_author WHERE book_id=id;
 INSERT INTO book_author SELECT id,value::bigint FROM jsonb_array_elements_text(data->'authorIds');
END $$;
CREATE PROCEDURE library_api.add_branch(IN data jsonb,INOUT id bigint) LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO branch(name,address,branch_type,phone,is_active) VALUES
 (btrim(data->>'name'),data->>'address',data->>'branchType',data->>'phone',coalesce((data->>'active')::boolean,true)) RETURNING branch_id INTO id;
 INSERT INTO storage_location(branch_id,code,name) VALUES(id,'main','Основное место хранения');
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'DUPLICATE_BRANCH';
END $$;
CREATE PROCEDURE library_api.update_branch(IN id bigint,IN data jsonb) LANGUAGE plpgsql AS $$
BEGIN
 UPDATE branch SET name=btrim(data->>'name'),address=data->>'address',branch_type=data->>'branchType',phone=data->>'phone',is_active=(data->>'active')::boolean WHERE branch_id=id;
 IF NOT FOUND THEN RAISE EXCEPTION 'ENTITY_NOT_FOUND'; END IF;
EXCEPTION WHEN unique_violation THEN RAISE EXCEPTION 'DUPLICATE_BRANCH';
END $$;
CREATE PROCEDURE library_api.log_user_exception(IN actor bigint, IN error_code text,IN context jsonb) LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO audit_log(user_id,operation,details) VALUES(actor,'USER_EXCEPTION',jsonb_build_object('code',error_code,'context',context));
END $$;
