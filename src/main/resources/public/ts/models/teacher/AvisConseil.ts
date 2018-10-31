import {Model, notify} from 'entcore';
import http from 'axios';

export class AvisConseil extends Model {
    id_eleve : string;
    id_periode : number;
    id_avis_conseil_bilan : number;
    avis: string;
    // avis: [];

    get api () {
        return {
            POST_AVIS_CONSEIL: '/competences/avis/conseil',
        };
    }

    constructor (idEleve:string, idPeriode:number) {
        super();
        this.id_eleve = idEleve;
        this.id_periode = idPeriode;
    }

    async getLibelleAvis () {
        try {
            let data = await http.get(`/competences/avis/bilan/periodique`);
            if(data.data !== undefined) {
                this.avis = data.data;
            }
        } catch (e) {
            notify.error('evaluations.avis.conseil.bilan.periodique.get.error');
        }
    }

    async syncAvisConseil () {
        try {
            let {data} = await http.get(`/competences/avis/conseil?id_eleve=${this.id_eleve}&id_periode=${this.id_periode}`);
            if(data.id_avis_conseil_bilan !== undefined) {
                this.id_avis_conseil_bilan = data.id_avis_conseil_bilan;
            }
        } catch (e) {
            notify.error('evaluations.avis.conseil.bilan.periodique.get.error');
        }
    }

    toJSON() {
        return {
            id_avis_conseil_bilan: this.id_avis_conseil_bilan,
            id_eleve: this.id_eleve,
            id_periode: this.id_periode,
        }
    }

    async saveAvisConseil (idAvisClasse) {
        this.id_avis_conseil_bilan = idAvisClasse.id;
        if (this.id_avis_conseil_bilan !== undefined) {
            try {
                await http.post(this.api.POST_AVIS_CONSEIL, this.toJSON());
            } catch (e) {
                notify.error('evaluations.avis.conseil.bilan.periodique.save.error');
            }
        }
    }

}