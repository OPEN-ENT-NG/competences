import {Defaultcolors} from "../../models/eval_niveau_comp";
import {_} from "entcore";
import {safeApply} from "./safeApply";

let initFirstColumn = function (chartOptionsEval) {
    //initialisation et rajout de la 1er colomn vide
    chartOptionsEval.tooltipLabels = [];
    chartOptionsEval.tooltipLabels.push(' ');
    chartOptionsEval.datasets.data = [];
    chartOptionsEval.datasets.data.push(-10);
    chartOptionsEval.datasets.labels = [];
    chartOptionsEval.datasets.labels.push(" ");
    chartOptionsEval.colors = [];
    chartOptionsEval.colors.push('#FFFFFF');
};

let initLastColumn = function (chartOptionsEval) {
    //rajout de la dernière colomn vide
    chartOptionsEval.datasets.data.push(-10);
    chartOptionsEval.datasets.labels.push(" ");
    chartOptionsEval.colors.push('#FFFFFF');
    chartOptionsEval.tooltipLabels.push(' ');
};

export let initChartsEval = async function ($scope) {
    if ($scope.detailCompetence !== undefined && $scope.detailCompetence !== null) {
        let ListEval = _.filter($scope.detailCompetence.competencesEvaluations, function (evalu) {
            return $scope.filterOwnerSuivi(evalu);
        });

        initFirstColumn($scope.chartOptionsEval);

        ListEval = _.sortBy(ListEval, function (evalu) {
            return evalu.evaluation_date;
        });

        for (let i = 0; i < ListEval.length; i++) {

            let fontText = $scope.mapLettres[ListEval[i].evaluation];
            if (!fontText) {
                fontText = " ";
            }
            $scope.chartOptionsEval.datasets.data.push({
                y: ListEval[i].evaluation + 2,
                x: $scope.getDateFormated(ListEval[i].evaluation_date),
                r: 10,
                label: fontText
            });
            $scope.chartOptionsEval.datasets.labels.push($scope.getDateFormated(ListEval[i].evaluation_date));
            let colorValue;
            if (ListEval[i].evaluation !== -1) {
                colorValue = $scope.mapCouleurs[ListEval[i].evaluation];
            }
            else {
                colorValue = Defaultcolors.unevaluated;
            }
            $scope.chartOptionsEval.colors.push(colorValue);

            let libelle = (ListEval[i].evaluation_libelle !== undefined)? ListEval[i].evaluation_libelle : ListEval[i].name ;
            if (ListEval[i].formative) {
                libelle += " (F)"
            }
            let ownerName = ListEval[i].owner_name;
            let tooltipLabel = (ownerName !== undefined)? `${libelle} : ${ownerName}` : libelle;
            $scope.chartOptionsEval.tooltipLabels.push(tooltipLabel);
        }

        initLastColumn($scope.chartOptionsEval)
    }
   await safeApply($scope);
};


export let initChartsEvalParents = async function ($scope) {
    if ($scope.detailCompetence !== undefined && $scope.detailCompetence !== null) {
        let ListEval = _.filter($scope.detailCompetence.competencesEvaluations, function (evalu) {
            return $scope.filterOwnerSuivi(evalu);
        });

        initFirstColumn($scope.chartOptionsEval);

        ListEval = _.sortBy(ListEval, function (evalu) {
            return evalu.date;
        });

        for (let i = 0; i < ListEval.length; i++) {

            let fontText = $scope.mapLettres[ListEval[i].evaluation];
            if (!fontText) {
                fontText = " ";
            }
            $scope.chartOptionsEval.datasets.data.push({
                y: ListEval[i].evaluation + 2,
                x: $scope.getDateFormated(ListEval[i].date),
                r: 10,
                label: fontText
            });
            $scope.chartOptionsEval.datasets.labels.push($scope.getDateFormated(ListEval[i].date));
            let colorValue;
            if (ListEval[i].evaluation !== -1) {
                colorValue = $scope.mapCouleurs[ListEval[i].evaluation];
            }
            else {
                colorValue = Defaultcolors.unevaluated;
            }
            $scope.chartOptionsEval.colors.push(colorValue);

            let libelle = (ListEval[i].evaluation_libelle !== undefined)? ListEval[i].evaluation_libelle : ListEval[i].name ;
            if (ListEval[i].formative) {
                libelle += " (F)"
            }
            let ownerName = ListEval[i].owner_name;
            let tooltipLabel = (ownerName !== undefined)? `${libelle} : ${ownerName}` : libelle;
            $scope.chartOptionsEval.tooltipLabels.push(tooltipLabel);
        }

        initLastColumn($scope.chartOptionsEval);
    }
    await safeApply($scope);
};