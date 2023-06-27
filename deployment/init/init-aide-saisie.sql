BEGIN;

  -- Initialisation des tables  d'aides à la saisie d'éléments du programme
  ----------------------------------
  ----------------------------------
  -- Domaines du cycle 3         --
  ----------------------------------
  ----------------------------------

TRUNCATE TABLE notes.domaine_enseignement CASCADE;

INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (1,'Éducation physique et sportive',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (2,'Arts plastiques',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (3,'Éducation musicale',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (4,'Français',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (5,'Histoire – Géographie',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (6,'Histoire',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (7,'Géographie',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (8,'Langues vivantes',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (9,'Mathématiques',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (10,'Sciences et technologie',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (11,'Histoire des Arts',2);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (12,'Éducation morale et civique',2);

-- Domaines du cycle 4
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (13,'Éducation physique et sportive',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (14,'Arts plastiques',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (15,'Éducation musicale',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (16,'Français',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (17,'Histoire – Géographie',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (18,'Histoire',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (19,'Géographie',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (20,'Langues vivantes',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (21,'Mathématiques',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (22,'Sciences de la vie et de la Terre',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (23,'Technologie',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (24,'Physique-Chimie',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (25,'Enseignement moral et civique',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (26,'Histoire des Arts',1);
INSERT INTO notes.domaine_enseignement(id,libelle,id_cycle) VALUES (27,'Éducation aux médias et à l’information (EMI)',1);

---------------------------------------
---------------------------------------
-- Sous-Domaines du cycle 3         --
---------------------------------------
---------------------------------------

-- Sous-Domaines : Éducation physique et sportive

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (1, 'Produire une performance optimale mesurable à une échéance donnée',1);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (2, 'Adapter ses déplacements à des environnements variés',1);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (3, 'S’exprimer devant les autres par une prestation artistique et acrobatique',1);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (4, 'Conduire et maitriser un affrontement collectif et interindividuel',1);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (5, 'Produire une performance optimale mesurable à une échéance donnée',1);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (6, 'Adapter ses déplacements à des environnements variés',1);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (7, 'S’exprimer devant les autres par une prestation artistique et acrobatique',1);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (8, 'Conduire et maitriser un affrontement collectif et interindividuel',1);

-- Sous-Domaines : Arts Plastiques

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (9, 'Compétences travaillées',2);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (10, 'Questionnements',2);

-- Sous-Domaines : Education musicale

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (11, 'Compétences travaillées',3);

-- Sous-Domaines : Français

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (12, 'Langage oral',4);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (13, 'Lecture et compréhension de l’écrit',4);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (14, 'Écriture',4);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (15, 'Étude de la langue (grammaire, orthographe, lexique)',4);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (16, 'Culture littéraire et artistique',4);

-- Sous-Domaines : Histoire – Géographie

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (17, 'Se repérer dans le temps : construire des repères historiques',5);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (18, 'Se repérer dans l’espace : construire des repères géographiques',5);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (19, 'Raisonner, justifier une démarche et les choix effectués',5);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (20, 'S’informer dans le monde du numérique',5);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (21, 'Comprendre un document',5);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (22, 'Pratiquer différents langages en histoire et en géographie',5);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (23, 'Coopérer et mutualiser',5);

-- Sous-Domaines : Histoire

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (24, 'La longue histoire de l’humanité et des migrations',6);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (25, 'Récits fondateurs, croyances et citoyenneté dans la Méditerranée antique au 1er millénaire avant J.-C.',6);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (26, 'L’Empire romain dans le monde antique',6);

-- Sous-Domaines : Géographie

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (27, 'Habiter une métropole',7);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (28, 'Habiter un espace de faible densité',7);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (29, 'Habiter les littoraux',7);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (30, 'Le monde habité',7);

-- Sous-Domaines : Langues vivantes

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (31, 'Écouter et comprendre',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (32, 'Lire et comprendre',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (33, 'Parler en continu',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (34, 'Écrire',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (35, 'Réagir et dialoguer',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (36, 'Découvrir des aspects culturels de la langue',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (37, 'La personne et la vie quotidienne',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (38, 'Repères géographiques, historiques et culturels des villes, pays et régions',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (39, 'Imaginaire',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (40, 'Grammaire',8);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (41, 'Phonologie',8);

-- Sous-Domaines : Mathématiques

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (42, 'Compétences travaillées',9);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (43, 'Nombres et calculs',9);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (44, 'Grandeurs et mesures',9);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (45, 'Espace et géométrie',9);

-- Sous-Domaines : Sciences et technologie

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (46, 'Compétences travaillées',10);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (47, 'Matière, mouvement, énergie, information',10);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (48, 'Le vivant, sa diversité et les fonctions qui les caractérisent',10);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (49, 'Matériaux et objets techniques',10);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (50, 'La Planète Terre, les êtres vivants dans leur environnement',10);

-- Sous-Domaines : Histoire des Arts

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (51, 'Compétences travaillées',11);

-- Sous-Domaines : Éducation morale et civique

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (52, 'La sensibilité : soi et les autres',12);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (53, 'Le droit et la règle : des principes pour vivre avec les autres',12);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (54, 'Le jugement : penser par soi-même et avec les autres',12);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (55, 'L’engagement : agir individuellement et collectivement',12);

---------------------------------------
---------------------------------------
-- Sous-Domaines du cycle 4         --
---------------------------------------
---------------------------------------

-- Sous-Domaines : Éducation physique et sportive

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (56, 'Produire une performance optimale mesurable à une échéance donnée',13);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (57, 'Adapter ses déplacements à des environnements variés',13);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (58, 'S’exprimer devant les autres par une prestation artistique et acrobatique',13);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (59, 'Conduire et maitriser un affrontement collectif et interindividuel',13);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (60, 'Produire une performance optimale mesurable à une échéance donnée',13);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (61, 'Adapter ses déplacements à des environnements variés',13);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (62, 'S’exprimer devant les autres par une prestation artistique et acrobatique',13);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (63, 'Conduire et maitriser un affrontement collectif et interindividuel',13);

-- Sous-Domaines : Arts Plastiques

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (64, 'Compétences travaillées',14);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (65, 'Questionnements',14);

-- Sous-Domaines : Education musicale

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (66, 'Compétences travaillées',15);

-- Sous-Domaines : Français

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (67, 'Langage oral',16);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (68, 'Écriture',16);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (69, 'Lecture et compréhension de l’écrit',16);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (70, 'Étude de la langue',16);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (71, 'Culture littéraire et artistique (Cinquième)',16);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (72, 'Culture littéraire et artistique (Quatrième)',16);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (73, 'Culture littéraire et artistique (Troisième)',16);

-- Sous-Domaines : Histoire – Géographie

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (74, 'Se repérer dans le temps : construire des repères historiques',17);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (75, 'Se repérer dans l’espace : construire des repères géographiques',17);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (76, 'Raisonner, justifier une démarche et les choix effectués',17);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (77, 'S’informer dans le monde du numérique',17);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (78, 'Analyser et comprendre un document',17);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (79, 'Pratiquer différents langages en histoire et en géographie',17);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (80, 'Coopérer et mutualiser',17);

-- Sous-Domaines : Histoire

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (81, 'Chrétientés et islam (VIe – XIIIe siècles), des mondes en contact (5ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (82, 'Société, Église et pouvoir politique dans l’occident féodal (XIe – XVe siècles) (5ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (83, 'Transformations de l’Europe et ouverture sur le monde aux XVIe et XVIIe siècles (5ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (84, 'Le XVIIIe siècle. Expansions, Lumières et révolutions (4ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (85, 'L’Europe et le monde au XIXe siècle (4ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (86, 'Société, culture et politique dans la France du XIXe siècle (4ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (87, 'L’Europe, un théâtre majeur des guerres totales (1914-1945) (3ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (88, 'Le monde depuis 1945 (3ème)',18);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (89, 'Françaises et Français dans une République repensée (3ème)',18);

-- Sous-Domaines : Géographie

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (90, 'La question démographique et l’inégal développement (5ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (91, 'Des ressources limitées, à gérer et à renouveler (5ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (92, 'Prévenir les risques, s’adapter au changement global (5ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (93, 'L’urbanisation du monde (4ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (94, 'Les mobilités humaines transnationales (4ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (95, 'Des espaces transformés par la mondialisation (4ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (96, 'Dynamiques territoriales de la France contemporaine (3ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (97, 'Pourquoi et comment aménager le territoire ? (3ème)',19);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (98, 'La France et l’Union européenne (3ème)',19);

-- Sous-Domaines : Langues vivantes

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (99, 'Écouter et comprendre',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (100, 'Lire et comprendre',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (101, 'Parler en continu',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (102, 'Écrire',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (103, 'Réagir et dialoguer',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (104, 'Découvrir des aspects culturels de la langue',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (105, 'Langages',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (106, 'Notions culturelles',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (107, 'Grammaire',20);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (108, 'Phonologie',20);

-- Sous-Domaines : Mathématiques

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (109, 'Algorithmique et programmation',21);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (110, 'Compétences travaillées',21);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (111, 'Nombres et calculs',21);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (112, 'Organisation et gestion de données',21);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (113, 'Grandeurs et mesures',21);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (114, 'Espace et géométrie',21);

-- Sous-Domaines : Sciences de la vie et de la Terre

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (115, 'Compétences travaillées',22);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (116, 'La planète terre, l’environnement et l’action humaine',22);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (117, 'Le vivant et son évolution',22);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (118, 'Le corps humain et la santé',22);

-- Sous-Domaines : Technologie

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (119, 'Pratiquer des démarches scientifiques et technologiques',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (120, 'Concevoir, créer, réaliser',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (121, 'S’approprier les outils et les méthodes',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (122, 'Pratiquer des langages',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (123, 'Mobiliser les outils numériques',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (124, 'Adopter un comportement éthique et responsable',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (125, 'Se situer dans l’espace et le temps',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (126, 'Design, innovation et créativité',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (127, 'Objets techniques, services et changements induits dans la société',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (128, 'Modélisation et simulation des objets et systèmes techniques',23);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (129, 'Informatique et programmation',23);

-- Sous-Domaines : Physique-Chimie

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (130, 'Compétences travaillées',24);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (131, 'Organisation et transformations de la matière',24);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (132, 'Mouvement et interaction',24);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (133, 'L’énergie et ses conversions',24);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (134, 'Signaux pour observer et communiquer',24);

-- Sous-Domaines : Enseignement moral et civique

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (135, 'La sensibilité : soi et les autres',25);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (136, 'Le jugement : penser par soi-même et avec les autres',25);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (137, 'Le droit et la règle : des principes pour vivre avec les autres',25);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (138, 'L’engagement : agir individuellement et collectivement',25);

-- Sous-Domaines : Histoire des Arts

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (139, 'Compétences travaillées',26);
INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (140, 'Thématiques',26);

-- Sous-Domaines : Éducation aux médias et à l’information (EMI)

INSERT INTO notes.sous_domaine_enseignement(id, libelle,id_domaine) VALUES (141, 'Compétences travaillées',27);


---------------------------------------
---------------------------------------
-- Propositions du cycle 3         --
---------------------------------------
---------------------------------------

-- Sous-Domaines : Éducation physique et sportive

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (1, 'Réaliser des efforts et enchainer plusieurs actions motrices', 1);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (2, 'Combiner une course, un saut, un lancer', 1);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (3, 'Mesurer et analyser une performance', 1);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (4, 'Assumer les rôles de chronométreur et d''observateur', 1);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (5, 'Réaliser un parcours dans plusieurs environnements inhabituels', 2);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (6, 'Connaitre et respecter les règles de sécurité', 2);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (7, 'Alerter en cas de problème', 2);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (8, 'Valider l''attestation scolaire du savoir nager (ASSN)', 2);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (9, 'Réaliser en petits groupes une séquence acrobatique ou à visée artistique', 3);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (10, 'Filmer une prestation pour la revoir et la faire évoluer', 3);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (11, 'Respecter les prestations des autres et accepter de se produire devant eux', 3);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (12, 'S’organiser pour gagner', 4);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (13, 'Maintenir un engagement moteur', 4);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (14, 'Respecter les partenaires, les adversaires et l''arbitre', 4);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (15, 'Assurer différents rôles (joueur, arbitre, observateur)', 4);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (16, 'Accepter le résultat de la rencontre', 4);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (460, 'Activités athlétiques Natation', 5);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (17, 'Parcours d’orientation / Parcours d’escalade / Savoir nager / Activités nautiques / Activités de roule (vélo, roller…)', 6);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (18, 'Danse / Activités gymniques / Arts du cirque', 7);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (19, 'Jeux traditionnels / Jeux collectifs avec ballon / Jeux de combat / Jeux de raquettes', 8);

-- Sous-Domaines : Arts plastiques

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (20, 'Expérimenter, produire, créer', 9);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (21, 'Mettre en oeuvre un projet artistique', 9);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (22, 'S’exprimer, analyser sa pratique, celle de ses pairs ; établir une relation avec celle des artistes, s’ouvrir à l’altérité', 9);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (23, 'Se repérer dans les domaines liés aux arts plastiques, être sensible aux questions de l’art.', 9);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (24, 'La représentation plastique et les dispositifs de présentation', 10);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (25, 'Les fabrications et la relation entre l’objet et l’espace', 10);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (26, 'La matérialité de la production plastique et la sensibilité aux constituants de l’oeuvre', 10);

-- Sous-Domaines : Education musicale

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (27, 'Chanter et interpréter', 11);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (28, 'Écouter, comparer et commenter', 11);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (29, 'Explorer, imaginer et créer', 11);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (30, 'Échanger, partager et argumenter', 11);

-- Sous-Domaines : Français

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (31, 'Écouter pour comprendre', 12);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (32, 'Parler en prenant en compte son auditoire', 12);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (33, 'Participer à des échanges', 12);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (34, 'Adopter une attitude critique', 12);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (35, 'Lire avec fluidité', 13);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (36, 'Comprendre un texte et l’interpréter', 13);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (37, 'Être un lecteur autonome', 13);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (38, 'Écrire à la main de manière fluide et efficace', 14);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (39, 'Écrire avec un clavier rapidement et efficacement', 14);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (40, 'Écrire pour réfléchir', 14);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (41, 'Produire des écrits variés', 14);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (42, 'Faire évoluer son texte', 14);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (43, 'Prendre en compte les normes de l’écrit', 14);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (44, 'Maitriser les relations entre l''oral et l''écrit', 15);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (45, 'Acquérir la structure, le sens et l''orthographe des mots', 15);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (46, 'Maitriser la forme des mots', 15);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (47, 'Observer le fonctionnement du verbe et l''orthographier', 15);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (48, 'Analyser la phrase simple', 15);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (49, 'Le monstre, aux limites de l’humain', 16);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (50, 'Récits d’aventures', 16);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (51, 'Récits de création ; création poétique', 16);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (52, 'Résister au plus fort : ruses, mensonges et masques', 16);

-- Sous-Domaines : Histoire – Géographie

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (53, 'Situer des grandes périodes historiques', 17);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (54, 'Ordonner des faits et les situer', 17);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (55, 'Utiliser des documents', 17);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (56, 'Mémoriser et mobiliser ses repères historiques', 17);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (57, 'Nommer et localiser les grands repères géographiques', 18);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (58, 'Appréhender la notion d’échelle géographique', 18);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (59, 'Mémoriser et mobiliser les repères géographiques', 18);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (60, 'Poser et se poser des questions', 19);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (61, 'Formuler des hypothèses', 19);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (62, 'Vérifier', 19);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (63, 'Justifier', 19);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (64, 'Connaitre et utiliser différents systèmes d’information', 20);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (65, 'Trouver, sélectionner et exploiter des informations dans une ressource numérique', 20);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (66, 'Identifier la ressource numérique utilisée', 20);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (67, 'Comprendre le sens général d’un document', 21);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (68, 'Identifier le document', 21);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (69, 'Extraire des informations d’un document', 21);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (70, 'Comprendre le sens général d’un document', 21);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (71, 'Écrire pour penser, argumenter, communiquer et échanger', 22);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (72, 'Reconnaitre un récit historique', 22);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (73, 'S’exprimer à l’oral', 22);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (74, 'S’approprier et utiliser un lexique historique et géographique', 22);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (75, 'Réaliser des productions', 22);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (78, 'Utiliser des cartes', 22);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (79, 'Organiser son travail dans le cadre d’un groupe', 23);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (80, 'Travailler en commun', 23);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (81, 'Utiliser les outils numériques dans le travail collectif', 23);

-- Sous-Domaines : Histoire

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (82, 'Les débuts de l’humanité', 24);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (83, 'La « révolution » néolithique', 24);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (84, 'Premiers États, premières écritures', 24);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (85, 'Le monde des cités grecques', 25);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (86, 'Rome du mythe à l’histoire', 25);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (87, 'La naissance du monothéisme juif dans un monde polythéiste', 25);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (89, 'Conquêtes, paix romaine et romanisation', 26);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (90, 'Des chrétiens dans l’empire', 26);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (91, 'Les relations de l’empire romain avec les autres mondes anciens : l’ancienne route de la soie et la Chine des Han', 26);

-- Sous-Domaines : Géographie

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (92, 'Les métropoles et leurs habitants', 27);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (93, 'La ville de demain', 27);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (94, 'Habiter un espace à forte(s) contrainte(s) naturelle(s) ou / et de grande biodiversité', 28);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (95, 'Habiter un espace de faible densité à vocation agricole', 28);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (96, 'Littoral industrialo-portuaire, littoral touristique', 29);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (97, 'La répartition de la population mondiale et ses dynamiques', 30);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (98, 'La variété des formes d’occupation spatiale dans le monde', 30);

-- Sous-Domaines : Langues vivantes

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (99, 'Des mots familiers et des expressions très courantes', 31);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (100, 'Comprendre une intervention brève, claire et simple', 31);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (101, 'Comprendre dans un message des mots familiers et des phrases très simples', 32);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (102, 'Comprendre des textes courts et simples', 32);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (103, 'Utiliser des expressions et des phrases simples
Se présenter brièvement, parler en termes simples de quelqu’un, d’une activité, d’un lieu', 33);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (104, 'Copier un modèle écrit, écrire un court message et renseigner un questionnaire simple', 34);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (105, 'Ecrire un texte court et articulé simplement', 34);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (106, 'Communiquer, de façon simple, avec l’aide d’un interlocuteur', 35);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (107, 'Communiquer de façon simple', 35);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (108, 'Identifier quelques grands repères culturels', 36);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (109, 'Repérer les indices culturels et mobiliser ses connaissances culturelles', 36);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (110, 'Corps humain, vêtements, modes de vie', 37);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (111, 'Portrait physique et moral', 37);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (112, 'Environnement urbain', 37);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (113, 'Situation géographique', 38);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (114, 'Caractéristiques physiques et repères culturels', 38);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (115, 'Figures historiques et contemporaines', 38);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (116, 'Pages d’histoire spécifiques', 38);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (117, 'Littérature de jeunesse', 39);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (118, 'Contes, mythes et légendes', 39);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (119, 'Héros et héroïnes', 39);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (120, 'Groupe nominal', 40);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (121, 'Groupe verbal', 40);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (122, 'Construction de la phrase', 40);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (123, 'Phonèmes', 41);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (124, 'Accents et rythmes', 41);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (125, 'Intonation', 41);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (126, 'Lien phonie / graphie', 41);

-- Sous-Domaines : Mathématiques

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (127, 'Chercher', 42);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (128, 'Modéliser', 42);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (129, 'Représenter', 42);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (130, 'Raisonner', 42);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (131, 'Calculer', 42);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (132, 'Communiquer', 42);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (133, 'Utiliser et représenter les grands nombres entiers, des fractions simples, les nombres décimaux', 43);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (134, 'Calculer avec des nombres entiers et des nombres décimaux', 43);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (135, 'Résoudre des problèmes en utilisant des fractions simples, les nombres décimaux et le calcul', 43);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (136, 'Comparer, estimer, mesurer des longueurs, des masses, des contenances, des durées', 44);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (137, 'Utiliser le lexique, les unités, les instruments de mesures spécifiques de ces grandeurs', 44);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (138, 'Résoudre des problèmes impliquant des longueurs, des masses, des contenances, des durées, des prix', 44);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (139, '(Se) repérer et (se) déplacer en utilisant des repères et des représentations', 45);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (140, 'Reconnaitre, nommer, décrire, reproduire quelques solides', 45);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (141, 'Reconnaitre, nommer, décrire, reproduire, construire quelques figures géométriques', 45);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (142, 'Reconnaitre et utiliser les notions d’alignement, d’angle droit, d’égalité de longueurs, de milieu, de symétrie', 45);

-- Sous-Domaines : Sciences et technologie

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (143, 'Pratiquer des démarches scientifiques et technologiques', 46);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (144, 'Concevoir, créer, réaliser', 46);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (145, 'S’approprier les outils et les méthodes', 46);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (146, 'Pratiquer des langages', 46);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (147, 'Mobiliser les outils numériques', 46);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (148, 'Adopter un comportement éthique et responsable', 46);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (149, 'Se situer dans l’espace et le temps', 46);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (150, 'États de la matière à l’échelle macroscopique', 47);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (151, 'Les différents types de mouvements', 47);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (152, 'Les différentes sources d’énergie', 47);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (153, 'Signal et information', 47);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (154, 'Les organismes', 48);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (155, 'Besoins en aliments de l’être humain ; transformation et conservation des aliments', 48);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (156, 'Développement et reproduction des êtres vivants', 48);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (157, 'Origine et devenir de la matière organique des êtres vivants', 48);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (158, 'Évolution des besoins et des objets', 49);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (159, 'Fonctionnement, fonctions et constitutions des objets techniques', 49);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (160, 'Familles de matériaux', 49);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (161, 'Concevoir et produire un objet technique', 49);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (162, 'Communication et gestion de l’information', 49);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (163, 'La Terre dans le système solaire et les conditions de vie sur la terre', 50);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (164, 'Les enjeux liés à l’environnement', 50);

-- Sous-Domaines : Histoire des Arts

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (165, 'Donner un avis argumenté sur ce que représente ou exprime une oeuvre d’art', 51);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (166, 'Dégager d’une oeuvre d’art ses principales caractéristiques', 51);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (167, 'Relier des caractéristiques d’une oeuvre d’art à des usages ainsi qu’au contexte de sa création', 51);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (168, 'Se repérer dans un musée, dans un lieu d’art, un site patrimonial', 51);

-- Sous-Domaines : Éducation morale et civique

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (169, 'Partager et réguler des émotions, des sentiments', 52);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (170, 'Respecter autrui et accepter les différences', 52);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (171, 'Manifester le respect des autres dans son langage et son attitude', 52);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (172, 'Comprendre le sens des symboles de la République', 52);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (173, 'Coopérer', 52);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (174, 'Comprendre les notions de droits et devoirs, les accepter et les appliquer', 53);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (175, 'Respecter tous les autres et notamment appliquer les principes de l’égalité des femmes et des hommes', 53);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (176, 'Reconnaitre les principes et les valeurs de la République et de l’Union européenne', 53);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (177, 'Reconnaitre les traits constitutifs de la République française', 53);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (178, 'Prendre part à une discussion, un débat ou un dialogue', 54);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (179, 'Nuancer son point de vue en tenant compte du point de vue des autres', 54);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (180, 'Comprendre la laïcité', 53);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (181, 'Prendre conscience des enjeux civiques de l’usage de l’informatique et de l’Internet', 54);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (182, 'Distinguer son intérêt personnel de l’intérêt collectif', 54);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (183, 'S’engager dans la réalisation d’un projet collectif', 55);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (184, 'Pouvoir expliquer ses choix et ses actes', 55);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (185, 'Savoir participer et prendre sa place dans un groupe', 55);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (186, 'Expliquer en mots simples la fraternité et la solidarité', 55);

---------------------------------------
---------------------------------------
-- Propositions du cycle 4         --
---------------------------------------
---------------------------------------

-- Sous-Domaines : Éducation physique et sportive

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (187, 'Gérer son effort, faire des choix pour réaliser la meilleure performance', 56);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (188, 'S’engager dans un programme de préparation individuel ou collectif', 56);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (189, 'Planifier et réaliser une épreuve combinée', 56);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (190, 'S’échauffer avant un effort', 56);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (191, 'Aider ses camarades et assumer différents rôles sociaux (juge d’appel, chronométreur…)',56 );

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (192, 'Réussir un déplacement planifié dans un milieu plus ou moins connu', 57);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (193, 'Gérer ses ressources pour réaliser un parcours', 57);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (194, 'Assurer la sécurité de son camarade', 57);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (195, 'Mobiliser les capacités expressives du corps', 58);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (196, 'Participer activement, au sein d’un groupe, à un projet artistique', 58);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (197, 'Apprécier des prestations', 58);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (198, 'Réaliser des actions décisives', 59);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (199, 'Adopter son engagement moteur en fonction de son état physique et du rapport de force', 59);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (200, 'Être solidaire de ses partenaires et respectueux de ses adversaires et de l’arbitre', 59);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (201, 'Observer et co-arbitrer', 59);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (202, 'Accepter le résultat de la rencontre et savoir l’analyser', 59);

---------------------------------------------------------------------------------------------------------------------------------------------------
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (203, 'Demi-fond / Courses de haies / Hauteur / Lancers / Sauts / Relais vitesse / Natation longue / Natation de vitesse', 60);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (204, 'Canoë-kayak, ski, VTT / Escalade Randonnée', 61);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (205, 'Aérobic / Acro sport et gymnastique / Arts du cirque / Danse', 62);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (206, 'Basket-ball / Football / Handball / Volley-ball / Rugby / Badminton / Tennis de table / Boxe française / Lutte / Judo / Jeux traditionnels', 63);


-- Sous-Domaines : Arts plastiques

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (207, 'Expérimenter, produire, créer', 64);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (208, 'Mettre en oeuvre un projet artistique', 64);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (209, 'S’exprimer, analyser sa pratique, celle de ses pairs', 64);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (210, 'Se repérer dans les domaines liés aux arts plastiques, être sensible aux questions de l’art', 64);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (211, 'La représentation ; images, réalité et fiction', 65);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (212, 'La matérialité de l’oeuvre ; l’objet et l’oeuvre', 65);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (213, 'L’oeuvre, l’espace, l’auteur, le spectateur', 65);

-- Sous-Domaines : Éducation musicale

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (214, 'Réaliser des projets musicaux d’interprétation ou de création', 66);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (215, 'Écouter, comparer et construire une culture musicale commune', 66);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (216, 'Explorer, imaginer, créer et produire', 66);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (217, 'Échanger, partager, argumenter et débattre', 66);

-- Sous-Domaines : Français

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (218, 'Comprendre des messages oraux', 67);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (219, 'S’exprimer de façon maitrisée', 67);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (220, 'Participer à des échanges', 67);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (221, 'Exploiter les ressources de la parole', 67);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (222, 'Communiquer par écrit ses sentiments et ses opinions', 68);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (223, 'Adopter les procédés d’écriture qui répondent à la consigne et à l’objectif', 68);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (224, 'Écrire pour réfléchir', 68);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (225, 'Exploiter des lectures pour enrichir son écrit', 68);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (226, 'S’initier à l’argumentation', 68);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (227, 'Lire des images, des documents et des textes non littéraires', 69);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (228, 'Lire des textes littéraires et fréquenter des oeuvres d’art', 69);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (229, 'Analyser une oeuvre et repérer ses effets esthétiques', 69);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (230, 'Connaitre les différences entre l’oral et l’écrit', 70);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (231, 'Maitriser la phrase simple', 70);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (232, 'Analyser la phrase complexe', 70);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (233, 'Connaitre le rôle de la ponctuation', 70);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (234, 'Maitriser les accords dans la phrase', 70);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (235, 'Maitriser le fonctionnement du verbe', 70);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (236, 'Maitriser l’usage du vocabulaire', 70);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (237, 'Connaitre des notions d’analyse littéraire', 70);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (238, 'Le voyage et l’aventure (récits d’aventures, de voyages …)', 71);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (239, 'Avec autrui : familles, amis, réseaux (comédies, récits d’enfance et d’adolescence …)', 71);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (240, 'Imaginer des univers nouveaux (contes merveilleux, romans d’anticipation …)', 71);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (241, 'Héros / héroïnes et héroïsmes (épopées, romans de chevalerie …)', 71);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (242, 'L’être humain est-il maître de la nature ? (descriptions, récits d’anticipation…)', 71);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (243, 'Dire l’amour (poèmes lyriques, tragédies…)', 72);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (244, 'Individu et société : confrontations de valeurs ? (tragédies, tragicomédies, romans, nouvelles…)', 72);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (245, 'La fiction pour interroger le réel (romans, nouvelles réalistes ou naturalistes…)', 72);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (246, 'Informer, s’informer, déformer ? (articles de presse…)', 72);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (247, 'La ville, lieu de tous les possibles ? (descriptions issues des romans du XIXe siècle, poèmes…)', 72);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (248, 'Se raconter, se représenter', 73);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (249, 'Dénoncer les travers de la société', 73);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (250, 'Visions poétiques du monde',73 );
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (251, 'Agir dans la cité : individu et pouvoir', 73);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (252, 'Progrès et rêves scientifiques', 73);

-- Sous-Domaines : Histoire-Géographie

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (253, 'Situer un fait', 74);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (254, 'Ordonner des faits les uns par rapport aux autres', 74);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (255, 'Mettre en relation des faits', 74);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (256, 'Identifier des continuités et des ruptures chronologiques', 74);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (257, 'Nommer et localiser les grands repères géographiques', 75);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (258, 'Nommer, localiser et caractériser un lieu', 75);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (259, 'Situer des lieux et des espaces', 75);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (260, 'Nommer, localiser et caractériser des espaces', 75);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (261, 'Utiliser des représentations analogiques et numériques des espaces', 75);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (262, 'Poser et se poser des questions à propos de situations historiques ou géographiques', 76);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (263, 'Construire des hypothèses d’interprétation de phénomènes historiques ou géographiques', 76);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (264, 'Vérifier', 76);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (265, 'Justifier', 76);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (266, 'Connaitre et utiliser différents systèmes d’information', 77);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (267, 'Trouver, sélectionner et exploiter des informations dans une ressource numérique', 77);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (268, 'Utiliser des ressources numériques', 77);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (269, 'Vérifier l’origine, la source des informations et leur pertinence', 77);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (270, 'Exercer son esprit critique sur les données numériques', 77);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (271, 'Comprendre le sens général d’un document', 78);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (272, 'Identifier le document et son point de vue particulier', 78);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (273, 'Extraire des informations pertinentes', 78);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (274, 'Confronter un document à ce qu’on peut connaitre par ailleurs du sujet étudié', 78);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (275, 'Utiliser ses connaissances pour expliciter, expliquer le document et exercer son esprit critique', 78);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (276, 'Écrire pour structurer, argumenter et communiquer', 79);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (277, 'Réaliser des productions', 79);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (278, 'S’approprier et utiliser un lexique historique et géographique', 79);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (279, 'S’initier aux techniques d’argumentation', 79);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (280, 'Organiser son travail dans le cadre d’un groupe', 80);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (281, 'Adapter son rythme de travail à celui du groupe', 80);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (282, 'Défendre ses choix', 80);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (283, 'Négocier une solution commune', 80);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (284, 'Utiliser les outils numériques dans le travail collectif', 80);

-- Sous-Domaines : Histoire

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (285, 'Byzance et l’Europe carolingienne', 81);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (286, 'De la naissance de l’islam à la prise de Bagdad par les Mongols', 81);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (287, 'L’ordre seigneurial', 82);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (288, 'L’émergence d’une nouvelle société urbaine', 82);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (289, 'L’affirmation de l’État monarchique dans le Royaume des Capétiens et des Valois', 82);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (290, 'Le monde au temps de Charles Quint et Soliman le Magnifique', 83);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (291, 'Humanisme, réformes et conflits religieux', 83);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (292, 'Du Prince de la Renaissance au roi absolu (François Ier, Henri IV, Louis XIV)', 83);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (293, 'Bourgeoisies marchandes, négoces internationaux, traites négrières et esclavage au XVIIIe siècle', 84);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (294, 'L’Europe des Lumières', 84);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (295, 'La Révolution française et l’Empire : nouvel ordre politique et société révolutionnée en France et en Europe', 84);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (296, 'L’Europe de la « révolution industrielle »', 85);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (297, 'Conquêtes et sociétés coloniales', 85);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (298, 'Une difficile conquête : voter de 1815 à 1870', 86);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (299, 'La Troisième République', 86);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (301, 'Conditions féminines dans une société en mutation', 86);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (302, 'Civils et militaires dans la Première Guerre mondiale', 87);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (303, 'Démocraties fragilisées et expériences totalitaires dans l’Europe de l’entre-deux-guerres', 87);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (304, 'La Deuxième Guerre mondiale, une guerre d’anéantissement', 87);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (305, 'La France défaite et occupée. Régime de Vichy, collaboration, Résistance', 87);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (306, 'Indépendances et construction de nouveaux États', 88);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (307, 'Un monde bipolaire au temps de la guerre froide', 88);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (308, 'Affirmation et mise en oeuvre du projet européen', 88);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (309, 'Enjeux et conflits dans le monde après 1989', 88);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (310, '1944-1947 : refonder la République, redéfinir la démocratie', 89);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (311, 'La Ve République, de la République gaullienne à l’alternance et à la cohabitation', 89);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (312, 'Femmes et hommes dans la société des années 1950 aux années 1980', 89);

-- Sous-Domaines : Géographie

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (313, 'La croissance démographique et ses effets', 90);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (314, 'Répartition de la richesse et de la pauvreté dans le monde', 90);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (315, 'L’énergie, l’eau : des ressources à ménager et à mieux utiliser', 91);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (316, 'L’alimentation dans le monde', 91);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (317, 'Le changement global et ses principaux effets géographiques régionaux', 92);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (318, 'Prévenir les risques industriels et technologiques', 92);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (319, 'Espace et paysages de l’urbanisation', 93);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (320, 'Des villes inégalement connectées aux réseaux de la mondialisation', 93);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (321, 'Un monde de migrants', 94);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (322, 'Le tourisme et ses espaces', 94);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (323, 'Mers et Océans : un monde maritimisé', 95);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (324, 'L’adaptation du territoire des États-Unis aux nouvelles conditions de la mondialisation', 95);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (325, 'Les dynamiques d’un grand ensemble géographique africain', 95);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (326, 'Les aires urbaines, une nouvelle géographie d’une France mondialisée', 96);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (327, 'Les espaces productifs et leurs évolutions', 96);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (328, 'Les espaces de faible densité et leurs atouts', 96);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (329, 'Aménager pour répondre aux inégalités croissantes entre territoires français', 97);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (330, 'Les territoires ultra-marins français', 97);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (331, 'L’Union européenne, un nouveau territoire de référence et d’appartenance', 98);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (332, 'La France et l’Europe dans le monde', 98);

-- Sous-Domaines : Langues vivantes

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (333, '[niveau A1] Comprendre des mots familiers et des expressions très courantes', 99);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (334, '[niveau A2] Comprendre une intervention brève', 99);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (335, '[niveau B1] Comprendre les points essentiels d’un message', 99);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (336, '[niveau A1] Comprendre dans un message des mots familiers et des phrases très simples', 100);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (337, '[niveau A2] Comprendre des textes courts et simples', 100);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (338, '[niveau B1] Comprendre des textes rédigés dans une langue courante et renvoyant à un sujet connu', 100);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (339, '[niveau A1] Utiliser des expressions et des phrases simples pour parler de soi', 101);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (340, '[niveau A2] Se présenter brièvement, parler en termes simples de quelqu’un, d’une activité, d’un lieu', 101);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (341, '[niveau B1] Prendre la parole sur des sujets connus', 101);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (342, '[niveau A1] Copier un modèle écrit, écrire un court message et renseigner un questionnaire simple', 102);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (343, '[niveau A2] Ecrire un texte court et articulé simplement', 102);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (344, '[niveau B1] Rédiger un texte court et construit sur un sujet connu', 102);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (345, '[niveau A1] Communiquer, de façon simple, avec l’aide de l’interlocuteur', 103);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (346, '[niveau A2] Communiquer de façon simple', 103);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (347, '[niveau B1] Prendre part spontanément à une conversation sur un sujet connu', 103);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (348, '[niveau A1] Identifier quelques grands repères culturels', 104);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (349, '[niveau A2] Repérer les indices culturels et mobiliser ses connaissances culturelles', 104);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (350, '[niveau B1] Repérer et comprendre les spécificités des pays et des régions concernés', 104);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (351, 'Codes socio-culturels', 105);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (352, 'Médias, modes de communication, réseaux sociaux, publicité', 105);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (353, 'Langages artistiques', 105);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (354, 'École et société', 106);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (355, 'Voyages et migrations', 106);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (356, 'Rencontres avec d’autres cultures', 106);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (357, 'Groupe nominal', 107);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (358, 'Groupe verbal', 107);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (359, 'Expression du temps', 107);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (360, 'Énoncés simples et complexes', 107);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (361, 'Construction de la phrase', 107);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (362, 'Régularités de la langue orale', 108);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (363, 'Variations dans les usages de la langue', 108);

-- Sous-Domaines : Mathématiques

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (364, 'Écrire, mettre au point un programme simple', 109);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (365, 'Chercher', 110);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (366, 'Modéliser', 110);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (367, 'Représenter', 110);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (368, 'Raisonner', 110);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (369, 'Calculer', 110);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (370, 'Communiquer', 110);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (371, 'Comparer, calculer, résoudre les problèmes', 111);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (372, 'Notions de divisibilité et de nombres premiers', 111);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (373, 'Calcul littéral', 111);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (374, 'Interpréter, représenter et traiter des données', 112);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (375, 'Comprendre et utiliser des notions élémentaires de probabilités', 112);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (376, 'Résoudre des problèmes de proportionnalité', 112);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (377, 'Calculer avec des grandeurs mesurables ; exprimer les résultats dans les unités adaptées', 113);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (378, 'Comprendre l’effet de quelques transformations sur des grandeurs géométriques', 113);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (380, 'Représenter l’espace', 114);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (381, 'Utiliser les notions de géométrie plane pour démontrer', 114);

-- Sous-Domaines : Sciences de la vie et de la Terre

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (382, 'Pratiquer des démarches scientifiques et technologiques', 115);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (383, 'Concevoir, créer, réaliser', 115);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (384, 'S’approprier les outils et les méthodes', 115);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (385, 'Pratiquer des langages', 115);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (386, 'Mobiliser les outils numériques', 115);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (387, 'Adopter un comportement éthique et responsable', 115);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (388, 'Se situer dans l’espace et le temps', 115);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (389, 'Phénomènes géologiques', 116);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (390, 'Météorologie et climatologie', 116);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (391, 'Impacts de l’action humaine', 116);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (392, 'Comportements responsables', 116);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (393, 'Organisation du monde vivant', 117);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (394, 'Mettre en relation des faits et établir des relations de causalité', 117);

---------------------------------------------------------------------------------------------------------------------------------------------------
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (395, 'Processus biologiques et organisme humain', 118);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (396, 'Comportements responsables en matière de santé', 118);

-- Sous-Domaines : Technologie

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (397, 'Pratiquer des démarches scientifiques et technologiques', 119);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (398, 'Concevoir, créer, réaliser', 120);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (399, 'S’approprier les outils et les méthodes', 121);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (400, 'Pratiquer des langages', 122);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (401, 'Mobiliser les outils numériques', 123);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (402, 'Adopter un comportement éthique et responsable', 124);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (403, 'Se situer dans l’espace et le temps', 125);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (404, 'Imaginer des solutions, matérialiser des idées', 126);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (405, 'Réaliser un prototype', 126);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (406, 'Évolution des objets et systèmes', 127);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (407, 'Outils de description', 127);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (408, 'Objets communicants et bonnes pratiques', 127);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (409, 'Fonctionnement et structure d’un objet', 128);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (410, 'Utiliser une modélisation, simulation des objets', 128);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (411, 'Fonctionnement d’un réseau informatique', 129);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (412, 'Écrire, mettre au point et exécuter un programme', 129);

-- Sous-Domaines : Physique-Chimie

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (413, 'Pratiquer des démarches scientifiques et technologiques', 130);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (414, 'Concevoir, créer, réaliser', 130);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (415, 'S’approprier les outils et les méthodes', 130);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (416, 'Pratiquer des langages', 130);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (417, 'Mobiliser les outils numériques', 130);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (418, 'Se situer dans l’espace et le temps', 130);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (419, 'États de la matière', 131);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (420, 'Transformations chimiques', 131);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (421, 'La matière dans l’univers', 131);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (422, 'Caractériser un mouvement', 132);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (423, 'Modéliser une interaction', 132);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (424, 'Sources, transferts conversions et formes d’énergie', 133);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (425, 'Conservation de l’énergie', 133);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (426, 'Circuits électriques simples et lois de l’électricité', 133);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (427, 'Les différents types de signaux', 134);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (428, 'Propriétés des signaux', 134);


-- Sous-Domaines : Enseignement moral et civique

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (429, 'Exprimer des sentiments moraux', 135);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (430, 'Comprendre que l’aspiration personnelle à la liberté suppose de reconnaître celle d’autrui', 135);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (431, 'Comprendre la diversité des sentiments d’appartenance civiques, sociaux, culturels, religieux', 135);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (432, 'Connaitre les principes, valeurs et symboles de la citoyenneté française et de la citoyenneté européenne', 135);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (433, 'Expliquer les différentes dimensions de l’égalité', 136);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (434, 'Comprendre les enjeux de la laïcité', 136);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (435, 'Reconnaître les grandes caractéristiques d’un État démocratique', 136);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (436, 'Expliquer les grands principes de la justice et leur lien avec le règlement intérieur et la vie de l’établissement', 137);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (437, 'Identifier les grandes étapes du parcours d’une loi dans la République française', 137);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (438, 'Définir les principaux éléments des grandes déclarations des Droits de l’homme', 137);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (439, 'Expliquer le lien entre l’engagement et la responsabilité', 138);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (440, 'Expliquer le sens et l’importance de l’engagement individuel ou collectif', 138);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (441, 'Connaitre les principaux droits sociaux', 138);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (442, 'Comprendre la relation entre l’engagement des citoyens et l’engagement des élèves', 138);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (443, 'Connaitre les grands principes qui régissent la défense nationale', 138);

-- Sous-Domaines : Histoire des arts

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (444, 'Décrire une oeuvre d’art par ses dimensions matérielles, formelles, de sens et d’usage, en employant un lexique adapté', 139);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (445, 'Associer une oeuvre à une époque et une civilisation à partir des éléments observés', 139);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (446, 'Proposer une analyse critique simple et une interprétation d’une oeuvre', 139);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (447, 'Construire un exposé de quelques minutes sur un petit ensemble d’oeuvres ou une problématique artistique', 139);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (448, 'Rendre compte de la visite d’un lieu de conservation ou de diffusion artistique ou de la rencontre avec un métier du patrimoine', 139);

---------------------------------------------------------------------------------------------------------------------------------------------------

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (449, 'Arts et société à l’époque antique et au haut Moyen-âge', 140);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (450, 'Formes et circulations artistiques', 140);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (451, 'Le sacre de l’artiste', 140);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (452, 'État, société, et modes de vie (XIIIe-XVIIIe).', 140);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (453, 'L’art au temps des Lumières et des révolutions', 140);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (454, 'De la Belle Epoque aux « années folles » : l’ère des avant-gardes (1870-1930)', 140);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (455, 'Les arts entre liberté et propagande (1910-1945)', 140);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (456, 'Les arts à l’ère de la consommation de masse (de 1945 à nos jours)', 140);

-- Sous-Domaines : Éducation aux médias et à l’information (EMI)

INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (457, 'Utiliser les médias et les informations de manière autonome', 141);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (458, 'Exploiter l’information de manière raisonnée', 141);
INSERT INTO notes.proposition(id, libelle, id_sous_domaine)	VALUES (459, 'Utiliser les médias de manière responsable', 141);

---------------------------------------------------------------------------------------------------------------------------------------------------



END;