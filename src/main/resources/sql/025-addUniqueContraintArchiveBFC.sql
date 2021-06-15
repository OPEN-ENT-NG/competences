ALTER TABLE notes.archive_bfc
ADD CONSTRAINT duplicate_files_properties UNIQUE (id_classe,id_eleve,id_etablissement,id_cycle);