import {notify, http} from 'entcore';
import {DefaultAvis} from "../common/DefaultAvis";

export class AvisConseil extends DefaultAvis {
    avis;
    // avis: [];

    get api () {
        return {
            AVIS_CONSEIL: '/competences/avis/conseil',
            NEW_OPINION: '/competences/avis/bilan/periodique'
        };
    }

    constructor (idEleve:string, idPeriode:number, idStructure:string) {
        super();
        this.id_eleve = idEleve;
        this.id_periode = idPeriode;
        this.id_structure = idStructure;
    }

    async getLibelleAvis () {
        try {
            let data = await http().get(`/competences/avis/bilan/periodique?id_structure=${this.id_structure}`);
            if(data.data !== undefined) {
                this.avis = data.data;

                let perso = {
                    id: 0,
                    libelle: "-- Personnalisé --",
                    type_avis: 0,
                    active: true,
                    id_etablissement: null
                };
                this.avis.push(perso);
            }
        } catch (e) {
            notify.error('evaluations.avis.conseil.bilan.periodique.get.error');
        }
    }

    async syncAvisConseil () {
        try {
            let {data} = await http().get(this.api.AVIS_CONSEIL + `?id_eleve=${this.id_eleve}&id_periode=${this.id_periode}&id_structure=${this.id_structure}`);
            if(data[0].id_avis_conseil_bilan !== undefined) {
                this.id_avis_conseil_bilan = data[0].id_avis_conseil_bilan;
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
            id_structure: this.id_structure,
        }
    }

    async saveAvisConseil (idAvisClasse) {
        this.id_avis_conseil_bilan = idAvisClasse;
        if (this.id_avis_conseil_bilan !== undefined && this.id_avis_conseil_bilan !== null) {
            try {
                await http().postJson(this.api.AVIS_CONSEIL, this.toJSON())
            } catch (e) {
                notify.error('evaluations.avis.conseil.bilan.periodique.save.error');
            }
        } else {
            this.id_avis_conseil_bilan = -1;
            await http().deleteJson(this.api.AVIS_CONSEIL, this.toJSON());
        }
    }

    async createNewOpinion (avisLibelle) {
        if(avisLibelle){
            try {
                let result = await http().postJson(this.api.NEW_OPINION, {
                    libelle: avisLibelle,
                    type_avis: 1,
                    id_etablissement: this.id_structure
                });
                return result.data.id;
            } catch (e) {
                notify.error('evaluations.avis.conseil.bilan.periodique.save.error');
            }
        }
    }

}