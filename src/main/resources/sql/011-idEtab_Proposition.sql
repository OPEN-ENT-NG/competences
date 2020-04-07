ALTER TABLE notes.proposition ADD COLUMN id_etablissement varchar(255);
CREATE SEQUENCE notes.proposition_seq_id;
ALTER TABLE notes.proposition
   ALTER COLUMN id SET DEFAULT nextval('notes.proposition_seq_id'::regclass);
SELECT SETVAL('notes.proposition_seq_id', COALESCE((SELECT MAX(id) + 1 FROM notes.proposition), 1), FALSE);

