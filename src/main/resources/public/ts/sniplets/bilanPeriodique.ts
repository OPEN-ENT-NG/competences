import {http, template} from 'entcore';

export const bilanPeriodique = {
    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
        },
        createPE: function () {
            this.opened = {
                lightbox : true
            };
        template.open('lightboxContainerCreatePE', 'behaviours/sniplet-bilanPeriodique');
        }
    }
}