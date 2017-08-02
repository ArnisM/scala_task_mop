SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = ON;
SET check_function_bodies = FALSE;
SET client_min_messages = WARNING;
SET row_security = OFF;

--
-- Name: news; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE news WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'Bosnian (Latin)_Bosnia and Herzegovina.1250' LC_CTYPE = 'Bosnian (Latin)_Bosnia and Herzegovina.1250';


ALTER DATABASE news
OWNER TO postgres;

\connect news

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = ON;
SET check_function_bodies = FALSE;
SET client_min_messages = WARNING;
SET row_security = OFF;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;

--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = public, pg_catalog;

--
-- Name: create_news(json); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION create_news(news_data JSON)
  RETURNS JSON
LANGUAGE plpgsql
AS $$
DECLARE

  news_id_    BIGINT;

  title_      VARCHAR := news_data ->> 'title';
  content_    TEXT := news_data ->> 'content';
  popularity_ INTEGER := news_data ->> 'popularity';
  tags_       TEXT [] := REPLACE(REPLACE(news_data ->> 'tag', '[', '{'), ']', '}');

BEGIN


  INSERT INTO news_item (title, "content", created_date, popularity, "tag") VALUES
    (title_, content_, now(), popularity_, tags_)
  RETURNING news_id
    INTO news_id_;

  RETURN row_to_json(t, TRUE) FROM (

  SELECT
  *
  FROM
  PUBLIC.news_item
  WHERE
  news_item.news_id = news_id_
  ) t;

END;
$$;


ALTER FUNCTION public.create_news(news_data JSON )
OWNER TO postgres;

--
-- Name: delete_news(character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION delete_news(title_ CHARACTER VARYING)
  RETURNS JSON
LANGUAGE plpgsql
AS $$
DECLARE

  to_return JSON;
BEGIN


  to_return := (SELECT row_to_json(t, TRUE)
                FROM (

                       SELECT *
                       FROM
                         public.news_item
                       WHERE
                         news_item.title = title_
                     ) t);
  DELETE FROM news_item
  WHERE title = title_;
  RETURN to_return;

END;
$$;


ALTER FUNCTION public.delete_news(title_ CHARACTER VARYING )
OWNER TO postgres;

--
-- Name: update_news(json); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION update_news(news_data JSON)
  RETURNS JSON
LANGUAGE plpgsql
AS $$
DECLARE

  news_id_    BIGINT := news_data ->> 'news_id';

  title_      VARCHAR := news_data ->> 'title';
  content_    TEXT := news_data ->> 'content';
  popularity_ INTEGER := news_data ->> 'popularity';
  tags_       TEXT [] := REPLACE(REPLACE(news_data ->> 'tag', '[', '{'), ']', '}');

BEGIN

  IF (news_id_ IS NOT NULL)
  THEN

    UPDATE news_item
    SET title    = COALESCE(title_, title), "content" = COALESCE(content_, "content"),
      popularity = COALESCE(popularity_, popularity), "tag" = COALESCE(tags_, "tag")
    WHERE news_id = news_id_;
    RETURN row_to_json(t, TRUE) FROM (

    SELECT
    *
    FROM
    PUBLIC.news_item
    WHERE
    news_item.news_id = news_id_
    ) t;
  END IF;
  IF (title_ IS NOT NULL)
  THEN
    UPDATE news_item
    SET title    = COALESCE(title_, title), "content" = COALESCE(content_, "content"),
      popularity = COALESCE(popularity_, popularity), "tag" = COALESCE(tags_, "tag")
    WHERE title = title_;
    RETURN row_to_json(t, TRUE) FROM (

    SELECT
    *
    FROM
    PUBLIC.news_item
    WHERE
    news_item.title = title_
    ) t;
  END IF;
  RETURN row_to_json(t, TRUE) FROM (

  SELECT
  *
  FROM
  PUBLIC.news_item
  WHERE
  news_item.news_id = news_id_
  ) t;

END;
$$;


ALTER FUNCTION public.update_news(news_data JSON )
OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = FALSE;

--
-- Name: news_item; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE news_item (
  news_id      BIGINT                                 NOT NULL,
  title        CHARACTER VARYING                      NOT NULL,
  content      TEXT,
  created_date TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
  popularity   INTEGER DEFAULT 0,
  tag          TEXT []
);


ALTER TABLE news_item
  OWNER TO postgres;

--
-- Name: news_item_news_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE news_item_news_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


ALTER TABLE news_item_news_id_seq
  OWNER TO postgres;

--
-- Name: news_item_news_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE news_item_news_id_seq OWNED BY news_item.news_id;

--
-- Name: news_item news_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY news_item
  ALTER COLUMN news_id SET DEFAULT nextval('news_item_news_id_seq' :: REGCLASS);

--
-- Name: news_item pk_news_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY news_item
  ADD CONSTRAINT pk_news_id PRIMARY KEY (news_id);

--
-- Name: news_item_title_uindex; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX news_item_title_uindex
  ON news_item USING BTREE (title);
