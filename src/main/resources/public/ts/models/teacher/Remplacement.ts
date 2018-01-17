import { Model, IModel, http } from 'entcore';
import { Enseignant } from './index';
import * as utils from '../../utils/teacher';

export class Remplacement extends Model implements IModel{

    // DATABASE FIELDS
    titulaire : Enseignant;
    remplacant : Enseignant;
    date_debut : any;
    date_fin : any;
    id_etablissement : string;


    // OTHER FIELDS
    selected : boolean;


    get api () {
        return {
            create : '/competences/remplacement/create',
            update : '/competences/remplacement/update',
            delete : '/competences/remplacement/delete'
        }
    }

    constructor(p? : any) {
        super();
        this.selected = false;

    }

    create () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.create, this.toJSON()).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    update () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.update, this.toJSON()).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }


    remove () : Promise<any> {
        var that = this;
        return new Promise((resolve, reject) => {
            http().delete(this.api.delete, this.toJSON()).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(that);
                }
            })
                .error(function () {
                    reject(that);
                });
        });
    }

    toJSON() {
        return {
            id_titulaire: this.titulaire.id,
            libelle_titulaire : this.titulaire.displayName,
            id_remplacant: this.remplacant.id,
            libelle_remplacant : this.remplacant.displayName,
            date_debut: utils.getFormatedDate(this.date_debut,"YYYY-MM-DD"),
            date_fin: utils.getFormatedDate(this.date_fin,"YYYY-MM-DD"),
            id_etablissement : this.id_etablissement
        }
    }
}