 ALTER  FUNCTION notes.masqueCompetence(idCompetence IN BIGINT, idEtablissement IN VARCHAR, valMasque IN BOOLEAN) RENAME TO function_masqueCompetence;
 ALTER  FUNCTION notes.updateDomaineCompetence(idCompetence IN BIGINT, idEtablissement IN VARCHAR, idDomaine IN BIGINT) RENAME TO function_updateDomaineCompetence;
 ALTER  FUNCTION clone_schema_with_sequences(  source_schema text, dest_schema text, include_recs boolean) RENAME TO function_clone_schema_with_sequences;
 ALTER FUNCTION  notes.merge_users(key character varying, data character varying) RENAME TO function_merge_users;
 ALTER FUNCTION notes.deleteCompetence(idCompetence IN BIGINT, idEtablissement IN VARCHAR) RENAME TO function_deleteCompetence;