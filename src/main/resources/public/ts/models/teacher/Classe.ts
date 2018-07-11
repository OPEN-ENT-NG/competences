import {Model, Collection, http, idiom as lang, _} from 'entcore';
import { Eleve, Periode, SuiviCompetenceClasse, Utils, BaremeBrevetEleves } from './index';
import * as utils from '../../utils/teacher';
declare let bundle:any;

export class Classe extends Model {
    eleves : Collection<Eleve>;
    id : string;
    name : string;
    type_groupe : number;
    periodes : Collection<Periode>;
    type_groupe_libelle : string;
    suiviCompetenceClasse : Collection<SuiviCompetenceClasse>;
    mapEleves : any;
    remplacement: boolean;
    id_cycle: any;
    selected : boolean;
    baremeBrevetEleves : BaremeBrevetEleves;

    public static  libelle = {
        CLASSE:'Classe',
        GROUPE: "Groupe d'enseignement",
        GROUPE_MANUEL: "Groupe manuel"
    };

    public static type = {
        CLASSE: 0,
        GROUPE: 1,
        GROUPE_MANUEL: 2
    };

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
        if (o !== undefined) this.updateData(o, false);
        this.collection(Eleve, {
            sync : () : Promise<any> => {
                return new Promise((resolve, reject) => {
                    this.mapEleves = {};
                    let url;
                    if(Utils.isChefEtab()){
                        url = this.type_groupe !== Classe.type.CLASSE ?
                            this.api.syncGroupe : this.api.syncClasseChefEtab;
                    }else {
                        url = this.type_groupe !== Classe.type.CLASSE ? this.api.syncGroupe : this.api.syncClasse;
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
                        res.push({id: null, id_type: null, id_classe: this.id});
                        this.periodes.load(res);
                        resolve();
                    }).error( (res) =>{
                        this.periodes.load([]);
                        resolve();
                    });
                });
            }
        });
    }

    public static get_type_groupe_libelle = (classe) => {
        let libelleClasse;

        if ( classe.type_groupe === Classe.type.CLASSE) {
            libelleClasse = Classe.libelle.CLASSE;
        } else if ( classe.type_groupe === Classe.type.GROUPE) {
            libelleClasse = Classe.libelle.GROUPE;
        }else if ( classe.type_groupe === Classe.type.GROUPE_MANUEL) {
            libelleClasse = Classe.libelle.GROUPE_MANUEL;
        }
        return libelleClasse;

    }

    filterEvaluableEleve (periode) {
        let res = _.omit(this, 'eleves');

        if (periode !== undefined) {
            res.eleves = {
                all: _.reject(this.eleves.all, function (eleve) {
                    return !eleve.isEvaluable(periode);
                })
            };
        }
        else {
            res.eleves = this.eleves;
        }
        return res;
    };

}