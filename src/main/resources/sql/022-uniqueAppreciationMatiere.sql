DELETE FROM notes.appreciation_matiere_periode a
    USING notes.appreciation_matiere_periode b
WHERE a.id < b.id
  AND a.id_matiere = b.id_matiere
  AND a.id_periode = b.id_periode
  AND a.id_eleve = b.id_eleve
  AND a.id_classe = b.id_classe;

ALTER TABLE notes.appreciation_matiere_periode
    DROP CONSTRAINT IF EXISTS unique_appreciation_matiere_periode,
    ADD CONSTRAINT unique_appreciation_matiere_periode UNIQUE (id_periode, id_eleve, id_classe, id_matiere);
