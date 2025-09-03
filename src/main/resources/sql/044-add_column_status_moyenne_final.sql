CREATE TYPE statut_type AS ENUM ('NN', 'EA', 'DI');
ALTER TABLE notes.moyenne_finale
    ADD COLUMN statut statut_type NOT NULL DEFAULT 'NN';
