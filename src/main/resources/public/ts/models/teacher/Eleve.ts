import { Model, Collection, _, http } from 'entcore';
import { Evaluation, SuiviCompetence } from './index';

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

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    get api() {
        return {
            getMoyenne : '/competences/eleve/' + this.id + '/moyenne?'
        }
    }

    constructor (o?: any) {
        super();
        if (o) {
            this.updateData(o);
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
                http().getJson(this.api.getMoyenne + idDevoirsURL).done(function (res) {
                    if (!res.error) {
                        this.moyenne = res.moyenne;
                    } else {
                        this.moyenne = "";
                    }
                }.bind(this));
                if(resolve && typeof(resolve) === 'function'){
                    resolve();
                }
            }
        });
    }
}