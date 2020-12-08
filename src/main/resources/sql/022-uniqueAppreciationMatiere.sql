ALTER TABLE notes.appreciation_matiere_periode ADD CONSTRAINT unique_appreciation_matiere_periode UNIQUE (id_periode, id_eleve, id_classe, id_matiere);
