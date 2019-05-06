import {notify} from 'entcore';
import http from 'axios';
import {DefaultAvis} from "../common/DefaultAvis";

export class AvisOrientation extends DefaultAvis {

    get api () {
        return {
            POST_AVIS_ORIENTATION: '/competences/avis/orientation',
        };
    }

    constructor (idEleve:string, idPeriode:number) {
        super();
        this.id_eleve = idEleve;
        this.id_periode = idPeriode;
    }

    async syncAvisOrientation () {
        try {
            let {data} = await http.get(`/competences/avis/orientation?id_eleve=${this.id_eleve}&id_periode=${this.id_periode}`);
            if(data.id_avis_conseil_bilan !== undefined) {
                this.id_avis_conseil_bilan = data.id_avis_conseil_bilan;
            }
        } catch (e) {
            notify.error('evaluations.avis.orientation.bilan.periodique.get.error');
        }
    }

    toJSON() {
        return {
            id_avis_conseil_bilan: this.id_avis_conseil_bilan,
            id_eleve: this.id_eleve,
            id_periode: this.id_periode,
        }
    }

    async saveAvisOrientation (idAvisClasse) {
        this.id_avis_conseil_bilan = idAvisClasse;
        if (this.id_avis_conseil_bilan !== undefined && this.id_avis_conseil_bilan !== null) {
            try {
                await http.post(this.api.POST_AVIS_ORIENTATION, this.toJSON());
            } catch (e) {
                notify.error('evaluations.avis.orientation.bilan.periodique.save.error');
            }
        }
        else {
            this.id_avis_conseil_bilan = -1;
            await http.delete(`/competences/avis/orientation?id_eleve=${this.id_eleve}&id_periode=${this.id_periode}`);
        }
    }

}