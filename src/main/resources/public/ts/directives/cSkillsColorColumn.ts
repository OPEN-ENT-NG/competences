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
import {ng, appPrefix, _} from 'entcore';

export let cSkillsColorColumn = ng.directive("cSkillsColorColumn", function(){
    return {
        restrict : 'E',
        scope : {
            devoir : '=',
            nameFunction: '=',
            selectedEleves: '=',
            selectedCompetences: '=',
            check : '='
        },
        templateUrl: "/"+appPrefix+"/public/template/directives/cSkillsColorColumn.html",
        controller : ['$scope', function($scope){
            $scope.compteur = 0;

            $scope.selectCompetences = function(competenceHeader){
                _.each($scope.eleves, function (eleve) {
                    var competencesNotesEleve = eleve.competencesNotesEleve;
                    var competenceEleve = _.findWhere(competencesNotesEleve, {id_competence: competenceHeader.id_competence});
                    competenceEleve.evaluation = competenceHeader.evaluation;
                });
                $scope.safeApply();
            };

            $scope.safeApply = function (fn) {
                var phase = this.$root.$$phase;
                if(phase === '$apply' || phase === '$digest') {
                    if(fn && (typeof(fn) === 'function')) fn();
                } else this.$apply(fn);
            };

            $scope.saveCompetences = function(competenceHeader){
                if(competenceHeader.modified) {
                    var _data = [];
                    var range = $scope.selectedEleves.length > 0 ? $scope.selectedEleves : $scope.devoir.eleves.all;
                    for (var i = 0; i < range.length; i++) {
                        var competence = range[i].evaluation.competenceNotes.findWhere({id_competence: competenceHeader.id_competence});
                        if (competence !== undefined) {
                            competence.evaluation = competenceHeader.evaluation;
                            _data.push(competence);
                        }
                    }
                    $scope.devoir.saveCompetencesNotes(_data);
                    competenceHeader.modified = false;
                }
            };

            $scope.init = function(competenceHeader){
                competenceHeader.selected = false;
                $scope.$on('initHeaderColumn', function () {
                    competenceHeader.evaluation = -1;
                    competenceHeader.modified = false;
                    competenceHeader.nomAvecDomaine = $scope.nameFunction(competenceHeader);
                    $scope.majHeaderColor(competenceHeader);
                })
            };

            $scope.switchColor = function(competenceHeader){
                if(competenceHeader.evaluation === -1){
                    competenceHeader.evaluation = $scope.devoir.maxOrdre;
                }else{
                    competenceHeader.evaluation = competenceHeader.evaluation -1;
                }
                competenceHeader.modified = true;
                $scope.selectCompetences(competenceHeader);
            };

            $scope.$on('changeHeaderColumn', function(event, competence){
                var competenceHeader = $scope.devoir.competences.findWhere({id_competence : competence.id_competence});
                $scope.majHeaderColor(competenceHeader);
            });

            $scope.majHeaderColor = function(competenceHeader) {
                // recuperation de la competence pour chaque eleve
                var allCompetencesElevesColumn = [];
                _.each($scope.devoir.eleves.all, function (eleve) {
                    if (eleve.evaluation.competenceNotes !== undefined && eleve.evaluation.competenceNotes.all.length > 0) {
                        var competenceEleve = eleve.evaluation.competenceNotes.findWhere({id_competence: competenceHeader.id_competence});
                        allCompetencesElevesColumn.push(competenceEleve);
                    }
                });


                if(allCompetencesElevesColumn !== undefined && allCompetencesElevesColumn.length > 0) {
                    // si toutes les competences ont la même note on colore evaluation de la même couleur
                    if (_.every(allCompetencesElevesColumn, function (competence) {
                            return (competence.evaluation === allCompetencesElevesColumn[0].evaluation);
                        })) {
                        competenceHeader.evaluation = allCompetencesElevesColumn[0].evaluation;
                    } else {
                        competenceHeader.evaluation = -1;
                    }
                }
                $scope.safeApply();
            };
        }]
    };
})