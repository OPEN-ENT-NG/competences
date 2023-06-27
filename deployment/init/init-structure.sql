begin;
-- ajout de l'echelle de converion
INSERT INTO notes.echelle_conversion_niv_note(valmin, valmax, id_structure, id_niveau)
SELECT 1, 1.51,id_etablissement,1
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note)
UNION
SELECT 1, 1.51, id_etablissement ,5
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note)

UNION
SELECT 1.51, 2.51, id_etablissement ,2
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note)

UNION
SELECT 1.51, 2.51, id_etablissement ,6
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note)

UNION
SELECT 2.51, 3.51, id_etablissement,3
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note)

UNION
SELECT 2.51, 3.51, id_etablissement ,7
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note)

UNION
SELECT 3.51, 4, id_etablissement ,4
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note)

UNION
SELECT 3.51, 4, id_etablissement ,8
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_structure FROM notes.echelle_conversion_niv_note);
-- Types de devoir
INSERT INTO notes.type(nom, id_etablissement, default_type, formative)
SELECT 'Formative', id_etablissement ,false, true
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_etablissement FROM notes.type)

UNION
SELECT 'Evaluation', id_etablissement, true, false
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_etablissement FROM notes.type);
-- Annotations
INSERT INTO notes.annotations(libelle, libelle_court, id_etablissement)
SELECT 'Non Noté', 'NN', id_etablissement
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_etablissement FROM notes.annotations)

UNION
SELECT 'Dispense', 'DISP', id_etablissement
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_etablissement FROM notes.annotations)

UNION
SELECT 'Non Rendu', 'NR', id_etablissement
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_etablissement FROM notes.annotations)

UNION
SELECT 'Absence', 'ABS', id_etablissement
FROM notes.etablissements_actifs
WHERE id_etablissement NOT in (SELECT id_etablissement FROM notes.annotations);
-- visibilité de l'élément signifiant

-- Pas nécessaire donc elle est commentée
-- Elle rend la visibilité de certaines compétences (la compétence id 475 ou 491) => elles sont commentées donc pas utile (voir init-data.sql)
-- UPDATE notes.perso_competences SET masque = FALSE WHERE (id_competence = 475 OR id_competence = 491) AND id_etablissement = '<id etablissement>';
commit;

