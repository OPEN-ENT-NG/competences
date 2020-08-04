ALTER TABLE notes.appreciation_matiere_periode
    ADD COLUMN id bigserial UNIQUE NOT NULL,
	DROP CONSTRAINT pk_appreciation_matiere_periode,
	ADD CONSTRAINT pk_appreciation_matiere_periode PRIMARY KEY (id, id_periode, id_classe, id_matiere, id_eleve);

CREATE TABLE notes.rel_appreciations_users_neo
(
    id bigserial NOT NULL,
    appreciation_matiere_periode_id bigint NOT NULL,
    user_id_neo character varying(255) NOT NULL,
    creation_date TIMESTAMP WITHOUT TIME ZONE,
    update_date TIMESTAMP WITHOUT TIME ZONE,
    UNIQUE (id),
    UNIQUE (user_id_neo, appreciation_matiere_periode_id),
    CONSTRAINT pk_appreciation_owner PRIMARY KEY (id),
    CONSTRAINT fk_id_appreciation FOREIGN KEY (appreciation_matiere_periode_id)
        REFERENCES  notes.appreciation_matiere_periode (id) MATCH SIMPLE
        ON UPDATE CASCADE
        ON DELETE CASCADE
);

COMMENT ON TABLE notes.rel_appreciations_users_neo
    IS 'For find user with update and create appreciation in appreciation_matiere_periode';
