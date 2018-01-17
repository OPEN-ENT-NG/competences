import { Model, Collection, model, http } from 'entcore';
import { Remplacement, Enseignant } from './index';

export class GestionRemplacement extends Model {
    remplacements : Collection<Remplacement> | any; // liste des remplacements en cours
    selectedRemplacements : Collection<Remplacement> | any; // liste des remplacements sélectionnés
    remplacement : Remplacement; // remplacementen cours d'ajout
    enseignants : Collection<Enseignant>; // liste des enseignants de l'établissment
    sortType : string; // type de tri de la liste des remplaçants
    sortReverse : boolean; // sens de tri de la liste des remplaçants
    showError : boolean; // condition d'affichage d'un message d'erreur
    confirmation : boolean; // condition d'affichage de la popup de confirmation
    selectAll : boolean; // booleen de sélection de tous/aucun remplacement/s

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    get api () {
        return {
            deleteMultiple : '/competences/remplacements/delete', // TODO A coder
            enseignants : '/competences/user/list?structureId='+model.me.structures[0]+'&profile=Teacher',
            remplacements : '/competences/remplacements/list'
        }
    }

    constructor(p? : any) {
        super();
        var that = this;

        this.showError = false;
        this.selectAll = false;
        this.confirmation = false;
        this.remplacement = new Remplacement();


        this.remplacement.date_debut = new Date();

        var today = new Date();
        today.setFullYear(today.getFullYear() + 1);
        this.remplacement.date_fin = today;

        this.collection(Enseignant, {
            sync : function () : Promise<any> {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.enseignants).done(function(res) {
                        this.load(res);
                        if(resolve && (typeof(resolve) === 'function')) {
                            resolve();
                        }
                    }.bind(this));
                });
            }
        });

        this.collection(Remplacement, {
            sync : function () : Promise<any> {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.remplacements).done(function(resRemplacements) {

                        this.removeAll();

                        for(var i=0; i<resRemplacements.length; i++) {
                            var remplacementJson = resRemplacements[i];

                            var remplacement = new Remplacement();
                            remplacement.titulaire = new Enseignant();
                            remplacement.titulaire.id = remplacementJson.id_titulaire;
                            remplacement.titulaire.displayName = remplacementJson.libelle_titulaire;

                            remplacement.remplacant = new Enseignant();
                            remplacement.remplacant.id = remplacementJson.id_remplacant;
                            remplacement.remplacant.displayName = remplacementJson.libelle_remplacant;


                            remplacement.date_debut = remplacementJson.date_debut;
                            remplacement.date_fin = remplacementJson.date_fin;
                            remplacement.id_etablissement = remplacementJson.id_etablissement;

                            this.all.push(remplacement);


                        }

                        if(resolve && (typeof(resolve) === 'function')) {
                            resolve();
                        }
                    }.bind(this));
                });
            }
        });

        this.selectedRemplacements = [];

    }
}