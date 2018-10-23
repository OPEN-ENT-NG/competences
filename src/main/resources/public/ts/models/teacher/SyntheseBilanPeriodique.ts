import {_, Model, notify} from 'entcore';
import http from 'axios';

export class SyntheseBilanPeriodique extends Model {
    id_eleve : string;
    id_typePeriode : number;
    synthese : string;

    get api () {
        return {
            DATA_SYNTHESE : '/competences/syntheseBilanPeriodique',
        };
    }

    constructor (idEleve:string, idTypePeriode:number) {
        super();
        this.id_eleve = idEleve;
        this.id_typePeriode = idTypePeriode;
    }

    async sync() {
        try {
            let {data} = await http.get(`/competences/syntheseBilanPeriodique?id_eleve=${this.id_eleve}&id_typePeriode=${this.id_typePeriode}`);
            if(data.synthese !== undefined) {
                this.synthese = data.synthese;
            }
        } catch (e) {
            notify.error('evaluation.synthese.bilan.periodique.get.error');
        }
    }

    toJSON() {
        return {
            synthese: this.synthese,
            id_eleve: this.id_eleve,
            id_typePeriode: this.id_typePeriode,
        }
    }

    async saveSynthese () {
        try {
            if (this.synthese !== undefined) {
                await http.post(this.api.DATA_SYNTHESE, this.toJSON());
            }
        } catch (e) {
            notify.error('evaluation.synthese.bilan.periodique.save.error');
        }
    }

}
