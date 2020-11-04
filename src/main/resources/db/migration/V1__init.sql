--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.14
-- Dumped by pg_dump version 9.6.14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
-- SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_with_oids = false;

-- Name: audit_message; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE audit_message (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp without time zone,
    image_identifier character varying(255) NOT NULL,
    message character varying(2048) NOT NULL,
    user_id character varying(255) NOT NULL
);

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: image; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE image (
    id bigint NOT NULL,
    version bigint NOT NULL,
    attribution character varying(255),
    contentmd5hash character varying(255),
    contentsha1hash character varying(255),
    copyright character varying(255),
    date_taken timestamp without time zone,
    date_uploaded timestamp without time zone,
    description character varying(8096),
    extension character varying(255),
    file_size bigint,
    height integer,
    image_identifier character varying(255) NOT NULL,
    mime_type character varying(255),
    original_filename_old character varying(255),
    parent_id bigint,
    square_thumb_size integer,
    thumb_height integer,
    thumb_width integer,
    uploader character varying(255),
    width integer,
    zoom_levels integer,
    mm_per_pixel double precision,
    harvestable boolean,
    creator character varying(255),
    data_resource_uid character varying(255),
    license character varying(255),
    rights character varying(255),
    rights_holder character varying(255),
    title character varying(255),
    date_deleted timestamp without time zone,
    recognised_license_id bigint,
    occurrence_id character varying(255),
    calibrated_by_user character varying(255),
    original_filename character varying(2048)
);

--
-- Name: image_keyword; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE image_keyword (
    id bigint NOT NULL,
    version bigint NOT NULL,
    image_id bigint NOT NULL,
    keyword character varying(255) NOT NULL
);

--
-- Name: image_meta_data_item; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE image_meta_data_item (
    id bigint NOT NULL,
    version bigint NOT NULL,
    image_id bigint NOT NULL,
    name character varying(1024) NOT NULL,
    source character varying(255),
    value character varying(8096) NOT NULL
);

--
-- Name: image_metadata_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE image_metadata_seq
    START WITH 280069337
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: image_tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE image_tag (
    id bigint NOT NULL,
    version bigint NOT NULL,
    image_id bigint NOT NULL,
    tag_id bigint NOT NULL
);

--
-- Name: image_thumbnail; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE image_thumbnail (
    id bigint NOT NULL,
    version bigint NOT NULL,
    height integer NOT NULL,
    image_id bigint NOT NULL,
    is_square boolean NOT NULL,
    name character varying(255) NOT NULL,
    width integer NOT NULL
);

--
-- Name: import_field_definition; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE import_field_definition (
    id bigint NOT NULL,
    version bigint NOT NULL,
    field_name character varying(255) NOT NULL,
    field_type character varying(255) NOT NULL,
    value character varying(1024) NOT NULL
);

--
-- Name: license; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE license (
    id bigint NOT NULL,
    version bigint NOT NULL,
    url character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    image_url character varying(255),
    acronym character varying(255) NOT NULL
);

--
-- Name: license_mapping; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE license_mapping (
    id bigint NOT NULL,
    version bigint NOT NULL,
    value character varying(255) NOT NULL,
    license_id bigint NOT NULL
);

--
-- Name: outsourced_job; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE outsourced_job (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_created timestamp without time zone,
    expected_duration_in_minutes integer NOT NULL,
    image_id bigint NOT NULL,
    task_type character varying(255) NOT NULL,
    ticket character varying(255) NOT NULL
);

--
-- Name: reversedelete; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE reversedelete (
    id character varying(50) NOT NULL
);

--
-- Name: search_criteria_definition; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE search_criteria_definition (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(1024),
    field_name character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    units character varying(255),
    value_type character varying(255) NOT NULL
);

--
-- Name: selected_image; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE selected_image (
    id bigint NOT NULL,
    version bigint NOT NULL,
    image_id bigint NOT NULL,
    user_id character varying(255) NOT NULL
);

--
-- Name: setting; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE setting (
    id bigint NOT NULL,
    version bigint NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    value character varying(255) NOT NULL
);

--
-- Name: staged_file; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE staged_file (
    id bigint NOT NULL,
    version bigint NOT NULL,
    date_staged timestamp without time zone NOT NULL,
    filename character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL
);

