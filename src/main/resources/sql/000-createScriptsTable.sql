-- Only one scripts table (not one per schema)
CREATE TABLE notes.scripts
(
  filename character varying(255) NOT NULL,
  passed timestamp without time zone NOT NULL DEFAULT now(),
  CONSTRAINT scripts_pkey PRIMARY KEY (filename)
);