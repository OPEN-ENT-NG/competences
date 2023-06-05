# À propos de l'application Compétences
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Département de la Seine-et-Marne, CGI, Région Hauts-de-France (ex Picardie)
* Développeur(s) : CGI
* Financeur(s) : Département de la Seine-et-Marne, CGI, Région Hauts-de-France (ex Picardie)
* Description : Application de gestion des compétences

## Présentation du module
Le module Evaluations permet la création d'évaluations en choisissant son mode d'évaluation par compétences ou par notes. Il permet également le suivi des résultats par les parents, la génération et la remontée au ministère des livrets scolaires et le suivi de la progression de l'élève tout au long des cycles. Des affichages graphiques permettent de facilité ce suivi pendant les conseils de classe.

## Configuration
<pre>
    {
      "config": {
        ...
        "node-pdf-generator" : {
           "authorization" : "${pdfGeneratorAuth}",
           "url": "${pdfGeneratorUrl}"
        }
      }
    }
</pre>
Dans votre springboard, vous devez inclure des variables d'environnement :
<pre>
pdfGeneratorAuth=${String}
pdfGeneratorUrl=${String}
</pre>

Il est nécessaire de mettre ***competences:true*** dans services du module vie scolaire afin de paramétrer les données de configuration de compétence.
<pre>
"services": {
     "competences": true,
     ...
 }
</pre>

## Archivage / Transition

### Prérequis

Dans le module viescolaire, il vous faudra prendre en compte la configuration `update-classes` la  `enable-date` 
<pre>
"update-classes" : {
   "timeout-transaction" : 1000,
   "enable-date" : "2022-09-26" // exemple
},
</pre>

Cette propriété `enable-date` sert à conditionner l'insertion et la persistance des **utilisateurs supprimés** si ce dernier est avant la date actuelle.
(Si nous sommes le 27 septembre 2022 et que l'event `userClassesUpdated` se déclenche avec les users supprimés, l'insertion se fera)

Vous devez connaitre le nombre d'établissement qui seront archivés avant la transition.

Cette requête vous permet de déterminer le nombre d'établissement qui devra être archivé :
```postgresql
SELECT COUNT(DISTINCT id_etablissement) FROM notes.devoirs WHERE eval_lib_historise = false 
-- (e.g 50)
```

### Avant l'appel de la transition

Avant de déclencher la transition, il faut lancer l'API `POST /competences/transition/before?year={{year_backup}}`  (avec un compte ADMC)
####
Exemple `year_backup` peut être "2020_2021" pour le nommage du schema backup.
####
Cette API va procéder : 
* Clonage des schémas notes et viesco
* Suppression de quelques tables (`viesco.rel_structures_personne_supp`, `viesco.rel_groupes_personne_supp`, `viesco.personnes_supp`, `notes.transition`, `notes.match_class_id_transition`)
* Mis à jour de table `match_class_id_transition` avec les infos sur la `table rel_groupe_cycle` et les informations sur les groupes stockées dans l'annuaire (Neo4j)

### Après l'appel de la transition
En config:

<pre>
...
"transition" : {
    "timeout-transaction" : 1000,
    "sql-version": "v2"
},
...
</pre>
- sql-version: 
  - v1: lors de la duplication des schema -> function_clone_schema_with_sequences (renommé de clone_schema_with_sequences) -> 014-clone_schema_with_sequences.sql
  - v2: lors de la duplication des schema -> function_clone_schema_with_sequences_v2

Cette requête vous permet de savoir quel établissement a été archivé :
```postgresql
SELECT * FROM notes.transition ;
```
|  id_etablissement | date |
| --- | --- |
| structure identifier | date.now() (e.g 2022-08-05 19:43:35.597778) |
| ... | ... |
###### _(e.g 50 lines)_

En se basant sur la requête du prérequis, si vous avez bien **50** éléments dans votre table transition c'est que la transition 
s'est bien passé et on pourra donc passer à la suite pour faire l'**alimentation**.
####
Dans le cas contraire, il faudra faire la restauration et recommencer toute la procédure.


### Après l'alimentation

Après l'alimentation, il faut lancer l'API `POST /competences/transition/after`  (avec un compte ADMC)

Cette API va procéder :
* Purge de nombreuses tables
* Mise à jour des identifiants des classes (pour la table `rel_groupe_cycle`) afin que cela puisse correspondre avec les nouveaux identifiants des classes dans l'annuaire (Neo4j)
* Suppression des sous-matières qui étaient reliées à des matières non manuelles
