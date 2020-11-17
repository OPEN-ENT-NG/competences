ALTER TABLE notes.archive_bulletins ADD COLUMN id_annee character varying;
UPDATE notes.archive_bulletins SET id_annee = '2019';
ALTER TABLE notes.archive_bulletins ALTER COLUMN id_annee SET NOT NULL;

ALTER TABLE notes.archive_bulletins
ADD CONSTRAINT unique_archive_bulletins UNIQUE (id_classe, id_eleve, id_etablissement, external_id_classe, id_periode, id_annee, id_parent);