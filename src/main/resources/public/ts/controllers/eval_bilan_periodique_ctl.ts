import { ng, template, model, http } from 'entcore';
import { SuiviCompetenceClasse, evaluations } from '../models/teacher';
import * as utils from '../utils/teacher';
import { Defaultcolors } from "../models/eval_niveau_comp";
import {Utils} from "../models/teacher/Utils";

declare let _:any;

export let evalBilanPeriodiqueCtl = ng.controller('EvalBilanPeriodiqueCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route','$timeout',
    async function ($scope, route, $rootScope, $location, $filter, $route, $timeout) {
        template.open('suivi-acquis', 'enseignants/bilan_periodique/display_suivi_acquis');

        $scope.selected = { suiviAcquis: true, projet: false, vieScolaire: false, graphique: false };

        $scope.openSuiviAcquis = function () {
            $scope.selected = { suiviAcquis: true, projet: false, vieScolaire: false, graphique: false };
            template.close('projet');
            template.close('vie-scolaire');
            template.close('graphique');
            utils.safeApply($scope);
            template.open('suivi-acquis', 'enseignants/bilan_periodique/display_suivi_acquis');
            utils.safeApply($scope);
        }

        $scope.openProjet = function () {
            $scope.selected = { projet: true };
            template.close('suivi-acquis');
            template.close('vie-scolaire');
            template.close('graphique');
            utils.safeApply($scope);
            template.open('projet', 'enseignants/bilan_periodique/display_projets');
            utils.safeApply($scope);
        }

        $scope.openVieScolaire = function () {
            $scope.selected = { vieScolaire: true };
            template.close('suivi-acquis');
            template.close('projet');
            template.close('graphique');
            utils.safeApply($scope);
            template.open('vie-scolaire', 'enseignants/bilan_periodique/display_vie_scolaire');
            utils.safeApply($scope);
        }

        $scope.openGraphique = function () {
            $scope.selected = { graphique: true };
            template.close('suivi-acquis');
            template.close('projet');
            template.close('vie-scolaire');
            utils.safeApply($scope);
            template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
            utils.safeApply($scope);
        }

        $scope.changeContentBilanPeriod = function () {
            $scope.informations.eleve = $scope.search.eleve;
        }

    }

]);
