import {Model, notify} from 'entcore';
import http from 'axios';

export class AppreciationCPE extends Model {
    id_eleve : string;
    id_periode : number;
    appreciation : string;

    get api () {
        return {
            DATA_APPRECIATION : '/competences/appreciation/CPE/bilan/periodique',
        };
    }

    constructor (idEleve:string, idPeriode:number) {
        super();
        this.id_eleve = idEleve;
        this.id_periode = idPeriode;
    }

    async syncAppreciationCPE () {
        try {
            let {data} = await http.get(`/competences/appreciation/CPE/bilan/periodique?id_eleve=${this.id_eleve}&id_periode=${this.id_periode}`);
            if(data.appreciation !== undefined) {
                this.appreciation = data.appreciation;
            }
        } catch (e) {
            notify.error('evaluations.appreciations.get.error');
        }
    }

    toJSON() {
        return {
            appreciation: this.appreciation,
            id_eleve: this.id_eleve,
            id_periode: this.id_periode,
        }
    }

    async saveAppreciationCPE () {
        try {
            if (this.appreciation !== undefined) {
                await http.post(this.api.DATA_APPRECIATION, this.toJSON());
            }
        } catch (e) {
            notify.error('evaluations.appreciations.save.error');
        }
    }

}