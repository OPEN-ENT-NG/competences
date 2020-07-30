/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import {Behaviours} from 'entcore';
import {visibilitymoyBFC} from './sniplets/visibilitymoyBFC';
import {itemsCompetences} from './sniplets/itemsCompetences';
import {linkGroupCycle} from "./sniplets/linkGroupCycle";
import {visibilityDNB} from "./sniplets/visibilityDNB";
import {bilanPeriodique} from "./sniplets/bilanPeriodique";
import {paramServices} from './sniplets/paramServices'
import {renameSubject} from "./sniplets/renameSubject";
import {opinionConference} from "./sniplets/opinionConference";
import {paramImportCSV} from "./sniplets/param_import_csv";
import {programElements} from "./sniplets/programsElement";
import {orderShowSubject} from "./sniplets/orderShowSubject";

Behaviours.register('competences', {
    rights: {
        workflow: {
            exportLSU: 'fr.openent.competences.controllers.LSUController|getXML',
            setVisibilityAverageBfc: 'fr.openent.competences.controllers.BFCController|setVisibility',
            paramCompetences: 'fr.openent.competences.controllers.CompetenceController|createCompetence',
            linkGroupCycle: 'fr.openent.competences.controllers.UtilsController|updateLinkGroupesCycles',
            createDispenseDomaineEleve: 'fr.openent.competences.controllers.DomaineController|createDispenseDomaineEleve',
            canUpdateBFCSynthese: 'fr.openent.competences.controllers.BFCController|updateSynthese',
            access:"fr.openent.competences.controllers.CompetencesController|view",
            exportBulletins:"fr.openent.competences.controllers.ExportPDFController|exportBulletins",
            canUpdateRetardAndAbscence:'fr.openent.competences.controllers.UtilsController|insertRetardOrAbscence',
            paramImportCSV:'fr.openent.competences.controllers.UtilsController|insertRetardOrAbscence',
            bilanPeriodique: "fr.openent.competences.controllers.ElementBilanPeriodiqueController|createElementBilanPeriodique",
            accessProjets: "fr.openent.competences.controllers.ElementBilanPeriodiqueController|createAppreciationSaisieProjet",
            canUpdateAppreciations: "fr.openent.competences.controllers.ElementBilanPeriodiqueController|createAppreciationBilanPeriodique",
            saveCompetenceNiveauFinal: "fr.openent.competences.controllers.CompetenceNoteController|saveCompetenceNiveauFinal",
            canSaisiSyntheseBilanPeriodique: "fr.openent.competences.controllers.BilanPeriodiqueController|createOrUpdateSyntheseBilanPeriodique",
            canSaisiAppreciationCPE: "fr.openent.competences.controllers.BilanPeriodiqueController|createOrUpdateAppreciationCPE",
            canSaveAppMatierePosiBilanPeriodique: "fr.openent.competences.controllers.NoteController|saveAppreciationMatiereAndPositionnement",
            paramServices: "fr.openent.competences.controllers.ServicesController|createService",
            orderShowSubject: "fr.openent.competences.controllers.OrderShowSubjectController|updateDevoirs",
            canCreateDevoir: "fr.openent.competences.controllers.DevoirController|createDevoir"
        },
        resource: {}
    },
    loadResources: async function(callback) {},
    sniplets: {
        visibilitymoyBFC: visibilitymoyBFC,
        visibilityDNB: visibilityDNB,
        itemsCompetences: itemsCompetences,
        linkGroupCycle: linkGroupCycle,
        epi_ap_parcours: bilanPeriodique,
        paramServices : paramServices,
        renameSubject: renameSubject,
        param_import_csv: paramImportCSV,
        opinionConference: opinionConference,
        programElements: programElements,
        orderShowSubject: orderShowSubject
    }
});
