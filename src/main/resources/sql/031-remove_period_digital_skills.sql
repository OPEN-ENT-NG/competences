
do $$
    declare c_name text;
begin
SELECT con.conname into c_name
FROM pg_catalog.pg_constraint con
         INNER JOIN pg_catalog.pg_class rel
                    ON rel.oid = con.conrelid
         INNER JOIN pg_catalog.pg_namespace nsp
                    ON nsp.oid = connamespace
WHERE nsp.nspname = 'notes'
  AND rel.relname = 'class_appreciation_digital_skills' AND contype = 'u';

execute format ('alter table notes.class_appreciation_digital_skills drop constraint %I', c_name);
end;
$$;
ALTER TABLE notes.class_appreciation_digital_skills DROP COLUMN period_type_id;
ALTER TABLE notes.class_appreciation_digital_skills ADD CONSTRAINT class_appreciation_digital_skills_uk UNIQUE (class_or_group_id);

do $$
    declare c_name text;
begin
SELECT con.conname into c_name
FROM pg_catalog.pg_constraint con
         INNER JOIN pg_catalog.pg_class rel
                    ON rel.oid = con.conrelid
         INNER JOIN pg_catalog.pg_namespace nsp
                    ON nsp.oid = connamespace
WHERE nsp.nspname = 'notes'
  AND rel.relname = 'student_appreciation_digital_skills' AND contype = 'u';

execute format ('alter table notes.student_appreciation_digital_skills drop constraint %I', c_name);
end;
$$;
ALTER TABLE notes.student_appreciation_digital_skills DROP COLUMN period_type_id;
ALTER TABLE notes.student_appreciation_digital_skills ADD CONSTRAINT student_appreciation_digital_skills_uk UNIQUE (structure_id, student_id);

do $$
    declare c_name text;
begin
SELECT con.conname into c_name
FROM pg_catalog.pg_constraint con
         INNER JOIN pg_catalog.pg_class rel
                    ON rel.oid = con.conrelid
         INNER JOIN pg_catalog.pg_namespace nsp
                    ON nsp.oid = connamespace
WHERE nsp.nspname = 'notes'
  AND rel.relname = 'student_digital_skills' AND contype = 'u';

execute format ('alter table notes.student_digital_skills drop constraint %I', c_name);
end;
$$;
ALTER TABLE notes.student_digital_skills DROP COLUMN period_type_id;
ALTER TABLE notes.student_digital_skills ADD CONSTRAINT student_digital_skills_uk UNIQUE (id_digital_skill, structure_id, student_id);