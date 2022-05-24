import {Defaultcolors} from "../../models/eval_niveau_comp";
import {_,moment} from "entcore";
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

let calculPeriodesAnnees = function ($scope) {
    if(!$scope.search.eleve.level)
        $scope.search.eleve.level = $scope.search.classe.name;
    let niveau = parseInt($scope.search.eleve.level.replace(/[^A-Za-z0-9]/g, '')[0]);
    if(!isNaN(niveau)) {
        let today = moment();
        let actualMonth = parseInt(today.format('M'));
        let actualYear = today.format('YYYY');
        let actualPeriode;
        if (actualMonth < 9) {
            let pastYear = (parseInt(actualYear) - 1).toString();
            actualPeriode = [moment("09-01-" + pastYear, "MM-DD-YYYY"), moment("08-31-" + actualYear, "MM-DD-YYYY")];
        } else {
            let afterYear = (parseInt(actualYear) + 1).toString();
            actualPeriode = [moment("09-01-" + actualYear, "MM-DD-YYYY"), moment("08-31-" + afterYear, "MM-DD-YYYY")];
        }
        $scope.periodesChart = [];
        for (let i = niveau; i <= 6; i++) {
            if (i != niveau) {
                actualPeriode = [moment(actualPeriode[0]).subtract(1, 'years'),
                    moment(actualPeriode[1]).subtract(1, 'years')];
            }
            let dataPeriode = {label: i.toString() + "ème", periode: actualPeriode};
            $scope.periodesChart.push(dataPeriode);
        }
    }
    $scope.periodesChart.reverse();
};

let calculPeriodesTrimestres = function ($scope) {
    $scope.trimesters = _.filter($scope.filteredPeriode, trimester =>{return trimester.id});
    $scope.periodesChart = [];
    let trimesterOrSemester = ($scope.trimesters.length == 2)? "Semestre " : "Trimestre ";
    for(let i = 0; i < $scope.trimesters.length;i++){
        let periode = [moment($scope.trimesters[i].timestamp_dt), moment($scope.trimesters[i].timestamp_fn)];
        let dataPeriode = {label:trimesterOrSemester+(i+1).toString(),periode:periode};
        $scope.periodesChart.push(dataPeriode);
    }
};

