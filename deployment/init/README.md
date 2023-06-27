# Init module competences

Initialiser les données de compétences pour un établissement

#### 1. Types

Type pour les évaluations libres

```sql
SELECT nextval('notes.type_id_seq')
```

```sql
INSERT INTO notes.type (id, nom, id_etablissement, default_type) VALUES (1, 'Evaluation libre', '', true);
```

#### 2. Matières + Sous matières + Types sous matières  + Référentiel de compétence + Cycles + Niveaux de compétences

Lancer le script `init-data.sql`

#### 3. Matières + Sous matières + Types sous matières  + Référentiel de compétence + Cycles + Niveaux de compétences

Lancer le script `dispense-domaine.sql`

#### 4. Matières + Sous matières + Types sous matières  + Référentiel de compétence + Cycles + Niveaux de compétences

Lancer le script `init-aide-saisie.sql`

#### 5. Scripts migration viescolaire

Executer dans l'ordre dans https://github.com/OPEN-ENT-NG/vie-scolaire/tree/1.36.0/deployment/viescolaire/migration

L'ordre à executer : 

* 0.8.0 = https://github.com/OPEN-ENT-NG/vie-scolaire/tree/1.36.0/deployment/viescolaire/migration/0.8.0
* 0.10.0 = https://github.com/OPEN-ENT-NG/vie-scolaire/tree/1.36.0/deployment/viescolaire/migration/0.10.0
* 1.1.0 = https://github.com/OPEN-ENT-NG/vie-scolaire/tree/1.36.0/deployment/viescolaire/migration/1.1.0
* 1.4.0 = https://github.com/OPEN-ENT-NG/vie-scolaire/tree/1.36.0/deployment/viescolaire/migration/1.4.0

Note : 0.5.2, 1.23.4 et 1.32.12 ne sont pas nécessaires et 0.11.0 a déjà été executé par l'étape 3/4

#### 6 - script init établissement 

Lancer le script `init-structure.sql`

Peut être lancé pour chaque nouvel établissement








