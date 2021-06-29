CREATE TABLE notes.class_appreciation_digital_skills (
    id bigserial NOT NULL,
    class_or_group_id character varying(255) NOT NULL,
    type_structure character (1) NOT NULL,
    period_type_id bigint NOT NULL,
    appreciation character varying (600) NOT NULL,
    CONSTRAINT class_appreciation_digital_skills_pkey PRIMARY KEY (id),
    CONSTRAINT class_appreciation_digital_skills_uk UNIQUE (class_or_group_id, period_type_id )
);

CREATE TABLE notes.student_appreciation_digital_skills (
    id bigserial NOT NULL,
    student_id character varying(255) NOT NULL,
    structure_id character varying(255) NOT NULL,
    period_type_id bigint NOT NULL,
    appreciation character varying (600) NOT NULL,
    CONSTRAINT student_appreciation_digital_skills_pkey PRIMARY KEY (id),
    CONSTRAINT student_appreciation_digital_skills_uk UNIQUE (structure_id,student_id, period_type_id )
);
CREATE INDEX student_appreciation_digital_skills_idx ON notes.student_appreciation_digital_skills (id);


