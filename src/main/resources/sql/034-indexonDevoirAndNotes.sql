CREATE INDEX id_devoirs_idx ON notes.devoirs(id);
CREATE INDEX id_devoirs_notes_idx ON notes.notes(id_devoir);