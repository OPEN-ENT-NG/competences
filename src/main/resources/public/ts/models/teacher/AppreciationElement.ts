import { Model, http } from 'entcore';

export class AppreciationElement extends Model {
    id : number;
    id_eleve : string;
    id_Element : number;
    id_appreciation : number;
    valeur : any;
    appreciation : any;
    oldAppreciation : any;

    get api () {
        return {
            createAppreciation : '/competences/appreciation',
            updateAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation,
            deleteAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation
        };
    }

    constructor (o? : any) {
        super();
        if (o) this.updateData(o, false);
    }

    toJSON () {
        let o = new AppreciationElement();
        if(this.id !== null) o.id = this.id;
        o.id_eleve  = this.id_eleve;
        o.id_Element = parseInt(this.id_Element.toString());
        o.valeur   = parseFloat(this.valeur);
        if (this.appreciation) o.appreciation = this.appreciation;
        return o;
    }

    saveAppreciation () : Promise<AppreciationElement> {
        return new Promise((resolve) => {
            if (!this.id_appreciation) {
                this.createAppreciation().then((data) => {
                    resolve(data);
                });
            } else {
                this.updateAppreciation().then((data) =>  {
                    resolve(data);
                });
            }
        });
    }

    createAppreciation () : Promise<AppreciationElement> {
        return new Promise((resolve) => {
            let _appreciation = {
                id_Element : this.id_Element,
                id_eleve  : this.id_eleve,
                valeur    : this.appreciation
            };
            http().postJson(this.api.createAppreciation, _appreciation).done ( function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            }) ;

        });

    }

    updateAppreciation () : Promise<AppreciationElement> {
        return new Promise((resolve) => {
            let _appreciation = {
                id : this.id_appreciation,
                id_Element : this.id_Element,
                id_eleve  : this.id_eleve,
                valeur    : this.appreciation
            };
            http().putJson(this.api.updateAppreciation, _appreciation).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });

        });
    }

    deleteAppreciation () : Promise<any> {
        return new Promise((resolve) => {
            http().delete(this.api.deleteAppreciation).done(function (data) {
                if(resolve && typeof(resolve) === 'function') {
                    resolve(data);
                }
            });
        });
    }
}