--
-- Name: staging_column_definition; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE staging_column_definition (
    id bigint NOT NULL,
    version bigint NOT NULL,
    field_definition_type character varying(255),
    field_name character varying(255) NOT NULL,
    format character varying(255),
    user_id character varying(255) NOT NULL
);

--
-- Name: subimage; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE subimage (
    id bigint NOT NULL,
    version bigint NOT NULL,
    height integer NOT NULL,
    parent_image_id bigint NOT NULL,
    subimage_id bigint NOT NULL,
    width integer NOT NULL,
    x integer NOT NULL,
    y integer NOT NULL
);

--
-- Name: tag; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE tag (
    id bigint NOT NULL,
    version bigint NOT NULL,
    path character varying(2048) NOT NULL
);

--
-- Name: user_preferences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE user_preferences (
                                  id bigint NOT NULL,
                                  version bigint NOT NULL,
                                  export_columns character varying(4096),
                                  user_id character varying(255) NOT NULL
);
--
-- Name: audit_message audit_message_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY audit_message
    ADD CONSTRAINT audit_message_pkey PRIMARY KEY (id);


--
-- Name: image_keyword image_keyword_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_keyword
    ADD CONSTRAINT image_keyword_pkey PRIMARY KEY (id);


--
-- Name: image_meta_data_item image_meta_data_item_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_meta_data_item
    ADD CONSTRAINT image_meta_data_item_pkey PRIMARY KEY (id);


--
-- Name: image image_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image
    ADD CONSTRAINT image_pkey PRIMARY KEY (id);


--
-- Name: image_tag image_tag_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_tag
    ADD CONSTRAINT image_tag_pkey PRIMARY KEY (id);


--
-- Name: image_thumbnail image_thumbnail_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_thumbnail
    ADD CONSTRAINT image_thumbnail_pkey PRIMARY KEY (id);


--
-- Name: import_field_definition import_field_definition_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY import_field_definition
    ADD CONSTRAINT import_field_definition_pkey PRIMARY KEY (id);


--
-- Name: license_mapping license_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY license_mapping
    ADD CONSTRAINT license_mapping_pkey PRIMARY KEY (id);


--
-- Name: license license_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY license
    ADD CONSTRAINT license_pkey PRIMARY KEY (id);


--
-- Name: outsourced_job outsourced_job_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY outsourced_job
    ADD CONSTRAINT outsourced_job_pkey PRIMARY KEY (id);


--
-- Name: reversedelete reversedelete_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY reversedelete
    ADD CONSTRAINT reversedelete_pkey PRIMARY KEY (id);


--
-- Name: search_criteria_definition search_criteria_definition_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY search_criteria_definition
    ADD CONSTRAINT search_criteria_definition_pkey PRIMARY KEY (id);


--
-- Name: selected_image selected_image_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY selected_image
    ADD CONSTRAINT selected_image_pkey PRIMARY KEY (id);


--
-- Name: setting setting_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY setting
    ADD CONSTRAINT setting_pkey PRIMARY KEY (id);


--
-- Name: staged_file staged_file_filename_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY staged_file
    ADD CONSTRAINT staged_file_filename_key UNIQUE (filename);


--
-- Name: staged_file staged_file_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY staged_file
    ADD CONSTRAINT staged_file_pkey PRIMARY KEY (id);


--
-- Name: staging_column_definition staging_column_definition_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY staging_column_definition
    ADD CONSTRAINT staging_column_definition_pkey PRIMARY KEY (id);


--
-- Name: subimage subimage_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY subimage
    ADD CONSTRAINT subimage_pkey PRIMARY KEY (id);


--
-- Name: tag tag_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY tag
    ADD CONSTRAINT tag_pkey PRIMARY KEY (id);


--
-- Name: user_preferences user_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY user_preferences
    ADD CONSTRAINT user_preferences_pkey PRIMARY KEY (id);


--
-- Name: image_datetaken_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_datetaken_idx ON image USING btree (date_taken DESC);


--
-- Name: image_dateuploaded; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_dateuploaded ON image USING btree (date_uploaded, id);


