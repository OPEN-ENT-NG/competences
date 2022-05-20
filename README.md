# À propos de l'application Compétences
* Licence : [AGPL v3](http://www.gnu.org/licenses/agpl.txt) - Copyright Département de la Seine-et-Marne, CGI, Région Hauts-de-France (ex Picardie)
* Développeur(s) : CGI
* Financeur(s) : Département de la Seine-et-Marne, CGI, Région Hauts-de-France (ex Picardie)
* Description : Application de gestion des compétences

##Configuration
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