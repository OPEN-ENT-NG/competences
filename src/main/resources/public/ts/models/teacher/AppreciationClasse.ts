import http from 'axios';
import {Mix} from "entcore-toolkit";
import {notify} from "entcore";

export class AppreciationClasse  {
    appreciation: string;
    id_classe: string;
    id_periode: number;
    id_matiere: string;
    endSaisie: boolean;


    // get api() {
    //     return {
    //         createOrUpdate : '/competences/appreciation/classe',
    //         get : '/competences/appreciation/classe?id_matiere=' + this.id_matiere+"&id_classe="+this.id_classe+"&id_periode="+this.id_periode
    //     }
    // }

    // constructor(o? : any) {
    //     super();
    //     if (o !== undefined) this.updateData(o, false);
    // }

    constructor(idClasse:string, idMatiere:string, idPeriode:number, endSaisie:boolean) {
        // super();
        this.id_classe = idClasse;
        this.id_matiere = idMatiere;
        this.id_periode = idPeriode;
        this.endSaisie = endSaisie;
    }


    async sync() {
        try {
            let {data} = await http.get(`/competences/appreciation/classe?id_matiere=${this.id_matiere}&id_classe=${this.id_classe}&id_periode=${this.id_periode}`);
            // Mix.extend(this, Mix.castAs(AppreciationClasse, data) );
            if(data.appreciation !== undefined) {
                this.appreciation = data.appreciation;
            }
        } catch (e) {
            notify.error('evaluations.releve.appreciation.classe.get.error');
        }
    }

    //  sync () : Promise<any> {
    //     return new Promise((resolve, reject) => {
    //         http().getJson(this.api.get).done(function(res) {
    //             if(res.length > 0) {
    //                 this.appreciation = res[0].appreciation;
    //             }
    //             if(resolve && (typeof(resolve) === 'function')) {
    //                 resolve();
    //             }
    //         }.bind(this));
    //     });
    // }


    toJSON() {
        return {
            appreciation: this.appreciation,
            id_classe: this.id_classe,
            id_periode: this.id_periode,
            id_matiere: this.id_matiere
        }
    }
    // save(): Promise<any> {
    //     return new Promise((resolve, reject) => {
    //         http().postJson(this.api.createOrUpdate, this.toJSON()).done(function (data) {
    //             if (resolve && (typeof (resolve) === 'function')) {
    //                 resolve();
    //             }
    //         });
    //     });
    // }

    async save () {
        try {
            await http.post(`/competences/appreciation/classe`, this.toJSON());
        } catch (e) {
            notify.error('evaluations.releve.appreciation.classe.save.error');
        }
    }

}