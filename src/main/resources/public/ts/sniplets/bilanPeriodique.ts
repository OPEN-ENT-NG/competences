import {http, template} from 'entcore';
import * as utils from "../utils/teacher";

console.log("peux-tu m'aider à débuguer petit console log ?");

export const bilanPeriodique = {
    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
            this.opened.lightboxCreatePE = false;
        },
        openCreatePE: function () {
            this.opened.lightboxCreatePE = true;
            template.open('lightboxCreatePE', '../../../competences/public/template/behaviours/sniplet-createProjetEducatif');
        }
    }
}