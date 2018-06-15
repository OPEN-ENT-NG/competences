import {Model, Collection, _, http, moment} from 'entcore';
import { Evaluation, SuiviCompetence } from './index';
import {Periode} from "./Periode";

export class Eleve extends Model {
    moyenne: number;
    evaluations : Collection<Evaluation>;
    evaluation : Evaluation;
    id : string;
    firstName: string;
    lastName: string;
    suiviCompetences : Collection<SuiviCompetence>;
    displayName: string;
    idClasse: string;
    idEtablissement :string;
    details : any;
    deleteDate : any;

    get api() {
        return {
            GET_MOYENNE : `/competences/eleve/${this.id}/moyenne?`,
            GET_DATA_FOR_DETAILS_RELEVE: `/competences/releve/informations/eleve/${this.id}`
        }
    }

    constructor (o?: any) {
        super();
        if (o) {
            this.updateData(o, false);
        }
        this.collection(Evaluation);
        this.collection(SuiviCompetence);
    }
    toString () {
        return this.hasOwnProperty("displayName") ? this.displayName : this.firstName+" "+this.lastName;
    }

    getMoyenne (devoirs?) : Promise<any> {
        return new Promise((resolve, reject) => {
            if (devoirs) {
                let idDevoirsURL = "";
                _.each(_.pluck(devoirs,'id'), (id) => {
                    idDevoirsURL += "devoirs=" + id + "&";
                });
                idDevoirsURL = idDevoirsURL.slice(0, idDevoirsURL.length - 1);
                http().getJson(this.api.GET_MOYENNE + idDevoirsURL).done(function (res) {
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

    getDetails (idEtablissement, idClasse, idMatiere) : Promise<any> {
        return new Promise( ((resolve, reject) => {
            let uri = this.api.GET_DATA_FOR_DETAILS_RELEVE
                + `?idEtablissement=${idEtablissement}&idClasse=${idClasse}&idMatiere=${idMatiere}`;
            http().getJson(uri).done( (res) => {
                if (!res.error) {
                    this.details = res;
                    resolve ();
                }
            });
        }))
    }

    isEvaluable(periode) {
        if (this.deleteDate === null) {
            return true;
        }
        else if(periode === undefined) {
            return true;
        }
        else if (periode.id_type === undefined) {
            return true;
        }
        else {
            let deleteDate = moment(this.deleteDate);
            let start = moment(periode.timestamp_dt);
            let end = moment(periode.timestamp_fn);

            return deleteDate.isBetween(start,end) || deleteDate.isAfter(end);
        }
    }
}