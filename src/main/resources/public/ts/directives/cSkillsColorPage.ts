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
 * Created by ledunoiss on 21/09/2016.
 */
import {ng, appPrefix, idiom as lang, _} from 'entcore';
import {getNN}  from '../utils/functions/utilsNN';
export let cSkillsColorPage = ng.directive("cSkillsColorPage", function(){
    return {
        restrict : 'E',
        scope : {
            devoir : '=',
            hasRight: '=',
            selectedEleves : '=',
            selectedCompetences : '=',
            niveauCompetences: '='
        },
        templateUrl: "/"+appPrefix+"/public/template/directives/cSkillsColorPage.html",
        controller : ['$scope', function($scope){
            $scope.selectColor = function(){

              //  if(confirm(text) === true){
                    let evaluation = $scope.eval;
                    var _datas = [];
                    var _range = $scope.selectedEleves.list.length > 0 ? $scope.selectedEleves.list
                        : $scope.devoir.eleves.all;
                    // Boucle sur les élèves
                    for (var i = 0; i < _range.length; i++) {
                        // On récupère l'évaluation de l'élève sur le devoir
                        var _eval = _range[i].evaluation;
                        if ($scope.selectedCompetences.length > 0) {
                            // Si on a des compétences de sélectionnées, on les récupère
                            var ids = [];
                            for (var g = 0; g < $scope.selectedCompetences.length; g++) {
                                ids.push($scope.selectedCompetences[g].id_competence);
                            }
                            // Pour chaque compétences Notes de l'élève
                            for (var j = 0; j < _eval.competenceNotes.all.length; j++) {
                                if (ids.indexOf(_eval.competenceNotes.all[j].id_competence) !== -1) {
                                    // Si la compétence est sélectionnée et qu'il n'y a pas d'annotation, on l'ajoute
                                    if (_eval.id_annotation === undefined || _eval.id_annotation < 1
                                        || ($scope.devoir.is_evaluated && _eval.valeur === getNN())){
                                        _eval.competenceNotes.all[j].evaluation = evaluation;
                                        _datas.push(_eval.competenceNotes.all[j]);
                                    }
                                }
                            }
                        } else {
                            for (var j = 0; j < _eval.competenceNotes.all.length; j++) {
                                if (_eval.id_annotation === undefined || _eval.id_annotation < 1
                                    || ($scope.devoir.is_evaluated && _eval.valeur === getNN())) {
                                    _eval.competenceNotes.all[j].evaluation = evaluation;
                                    _datas.push(_eval.competenceNotes.all[j]);
                                }
                            }
                        }
                    }
                    _.map($scope.selectedCompetences, function (comp) {
                        $scope.$emit('majHeaderColumn', comp);
                        comp.selected = false;
                    });
                    $scope.selectedCompetences = [];
                    _.map($scope.selectedEleves.list, function (eleve) {
                        eleve.selected = false;
                    });
                    $scope.selectedEleves.list = [];
                    $scope.selectedEleves.all = false;
                    $scope.devoir.saveCompetencesNotes(_datas);

                    $scope.annuler();
                //}
            };
            $scope.text =lang.translate('evaluation.action.confirme.initialise.skills') + "\n\n"
                            + lang.translate('evaluation.continue')+ "\n";

            $scope.eval = "";
            $scope.opened = {
                lightbox : false
            };
            $scope.confirme = function(evaluation){
                if($scope.devoir.endSaisie && !$scope.hasRight){
                    $scope.text= lang.translate('evaluations.devoir.uncancelable');
                } // des élèves et des compétences sélectionnées
                else if($scope.selectedEleves.list.length > 0 && $scope.selectedCompetences.length > 0) {
                    $scope.text = lang.translate('evaluation.action.evaluate.students.for.skills');

                    // des élèves et aucune compétence sélectionnée
                } else if($scope.selectedEleves.list.length > 0 && $scope.selectedCompetences.length === 0) {
                    $scope.text = lang.translate('evaluation.action.evaluate.students.for.all.skills');

                    // aucun élève et des compétences sélectionnées
                } else  if($scope.selectedEleves.list.length === 0 && $scope.selectedCompetences.length === 0) {
                    $scope.text = lang.translate('evaluation.action.evaluate.all.students.for.skills');
                }
                $scope.eval = evaluation;
                $scope.opened.lightbox = true;
                $scope.devoir.calculStats();
            };
            $scope.annuler = function () {
                $scope.text = lang.translate('evaluation.action.confirme.initialise.skills');
                $scope.eval = "";
                $scope.opened.lightbox = false;

            };
        }]
    };
});