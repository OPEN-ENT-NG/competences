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

/**
 * Created by ledunoiss on 12/09/2016.
 */
import { routes, ng, model } from 'entcore';
import { evaluations } from './models/teacher';

ng.addRequiredModule('chart.js');


//CONTROLLERS
import {evaluationsController} from './controllers/eval_teacher_ctl';
import {evalAcuTeacherController} from './controllers/eval_acu_teacher_ctl';
import {evalSuiviEleveCtl} from './controllers/eval_suivi_eleve_ctl';
import {evalSuiviCompetenceClasseCtl} from './controllers/eval_suivi_competences_classe_ctl';
import {evalBilanPeriodiqueCtl} from './controllers/eval_bilan_periodique_ctl';
import {exportControleur} from './controllers/eval_export_controller';
import {evalBulletinCtl} from "./controllers/eval_print_bulletin_ctl";

ng.controllers.push(evaluationsController);
ng.controllers.push(evalAcuTeacherController);
ng.controllers.push(evalSuiviEleveCtl);
ng.controllers.push(evalSuiviCompetenceClasseCtl);
ng.controllers.push(evalBilanPeriodiqueCtl);
ng.controllers.push(exportControleur);
ng.controllers.push(evalBulletinCtl);

//FILTERS
import {uniqueFilter} from './utils/filters/unique';
import {customSearchFilter} from './filters/customSearch';
import {customSearchCompetencesFilter} from './filters/customSearchCompetences';
import {getMatiereClasseFilter} from './utils/filters/getMatiereClasse';
import {getEnseignantClasseFilter} from './utils/filters/getEnseignantClasse';
import {customClassFilters} from './filters/customClassPeriodeFilter';
import {customPeriodeFilters} from "./filters/customClassPeriodeFilter";
import {customClassPeriodeFilters} from "./filters/customClassPeriodeFilter";

ng.filters.push(uniqueFilter);
ng.filters.push(customSearchFilter);
ng.filters.push(customSearchCompetencesFilter);
ng.filters.push(getMatiereClasseFilter);
ng.filters.push(getEnseignantClasseFilter);
ng.filters.push(customClassFilters);
ng.filters.push(customPeriodeFilters);
ng.filters.push(customClassPeriodeFilters);

//DIRECTIVES
import {cFilAriane} from './utils/directives/cFilAriane';
import {navigable} from './utils/directives/navigable';
import {navigableCompetences} from './directives/cNavigableCompetences';
import {navigatable} from './utils/directives/navigatable';
import {tabs} from './utils/directives/tabs';
import {pane} from './utils/directives/pane';
import {cSkillNoteDevoir} from './directives/cSkillNoteDevoir';
import {cSkillsColorColumn} from './directives/cSkillsColorColumn';
import {cSkillsColorPage} from './directives/cSkillsColorPage';
import {cSkillsList} from './directives/cSkillsList';
import {autofocus} from './utils/directives/autofocus';
import {sticky} from './utils/directives/sticky';
import {proportionSuiviCompetence} from './directives/ProportionSuiviCompetence';
import {rzslider} from './utils/directives/slider';
import { structureLoader } from './utils/directives/structureLoading';
import {inputTextList} from './directives/inputTextList';
import { cSkillsBubble } from './directives/cSkillsBubble';
import {messageLoader} from "./utils/directives/messageLoading";
import {teachingsSkills} from "./directives/teachingsSkills";

ng.directives.push(cFilAriane);
ng.directives.push(navigable);
ng.directives.push(navigatable);
ng.directives.push(navigableCompetences);
ng.directives.push(tabs);
ng.directives.push(pane);
ng.directives.push(cSkillNoteDevoir);
ng.directives.push(cSkillsColorColumn);
ng.directives.push(cSkillsColorPage);
ng.directives.push(cSkillsList);
ng.directives.push(autofocus);
ng.directives.push(sticky);
ng.directives.push(proportionSuiviCompetence);
ng.directives.push(rzslider);
ng.directives.push(structureLoader);
ng.directives.push(messageLoader);
ng.directives.push(inputTextList);
ng.directives.push(teachingsSkills);

ng.directives.push(cSkillsBubble);

routes.define(function($routeProvider){
    $routeProvider
        .when('/devoirs/list',{action:'listDevoirs'})
        .when('/devoir/create',{action:'createDevoir'})
        .when('/devoir/:idDevoir/edit', {action : 'editDevoir'})
        .when('/devoir/:devoirId', {action:'viewNotesDevoir'})
        .when('/releve', {action:'displayReleveNotes'})
        .when('/competences/eleve', {action : 'displaySuiviEleve'})
        .when('/competences/classe', {action : 'displaySuiviCompetencesClasse'})
        .when('/remplacements/list',{action:'listRemplacements'})
        .when('/remplacement/create',{action:'createRemplacements'})
        .when('/projets',{action:'displayEpiApParcours'})
        .when('/conseil/de/classe',{action:'displayBilanPeriodique'})
        .when('/export',{action:'export'})
        .when('/disabled', {action : 'disabled'})
        .when('/bulletin', {action : 'bulletin'})
        .when('/',{action:'accueil'})
        .otherwise({
            redirectTo : '/'
        });
});

declare let require: any;
export let Color = require('color');

model.build = async function () {
    await model.me.workflow.load(['viescolaire']);
    (this as any).evaluations = evaluations;
};