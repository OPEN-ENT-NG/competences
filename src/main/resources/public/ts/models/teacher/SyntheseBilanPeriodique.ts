import {_, Model, notify} from 'entcore';
import http from 'axios';

export class SyntheseBilanPeriodique extends Model {
    id_eleve : string;
    id_typePeriode : number;
    synthese : string;
    id_structure : string;
    id_classe : string;

    get api () {
        return {
            DATA_SYNTHESE : '/competences/syntheseBilanPeriodique',
        };
    }

    constructor (idEleve:string, idTypePeriode:number, idStructure:string, idClasse:string) {
        super();
        this.id_eleve = idEleve;
        this.id_typePeriode = idTypePeriode;
        this.id_structure = idStructure;
        this.id_classe = idClasse;
    }

    async syncSynthese() {
        try {
            let {data} = await http.get(`/competences/syntheseBilanPeriodique?id_eleve=${this.id_eleve}
            &id_typePeriode=${this.id_typePeriode}&id_structure=${this.id_structure}`);
            if(data.all !== undefined) {
                this.synthese = data[0].synthese;
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
            id_structure: this.id_structure,
            id_classe: this.id_classe,
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
