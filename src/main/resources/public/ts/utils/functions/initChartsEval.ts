import {Defaultcolors} from "../../models/eval_niveau_comp";
import {_} from "entcore";
import {safeApply} from "./safeApply";

export let initChartsEval = async function ($scope) {
    if ($scope.detailCompetence !== undefined && $scope.detailCompetence !== null) {
        let ListEval = _.filter($scope.detailCompetence.competencesEvaluations, function (evalu) {
            return $scope.filterOwnerSuivi(evalu);
        });
        //initialisation et rajout de la 1er colomn vide
        $scope.chartOptionsEval.tooltipLabels = [];
        $scope.chartOptionsEval.tooltipLabels.push(' ');
        $scope.chartOptionsEval.datasets.data = [];
        $scope.chartOptionsEval.datasets.data.push(-10);
        $scope.chartOptionsEval.datasets.labels = [];
        $scope.chartOptionsEval.datasets.labels.push(" ");
        $scope.chartOptionsEval.colors = [];
        $scope.chartOptionsEval.colors.push('#FFFFFF');
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

        //rajout de la dernière colomn vide
        $scope.chartOptionsEval.datasets.data.push(-10);
        $scope.chartOptionsEval.datasets.labels.push(" ");
        $scope.chartOptionsEval.colors.push('#FFFFFF');
        $scope.chartOptionsEval.tooltipLabels.push(' ');
    }
   await safeApply($scope);
};
