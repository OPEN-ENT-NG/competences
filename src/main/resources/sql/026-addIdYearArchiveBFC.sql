ALTER TABLE notes.archive_bfc ADD COLUMN id_annee character varying;
UPDATE notes.archive_bfc SET id_annee = '2019';
ALTER TABLE notes.archive_bfc ALTER COLUMN id_annee SET NOT NULL;

ALTER TABLE notes.archive_bfc DROP CONSTRAINT IF EXISTS duplicate_files_properties;
ALTER TABLE notes.archive_bfc ADD CONSTRAINT duplicate_files_properties UNIQUE (id_classe, id_eleve, id_etablissement, id_cycle, id_annee);