--
-- Name: image_keyword_image_id_keyword_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_keyword_image_id_keyword_idx ON image_keyword USING btree (image_id, keyword);


--
-- Name: image_md5hash_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_md5hash_idx ON image USING btree (id, contentmd5hash);


--
-- Name: image_meta_data_item_image_id_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_meta_data_item_image_id_idx ON image_meta_data_item USING btree (image_id);


--
-- Name: image_meta_data_item_image_name_type_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_meta_data_item_image_name_type_idx ON image_meta_data_item USING btree (image_id, name, source);


--
-- Name: image_meta_data_item_name_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_meta_data_item_name_idx ON image_meta_data_item USING btree (name);


--
-- Name: image_originalfilename_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_originalfilename_idx ON image USING btree (original_filename_old);


--
-- Name: image_tag_image_id_tag_id_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_tag_image_id_tag_id_idx ON image_tag USING btree (image_id, tag_id);


--
-- Name: image_thumbnail_image_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_thumbnail_image_idx ON image_thumbnail USING btree (image_id, name);


--
-- Name: image_thumbnail_image_name_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_thumbnail_image_name_idx ON image_thumbnail USING btree (image_id, name);


--
-- Name: image_thumbnail_name_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX image_thumbnail_name_idx ON image_thumbnail USING btree (name);


--
-- Name: imageidentifier_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX imageidentifier_idx ON image USING btree (image_identifier);


--
-- Name: new_image_originalfilename_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX new_image_originalfilename_idx ON image USING btree (original_filename);


--
-- Name: selected_image_user_image_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX selected_image_user_image_idx ON selected_image USING btree (user_id, image_id);


--

--
-- Name: userprefs_userid_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX userprefs_userid_idx ON user_preferences USING btree (user_id);


--
-- Name: image_thumbnail fk33563f08ea6a3d60; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_thumbnail
    ADD CONSTRAINT fk33563f08ea6a3d60 FOREIGN KEY (image_id) REFERENCES image(id);


--
-- Name: image fk5faa95b990d67b1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image
    ADD CONSTRAINT fk5faa95b990d67b1 FOREIGN KEY (parent_id) REFERENCES image(id);


--
-- Name: outsourced_job fk68bcbe19ea6a3d60; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY outsourced_job
    ADD CONSTRAINT fk68bcbe19ea6a3d60 FOREIGN KEY (image_id) REFERENCES image(id);


--
-- Name: selected_image fk82c55eb7ea6a3d60; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY selected_image
    ADD CONSTRAINT fk82c55eb7ea6a3d60 FOREIGN KEY (image_id) REFERENCES image(id);


--
-- Name: subimage fk8495d31b34e9bba0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY subimage
    ADD CONSTRAINT fk8495d31b34e9bba0 FOREIGN KEY (subimage_id) REFERENCES image(id);


--
-- Name: subimage fk8495d31bbfd28555; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY subimage
    ADD CONSTRAINT fk8495d31bbfd28555 FOREIGN KEY (parent_image_id) REFERENCES image(id);


--
-- Name: image_meta_data_item fk975f1032ea6a3d60; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_meta_data_item
    ADD CONSTRAINT fk975f1032ea6a3d60 FOREIGN KEY (image_id) REFERENCES image(id);


--
-- Name: image_keyword fka999e305ea6a3d60; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_keyword
    ADD CONSTRAINT fka999e305ea6a3d60 FOREIGN KEY (image_id) REFERENCES image(id);


--
-- Name: image_tag fkcbad72b6ea6a3d60; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_tag
    ADD CONSTRAINT fkcbad72b6ea6a3d60 FOREIGN KEY (image_id) REFERENCES image(id);


--
-- Name: image_tag fkcbad72b6fbb8ca80; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY image_tag
    ADD CONSTRAINT fkcbad72b6fbb8ca80 FOREIGN KEY (tag_id) REFERENCES tag(id);


--
-- Name: license_mapping fktetx2lihs6cq4swvdhaqeco7s; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY license_mapping
    ADD CONSTRAINT fktetx2lihs6cq4swvdhaqeco7s FOREIGN KEY (license_id) REFERENCES license(id);


--
-- PostgreSQL database dump complete
--
