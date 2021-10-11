ALTER TABLE notes.class_appreciation_digital_skills DROP CONSTRAINT class_appreciation_digital_skills_uk;
ALTER TABLE notes.class_appreciation_digital_skills DROP COLUMN period_type_id;
ALTER TABLE notes.class_appreciation_digital_skills ADD CONSTRAINT class_appreciation_digital_skills_uk UNIQUE (class_or_group_id);

ALTER TABLE notes.student_appreciation_digital_skills DROP CONSTRAINT student_appreciation_digital_skills_uk;
ALTER TABLE notes.student_appreciation_digital_skills DROP COLUMN period_type_id;
ALTER TABLE notes.student_appreciation_digital_skills ADD CONSTRAINT student_appreciation_digital_skills_uk UNIQUE (structure_id, student_id);

ALTER TABLE notes.student_digital_skills DROP CONSTRAINT student_digital_skills_uk;
ALTER TABLE notes.student_digital_skills DROP COLUMN period_type_id;
ALTER TABLE notes.student_digital_skills ADD CONSTRAINT student_digital_skills_uk UNIQUE (id_digital_skill, structure_id, student_id);