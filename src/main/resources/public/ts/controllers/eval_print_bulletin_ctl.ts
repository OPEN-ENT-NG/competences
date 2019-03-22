import {_, ng, notify, idiom as lang} from "entcore";
import {ExportBulletins} from "../models/common/ExportBulletins";
import * as utils from '../utils/teacher';
import {evaluations, Utils} from "../models/teacher";
import http from "axios";


declare let $ : any;
declare let Chart: any;


export let evalBulletinCtl = ng.controller('EvaluationsBulletinsController', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route', '$timeout',
    function ($scope) {

        let runMessageLoader = async function () {
           await Utils.runMessageLoader($scope);
        };

        let stopMessageLoader = async function(){
            await Utils.stopMessageLoader($scope);
        };

        let selectPersonnalisation = (id_cycle) => {

            if (evaluations.structure.cycle.id_cycle !== id_cycle) {
                $scope.niveauCompetences = $scope.selectCycleForView(id_cycle);
            }
        };

        // Fonction d'initialisation de la vue de l'export des bulletins
        $scope.initBulletin = async function ( ) {
            await runMessageLoader();

            // Initialisation des classes sélectionnables
            $scope.printClasses = {
                all : _.filter($scope.classes.all, (classe) => {
                    return classe.type_groupe === 0;
                })
            };
            _.forEach($scope.printClasses.all, (classe) => {
                classe.selected = false;
            });

            $scope.currentModel = undefined;
            $scope.print = {};

            // Récupération du logo de l'établissement, de la signature et du nom du CE
            try {
                let infosStructure = await ExportBulletins.getInfosStructure($scope.structure.id);

                $scope.print.imgStructure = infosStructure.data.imageStucture.path;
                $scope.print.nameCE = infosStructure.data.nameAndBrad.name;
                $scope.print.imgSignature = infosStructure.data.nameAndBrad.path;
                let models = await http.get(`/competences/matieres/models/${$scope.structure.id}`);
                $scope.models = {
                    all: models.data
                };
            }
            catch (e) {
                console.log(e);
                await stopMessageLoader();
            }
            $scope.filteredPeriodes = [];
            $scope.filterEleves =  [];
            $scope.allElevesClasses = [];
            $scope.isForBulletin = true;
            $scope.selected = {
                periode : undefined
            };
            $scope.lang = lang;
            await stopMessageLoader();
        };

        $scope.setImageStructure = async () => {
           await ExportBulletins.setImageStructure($scope.structure.id, $scope.print.imgStructure);
        };

        $scope.setInformationsCE = async () => {
            await ExportBulletins.setInformationsCE($scope.structure.id, $scope.print.imgSignature,
                $scope.print.nameCE);
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
            options.type = $scope.selected.periode.type;
            let students = _.filter($scope.allElevesClasses, function (student) {
                return student.selected === true && _.contains($scope.selected.periode.classes, student.idClasse);
            });
            options.idStudents = _.pluck(students, 'id');

            if (_.where($scope.allElevesClasses, {selected: true}).length === 0) {
                notify.info('evaluations.choose.student');
                return ;
            }
            if (_.isEmpty(options.idStudents)){
                notify.info('evaluations.choose.student.for.periode');
                return ;
            }
            await runMessageLoader();

            let classes = _.groupBy($scope.allElevesClasses, 'classeName');
            for ( let key in classes) {
                if (classes.hasOwnProperty(key)) {
                    let val = classes[key];
                    options.classeName = key;
                    options.idStructure = $scope.structure.id;
                    if (val !== undefined && val.length > 0) {
                        options.idClasse = val[0].idClasse;
                        if (options.showBilanPerDomaines === true) {
                            selectPersonnalisation(val[0].id_cycle);
                        }
                    }
                    options.students = _.filter(val, function (student) {
                        return student.selected === true && _.contains($scope.selected.periode.classes, student.idClasse);
                    });
                    options.idStudents = _.pluck(options.students, 'id');
                    if (options.idStudents !== undefined && options.idStudents.length > 0) {
                        try {

                            await ExportBulletins.generateBulletins(options, $scope);
                        }
                        catch (e) {
                            await stopMessageLoader();
                        }
                    }
                }
            }
           await stopMessageLoader();

        };

        $scope.chooseClasse = async function (classe) {
            await Utils.chooseClasse(classe,$scope, true);
            await utils.safeApply($scope);
        };

        $scope.chooseStudent = async function (student) {
            student.selected = !student.selected;
            await utils.safeApply($scope);
        };

        $scope.switchAll =  async function (collection , b, isClasse) {
            await Utils.switchAll(collection, b,isClasse, $scope, true);
            await utils.safeApply($scope);
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


        $scope.openModel = async function(model){
          $scope.opened.lightboxModel = true;
          $scope.currentModel = model;
          await utils.safeApply($scope);
        };
    }
]);