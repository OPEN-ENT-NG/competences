import {notify, template} from 'entcore';
import http from "axios";


export const bilanPeriodique = {
    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
        },
        openCreatePE: function () {
            this.opened.lightboxCreatePE = true;
            template.open('lightboxCreatePE', '../../../competences/public/template/behaviours/sniplet-createProjetEducatif');
        },

        openCreateTheme: function() {
            this.opened.lightboxCreateTheme = true;
            template.open('lightboxCreateTheme', '../../../competences/public/template/behaviours/sniplet-createTheme');
        },

        async createThematique (thematique) {
            try {
                console.log("here");
                if(thematique !== undefined && thematique !== null) {

                    if(thematique.code !== undefined && thematique.code !== null
                        && thematique.libelle !== undefined && thematique.libelle !== null){
                        await http.post('/competences/thematique', {code: thematique.code, libelle: thematique.libelle, type: 1});
                    }
                }
            } catch (e) {
                notify.error('Problème lors de la création');
                console.error('Problème lors de la création');
                throw e;
            }
        },

        async updateThematique (): Promise<void> {
            await http.put(this.api.update,this.toJSON());
        }
    }
}