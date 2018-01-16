import { http, Model, _ } from 'entcore';
export class Matiere extends Model {
    id: string;
    name: string;
    externalId: string;
    ens: any = [];
    moyenne: number;

    get api () {
        return {
            calculMoyenne: '/competences/eleve/'
        };
    }

    /**
     * Recupère la moyenne d'un élève en fonction de devoirs
     * donnés en paramètre
     * @param id_eleve id de l'élève
     * @param devoirs Les devoirs pris en compte pour le calcul de moyenne
     * @returns {Promise<any>} Promesse de retour
     */
    getMoyenne (id_eleve, devoirs?) : Promise<any> {
        return new Promise((resolve, reject) => {
            if (devoirs) {
                let idDevoirsURL = "";

                _.each(_.pluck(devoirs,'id'), (id) => {
                    idDevoirsURL += "devoirs=" + id + "&";
                });
                idDevoirsURL = idDevoirsURL.slice(0, idDevoirsURL.length - 1);

                http().getJson(this.api.calculMoyenne + id_eleve + "/moyenne?" + idDevoirsURL).done(function (res) {
                    if (!res.error) {
                        this.moyenne = res.moyenne;
                    } else {
                        this.moyenne = "";
                    }
                    if(resolve && typeof(resolve) === 'function'){
                        resolve();
                    }
                }.bind(this));
            }
        });
    }
}