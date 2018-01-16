/**
 * Created by ledunoiss on 21/09/2016.
 */
import { ng, appPrefix, $, _ } from 'entcore';
import * as utils from '../utils/teacher';

export let cSkillNoteDevoir = ng.directive('cSkillNoteDevoir', function($compile){
    return {
        restrict : 'E',
        scope : {
            competence : '=',
            nbEleve : '=',
            nbCompetencesDevoir : '=',
            currentDevoir   : '=',
            disabled : '=?',
            focus : '=',
            blur : '=',
            indexRow:'=',
            indexColumn:'=',
            eleve:'=',
            eleves:'=',
            getEleveInfo:'=',
            selectedCompetences: '=',
            mapCouleurs: '=',
            mapLettres: '='
        },
        templateUrl : "/"+appPrefix+"/public/template/directives/cSkillNoteDevoir.html",
        controller : ['$scope', function($scope){
            $scope.color = -1;
            $scope.modified = false;
            $scope.compteur = 0;
            $scope.switchColor = function(){
                if (!$scope.disabled) {
                    if ($scope.competence.evaluation === -1) {
                        $scope.competence.evaluation = $scope.currentDevoir.maxOrdre;
                    } else {
                        $scope.competence.evaluation = $scope.competence.evaluation - 1;
                    }
                    $scope.$emit('majHeaderColumn', $scope.competence);
                    $scope.modified = $scope.competence.oldValeur !== $scope.competence.evaluation;
                }
            };


            $scope.isCompetenceHeaderSelected = function() {

               return  _.where($scope.selectedCompetences,{id_competence : $scope.competence.id_competence}).length > 0;

            };

            $scope.getNbElevesSelected = function() {
                return  _.where($scope.eleves,{selected: true}).length;
            };

            $scope.highlightCompetenceNote = function() {

                var nbColumnSelected = $scope.selectedCompetences.length;
                var eleveSelected =  $scope.eleve.selected;
                var nbElevesSelected = $scope.getNbElevesSelected();

                // cas 1 : aucune colonne sélectionnée et élève sélectionné
                if(nbColumnSelected === 0 && eleveSelected) {
                    return true;
                }

                // cas 2 : au moins 1 colonne sélectionnée
                if(nbColumnSelected > 0) {
                    // si l'élève est sélectionné alors dans ce cas on met en évidence que si on est sur la colonne sélectionnée
                    if(eleveSelected && $scope.isCompetenceHeaderSelected()) {
                        return true;
                    }

                    // si aucun élève est sélectionné, alors, on met en évidence toute la colonne (si celle ci est sélectionnée)
                    if(nbElevesSelected === 0 && $scope.isCompetenceHeaderSelected()) {
                        return true
                    }
                }

                // dans tous les autres cas on ne met rien en évidence
                return false;

            };

            $scope.keys = {
                numbers : {zero : 96, one : 97, two : 98, three : 99, four : 100},
                shiftNumbers : {zero : 48, one : 49, two : 50, three : 51, four : 52}
            };

            $scope.init = function () {
                $scope.disabled = $scope.disabled !== undefined ? $scope.disabled : false;
                $scope.competence.oldValeur = $scope.competence.evaluation;
            };

            $scope.keyColor = function ($event) {
                if (!$scope.disabled) {
                    var key = $event.keyCode | $event.which;

                    switch (key) {
                        case $scope.keys.numbers.zero :
                        case $scope.keys.shiftNumbers.zero : {
                            $scope.competence.evaluation = -1;
                        }
                            break;
                        case $scope.keys.numbers.one :
                        case $scope.keys.shiftNumbers.one : {
                            $scope.competence.evaluation = 0;
                        }
                            break;
                        case $scope.keys.numbers.two :
                        case $scope.keys.shiftNumbers.two : {
                            $scope.competence.evaluation = 1;
                        }
                            break;
                        case $scope.keys.numbers.three :
                        case $scope.keys.shiftNumbers.three: {
                            $scope.competence.evaluation = 2;
                        }
                            break;
                        case $scope.keys.numbers.four :
                        case $scope.keys.shiftNumbers.four : {
                            $scope.competence.evaluation = 3;
                        }
                            break;
                    }
                    $scope.$emit('majHeaderColumn', $scope.competence);
                    $scope.modified = $scope.competence.oldValeur !== $scope.competence.evaluation;
                }
            };

            $scope.saveCompetence = function() {
                if (!$scope.disabled) {
                    if ($scope.modified === true) {
                        $scope.competence.save().then(() => {
                            $scope.currentDevoir.calculStats().then(() => {
                                utils.safeApply($scope);
                            });
                            $scope.modified = false;
                            $scope.competence.oldValeur = $scope.competence.evaluation;
                        });
                    }
                }
            };
        }]
    }
});