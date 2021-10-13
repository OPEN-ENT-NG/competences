ALTER TABLE notes.archive_bfc ADD column modified timestamp without time zone;
UPDATE notes.archive_bfc
SET modified = arch2.created
    FROM (SELECT id_file,created FROM notes.archive_bfc) arch2
WHERE archive_bfc.id_file = arch2.id_file;
ALTER TABLE notes.archive_bfc ALTER COLUMN modified SET NOT NULL;
