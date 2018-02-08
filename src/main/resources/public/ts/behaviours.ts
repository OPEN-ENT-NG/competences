import {Behaviours, http} from 'entcore';
import { averageBFC } from './sniplets/averageBFC';
import { itemsCompetences } from './sniplets/itemsCompetences';

Behaviours.register('competences', {
    rights: {
        workflow: {
            'export-lsun': 'fr.openent.evaluations.controller.LSUController|getXML',
            setVisibilityAverageBfc: 'fr.openent.competences.controllers.BFCController|setVisibility'
        },
        resource: {}
    },
    loadResources: async function(callback) {},
    sniplets: {
        averageBFC: averageBFC,
        itemsCompetences: itemsCompetences
    }
});
