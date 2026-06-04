CREATE TYPE notes.statut_type AS ENUM ('NN', 'EA', 'DISP');
ALTER TABLE notes.moyenne_finale
    ADD COLUMN statut notes.statut_type;
