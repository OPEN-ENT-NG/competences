CREATE INDEX id_eleve_competence_niveau_final ON notes.competence_niveau_final USING btree(id_eleve);
CREATE INDEX id_eleve_competences_notes ON notes.competences_notes USING btree(id_eleve);