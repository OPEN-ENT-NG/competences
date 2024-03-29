import {_, ng, notify, idiom as lang, Me, template} from "entcore";
import {ExportBulletins} from "../models/common/ExportBulletins";
import * as utils from '../utils/teacher';
import {evaluations, Utils} from "../models/teacher";
import http from "axios";
import {ReportModelPrintExport} from "../models/teacher/ReportModelPrintExport";
import {ReportModelPrintExportServiceType} from "../services/type";
import {ReportModelPrintExportConstant} from "../constants";


declare let $ : any;
declare let Chart: any;


export let evalBulletinCtl = ng.controller('EvaluationsBulletinsController', [
    '$scope', 'ReportModelPrintExportService',
    function ($scope, ReportModelPrintExportService:ReportModelPrintExportServiceType) {


        $scope.updateMentionClass = false;
        $scope.updateOrientationOpinion = false;
        $scope.display = {
            bulletinAlert: false
        };
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
        $scope.initBulletin = async function () {
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
            await getPreferences();
            $scope.error = {};
            if(Utils.isNotNull($scope.opened)) {
                $scope.opened.coefficientConflict = false;
            } else {
                $scope.opened = {
                    coefficientConflict : false
                };
            }

            // Récupération du logo de l'établissement, de la signature et du nom du CE
            try {
                let models = await http.get(`/competences/matieres/models/${$scope.structure.id}`);
                $scope.models = {
                    all: models.data
                };
            }
            catch (e) {
                console.error(e);
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

        $scope.changeEtablissementBulletin = () => {
            let init = () => {
                $scope.initReferences();
                $scope.initBulletin();

                utils.safeApply($scope);
            };

            if (!evaluations.structure.isSynchronized) {
                evaluations.structure.sync().then(() => {
                    init();
                });
            } else {
                init();
            }
        }

        $scope.setImageStructure = async () => {
            await ExportBulletins.setImageStructure($scope.structure.id, $scope.print.imgStructure);
        };

        $scope.setInformationsCE = async () => {
            await ExportBulletins.setInformationsCE($scope.structure.id, $scope.print.imgSignature,
                $scope.print.nameCE);
        };

        function validForm( selectedClasses) {
            let isvalid = true;
            if (_.isEmpty(selectedClasses)) {
                notify.info('evaluations.choose.classe');
                isvalid = false;
            }
            if (!_.isEmpty(selectedClasses) && _.isEmpty($scope.filteredPeriodes)) {
                notify.info('evaluations.classes.are.not.initialized');
                isvalid = false;
            }

            if ($scope.selected === undefined || $scope.selected.periode === undefined) {
                notify.info('evaluations.choose.periode');
                isvalid = false;
            }
            if (_.where($scope.allElevesClasses, {selected: true}).length === 0) {
                notify.info('evaluations.choose.student');
                isvalid = false;
            }
            return isvalid;
        }

        function getOptions(options) {
            options.mentionOpinion = $scope.mentionClass;
            options.orientationOpinion = $scope.orientationOpinion;

            options.idPeriode = $scope.selected.periode.id_type;
            options.type = $scope.selected.periode.type;
            let students = _.filter($scope.allElevesClasses, function (student) {
                return student.selected === true && _.contains($scope.selected.periode.classes, student.idClasse);
            });
            options.idStudents = _.pluck(students, 'id');

            if (options.addOtherTeacher) {
                let otherTeacher = _.findWhere($scope.enseignants.all, {id: options.otherTeacherId});
                if (otherTeacher && otherTeacher.displayName) {
                    options.otherTeacherName = " : ";
                    if (otherTeacher.civility)
                        options.otherTeacherName += otherTeacher.civility;
                    let initial = " ";
                    if (otherTeacher.firstName && otherTeacher.firstName.length > 0) {
                        initial = " " + otherTeacher.firstName[0] + ". ";
                    }
                    options.otherTeacherName += initial;
                    if (otherTeacher.firstName)
                        options.otherTeacherName += otherTeacher.lastName;
                }
            }
        }

        $scope.generateBulletin = async function (options) {

            if (Me && Me.preferences) {
                if (Utils.isNull(Me.preferences.competences)) {
                    Me.preferences.competences = {};
                }

                Me.preferences.competences.printBulletin = Object.assign({}, $scope.print);
                delete Me.preferences.competences.printBulletin.students;
                await Me.savePreference('competences');
            }
            saveReportModel(userReportModel);
            let selectedClasses = _.where($scope.printClasses.all, {selected: true});

            //REWORK POSSIBLE : fonction renvoie un booleen pour stop + alert qui change
            if(!validForm(selectedClasses)){
                return ;
            }


            //TODO envoyer requête au back pour check
            getOptions(options);

            if (_.isEmpty(options.idStudents)) {
                notify.info('evaluations.choose.student.for.periode');
                return;
            }
            await runMessageLoader();
            let studentsToSend = []
            let studentSelected =  _.filter($scope.allElevesClasses, function (student) {
                return student.selected === true && _.contains($scope.selected.periode.classes, student.idClasse);
            });

            if(studentSelected != undefined && studentSelected.length > 0) {
                try {
                    studentSelected.forEach(student => {
                        let studentTemp = {
                            id:student.id,
                            idClasse:student.idClasse
                        }
                        studentsToSend.push(studentTemp)
                    });
                    let {status} = await ExportBulletins.checkBulletins(studentsToSend,$scope.selected.periode.id_type,$scope.structure.id);
                    if(status == 201){
                        $scope.optionsBulletins = options ;
                        $scope.display.bulletinAlert = true;
                        utils.safeApply($scope);
                        return
                    } else {
                        $scope.sendBulletins(options);
                    }

                } catch (e) {
                    await stopMessageLoader();
                }
            }

        };
        $scope.sendBulletins = async (options) =>{
            let classes = _.groupBy($scope.allElevesClasses, 'classeName');

            for (let classe in classes) {
                if (classes.hasOwnProperty(classe)) {
                    let val = classes[classe];
                    options.classeName = classe;
                    options.idStructure = $scope.structure.id;
                    if (val !== undefined && val.length > 0) {
                        options.idClasse = val[0].idClasse;
                        if (options.showBilanPerDomaines === true || !$scope.niveauCompetences) {
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
                        } catch (e) {
                            await stopMessageLoader();
                        }
                    }
                }
            }
            await stopMessageLoader();
        }

        $scope.cancelBulletinDuplicateForm=  async () =>{
            $scope.display.bulletinAlert = false;
            await stopMessageLoader();
        }

        $scope.validBulletinDuplicateForm=  async () =>{
            $scope.display.bulletinAlert = false;
            //TODO plûtot appeller la boucle là
            await $scope.sendBulletins( $scope.optionsBulletins);
            $scope.optionsBulletins = {};
            await stopMessageLoader();
        }
        $scope.chooseClasse = async function (classe) {
            await Utils.chooseClasse(classe, $scope, true);
            utils.sortByLastnameWithAccentIgnored($scope.allElevesClasses);
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

        $scope.isLightBoxReportModelOpen = false;
        $scope.openLightBoxSelectModelReport = async function () {
            $scope.isLightBoxReportModelOpen = true;
        };

        $scope.closeLightBoxSelectModelReport = async function (reportModel = undefined) {
            if (reportModel) syncPreferences(reportModel);
            userReportModel = reportModel;
            $scope.isLightBoxReportModelOpen = false;
        };

        let defaultValueMentionClass: String, defaultValueOrientationOpinion: String;
        $scope.openEditLabel = function (label: string): void {
            $scope[label] = !$scope[label];
            defaultValueMentionClass = $scope.mentionClass;
            defaultValueOrientationOpinion = $scope.orientationOpinion;
        };

        $scope.closeEditLabel = function (label: string): void {
            $scope[label] = false;
            if ($scope.mentionClass === "") $scope.mentionClass = defaultValueMentionClass;
            if ($scope.orientationOpinion === "") $scope.orientationOpinion = defaultValueOrientationOpinion;
        };

        $scope.resetOpinions = () => {
            if (!$scope.print.orientationOpinion) {
                if ($scope.selected.periode) {
                    if ($scope.selected.periode.id_type === 5 || $scope.selected.periode.id_type === 2) {
                        $scope.orientationOpinion = lang.translate("orientation.avis.LastTrimester");
                    } else {
                        $scope.orientationOpinion = lang.translate("orientation.avis.FirstSecondTrimester");
                    }
                }
            } else {
                $scope.orientationOpinion = $scope.print.orientationOpinion;
            }
            $scope.mentionClass = $scope.print.mentionClass
                ? $scope.print.mentionClass
                : lang.translate("conseil.avis.mention");
        };

        $scope.reinitializeLabel = function (model: string): void {
            if (model === "mentionClass") $scope.mentionClass = lang.translate("conseil.avis.mention");
            if (model === "orientationOpinion") {
                if ($scope.selected.periode) {
                    if ($scope.selected.periode.id_type === 5 || $scope.selected.periode.id_type === 2) {
                        $scope.orientationOpinion = lang.translate("orientation.avis.LastTrimester");
                        return
                    }
                }
                $scope.orientationOpinion = lang.translate("orientation.avis.FirstSecondTrimester");
            }
        };

        let userReportModel: ReportModelPrintExport = new ReportModelPrintExport(undefined);

        async function getPreferences(): Promise<void> {
            const competences = await Me.preference('competences');
            const optionsPrintBulletin = (Utils.isNull(competences) ? undefined : competences.printBulletin);
            const idModelBulletinSelected =  (Utils.isNull(competences) ? undefined : competences.idModelBulletinSelected);
            $scope.print = (Utils.isNull(optionsPrintBulletin)) ? {} : optionsPrintBulletin;
            if(idModelBulletinSelected) {
                await getReportModelAllSelected(idModelBulletinSelected._id);
            } else {
                await syncPreferences(userReportModel);
            }
            utils.safeApply($scope);
        }

        async function getReportModelAllSelected(idModel : string): Promise<void> {
            userReportModel = await ReportModelPrintExportService.getModelSelected(idModel);
            if(userReportModel) await syncPreferences(userReportModel);

        }

        async function syncPreferencesText(): Promise<void> {
            $scope.resetOpinions();
            let infosStructure = await ExportBulletins.getInfosStructure($scope.structure.id);
            $scope.print.imgStructure || infosStructure.data.imgStructure.path;
            $scope.print.nameCE || infosStructure.data.nameAndBrad.name;
            $scope.print.imgSignature || infosStructure.data.nameAndBrad.path;
        }

        async function syncPreferences(reportModel: ReportModelPrintExport) {
            $scope.print = {
                ...$scope.print,
                ...reportModel.getPreferencesText(),
                ...reportModel.getPreferencesCheckbox()
            };

            await syncPreferencesText();
            utils.safeApply($scope);
        }

        const {
            KEY_PREFERENCES_CHECKBOX,
            KEY_PREFERENCES_TEXT,
        } = ReportModelPrintExportConstant;
        function saveReportModel(reportModel: ReportModelPrintExport): void {
            if (!reportModel) return;
            $scope.print.mentionClass = $scope.mentionClass;
            $scope.print.orientationOpinion = $scope.orientationOpinion;
            reportModel.setPreferencesCheckboxWithClean($scope.print);
            reportModel.setPreferencesTextWithClean($scope.print);
            reportModel.put([KEY_PREFERENCES_CHECKBOX, KEY_PREFERENCES_TEXT]);
        }
    }
]);