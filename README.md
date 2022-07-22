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
