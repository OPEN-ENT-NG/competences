CREATE OR REPLACE FUNCTION notes.deleteCompetence(idCompetence IN BIGINT, idEtablissement IN VARCHAR)
  RETURNS VARCHAR AS $$
DECLARE
  nbDevoir   INTEGER;
  isLast     BOOLEAN;
  isManuelle BOOLEAN;

BEGIN
  SELECT id_etablissement IS NOT NULL
  INTO isManuelle
  FROM notes.competences
  WHERE id = idCompetence;

  IF isManuelle
  THEN
    WITH isLastQuery AS
    (SELECT count(compDom1.id_competence) = 1 AS isLastOfDom
     FROM (SELECT *
           FROM notes.rel_competences_domaines
             RIGHT JOIN notes.competences
               ON id_competence = id
           WHERE competences.id_etablissement IS NULL OR competences.id_etablissement = idEtablissement) AS compDom1
       INNER JOIN
       (SELECT *
        FROM notes.rel_competences_domaines
        WHERE id_competence = idCompetence) AS compDom2
         ON compDom1.id_domaine = compDom2.id_domaine
     GROUP BY compDom1.id_domaine)

    SELECT bool_or(isLastOfDom)
    INTO isLast
    FROM isLastQuery;

    IF isLast
    THEN
      RAISE NOTICE 'DERNIERE';
      RETURN 'SUPP_KO_LAST';
    ELSE
      SELECT count(*)
      INTO nbDevoir
      FROM notes.competences_devoirs
      WHERE id_competence = idCompetence;

      IF (nbDevoir > 0)
      THEN
        RAISE NOTICE 'MASQUAGE';
        INSERT INTO notes.perso_competences (id_competence, id_etablissement, masque)
        VALUES (idCompetence, idEtablissement, TRUE)
        ON CONFLICT (id_competence, id_etablissement)
          DO UPDATE
            SET masque = TRUE;
            RETURN 'MASQUAGE';

      ELSE
        RAISE NOTICE 'SUPPRESSION';
        DELETE FROM notes.competences
        WHERE id = idCompetence;
        RETURN 'SUPP_OK';
      END IF;

    END IF;
  ELSE
    RAISE NOTICE 'SUPPRPERSO';
    DELETE FROM notes.perso_competences WHERE id_competence = idCompetence AND id_etablissement = idEtablissement;
    RETURN 'SUPP_PERSO_OK';
  END IF;
END;
$$
LANGUAGE PLPGSQL;
CREATE OR REPLACE FUNCTION notes.masqueCompetence(idCompetence IN BIGINT, idEtablissement IN VARCHAR, valMasque IN BOOLEAN)
  RETURNS VARCHAR AS $$
DECLARE
  isLast     BOOLEAN;
  nbDevoir   INTEGER;

BEGIN

  WITH isLastQuery AS
  (SELECT count(compDom1.id_competence) = 1 AS isLastOfDom
   FROM (SELECT *
         FROM notes.rel_competences_domaines
           RIGHT JOIN notes.competences
             ON id_competence = id
         WHERE competences.id_etablissement IS NULL OR competences.id_etablissement = idEtablissement) AS compDom1
     INNER JOIN
     (SELECT *
      FROM notes.rel_competences_domaines
      WHERE id_competence = idCompetence) AS compDom2
       ON compDom1.id_domaine = compDom2.id_domaine
   GROUP BY compDom1.id_domaine)

  SELECT bool_or(isLastOfDom)
  INTO isLast
  FROM isLastQuery;

  IF isLast
  THEN

    RAISE NOTICE 'DERNIERE';
    RETURN 'LAST';

  ELSE
    RAISE NOTICE 'ADDMASK';

    INSERT INTO notes.perso_competences (id_competence, id_etablissement, masque)
    VALUES (idCompetence, idEtablissement, TRUE)
    ON CONFLICT (id_competence, id_etablissement)
      DO UPDATE
        SET masque = valMasque;
    SELECT count(*)
    INTO nbDevoir
    FROM notes.competences_devoirs
    WHERE id_competence = idCompetence;

    IF (nbDevoir > 0)
      THEN
      RETURN 'use';
      ELSE
      RETURN 'notUse';
    END IF;
  END IF;

END;
$$
LANGUAGE PLPGSQL;