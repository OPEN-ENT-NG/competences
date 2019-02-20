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
import { routes, ng } from 'entcore';
import {evaluationsController} from './controllers/eval_parent_ctl';
import {cRoundAvatar} from "./utils/directives/cRoundAvatar";
import {releveController} from "./controllers/eval_parent_releve_ctrl";
import {cFilAriane} from "./utils/directives/cFilAriane";
import {listController} from "./controllers/eval_parent_devoirs";
import {customSearchFilter} from "./filters/customSearch";
import {uniqueFilter} from "./utils/filters/unique";
import {cSkillsBubble} from "./directives/cSkillsBubble";
import {proportionSuiviCompetence} from "./directives/ProportionSuiviCompetence";
import {evalBilanPeriodiqueCtl} from './controllers/eval_bilan_periodique_ctl';
import {messageLoader} from "./utils/directives/messageLoading";


ng.addRequiredModule('chart.js');
// controllers
ng.controllers.push(evaluationsController);
ng.controllers.push(releveController);
ng.controllers.push(listController);
ng.controllers.push(evalBilanPeriodiqueCtl);

// directives
ng.directives.push(cRoundAvatar);
ng.directives.push(cFilAriane);
ng.directives.push(proportionSuiviCompetence);
ng.directives.push(cSkillsBubble);
ng.directives.push(messageLoader);


// filters
ng.filters.push(customSearchFilter);
ng.filters.push(uniqueFilter);

routes.define(function($routeProvider) {
    $routeProvider
        .when('/',{action:'accueil'})
        .when('/devoirs/list', {action:'listDevoirs'})
        .when('/devoir/:devoirId', {action:'viewDevoir'})
        .when('/releve', {action:'displayReleveNotes'})
        .when('/competences/eleve', {action:'displayBilanDeCompetence'})
        .when('/bilan/periodique', {action:'displayBilanPeriodique'})
        .otherwise({
            redirectTo : '/'
        });
});