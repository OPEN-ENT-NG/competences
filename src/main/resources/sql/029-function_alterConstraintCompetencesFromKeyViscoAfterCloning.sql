CREATE OR REPLACE FUNCTION notes.function_renameConstraintFromViescoAfterClonning() RETURNS void AS
$BODY$
BEGIN
ALTER TABLE notes.devoirs DROP CONSTRAINT IF EXISTS fk_sousmatiere_id;
ALTER TABLE notes.devoirs ADD CONSTRAINT fk_sousmatiere_id FOREIGN KEY (id_sousmatiere) REFERENCES viesco.type_sousmatiere (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE notes.devoirs DROP CONSTRAINT IF EXISTS fk_devoirs_type_periode;
ALTER TABLE notes.devoirs ADD CONSTRAINT fk_devoirs_type_periode FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE notes.appreciation_classe DROP CONSTRAINT IF EXISTS fk_type_periode_id;
ALTER TABLE notes.appreciation_classe ADD CONSTRAINT fk_type_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE notes.appreciation_elt_bilan_periodique_classe DROP CONSTRAINT IF EXISTS fk_type_periode_id;
ALTER TABLE notes.appreciation_elt_bilan_periodique_classe ADD CONSTRAINT fk_type_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE notes.appreciation_elt_bilan_periodique_eleve DROP CONSTRAINT IF EXISTS fk_type_periode_id;
ALTER TABLE notes.appreciation_elt_bilan_periodique_eleve ADD CONSTRAINT fk_type_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE notes.appreciation_matiere_periode DROP CONSTRAINT IF EXISTS fk_type_periode_id;
ALTER TABLE notes.appreciation_matiere_periode ADD CONSTRAINT fk_type_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE notes.competence_niveau_final DROP CONSTRAINT IF EXISTS fk_periode_id;
ALTER TABLE notes.competence_niveau_final ADD CONSTRAINT fk_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE notes.element_programme DROP CONSTRAINT IF EXISTS fk_type_periode_id;
ALTER TABLE notes.element_programme ADD CONSTRAINT fk_type_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE notes.moyenne_finale DROP CONSTRAINT IF EXISTS fk_type_periode_id;
ALTER TABLE notes.moyenne_finale ADD CONSTRAINT fk_type_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE notes.positionnement DROP CONSTRAINT IF EXISTS fk_type_periode_id;
ALTER TABLE notes.positionnement ADD CONSTRAINT fk_type_periode_id FOREIGN KEY (id_periode) REFERENCES viesco.rel_type_periode (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE;
END;
$BODY$
LANGUAGE plpgsql ;

