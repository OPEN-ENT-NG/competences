import {_, ng, notify} from "entcore";
import {ExportBulletins} from "../models/common/ExportBulletins";
import * as utils from '../utils/teacher';
import {Classe} from "../models/teacher";


declare let $ : any;
declare let Chart: any;


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
            $scope.isForBulletin = true;
            $scope.selected = {
                periode : undefined
            };
            $("#imgStructure").change(function(){
                $scope.readURL(this, true);
            });

            /*
            $("#imgSignature").change(function(){
                $scope.readURL(this, false);
            });
            */
        };


        $scope.generateBulletin = async function (options){
            let selectedClasses = _.where($scope.printClasses.all, {selected : true});

            if (_.isEmpty(selectedClasses)) {
                notify.info('evaluations.choose.classe');
                return ;
            }
            if (!_.isEmpty(selectedClasses) && _.isEmpty($scope.filteredPeriodes)) {
                notify.info('evaluations.classes.are.not.initialized');
                return ;
            }

            if($scope.selected === undefined
                || $scope.selected.periode === undefined) {
                notify.info('evaluations.choose.periode');
                return ;
            }

            options.idPeriode = $scope.selected.periode.id_type;
            options.students = _.filter($scope.allElevesClasses, function (student) {
                return student.selected === true && _.contains($scope.selected.periode.classes, student.idClasse);
            });
            options.idStudents = _.pluck(options.students, 'id');

            if (_.where($scope.allElevesClasses, {selected: true}).length === 0) {
                notify.info('evaluations.choose.student');
                return ;
            }
            if (_.isEmpty(options.idStudents)){
                notify.info('evaluations.choose.student.for.periode');
                return ;
            }
            $scope.opened.displayMessageLoader = true;

            let disableMessageLoader = ()=> {
                $scope.opened.displayMessageLoader = false;
                utils.safeApply($scope);
            };

            utils.safeApply($scope);
            let classes = _.groupBy($scope.allElevesClasses, 'classeName');
            for ( let key in classes) {
                let val = classes[key];
                options.classeName = key;
                options.idStudents = _.pluck(_.filter(val, function (student) {
                    return student.selected === true && _.contains($scope.selected.periode.classes, student.idClasse);
                }), 'id');
                if(options.idStudents!== undefined && options.idStudents.length > 0){
                   try {
                       await ExportBulletins.generateBulletins(options, $scope);
                   }
                   catch (e) {
                      disableMessageLoader();
                   }
                }
            }
           disableMessageLoader();

        };

        $scope.chooseClasse = async function (classe) {
            classe.selected = !classe.selected;
            $scope.opened.displayMessageLoader = true;
            utils.safeApply($scope);
            if (classe.synchronized.periodes !== true) {
                await classe.periodes.sync();
            }
            if (classe.synchronized.eleves !== true) {
                await classe.eleves.sync();
            }
            await $scope.updateFilters();
            $scope.opened.displayMessageLoader = false;
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
                $scope.opened.displayMessageLoader = true;
                utils.safeApply($scope);
                await $scope.updateFilters();
                $scope.opened.displayMessageLoader = false;
                utils.safeApply($scope);
            }
            utils.safeApply($scope);
        };

        $scope.readURL = function(input, isStructure) {
            if (input.files && input.files[0]) {
                let reader = new FileReader();

                reader.onload = function (e) {
                    if(isStructure) {
                        $('#displayImgStructure').attr('src', reader.result);
                        $scope.print.imgStructure = reader.result;
                    }
                    else{
                        $('#displayImgSignature').attr('src', reader.result);
                        $scope.print.imgSignature = reader.result;
                    }
                };

                reader.readAsDataURL(input.files[0]);
            }
        };

        $scope.checkIfOneStudent = function () {
            let oneStudentSelected =  _.filter($scope.allElevesClasses, function (student) {
                return student.selected === true;
            });
            let show = oneStudentSelected !== undefined && oneStudentSelected.length === 1;
            if(!show) {
                $scope.print.showBilanPerDomaines = false;
            }
            return show;
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

                // synchronisation de toutes les périodes et les élèves des classes sélectionnées
                let allPromise = [];
                _.forEach(selectedClasses, (classe: Classe)=> {
                    allPromise.push( Promise.all([classe.periodes.sync(), classe.eleves.sync()]));
                });

                await Promise.all(allPromise);
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
                        let periodeToset = _.findWhere($scope.filteredPeriodes, {id_type: periode.id_type});
                        if (periodeToset === undefined) {
                            let classe = [];
                            classe.push(periode.id_classe);
                            $scope.filteredPeriodes.push(
                                {
                                    id_type: periode.id_type,
                                    periode: periode,
                                    classes: classe
                                });
                        }
                        else {
                            periodeToset.classes.push(periode.id_classe);
                        }
                    }
                });

                if ($scope.selected.periode !== undefined) {
                    $scope.selected.periode = _.findWhere($scope.filteredPeriodes, {
                        id_type: $scope.selected.periode.id_type
                    });
                }
                if (!_.isEmpty($scope.allElevesClasses)) {
                    $scope.allElevesClasses = _.sortBy($scope.allElevesClasses, function (eleve)  {
                        return eleve.lastName + ' ' + eleve.firstName;
                    })
                }
            }
        };
    }
]);