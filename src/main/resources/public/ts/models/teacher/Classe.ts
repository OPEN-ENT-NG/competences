import { Model, Collection, http } from 'entcore';
import { Eleve, Periode, SuiviCompetenceClasse, Utils } from './index';
import * as utils from '../../utils/teacher';

export class Classe extends Model {
    eleves : Collection<Eleve>;
    id : number;
    name : string;
    type_groupe : number;
    periodes : Collection<Periode>;
    type_groupe_libelle : string;
    suiviCompetenceClasse : Collection<SuiviCompetenceClasse>;
    mapEleves : any;
    remplacement: boolean;
    id_cycle: any;
    selected : boolean;

    get api () {
        return {
            syncClasse: '/viescolaire/classes/' + this.id + '/users?type=Student',
            syncGroupe : '/viescolaire/groupe/enseignement/users/' + this.id + '?type=Student',
            syncClasseChefEtab : '/viescolaire/classes/'+this.id+'/users',
            syncPeriode : '/viescolaire/periodes?idGroupe=' + this.id
        }
    }

    constructor (o? : any) {
        super();
        if (o !== undefined) this.updateData(o);
        this.collection(Eleve, {
            sync : () : Promise<any> => {
                return new Promise((resolve, reject) => {
                    this.mapEleves = {};
                    let url;
                    if(Utils.isChefEtab()){
                        url = this.type_groupe === 1 ? this.api.syncGroupe : this.api.syncClasseChefEtab;
                    }else {
                        url = this.type_groupe === 1 ? this.api.syncGroupe : this.api.syncClasse;
                    }
                    http().getJson(url).done((data) => {
                        // On tri les élèves par leur lastName en ignorant les accents
                        utils.sortByLastnameWithAccentIgnored(data);
                        this.eleves.load(data);
                        for (var i = 0; i < this.eleves.all.length; i++) {
                            this.mapEleves[this.eleves.all[i].id] = this.eleves.all[i];
                        }
                        this.trigger('sync');
                        resolve();
                    });
                });
            }
        });
        this.collection(SuiviCompetenceClasse);
        this.collection(Periode, {
            sync : async (): Promise<any> => {
                return new Promise((resolve, reject) => {
                    http().getJson(this.api.syncPeriode).done((res) => {
                        res.push({id: null});
                        this.periodes.load(res);
                        resolve();
                    });
                });
            }
        });
    }
}