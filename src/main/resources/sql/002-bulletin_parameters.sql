CREATE TABLE notes.bulletin_parameters
(
  id_student character varying(255) NOT NULL,
  id_periode integer NOT NULL,
  params character varying(2000) NOT NULL,
  CONSTRAINT bulletin_paramters_pk PRIMARY KEY (id_student,id_periode)
);