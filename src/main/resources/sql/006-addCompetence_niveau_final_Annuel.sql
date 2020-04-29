CREATE TABLE notes.competence_niveau_final_annuel
(
	id_eleve character varying(255) NOT NULL,
	niveau_final integer,
	id_competence bigint NOT NULL,
	id_matiere character varying(255) NOT NULL,
	CONSTRAINT pk_niveau_final_annuel PRIMARY KEY (id_eleve, id_competence, id_matiere),
	CONSTRAINT fk_competences_id FOREIGN KEY (id_competence)
	REFERENCES notes.competences (id) MATCH SIMPLE
	ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT competence_niveau_final_annuel UNIQUE (id_eleve, id_competence, id_matiere)
);