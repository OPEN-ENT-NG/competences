import {Behaviours, http} from 'entcore';
import { averageBFC } from './sniplets/averageBFC';
import { itemsCompetences } from './sniplets/itemsCompetences';
import {linkGroupCycle} from "./sniplets/linkGroupCycle";

Behaviours.register('competences', {
    rights: {
        workflow: {
            exportLSU: 'fr.openent.competences.controllers.LSUController|getXML',
            setVisibilityAverageBfc: 'fr.openent.competences.controllers.BFCController|setVisibility',
            paramCompetences: 'fr.openent.competences.controllers.CompetenceController|createCompetence',
            linkGroupCycle: 'fr.openent.competences.controllers.UtilsController|updateLinkGroupesCycles',
            canUpdateBFCSynthese: 'fr.openent.competences.controllers.BFCController|updateSynthese'
        },
        resource: {}
    },
    loadResources: async function(callback) {},
    sniplets: {
        averageBFC: averageBFC,
        itemsCompetences: itemsCompetences,
        linkGroupCycle: linkGroupCycle
    }
});
