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

### Avant la transition

Avant de déclencher la transition, il faut lancer l'API `POST /competences/transition/before?year={{year_backup}}`  (avec un compte ADMC)
####
Exemple `year_backup` peut être "2020_2021" pour le nommage du schema backup.
####
Cette API va procéder : 
* Clonage des schémas notes et viesco
* Suppression de quelques tables (`viesco.rel_structures_personne_supp`, `viesco.rel_groupes_personne_supp`, `viesco.personnes_supp`, `notes.transition`, `notes.match_class_id_transition`)
* Mis à jour de table `match_class_id_transition` avec les infos sur la `table rel_groupe_cycle` et les informations sur les groupes stockées dans l'annuaire (Neo4j)


### Après la transition
Après la transition, il faut lancer l'API `POST /competences/transition/after`  (avec un compte ADMC)

Cette API va procéder :
* Purge de nombreuses tables
* Mise à jour des identifiants des classes (pour la table `rel_groupe_cycle`) afin que cela puisse correspondre avec les nouveaux identifiants des classes dans l'annuaire (Neo4j)
* Suppression des sous-matières qui étaient reliées à des matières non manuelles