export let initChartsEval = async function ($scope) {
    $scope.chartOptionsEval.datasets.data.length = $scope.chartOptionsEval.datasets.labels.length =
        $scope.chartOptionsEval.colors.length = 0;
    $scope.periodesChart = undefined;
    if ($scope.detailCompetence !== undefined && $scope.detailCompetence !== null) {
        let ListEval = _.filter($scope.detailCompetence.competencesEvaluations, function (evalu) {
            return $scope.filterOwnerSuivi(evalu);
        });
        let unique = [];
        let distinct = [];
        ListEval.forEach((comp) => {
            if (!unique[comp.id_competences_notes]) {
                distinct.push(comp);
                unique[comp.id_competences_notes] = 1;
            }
        });
        ListEval = distinct;
        if($scope.displayCycle)
            calculPeriodesAnnees($scope);
        else if(!$scope.search.periode.id && !$scope.search.periode.libelle)
            calculPeriodesTrimestres($scope);
        else
            initFirstColumn($scope.chartOptionsEval);

        ListEval = _.sortBy(ListEval, function (evalu) {
            return evalu.evaluation_date;
        });

        let actualPeriode = undefined;
        let beginningYear = undefined;
        let endYear = undefined;
        if($scope.periodesChart && $scope.periodesChart.length>0){
            actualPeriode = $scope.periodesChart[0];
            beginningYear = moment(actualPeriode.periode[0].format());
            endYear = moment(actualPeriode.periode[1].format());
        }

        for (let i = 0; i < ListEval.length; i++) {
            let fontText = $scope.mapLettres[ListEval[i].evaluation];
            if (!fontText) {
                fontText = " ";
            }

            let data = {
                y: ListEval[i].evaluation + 2,
                x: $scope.getDateFormated(ListEval[i].evaluation_date),
                r: 10,
                label: fontText
            };

            let libelle = (ListEval[i].evaluation_libelle !== undefined)
                ? ListEval[i].evaluation_libelle : ListEval[i].name;
            if (ListEval[i].formative) {
                libelle += " (F)"
            }
            let ownerName = ListEval[i].owner_name;
            let tooltipLabel = (ownerName !== undefined) ? `${libelle} : ${ownerName}` : libelle;

            let colorValue;
            if (ListEval[i].evaluation !== -1) {
                colorValue = $scope.mapCouleurs[ListEval[i].evaluation];
            } else {
                colorValue = Defaultcolors.unevaluated;
            }

            let indexData = _.findIndex($scope.chartOptionsEval.datasets.data, {x : data.x, y : data.y});
            if(indexData === -1) {
                $scope.chartOptionsEval.datasets.data.push(data);
                $scope.chartOptionsEval.tooltipLabels.push([tooltipLabel]);
                $scope.chartOptionsEval.colors.push(colorValue);
            }
            else {
                $scope.chartOptionsEval.tooltipLabels[indexData].push(tooltipLabel);
            }

            if (actualPeriode){
                let j = 1;
                while ((moment(ListEval[i].evaluation_date).isBefore(beginningYear) ||
                    moment(ListEval[i].evaluation_date).isAfter(endYear)) &&
                !(moment(ListEval[i].evaluation_date).isSame(endYear, 'day')) &&
                !(moment(ListEval[i].evaluation_date).isSame(beginningYear, 'day'))) {
                    actualPeriode = $scope.periodesChart[j];
                    beginningYear = moment(actualPeriode.periode[0].format());
                    endYear = moment(actualPeriode.periode[1].format());
                    j++;
                }
                if (((moment(ListEval[i].evaluation_date).isBefore(endYear) &&
                    moment(ListEval[i].evaluation_date).isAfter(beginningYear)) ||
                    moment(ListEval[i].evaluation_date).isSame(endYear, 'day') ||
                    moment(ListEval[i].evaluation_date).isSame(beginningYear, 'day')) &&
                    !$scope.chartOptionsEval.datasets.labels.includes(actualPeriode.label)) {
                    $scope.chartOptionsEval.datasets.labels.push(actualPeriode.label);
                }
            }

            if(!_.contains($scope.chartOptionsEval.datasets.labels, $scope.getDateFormated(ListEval[i].evaluation_date))) {
                $scope.chartOptionsEval.datasets.labels.push($scope.getDateFormated(ListEval[i].evaluation_date));
            }
        }

        initLastColumn($scope.chartOptionsEval);
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

            let data = {
                y: ListEval[i].evaluation + 2,
                x: $scope.getDateFormated(ListEval[i].date),
                r: 10,
                label: fontText
            };

            let libelle = (ListEval[i].evaluation_libelle !== undefined)
                ? ListEval[i].evaluation_libelle : ListEval[i].name;
            if (ListEval[i].formative) {
                libelle += " (F)"
            }
            let ownerName = ListEval[i].owner_name;
            let tooltipLabel = (ownerName !== undefined) ? `${libelle} : ${ownerName}` : libelle;

            let colorValue;
            if (ListEval[i].evaluation !== -1) {
                colorValue = $scope.mapCouleurs[ListEval[i].evaluation];
            } else {
                colorValue = Defaultcolors.unevaluated;
            }

            let indexData = _.findIndex($scope.chartOptionsEval.datasets.data, {x : data.x, y : data.y});
            if(indexData === -1) {
                $scope.chartOptionsEval.datasets.data.push(data);
                $scope.chartOptionsEval.tooltipLabels.push([tooltipLabel]);
                $scope.chartOptionsEval.colors.push(colorValue);
            }
            else {
                $scope.chartOptionsEval.tooltipLabels[indexData].push(tooltipLabel);
            }

            if(!_.contains($scope.chartOptionsEval.datasets.labels, $scope.getDateFormated(ListEval[i].date))) {
                $scope.chartOptionsEval.datasets.labels.push($scope.getDateFormated(ListEval[i].date));
            }
        }

        initLastColumn($scope.chartOptionsEval);
    }
    await safeApply($scope);
};