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