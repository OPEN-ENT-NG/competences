CREATE INDEX competences_devoirs_idx ON notes.competences_devoirs(id_devoir);

CREATE INDEX rel_annotations_devoirs_idx ON notes.rel_annotations_devoirs(id_devoir);

CREATE INDEX rel_devoirs_groupes_idx ON notes.rel_devoirs_groupes(id_devoir);