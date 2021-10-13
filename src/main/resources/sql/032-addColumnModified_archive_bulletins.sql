ALTER TABLE notes.archive_bulletins ADD column modified timestamp without time zone;
UPDATE notes.archive_bulletins
SET modified = arch2.created
    FROM (SELECT id_file,created FROM notes.archive_bulletins) arch2
WHERE archive_bulletins.id_file = arch2.id_file;
ALTER TABLE notes.archive_bulletins ALTER COLUMN modified SET NOT NULL;