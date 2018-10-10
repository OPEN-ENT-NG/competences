import {_, ng, notify} from "entcore";
import {ExportBulletins} from "../models/common/ExportBulletins";
import * as utils from '../utils/teacher';
import {Classe} from "../models/teacher";

export let evalBulletinCtl = ng.controller('EvaluationsBulletinsController', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route', '$timeout',
    function ($scope) {
        $scope.initBulletin = function ( ) {
            $scope.printClasses = {
                all : _.filter($scope.classes.all, (classe) => {
                    return classe.type_groupe === 0;
                })
            };
            _.forEach($scope.printClasses.all, (classe) => {
                classe.selected = false;
            });
            $scope.print = {};
            $scope.filteredPeriodes = [];
            $scope.filterEleves =  [];
            $scope.allElevesClasses = [];
            $scope.selected = {
                periode : undefined
            };
        };


        $scope.generateBulletin = async function (options){
            if($scope.selected === undefined
                || $scope.selected.periode === undefined || _.isEmpty($scope.filteredPeriodes)) {
                notify.info('evaluations.choose.periode');
                return ;
            }

            options.idPeriode = $scope.selected.periode.id_type;
            options.idStudents = _.pluck(_.where($scope.allElevesClasses, {selected:true}), 'id');

            if (_.isEmpty(options.idStudents)){
                notify.info('evaluations.choose.student');
                return ;
            }
            $scope.opened.displayMessageLoader = true;
            utils.safeApply($scope);
            await ExportBulletins.generateBulletins(options);
            $scope.opened.displayMessageLoader = false;
            utils.safeApply($scope);

        };

        $scope.chooseClasse = async function (classe, together?) {
            classe.selected = !classe.selected;
            if (classe.synchronized.periodes !== true) {
                await classe.periodes.sync();
            }
            if (classe.synchronized.eleves !== true) {
                await classe.eleves.sync();
            }
            $scope.updateFilters();
            utils.safeApply($scope);
        };

        $scope.chooseStudent = function (student) {
          student.selected = !student.selected;
          utils.safeApply($scope);
        };

        $scope.switchAll =  async function (collection , b, isClasse) {
            _.forEach(collection ,async (c) => {
                c.selected = b;
            });
            if(isClasse === true){
                $scope.updateFilters();
            }
            utils.safeApply($scope);
        };

        // Permet de mettre à jour les périodes et les élèves dans les listes en fonction de la classe
        $scope.updateFilters = async function () {
            let selectedClasses = _.where($scope.printClasses.all, {selected : true});
            $scope.filteredPeriodes = [];
            if (selectedClasses.length === 0) {
                $scope.allElevesClasses = [];
                $scope.filteredPeriodes = [];
                utils.safeApply($scope);
                return;
            }
            else {


                await Promise.all(_.union(_.map(selectedClasses, (classe: Classe)=> {
                    return [classe.periodes.sync.call(classe), classe.eleves.sync.call(classe)];
                })));


                $scope.allElevesClasses = [];
                let periodes = [];

                _.forEach(selectedClasses, (classe) => {
                    _.map(classe.eleves.all, (eleve) => {
                        $scope.allElevesClasses.push(eleve);
                    });
                    periodes = _.union(periodes, classe.periodes.all);
                });
                $scope.filteredPeriodes = [];
                _.forEach(periodes, (periode) => {
                    if(periode.id_type !== undefined) {
                        if (_.where($scope.filteredPeriodes, {id_type: periode.id_type}).length === 0) {

                            $scope.filteredPeriodes.push({id_type: periode.id_type, periode: periode});
                        }
                    }
                });

                if ($scope.selected.periode !== undefined) {
                    $scope.selected.periode = _.findWhere($scope.filteredPeriodes, {
                        id_type: $scope.selected.periode.id_type
                    });
                }

                utils.safeApply($scope);
            }
        };
    }
]);