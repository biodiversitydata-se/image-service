--  View used for full DB exports
DROP FUNCTION IF EXISTS export_images;

DROP VIEW IF EXISTS export_images;
CREATE VIEW export_images AS
SELECT
    data_resource_uid AS "dataResourceUid",
    occurrence_id as "occurrenceID",
    CONCAT( '${baseUrl}/image/proxyImageThumbnailLarge?imageId=', image_identifier) AS identifier,
    regexp_replace(creator, E'[\\n\\r]+', ' ', 'g' ) AS creator,
    date_taken AS created,
    regexp_replace(title, E'[\\n\\r]+', ' ', 'g' ) AS title,
    mime_type AS format,
    regexp_replace(license, E'[\\n\\r]+', ' ', 'g' ) AS license,
    regexp_replace(rights, E'[\\n\\r]+', ' ', 'g' ) AS rights,
    regexp_replace(rights_holder, E'[\\n\\r]+', ' ', 'g' ) AS "rightsHolder",
    CONCAT('${baseUrl}/image/', image_identifier) AS "references",
    regexp_replace(description, E'[\\n\\r]+', ' ', 'g' ) as description,
    extension as extension,
    l.acronym  as "recognisedLicense",
    i.date_deleted as "dateDeleted",
    contentmd5hash as md5hash,
    file_size as "fileSize",
    width as width,
    height as height,
    zoom_levels as "zoomLevels",
    image_identifier as "imageIdentifier"
FROM image i
    LEFT OUTER JOIN license l ON l.id = i.recognised_license_id
ORDER BY data_resource_uid;

--  Function used for regeneration of elastic search index
DROP FUNCTION IF EXISTS export_index;

DROP VIEW IF EXISTS export_index;
CREATE VIEW export_index AS
SELECT
    image_identifier as "imageIdentifier",
    contentmd5hash as "contentmd5hash",
    contentsha1hash as "contentsha1hash",
    mime_type AS format,
    original_filename AS originalFilename,
    extension as extension,
    TO_CHAR(date_uploaded :: DATE, 'yyyy-mm-dd') AS "dateUploaded",
    TO_CHAR(date_taken :: DATE, 'yyyy-mm-dd') AS "dateTaken",
    file_size as "fileSize",
    height as height,
    width as width,
    zoom_levels as "zoomLevels",
    data_resource_uid AS "dataResourceUid",
    regexp_replace(regexp_replace(creator, '[|''"&]+',''), E'[\\n\\r]+', ' ', 'g' ) AS creator,
    regexp_replace(regexp_replace(title, '[|''"&]+',''), E'[\\n\\r]+', ' ', 'g' ) AS title,
    regexp_replace(regexp_replace(description, '[|''"&]+',''), E'[\\n\\r]+', ' ', 'g' )  AS description,
    regexp_replace(regexp_replace(rights, '[|''"&]+',''), E'[\\n\\r]+', ' ', 'g' )  AS rights,
    regexp_replace(regexp_replace(rights_holder, '[|''"&]+',''), E'[\\n\\r]+', ' ', 'g' )  AS "rightsHolder",
    regexp_replace(regexp_replace(license, '[|''"&]+',''), E'[\\n\\r]+', ' ', 'g' )  AS license,
    thumb_height AS "thumbHeight",
    thumb_width AS "thumbWidth",
    harvestable,
    l.acronym  as "recognisedLicence",
    occurrence_id AS "occurrenceID",
    audience AS "audience",
    source AS "source",
    contributor AS "contributor",
    type AS "type",
    created AS "created",
    dc_references AS "references",
    TO_CHAR(date_uploaded :: DATE, 'yyyy-mm') AS "dateUploadedYearMonth"
FROM image i
     LEFT OUTER JOIN license l ON l.id = i.recognised_license_id
WHERE date_deleted is NULL AND is_duplicate_of_id IS NULL;

--  Function used for exporting metadata for images associated with a dataset
DROP FUNCTION IF EXISTS export_dataset;
CREATE OR REPLACE FUNCTION export_dataset(uid varchar)
RETURNS TABLE(
    imageID text,
    identifier text,
    audience text,
    contributor text,
    created text,
    creator text,
    description text,
    format text,
    license text,
    publisher text,
    "references" text,
    "rightsHolder" text,
    source text,
    title text,
    type text,
    is_duplicate_of_id bigint
) AS $$
select
    i.image_identifier as "imageID",
    NULLIF(regexp_replace(i.original_filename, '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "identifier",
    NULLIF(regexp_replace(i.audience,          '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "audience",
    NULLIF(regexp_replace(i.contributor,       '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "contributor",
    NULLIF(regexp_replace(i.created,           '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "created",
    NULLIF(regexp_replace(i.creator,           '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "creator",
    NULLIF(regexp_replace(i.description,       '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "description",
    NULLIF(regexp_replace(i.mime_type,         '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "format",
    NULLIF(regexp_replace(i.license,           '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "license",
    NULLIF(regexp_replace(i.publisher,         '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "publisher",
    NULLIF(regexp_replace(i.dc_references,     '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "references",
    NULLIF(regexp_replace(i.rights_holder,     '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "rightsHolder",
    NULLIF(regexp_replace(i.source,            '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "source",
    NULLIF(regexp_replace(i.title,             '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "title",
    NULLIF(regexp_replace(i.type,              '\\P{Cc}\\P{Cn}\\P{Cs}\\P{Cf}',  '', 'g'), '')  AS  "type",
    i.is_duplicate_of_id
    from image i
    where data_resource_uid = $1

$$ LANGUAGE SQL;

--  Function used for exporting mapping of URL -> imageID for a dataset
DROP FUNCTION IF EXISTS export_dataset_mapping;
CREATE OR REPLACE FUNCTION export_dataset_mapping(uid varchar)
RETURNS TABLE(
    imageID text,
    url text
) AS $$
select
    image_identifier as "imageID",
    original_filename as "url"
    from image i
    where data_resource_uid = $1
$$ LANGUAGE SQL;


--  Function used for exporting mapping of URL -> imageID for all dataset
DROP FUNCTION IF EXISTS export_mapping;

DROP VIEW IF EXISTS export_mapping;
CREATE VIEW export_mapping AS
SELECT
    data_resource_uid,
    image_identifier as "imageID",
    original_filename as "url"
FROM image i
WHERE data_resource_uid is NOT NULL;