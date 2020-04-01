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
 * Created by ledunoiss on 09/11/2016.
 */

import { ng, appPrefix, _,moment } from 'entcore';
import * as utils from '../utils/teacher';
import {DefaultLetters} from "../models/eval_niveau_comp";
import {Utils} from "../models/teacher";

/**
 * Directive de proportions de compétences
 */
export let proportionSuiviCompetence = ng.directive('proportionSuiviCompetence', function () {
    return {
        restrict : 'E',
        scope : {
            evaluations : '=',
            filter : '=',
            user : '=',
            isClasse : '=',
            mapCouleurs : '=',
            mapLettres : '=',
            majProportions : '=?',
            addToToolTip : '=?',
            isCycle : '=?',
            level : '=?',
            trimesters : '=?',
            isYear : '=?'
        },
        templateUrl : "/"+appPrefix+"/public/template/directives/cProportionSuiviCompetence.html",
        controller : ['$scope', function ($scope) {

            $scope.isClasse = $scope.isClasse !== undefined ? $scope.isClasse : false;


            /**
             * Listener sur la variable filter. Si modification de la variable, recalcule des proportions
             */
            $scope.$watch('filter', function (newValue, oldValue) {
                if (newValue !== oldValue) {
                    $scope.calculProportion();
                }
            }, true);

            /**
             * Met à jour le text affiché sur les bar de proportion et les tooltips
             */
            $scope.majInlineText = () => {
                $scope.mapProportionLettres = _.mapObject($scope.mapLettres, function (val, key) {
                    let letter = (val === " ")? DefaultLetters[key]: val;
                    return letter;
                });
            };

            $scope.$watch('mapLettres',  function(newValue, oldValue) {
                if (newValue !== oldValue) {
                    $scope.majInlineText();
                    $scope.calculProportion();
                    utils.safeApply($scope);
                }
            });

            $scope.$watch('majProportions', function(newValue, oldValue) {
                if (newValue !== oldValue) {
                    $scope.calculProportion();
                    utils.safeApply($scope);
                }
            });
            /**
             * Retourne la valeur d'une clé i18n passées en paramètres
             * @param key clé i18n
             * @returns {String} valeur i18n
             */
            $scope.translate = function (key) {
                return utils.translate(key);
            };

            /**
             * Calcul la proportion d'évaluations pour une compétence
             */
            $scope.calculProportion = function () {
                $scope.proportion = [];
                for (var i = -1; i < 4; i++) {
                    $scope.proportion.push({
                        eval : i,
                        percent : 0,
                        nb : 0
                    });
                }
                if ($scope.filter.mine === 'true' || $scope.filter.mine === true) {
                    $scope.competencesEvaluations = _.filter($scope.competencesEvaluations, function (evaluation) {
                        return evaluation.owner === $scope.user.userId;
                    });
                }
                if (Utils.isNotNull($scope.competencesEvaluations) && $scope.competencesEvaluations.length > 0 ) {
                    var nbEleves = 0;
                    if ($scope.isClasse == true) {
                        var elevesMap = {};

                        var nbCompetencesEvaluations = $scope.competencesEvaluations.length;

                        for (var i = 0; i < nbCompetencesEvaluations; ++i) {

                            var competencesEval = $scope.competencesEvaluations[i];

                            if (!elevesMap.hasOwnProperty(competencesEval.id_eleve)) {
                                elevesMap[competencesEval.id_eleve] = competencesEval;
                                $scope.proportion[(competencesEval.evaluation) + 1].nb++;
                                nbEleves++;
                            } else if (parseInt(elevesMap[competencesEval.id_eleve].evaluation) < parseInt(competencesEval.evaluation)) {
                                $scope.proportion[(elevesMap[competencesEval.id_eleve].evaluation) + 1].nb--;
                                elevesMap[competencesEval.id_eleve] = competencesEval;
                                $scope.proportion[parseInt(competencesEval.evaluation) + 1].nb++;
                            }
                        }
                    }

                    var nbProportion = $scope.proportion.length;

                    for (var i = 0; i < nbProportion; ++i) {
                        if ($scope.isClasse == true) {

                            // si aucune competence evaluee on n'affiche pas la proportion
                            if($scope.proportion[0].percent === 100) {
                                $scope.proportion[0].percent = 0;
                                $scope.proportion[0].nb = 0;
                            } else {
                                var nb = $scope.proportion[i].nb;
                                $scope.proportion[i].percent = (nb / nbEleves) * 100;
                                $scope.proportion[i].nb = nb;
                            }

                        } else {
                            var nb = _.where($scope.competencesEvaluations, {evaluation : parseInt($scope.proportion[i].eval)});
                            $scope.proportion[i].percent = (nb.length / $scope.competencesEvaluations.length) * 100;
                            $scope.proportion[i].nb = nb.length;
                        }
                        let proportion = $scope.proportion[i];
                        let print = `${proportion.nb} ${$scope.mapProportionLettres[proportion.eval]}`;
                        $scope.proportion[i].print = print;
                        if(!$scope.addToToolTip)
                            $scope.addToToolTip = "";
                        else
                            $scope.addToToolTip = " " + $scope.addToToolTip;
                        $scope.proportion[i].tooltip = print + "<br> " + proportion.percent.toFixed(0).toString() +
                            " " + $scope.translate('%') + $scope.addToToolTip;
                    }
                }
            };

            $scope.calculPeriodesAnnes = function () {
                let niveau = parseInt($scope.level.replace(/[^A-Za-z0-9]/g, '')[0]);
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
                    $scope.periodes = [];
                    for(let i = niveau; i<=6;i++){
                        if(i!=niveau) {
                            actualPeriode = [moment(actualPeriode[0]).subtract(1, 'years'),
                                moment(actualPeriode[1]).subtract(1, 'years')];
                        }
                        let dataPeriode = {label:i.toString()+"ème",periode:actualPeriode};
                        $scope.periodes.push(dataPeriode);
                    }
                }else{
                    $scope.isCycle = false;
                }
            };

            $scope.calculPeriodesTrimestres = function () {
                $scope.trimesters = _.filter($scope.trimesters, trimester =>{return trimester.id});
                $scope.periodes = [];
                let trimesterOrSemester = ($scope.trimesters.length == 2)? "Semestre " : "Trimestre ";
                for(let i = 0; i < $scope.trimesters.length;i++){
                    let periode = [moment($scope.trimesters[i].timestamp_dt), moment($scope.trimesters[i].timestamp_fn)];
                    let dataPeriode = {label:trimesterOrSemester+(i+1).toString(),periode:periode};
                    $scope.periodes.push(dataPeriode);
                }
            };

            $scope.majInlineText();
            if($scope.isCycle || $scope.isYear){
                if($scope.isCycle) {
                    $scope.calculPeriodesAnnes();
                    $scope.periodes.reverse();
                } else {
                    $scope.calculPeriodesTrimestres();
                }
                let nbPeriodes = $scope.periodes.length;
                for(let i = 0; i < nbPeriodes; i++){
                    let beginningYear = moment($scope.periodes[i].periode[0].format());
                    let endYear = moment($scope.periodes[i].periode[1].format());
                    $scope.competencesEvaluations = _.filter($scope.evaluations, evaluation => {
                        return moment(evaluation.evaluation_date).isBefore(endYear) &&
                            moment(evaluation.evaluation_date).isAfter(beginningYear);
                    });
                    $scope.calculProportion();
                    $scope.periodes[i].proportion = $scope.proportion.slice();
                    $scope.periodes[i].percent = (($scope.competencesEvaluations.length / $scope.evaluations.length) * 100)-0.3;
                }
            }else{
                $scope.competencesEvaluations = $scope.evaluations;
                $scope.calculProportion();
            }
        }]
    };
});