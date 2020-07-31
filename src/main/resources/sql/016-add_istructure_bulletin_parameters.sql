ALTER TABLE notes.bulletin_parameters ADD COLUMN id_structure character varying(36) NOT NULL;
ALTER TABLE notes.bulletin_parameters DROP CONSTRAINT bulletin_paramters_PK;
ALTER TABLE notes.bulletin_parameters ADD CONSTRAINT bulletin_parameters_PK PRIMARY KEY (id_student, id_periode, id_structure);