import {Behaviours, http} from 'entcore';
import {visibilitymoyBFC} from './sniplets/visibilitymoyBFC';
import { itemsCompetences } from './sniplets/itemsCompetences';
import {linkGroupCycle} from "./sniplets/linkGroupCycle";
import {visibilityDNB} from "./sniplets/visibilityDNB";
import {bilanPeriodique} from "./sniplets/bilanPeriodique";

Behaviours.register('competences', {
    rights: {
        workflow: {
            exportLSU: 'fr.openent.competences.controllers.LSUController|getXML',
            setVisibilityAverageBfc: 'fr.openent.competences.controllers.BFCController|setVisibility',
            paramCompetences: 'fr.openent.competences.controllers.CompetenceController|createCompetence',
            linkGroupCycle: 'fr.openent.competences.controllers.UtilsController|updateLinkGroupesCycles',
            createDispenseDomaineEleve: 'fr.openent.competences.controllers.DomaineController|createDispenseDomaineEleve',
            canUpdateBFCSynthese: 'fr.openent.competences.controllers.BFCController|updateSynthese',
            access:"fr.openent.competences.controllers.CompetencesController|view"
        },
        resource: {}
    },
    loadResources: async function(callback) {},
    sniplets: {
        visibilitymoyBFC: visibilitymoyBFC,
        visibilityDNB: visibilityDNB,
        itemsCompetences: itemsCompetences,
        linkGroupCycle: linkGroupCycle,
        bilanPeriodique: bilanPeriodique
    }
});
