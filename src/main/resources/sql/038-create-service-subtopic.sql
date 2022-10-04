CREATE TABLE notes.services_subtopic
(
  id_subtopic   bigint NOT NULL,
  id_teacher    character varying(36) NOT NULL,
  id_structure    character varying(36) NOT NULL,
  id_topic      character varying(36) NOT NULL,
  id_group      character varying(36) NOT NULL,
  coefficient   numeric DEFAULT (1) NOT NULL,
  CONSTRAINT pk_services_subtopic PRIMARY KEY (id_teacher, id_topic, id_group,id_subtopic)
);
CREATE INDEX services_subtopicIdTeacher on notes.services_subtopic(id_teacher) ;
CREATE INDEX services_subtopiceIdSubTopic on notes.services_subtopic(id_subtopic) ;