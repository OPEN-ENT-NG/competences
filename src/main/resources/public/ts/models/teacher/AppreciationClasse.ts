import http from 'axios';
import {Mix} from "entcore-toolkit";
import {notify} from "entcore";

export class AppreciationClasse  {
    appreciation: string;
    id_classe: string;
    id_periode: number;
    id_matiere: string;
    endSaisie: boolean;
    idEtablissement: string;

    constructor(idClasse:string, idMatiere:string, idPeriode:number, endSaisie:boolean, idEtablissement:string) {
        // super();
        this.id_classe = idClasse;
        this.id_matiere = idMatiere;
        this.id_periode = idPeriode;
        this.endSaisie = endSaisie;
        this.idEtablissement = idEtablissement;
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

    toJSON() {
        return {
            appreciation: this.appreciation,
            id_classe: this.id_classe,
            id_periode: this.id_periode,
            id_matiere: this.id_matiere,
            idEtablissement: this.idEtablissement
        }
    }

    async save () {
        try {
            await http.post(`/competences/appreciation/classe`, this.toJSON());
        } catch (e) {
            notify.error('evaluations.releve.appreciation.classe.save.error');
        }
    }

}