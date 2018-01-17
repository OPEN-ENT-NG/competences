import { Model, http } from 'entcore';

export class BilanFinDeCycle extends Model {
    id : number;
    id_eleve : string;
    id_domaine : number;
    id_etablissement : string;
    owner : string;
    valeur : number;

    constructor(p? : any) {
        super();
        if(p !== undefined){
            this.id = p.id;
            this.id_eleve = p.id_eleve;
            this.id_domaine = p.id_domaine;
            this.id_etablissement = p.id_etablissement;
            this.owner = p.owner;
            this.valeur = p.valeur;
        }
    }

    get api () {
        return {
            createBFC : '/competences/bfc',
            updateBFC : '/competences/bfc?id=' + this.id,
            deleteBFC : '/competences/bfc?id=' + this.id
        }
    }

    saveBilanFinDeCycle () : Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            if (!this.id) {
                this.createBilanFinDeCycle().then((data) => {
                    resolve(data);
                });
            } else {
                this.updateBilanFinDeCycle().then((data) =>  {
                    resolve(data);
                });
            }
        });
    }

    createBilanFinDeCycle () : Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            var _bilanFinDeCycle = {
                id_eleve : this.id_eleve,
                id_domaine : this.id_domaine,
                id_etablissement : this.id_etablissement,
                owner : this.owner,
                valeur : this.valeur
            };
            http().postJson(this.api.createBFC, _bilanFinDeCycle).done ( function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            }) ;
        });
    }

    updateBilanFinDeCycle () : Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            var _bilanFinDeCycle = {
                id : this.id,
                id_eleve : this.id_eleve,
                id_domaine : this.id_domaine,
                id_etablissement : this.id_etablissement,
                owner : this.owner,
                valeur : this.valeur
            };
            http().putJson(this.api.updateBFC, _bilanFinDeCycle).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    deleteBilanFinDeCycle () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.deleteBFC).done(function (data) {
                if(resolve && typeof(resolve) === 'function'){
                    resolve(data);
                }
            });
        });
    }
}