ALTER TABLE notes.devoirs ALTER COLUMN diviseur TYPE numeric USING (diviseur :: numeric);
ALTER TABLE notes.devoirs ADD CONSTRAINT check_diviseur_positive CHECK (diviseur >= 0::numeric)