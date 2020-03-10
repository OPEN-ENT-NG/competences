ALTER TABLE notes.avis_conseil_bilan_periodique ADD COLUMN id_etablissement varchar(255);
SELECT SETVAL('notes.avis_conseil_bilan_periodique_id_seq', COALESCE((SELECT MAX(id) + 1 FROM notes.avis_conseil_bilan_periodique), 1), FALSE);
