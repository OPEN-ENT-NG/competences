CREATE TABLE notes.domaines_digital_skills (
    id bigserial NOT NULL,
    libelle character varying(255) NOT NULL,
    CONSTRAINT domaines_digital_skills_pkey PRIMARY KEY (id)
);

INSERT INTO notes.domaines_digital_skills (libelle) VALUES ('Information et données');
INSERT INTO notes.domaines_digital_skills (libelle) VALUES ('Communication et collaboration');
INSERT INTO notes.domaines_digital_skills (libelle) VALUES ('Création de contenus');
INSERT INTO notes.domaines_digital_skills (libelle) VALUES ('Protection et sécurité');
INSERT INTO notes.domaines_digital_skills (libelle) VALUES ('Environnement numérique');

CREATE TABLE notes.digital_skills (
    id bigserial NOT NULL,
    id_domaine bigint NOT NULL,
    libelle character varying(255) NOT NULL,
    code character varying(255) NOT NULL,
    CONSTRAINT digital_skills_pkey PRIMARY KEY (id)
);

INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (1, 'Mener une recherche et une veille d''information', 'CN_INF_MEN');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (1, 'Gérer des données', 'CN_INF_GER');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (1, 'Traiter des données', 'CN_INF_TRA');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (2, 'Interagir', 'CN_COM_INT');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (2, 'Partager et publier', 'CN_COM_PAR');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (2, 'Collaborer', 'CN_COM_COL');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (2, 'S''insérer dans le monde numérique', 'CN_COM_SIN');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (3, 'Développer des documents textuels', 'CN_CRE_TEX');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (3, 'Développer des documents multimédia', 'CN_CRE_MUL');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (3, 'Adapter les documents à leur finalité', 'CN_CRE_ADA');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (3, 'Programmer', 'CN_CRE_PRO');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (4, 'Sécuriser l''environnement numérique', 'CN_PRO_SEC');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (4, 'Protéger les données personnelles et la vie privée', 'CN_PRO_DON');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (4, 'Protéger la santé, le bien-être et l''environnement', 'CN_PRO_SAN');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (5, 'Résoudre des problèmes techniques', 'CN_ENV_RES');
INSERT INTO notes.digital_skills (id_domaine, libelle, code) VALUES (5, 'Évoluer dans un environnement numérique', 'CN_ENV_EVO');

CREATE TABLE notes.student_digital_skills (
    id bigserial NOT NULL,
    id_digital_skill bigint NOT NULL,
    level bigint NOT NULL,
    student_id character varying(255) NOT NULL,
    structure_id character varying(255) NOT NULL,
    period_type_id bigint NOT NULL,
    CONSTRAINT student_digital_skills_pkey PRIMARY KEY (id),
    CONSTRAINT student_digital_skills_uk UNIQUE (id_digital_skill, structure_id, student_id, period_type_id)
);