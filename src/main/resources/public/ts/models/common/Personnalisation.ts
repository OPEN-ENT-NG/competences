import {evaluations} from "../teacher";
import {_} from "entcore";
import {Defaultcolors} from "../eval_niveau_comp";
import * as utils from "../../utils/teacher";


export let selectCycleForView = function ($scope, $location, id_cycle?) {
    let idCycle = id_cycle;

    if ($scope.currentDevoir && $location.path() === `/devoir/${$scope.currentDevoir.id}`) {
        idCycle = $scope.classes.findWhere({id: $scope.currentDevoir.id_groupe}).id_cycle;
    }

    if (idCycle === undefined
        && $scope.search.classe !== undefined && $scope.search.classe !== null
        && $scope.search.classe !== '*') {
        idCycle = $scope.search.classe.id_cycle;
    }
    evaluations.structure.cycle = _.findWhere(evaluations.structure.cycles, {
        id_cycle: idCycle
    });
    if (evaluations.structure.cycle === undefined) {
        evaluations.structure.cycle = evaluations.structure.cycles[0];
    }
    $scope.structure.cycle = evaluations.structure.cycle;
    return evaluations.structure.cycle.niveauCompetencesArray;
};


export let updateColorAndLetterForSkills = function ($scope, $location) {
    $scope.niveauCompetences = [];
    let niveauCompetence = selectCycleForView($scope, $location , $scope.search.classe.id_cycle);
    $scope.mapCouleurs = {"-1": Defaultcolors.unevaluated};
    $scope.mapLettres = {"-1": " "};
    $scope.selected.colors = {
        0: true,
    };
    _.forEach(niveauCompetence, function (niv) {
        $scope.mapCouleurs[niv.ordre - 1] = niv.couleur;
        $scope.mapLettres[niv.ordre - 1] = niv.lettre;
        niv.selected = true;
        $scope.niveauCompetences.push(niv);
        $scope.selected.colors[niv.ordre] = true;
    });
};


export let updateNiveau = function (usePerso ,$scope) {

};
