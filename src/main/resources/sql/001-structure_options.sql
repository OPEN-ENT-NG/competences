CREATE TABLE notes.structure_options
(
  id_structure character varying(255) NOT NULL,
  presences_sync BOOLEAN DEFAULT FALSE,
  CONSTRAINT structure_options_pk PRIMARY KEY (id_structure)
);