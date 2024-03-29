import {notify, idiom as lang, ng, template, model, Behaviours, angular, toasts} from 'entcore';
import * as utils from '../utils/teacher';
import {ElementBilanPeriodique} from "../models/teacher/ElementBilanPeriodique";
import {BilanPeriodique} from "../models/teacher/BilanPeriodique";
import {Classe, evaluations, Periode, Utils} from "../models/teacher";
import {SyntheseBilanPeriodique} from "../models/teacher/SyntheseBilanPeriodique";
import {AppreciationCPE} from "../models/teacher/AppreciationCPE";
import {AvisConseil} from "../models/teacher/AvisConseil";
import {AvisOrientation} from "../models/teacher/AvisOrientation";
import {updateColorAndLetterForSkills, updateNiveau} from "../models/common/Personnalisation";
import {ComparisonGraph} from "../models/common/ComparisonGraph";
import {conseilGraphiques, conseilColumns, PreferencesUtils} from "../utils/preferences";
import {AppreciationSubjectPeriodStudent} from "../models/teacher/AppreciationSubjectPeriodStudent";
import {LengthLimit,Common} from "../constants";
import {DigitalSkills} from "../models/teacher/digital_skills/DigitalSkills";
import {isValidClasse} from "../utils/teacher";
import {ClassesService} from "../services/classes.service";


declare let _: any;

export let evalBilanPeriodiqueCtl = ng.controller('EvalBilanPeriodiqueCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route', '$timeout',
    async function ($scope, route, $rootScope, $location, $filter) {
        await PreferencesUtils.initPreference();
        template.close('suivi-acquis');
        template.close('projet');
        template.close('vie-scolaire');
        template.close('graphique');
        template.close('graphMatiere');
        template.close('graphDomaine');
        template.close('synthese');
        template.close('digital-skills');
        utils.safeApply($scope);

        let finSaisieBilan: boolean;
        $scope.critereIsEmpty = true;
        $scope.showAvisOrientation = false;
        $scope.showMoyGeneral = false;
        $scope.showHistorise = false;
        $scope.opened.criteres = true;
        $scope.opened.avis = true;
        $scope.opened.ensCplt = true;
        $scope.showGraphMatLoader = false;
        $scope.showGraphDomLoader = false;
        $scope.showDigitalSkills = false;
        $scope.selected = {suiviAcquis: true, projet: false, vieScolaire: false, graphique: false,
            bfc: false, digitalSkills: false};
        $scope.graphDom = {opened: false, comparison: false, darkness: true, infoGrouped: false};
        $scope.graphMat = {opened: false, comparison: false, darkness: true, infoGrouped: false};
        $scope.opened.bfcPeriode = undefined;
        $scope.opened.coefficientConflict = false;
        $scope.canLoadStudent = false;
        //init graph choices
        $scope.graph = {competences : true, notes : false, type: "baton", typeDom: "baton"};
        if(PreferencesUtils.isNotEmpty(conseilGraphiques)){
            $scope.graph = PreferencesUtils.getPreferences(conseilGraphiques);
        }
        $scope.showColumns = {moyEleve: true, moyClasse: true, pos: true, skillsValidatedPercentage: true};

        if(PreferencesUtils.isNotEmpty(conseilColumns)){
            $scope.showColumns = PreferencesUtils.getPreferences(conseilColumns);
        }
        $scope.MAX_LENGTH_600 = LengthLimit.MAX_600;
        $scope.MAX_LENGTH_300 = LengthLimit.MAX_300;

        $scope.showPopUpColumn = false;

        $scope.canSaisiSyntheseBilanPeriodique = () => {
            return (Utils.canSaisiSyntheseBilanPeriodique() || Utils.isChefEtabOrHeadTeacher($scope.search.classe))
                && finSaisieBilan;
        }

        $scope.canSaveAppMatierePosiBilanPeriodique = () => {
            return ((Utils.canSaveAppMatierePosiBilanPeriodique() && Utils.userHasService($scope.search.classe))
                    || Utils.isChefEtabOrHeadTeacher($scope.search.classe))
                && finSaisieBilan;
        }

        $scope.canUpdateAvisConseilOrientation = () => {
            return (Utils.canUpdateAvisConseilOrientation() || Utils.isChefEtabOrHeadTeacher($scope.search.classe))
                && finSaisieBilan;
        }

        $scope.canUpdateAppreciations = () => {
            return (Utils.canUpdateAppreciations() || Utils.isChefEtabOrHeadTeacher($scope.search.classe))
                && finSaisieBilan
        }

        $scope.canSaisiAppreciationCPE = () => {
            return Utils.canSaisiAppreciationCPE() && finSaisieBilan;
        }

        $scope.canSaveDigitalSkills = () => {
            return Utils.canSaveDigitalSkils() && finSaisieBilan;
        }

        $scope.canUpdateRetardAndAbsence = () => {
            return Utils.canUpdateRetardAndAbsence() && finSaisieBilan;
        }

        $scope.absencesRetardsFromPresences = () => {
            let fromPresences = false;
            if(!_.isEmpty($scope.search.eleve.evenements))
                if($scope.search.eleve.evenements[0].from_presences)
                    fromPresences = true;

            return fromPresences;
        }

        $scope.displayBilanPeriodique = () => {
            let isNotEmptyClasse = ($scope.search.classe !== '*' && $scope.search.classe !== null
                && $scope.search.classe !== undefined);
            let isNotEmptyStudent = ($scope.search.eleve !== '*' && $scope.search.eleve !== null
                && $scope.search.eleve !== undefined && $scope.search.eleve !== '');
            let isNotEmptyPeriode = ($scope.search.periode !== '*' && $scope.search.periode !== null
                && $scope.search.periode !== undefined);

            if (model.me.type === 'PERSRELELEVE') {
                $scope.critereIsEmpty = !(isNotEmptyPeriode);
            } else {
                $scope.canLoadStudent = isNotEmptyClasse && isNotEmptyPeriode && isNotEmptyStudent;
                $scope.critereIsEmpty = !(isNotEmptyClasse && isNotEmptyPeriode && isNotEmptyStudent);
            }
            let isClassAndCycle3 = isNotEmptyClasse ? $scope.search.classe.type_groupe === Common.TYPE_GROUP &&
                $scope.search.classe.id_cycle === Common.CYCLE_3 : false;
            $scope.showDigitalSkills = !$scope.critereIsEmpty && isClassAndCycle3;
            utils.safeApply($scope);
        };

        if (model.me.type === 'PERSRELELEVE') {
            template.open('left-menu', 'enseignants/bilan_periodique/left-side-parent-bilanperiodique');
            $scope.displayBilanPeriodique();
        } else {
            template.open('left-menu', 'enseignants/bilan_periodique/left-side-bilanperiodique');
            $scope.displayBilanPeriodique();
        }

        $scope.filterTrimestres = (item) => {
            return item.id_type > -1 || $scope.selected.bfc === true;
        };

        let closeTemplateButNot = (notThis) => {
            let allTemplate = ['suivi-acquis', 'vie-scolaire', 'graphique',
                'synthese', 'projet', 'bfc', 'digital-skills'];

            _.forEach(allTemplate, (_template) => {
                if (notThis instanceof Array) {
                    if (!_.contains(notThis, template) || _.isEmpty(notThis)) {
                        template.close(_template);
                    }
                } else {
                    if (notThis !== _template) {
                        template.close(_template);
                    }
                }
            });
        };

        //////    Onglets du bilan périodique   //////

        $scope.openSuiviAcquis = async () => {
            $scope.selected = {suiviAcquis: true, projet: false, vieScolaire: false, graphique: false, bfc: false};
            closeTemplateButNot('suivi-acquis');

            await $scope.syncPeriodesBilanPeriodique();
            $scope.displayBilanPeriodique();

            if($scope.elementBilanPeriodique === undefined){
                $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                    $scope.search.periode.id_type, $scope.structure, $scope.filteredPeriode);
            }

            /* Dans le cas où la période change automatiquement avec un changement d'onglet */
            $scope.elementBilanPeriodique.idPeriode = $scope.search.periode.id_type;
            $scope.elementBilanPeriodique.suivisAcquis.idPeriode = $scope.search.periode.id_type;
            let group: Classe = !!$scope.search && !!$scope.search.classe &&
            $scope.search.classe.type_groupe === Classe.type.GROUPE ? $scope.search.classe : null;
            await $scope.elementBilanPeriodique.suivisAcquis.getSuivisDesAcquis(group);

            await utils.safeApply($scope);
            template.open('suivi-acquis', 'enseignants/bilan_periodique/display_suivi_acquis');
            template.open('synthese', 'enseignants/bilan_periodique/display_synthese');
            await utils.safeApply($scope);
        };

        $scope.openProjet = async () => {
            $scope.selected = {projet: true};
            closeTemplateButNot('projet');

            await $scope.syncPeriodesBilanPeriodique();
            $scope.displayBilanPeriodique();

            await utils.safeApply($scope);
            template.open('projet', 'enseignants/bilan_periodique/display_projets');
            await $scope.getElementsBilanBilanPeriodique("isBilanPeriodique");
            await utils.safeApply($scope);
        };

        $scope.openVieScolaire = async function () {
            $scope.selected = {vieScolaire: true};
            closeTemplateButNot('vie-scolaire');

            await $scope.syncPeriodesBilanPeriodique();
            $scope.displayBilanPeriodique();

            if (_.isEmpty($scope.search.eleve.evenements)) {
                await $scope.search.eleve.getEvenements($scope.structure.id);
                await $scope.setHistoriqueEvenement();
            } else {
                $scope.search.eleve.evenement = _.findWhere($scope.search.eleve.evenements,
                    {id_periode: $scope.search.periode.id_type});
                $scope.elementBilanPeriodique.appreciationCPE = new AppreciationCPE($scope.informations.eleve.id,
                    $scope.search.periode.id_type, $scope.structure.id);
                await $scope.elementBilanPeriodique.appreciationCPE.syncAppreciationCPE();
                await utils.safeApply($scope);
                template.open('vie-scolaire', 'enseignants/bilan_periodique/display_vie_scolaire');
            }

            await utils.safeApply($scope);
        };

        $scope.openGraphique = async function () {
            try {
                $scope.selected = {graphique: true};
                closeTemplateButNot('graphique');

                await $scope.syncPeriodesBilanPeriodique();
                $scope.displayBilanPeriodique();

                await Utils.runMessageLoader($scope);
                if($scope.elementBilanPeriodique.syntheseBilanPeriodique === undefined){
                    $scope.elementBilanPeriodique.syntheseBilanPeriodique = new SyntheseBilanPeriodique(
                        $scope.informations.eleve.id, $scope.search.periode.id_type, $scope.structure.id, $scope.search.classe.id);
                    await $scope.elementBilanPeriodique.syntheseBilanPeriodique.syncSynthese();
                }
                await utils.safeApply($scope);
                template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
                template.open('synthese', 'enseignants/bilan_periodique/display_synthese');
                await Utils.stopMessageLoader($scope);
                if($scope.graphMat.opened){
                    $scope.openMatiere();
                }
                if($scope.graphDom.opened){
                    $scope.openDomaine();
                }
            } catch (e) {
                await Utils.stopMessageLoader($scope);
            }
        };

        $scope.openBFC = async function (bfcPeriode?) {
            await Utils.runMessageLoader($scope);
            $scope.selected = {bfc: false};
            closeTemplateButNot('bfc');

            try {
                if (bfcPeriode === undefined) {
                    let cycle = _.findWhere($scope.filteredPeriode, {isCycle: true});
                    let year = _.findWhere($scope.filteredPeriode, {isCycle: false});
                    if (year === undefined) {
                        $scope.filteredPeriode.push($scope.buildYear());
                    }
                    if (cycle === undefined) {
                        cycle = $scope.buildCycle();
                        $scope.filteredPeriode.push(cycle);
                    }
                    bfcPeriode = cycle;
                    $scope.search.periode = cycle;
                }

                $scope.opened.bfcPeriode = bfcPeriode;
                await utils.safeApply($scope);
                template.open('bfc', 'enseignants/suivi_eleve/content');
                $scope.selected = {bfc: true};
                await utils.safeApply($scope);
            } catch (e) {
                console.error(e);
                await Utils.stopMessageLoader($scope);
            }
        };

        $scope.openDigitalSkills = async () => {
            $scope.selected = {digitalSkills: true};
            closeTemplateButNot('digital-skills');

            await $scope.syncPeriodesBilanPeriodique();
            $scope.displayBilanPeriodique();

            $scope.digitalSkills = new DigitalSkills($scope.search, $scope.structure);
            await $scope.digitalSkills.sync();

            template.open('digital-skills', 'enseignants/bilan_periodique/diplay_digital_skills');
            await utils.safeApply($scope);
        }

        $scope.setHistoriqueEvenement = async function () {
            let classePeriodes = $scope.filteredPeriode;
            utils.setHistoriqueEvenement($scope, $scope.search.eleve, classePeriodes);

            $scope.elementBilanPeriodique.appreciationCPE = new AppreciationCPE($scope.informations.eleve.id,
                $scope.search.periode.id_type, $scope.structure.id);
            await $scope.elementBilanPeriodique.appreciationCPE.syncAppreciationCPE();
            await utils.safeApply($scope);
            template.open('vie-scolaire', 'enseignants/bilan_periodique/display_vie_scolaire');
            await utils.safeApply($scope);
        };

        $scope.buildCycle = function () {
            return {libelle: lang.translate('viescolaire.utils.cycle.tolower'), id: null, isCycle: true, id_type: -2};
        };

        $scope.buildYear = function () {
            return {libelle: lang.translate('viescolaire.utils.annee'), id: null, isCycle: false, id_type: -1};
        };

        $scope.switchOpenMatiere = async function () {
            $scope.graphMat.opened = !$scope.graphMat.opened;
            if($scope.graphMat.opened){
                $scope.openMatiere();
            }
        }

        $scope.savePreferences = function () {
            let arrayKeys = [], datasArray = [];
            datasArray.push($scope.graph);
            arrayKeys.push(conseilGraphiques);
            PreferencesUtils.savePreferences(arrayKeys, datasArray);
        };

        $scope.saveColumnsPreferences = function () {
            let arrayKeys = [], datasArray = [];
            datasArray.push($scope.showColumns);
            arrayKeys.push(conseilColumns);
            PreferencesUtils.savePreferences(arrayKeys, datasArray);
        };

        let subjectTemp;

        function initAppreciation(): void {
            subjectTemp = undefined;
            $scope.appreciationBackUp = undefined;
        }
        initAppreciation();

        $scope.setPreviousAppreciationSubject = function (subject) {
            subject.setPreviousAppreciationMatiere();
        };

        function preparedDataForAppreciation(subject):AppreciationSubjectPeriodStudent{
            const appreciationSubjectPeriodStudentPrepared = {
                idStudent: subject.idEleve,
                appreciation: subject.appreciationByClasse.appreciation,
                idStructure: subject.idEtablissement,
                idSubject: subject.id_matiere,
                idPeriod: subject.idPeriode,
                idClass: subject.appreciationByClasse.idClasse,
            }
            return new AppreciationSubjectPeriodStudent(appreciationSubjectPeriodStudentPrepared);
        }

        $scope.appreciationBlurCheck = function (subject): void {
            if($scope.opened.lightboxConfirmCleanAppreciation) {
                return;
            }
            $scope.appreciationSubjectPeriod = preparedDataForAppreciation(subject);
            if ($scope.appreciationSubjectPeriod.appreciation === undefined) {
                return;
            }
            if (subject.previousAppreciationMatiere === $scope.appreciationSubjectPeriod.appreciation) {
                return;
            } else if ($scope.appreciationSubjectPeriod.appreciation.length > 0) {
                if (subject.previousAppreciationMatiere) {
                    $scope.appreciationSubjectPeriod.put();
                } else {
                    $scope.appreciationSubjectPeriod.post();
                }
            } else {
                $scope.appreciationBackUp = subject.previousAppreciationMatiere;
                subjectTemp = subject;
                template.open('lightboxConfirmCleanAppreciation', '/enseignants/informations/lightbox_confirm_clean_appreciation');
                $scope.opened.lightboxConfirmCleanAppreciation = true;
            }
            utils.safeApply($scope);
        };

        $scope.deleteAppreciationSubjectStudent = async function (appreciationSubjectPeriod:AppreciationSubjectPeriodStudent):Promise<void> {
            let isDeleted:Boolean = false;
            try{
                await appreciationSubjectPeriod.delete();
                isDeleted = true;
            } catch(error) {
                console.error(error);
                isDeleted = false;
            } finally {
                $scope.closeLightboxConfirmCleanAppreciation(isDeleted);
            }
        };

        $scope.closeLightboxConfirmCleanAppreciation = async function (isDeleted: Boolean): Promise<void> {
            if (!isDeleted) subjectTemp.goBackAppreciation();
            $scope.opened.lightboxConfirmCleanAppreciation = false;
            template.close('lightboxConfirmCleanAppreciation');
            initAppreciation();
        };

        $scope.unlessOneChecked = function (checkboxClick){
            if(!($scope.graph.competences || $scope.graph.notes))
                if(checkboxClick == 'competences')
                    $scope.graph.competences = true;
                else
                    $scope.graph.notes = true;
        };

        //////            Graph de l'onglet graphique            //////
        $scope.openMatiere = async function () {
            template.close('graphMatiere');
            $scope.showGraphMatLoader = true;
            await utils.safeApply($scope);

            if(!$scope.informations.eleve.configMixedChart || !$scope.informations.eleve.configRadarChart){
                let promiseOpenMatiere = [];
                promiseOpenMatiere.push($scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, false,
                    $scope.niveauCompetences, $scope.search.periode.id_type));

                if ($scope.graphMat !== undefined && $scope.graphMat.comparison === true) {
                    promiseOpenMatiere.push($scope.drawComparison($scope.filteredPeriode, false, $scope.graphMat.darkness,
                        $scope.graphMat.infoGrouped, true));
                }
                await Promise.all(promiseOpenMatiere);
            }
            template.open('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject');
            $scope.showGraphMatLoader = false;
            await utils.safeApply($scope);
        };

        $scope.drawComparison = async function (periodes, forDomaine, withdarkness, tooltipGroup,
                                                withoutNotification?) {
            let conditionTocheck = {};
            let fieldToCheck = (forDomaine === true) ? 'comparisonDom' : 'comparison';
            conditionTocheck[fieldToCheck] = true;
            let selectedPeriodes = _.where(periodes, conditionTocheck);

            if (_.isEmpty(selectedPeriodes) || selectedPeriodes.length < 2) {
                if (withoutNotification !== true) {
                    notify.info('evaluations.choose.at.least.two.periode');
                }
                return;
            }

            if ($scope.informations.eleve === undefined) {
                if (withoutNotification !== true) {
                    notify.info('evaluations.choose.student.for.periode');
                }
                return;
            }

            let templateToSet = (forDomaine === true) ? 'comparisonGraphDom' : 'comparisonGraphMatiere';

            template.close(templateToSet);
            if(forDomaine) {
                $scope.graphDom.comparison = false;
                $scope.showGraphDomLoader = true;
            } else {
                $scope.graphMat.comparison = false;
                $scope.showGraphMatLoader = true;
            }
            try {
                await ComparisonGraph.buildComparisonGraph($scope.informations.eleve, selectedPeriodes,
                    evaluations.structure, forDomaine, $scope.niveauCompetences, withdarkness, tooltipGroup);
                let templateToOpen = forDomaine ? 'domaine' : 'subject';
                template.open(templateToSet, `enseignants/bilan_periodique/graph/comparison_graph_${templateToOpen}`);
            } catch (e) {}
            if(forDomaine) {
                $scope.graphDom.comparison = true;
                $scope.showGraphDomLoader = false;
            } else {
                $scope.graphMat.comparison = true;
                $scope.showGraphMatLoader = false;
            }
        };

        $scope.switchOpenDomaine = async function () {
            $scope.graphDom.opened = !$scope.graphDom.opened;
            if($scope.graphDom.opened){
                $scope.openDomaine();
            }
        };

        $scope.openDomaine = async function () {
            template.close('graphDomaine');
            $scope.showGraphDomLoader = true;
            await utils.safeApply($scope);
            if(!$scope.informations.eleve.configMixedChartDomaine || !$scope.informations.eleve.configRadarChartDomaine) {
                let promiseDomaine = [];
                if (template.contains('comparisonGraphDom', 'enseignants/bilan_periodique/graph/comparison_graph_domaine')) {
                    promiseDomaine.push($scope.drawComparison($scope.filteredPeriode, true, $scope.graphDom.darkness,
                        $scope.graphDom.infoGrouped, true));
                }
                promiseDomaine.push($scope.elementBilanPeriodique.getDataForGraph($scope.informations.eleve, true,
                    $scope.niveauCompetences, $scope.search.periode.id_type));
                await Promise.all(promiseDomaine);
            }
            template.open('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine');
            $scope.showGraphDomLoader = false;
            await utils.safeApply($scope);
        };

        //////      Changement d'élèves à l'aide des flèches         //////
        $scope.incrementEleve = async function (num) {
            let index = _.findIndex($scope.filteredEleves.all, {id: $scope.search.eleve.id});
            await Utils.runMessageLoader($scope);
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.filteredEleves.all.length) {
                $scope.search.eleve = $scope.filteredEleves.all[index + parseInt(num)];
                await $scope.changeContent();
                await utils.safeApply($scope);
            }
        };

        $scope.hideArrow = function (num) {
            let index = _.findIndex($scope.search.classe ? $scope.search.classe.eleves.all : undefined,
                {id: $scope.search.eleve ? $scope.search.eleve.id : undefined});
            return !(index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.search.classe.eleves.all.length);
        };

        $scope.changeContent = async function () {
            if ($scope.search.classe.eleves && $scope.search.classe.eleves.length() === 0) {
                await $scope.search.classe.eleves.sync();
            }
            let periode : Periode = ($scope.search.periode.id !== null) ?
                $scope.search.classe.periodes.all.find((p) => p.id_type == $scope.search.periode.id_type ): null
            $scope.filteredEleves = $scope.search.classe.filterEvaluableEleve(periode).eleves;
            if (!$scope.canLoadStudent) {
                return;
            }
            let allPromise = [$scope.getEleveInfo($scope.search.eleve)];
            if (!$scope.critereIsEmpty) {
                await $scope.updateColorAndLetterForSkills();
                if ($scope.selected.bfc !== true) {
                    finSaisieBilan = !_.find($scope.search.classe.periodes.all,
                        {id_type: $scope.search.periode.id_type}).publication_bulletin;
                }

                let periode = !!$scope.search.classe && !!$scope.search.classe.periodes ?
                    _.findWhere($scope.search.classe.periodes.all, {id_type: $scope.search.periode.id_type}) : [];
                if (Utils.isNotDefault($scope.search.eleve)) {
                    if(!$scope.search.eleve.isEvaluable(periode)){
                        notify.info('evaluations.student.is.no.more.evaluable');
                        $scope.search.eleve = null;
                        $scope.informations.eleve = $scope.search.eleve;
                        $scope.critereIsEmpty = true;
                        await utils.safeApply($scope);
                        return;
                    }
                }
                $scope.informations.eleve = $scope.search.eleve;

                $scope.informations.eleve.configMixedChartDomaine = $scope.informations.eleve.configRadarChartDomaine =
                    $scope.informations.eleve.configMixedChart = $scope.informations.eleve.configRadarChart = undefined;

                $scope.elementBilanPeriodique = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                    $scope.search.periode.id_type, $scope.structure, $scope.filteredPeriode);

                if ($scope.selected.graphique) {
                    allPromise.push($scope.openGraphique());
                }

                if ($scope.selected.suiviAcquis) {
                    await $scope.openSuiviAcquis();
                }

                if ($scope.selected.vieScolaire) {
                    await $scope.openVieScolaire();
                }

                if ($scope.selected.projet) {
                    await $scope.openProjet();
                }
                if ($scope.selected.bfc) {
                    await $scope.openBFC($scope.search.periode);
                }

                if ($scope.selected.digitalSkills){
                    await $scope.openDigitalSkills();
                }

                await Utils.awaitAndDisplay(allPromise, $scope, undefined, $scope.selected.bfc);

                await $scope.syncAllAvisSyntheses();

            }
            utils.safeApply($scope);
        };

        $scope.syncAllAvisSyntheses = async function() {
            $scope.oldElementsBilanPeriodique = [];
            $scope.elementBilanPeriodique.avisConseil = new AvisConseil($scope.informations.eleve ? $scope.informations.eleve.id : undefined,
                $scope.search.periode.id_type, $scope.structure.id);
            $scope.elementBilanPeriodique.avisOrientation = new AvisOrientation($scope.informations.eleve ? $scope.informations.eleve.id : undefined,
                $scope.search.periode.id_type, $scope.structure.id);
            $scope.elementBilanPeriodique.syntheseBilanPeriodique = new SyntheseBilanPeriodique($scope.informations.eleve ? $scope.informations.eleve.id : undefined,
                $scope.search.periode.id_type, $scope.structure.id, $scope.search.classe.id);

            const res = await $scope.elementBilanPeriodique.getAllAvisSyntheses();
            // Somehow $scope.elementBilanPeriodique.avisConseil.avis can be undefined so we encounter:
            // TypeError: Cannot set property 'avis' of undefined then we handle this error by setting empty object that will be set
            if (!$scope.elementBilanPeriodique.avisConseil) {
                $scope.elementBilanPeriodique.avisConseil = {};
            }
            $scope.elementBilanPeriodique.avisConseil.avis = res.libelleAvis;

            for (const periode of $scope.search.classe.periodes.all.sort((a, b) => (a.id_type > b.id_type) ? 1 : -1)) {
                if(periode.id_type != null){
                    let oldElement = new ElementBilanPeriodique($scope.search.classe, $scope.search.eleve,
                        periode, $scope.structure, $scope.filteredPeriode);
                    oldElement.syntheseBilanPeriodique = new SyntheseBilanPeriodique($scope.informations.eleve.id,
                        periode, $scope.structure.id, $scope.search.classe.id);
                    oldElement.avisConseil = new AvisConseil($scope.informations.eleve.id,
                        periode, $scope.structure.id);
                    oldElement.avisOrientation = new AvisOrientation($scope.informations.eleve.id,
                        periode, $scope.structure.id);
                    let synthesePeriode = _.findWhere(res.syntheses, {id_typeperiode : periode.id_type});
                    let avisConseilPeriode = _.findWhere(res.avisConseil, {id_periode : periode.id_type});
                    let avisOrientationPeriode = _.findWhere(res.avisOrientation, {id_periode : periode.id_type});
                    if(synthesePeriode)
                        oldElement.syntheseBilanPeriodique.synthese = synthesePeriode.synthese;
                    if(avisConseilPeriode)
                        oldElement.avisConseil.id_avis_conseil_bilan = avisConseilPeriode.id_avis_conseil_bilan;
                    if(avisOrientationPeriode)
                        oldElement.avisOrientation.id_avis_conseil_bilan = avisOrientationPeriode.id_avis_conseil_bilan;
                    $scope.oldElementsBilanPeriodique.push(oldElement);
                    if($scope.search.periode.id_type == periode.id_type){
                        if(synthesePeriode)
                            $scope.elementBilanPeriodique.syntheseBilanPeriodique.synthese = synthesePeriode.synthese;
                        if(avisConseilPeriode)
                            $scope.elementBilanPeriodique.avisConseil.id_avis_conseil_bilan = avisConseilPeriode.id_avis_conseil_bilan;
                        if(avisOrientationPeriode)
                            $scope.elementBilanPeriodique.avisOrientation.id_avis_conseil_bilan = avisOrientationPeriode.id_avis_conseil_bilan;
                    }
                }
            }

            $scope.search.avisClasse = _.find($scope.elementBilanPeriodique.avisConseil.avis,
                {id: $scope.elementBilanPeriodique.avisConseil.id_avis_conseil_bilan});
            $scope.previousClassOpinion = $scope.search.avisClasse;
            $scope.search.avisOrientation = _.find($scope.elementBilanPeriodique.avisConseil.avis,
                {id: $scope.elementBilanPeriodique.avisOrientation.id_avis_conseil_bilan});
            $scope.previousOrientationOpinion = $scope.search.avisOrientation;
        };

        $scope.deleteStudent = async function () {
            $scope.informations.eleve = null;
            $scope.search.eleve = null;
            $scope.critereIsEmpty = true;
            await $scope.changeContent();
        };

        $scope.filterAvis = function (param) {
            return (avis) => {
                if(param.some(x => x === avis.type_avis && avis.active)){
                    return avis;
                }
            }
        };

        /**
         * Saisir projet   -   Bilan Periodique
         */

        $scope.getElementsBilanBilanPeriodique = async function (param?) {
            if ($scope.bilanPeriodique !== undefined) {
                delete $scope.bilanPeriodique;
            }
            if (Utils.isNotDefault($scope.search.periode) && Utils.isNotDefault($scope.search.classe)) {
                if ($scope.search.classe.periodes.length() === 0) {
                    await $scope.search.classe.periodes.sync();
                }

                let periodesClasse = $scope.search.classe.periodes.all;
                let _p = _.findWhere(periodesClasse, {id_type: $scope.search.periode.id_type});
                _p = (Utils.isNull(_p)? _.findWhere(periodesClasse, {id_type: $scope.search.periode.id}) : _p);
                if (_p) {
                    if (!$scope.bilanPeriodique || param === "isClassChanging") {
                        $scope.bilanPeriodique = new BilanPeriodique($scope.search.periode, $scope.search.classe);
                    }

                    if (param === "isClassChanging" || $scope.bilanPeriodique.elements === undefined) {
                        if ($scope.search.classe.eleves.length() === 0) {
                            await $scope.search.classe.eleves.sync();
                        }
                        await $scope.bilanPeriodique.syncElements(param);
                    }

                    if ($scope.bilanPeriodique.elements !== undefined && $scope.bilanPeriodique.elements.length > 0) {
                        await $scope.bilanPeriodique.syncAppreciations($scope.bilanPeriodique.elements,
                            $scope.search.periode, $scope.search.classe, $scope.structure);
                        await utils.safeApply($scope);
                    } else {
                        delete $scope.bilanPeriodique;
                    }
                } else {
                    $scope.search.periode = null;
                    delete $scope.bilanPeriodique;
                }
            } else {
                delete $scope.bilanPeriodique;
            }

            if (param === "isClassChanging") {
                if ($scope.search.classe.periodes.length() === 0) {
                    await $scope.search.classe.periodes.sync();
                }
                let typesPeriodes = $scope.structure.typePeriodes.all;
                $scope.filteredPeriode = $filter('customClassPeriodeFilters')(typesPeriodes, $scope.search);

                if ($scope.selected.bfc === true) {
                    let cycle = _.findWhere($scope.filteredPeriode, {isCycle: true});
                    let year = _.findWhere($scope.filteredPeriode, {isCycle: false});
                    if (year === undefined) {
                        $scope.filteredPeriode.push($scope.buildYear());
                    }
                    if (cycle === undefined) {
                        $scope.filteredPeriode.push($scope.buildCycle());
                    }

                    if ($scope.search.periode === undefined) {
                        $scope.search.periode = _.findWhere($scope.filteredPeriode, {isCycle: true});
                    } else if(Utils.isNotDefault($scope.search.periode)) {
                        $scope.search.periode = _.findWhere($scope.filteredPeriode, {id: $scope.search.periode.id});
                    }
                }
            }

            if (param === "isBilanPeriodique" && $scope.bilanPeriodique !== undefined) {
                _.forEach($scope.bilanPeriodique.appreciations, (appreciation) => {
                    if ($scope.elementsDisplay === undefined) {
                        $scope.elementsDisplay = [_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique})];
                    }
                    else if (!_.findWhere($scope.elementsDisplay, {id: appreciation.id_elt_bilan_periodique})) {
                        $scope.elementsDisplay.push(_.findWhere($scope.bilanPeriodique.elements, {id: appreciation.id_elt_bilan_periodique}));
                    }
                });

                $scope.informations.eleve = _.findWhere($scope.bilanPeriodique.classe.eleves.all, {id: $scope.search.eleve.id});
            }
            if(Utils.isNotNull($scope.informations) && Utils.isNotNull($scope.informations.eleve)) {
                await $scope.getEleveInfo($scope.informations.eleve);
            }
            await utils.safeApply($scope);
        };


        /**
         * Séquence d'enregistrement d'une appréciation
         * @param element sur lequel est faite l'appréciation
         * @param eleve élève propriétaire de l'appréciation
         */

        $scope.saveAppElement = function (element, isBilanPeriodique, eleve?) {
            if (eleve) {
                if (Utils.isNotNull(eleve.appreciations) &&
                    Utils.isNotNull(eleve.appreciations[$scope.search.periode.id])) {
                    if (eleve.appreciations[$scope.search.periode.id][element.id] !== undefined) {
                        if (eleve.appreciations[$scope.search.periode.id][element.id].length <= $scope.MAX_LENGTH_600) {
                            $scope.bilanPeriodique.saveAppreciation($scope.search.periode, element, eleve, $scope.search.classe, isBilanPeriodique);
                        }
                        else {
                            notify.error(lang.translate("error.char.outbound") +
                                $scope.MAX_LENGTH_600);
                        }
                    }
                }
            } else {
                if (element.appreciationClasse[$scope.search.periode.id][$scope.search.classe.id] !== undefined) {
                    if (element.appreciationClasse[$scope.search.periode.id][$scope.search.classe.id].length <= $scope.MAX_LENGTH_600) {
                        $scope.bilanPeriodique.saveAppreciation($scope.search.periode, element, null,
                            $scope.search.classe, isBilanPeriodique);
                    }
                    else {
                        notify.error(lang.translate("error.char.outbound") +
                            $scope.MAX_LENGTH_600);
                    }
                }
            }
            utils.safeApply($scope);
        };


        $scope.filterElements = (type) => {
            return (item) => {
                if ($scope.selected.AP) {
                    return !item.theme;
                }
                else if ($scope.selected.parcours) {
                    return !item.libelle;
                }
                else if ($scope.selected.EPI) {
                    return item.theme && item.libelle;
                }
                else {
                    return item.type === type;
                }
            };
        };


        $scope.syncPeriodesBilanPeriodique = async function () {
            if (!!$scope.search.classe && $scope.search.classe.periodes &&
                $scope.search.classe.periodes.all.length === 0) {
                await $scope.search.classe.periodes.sync();
            }
            $scope.filteredPeriode = $filter('customClassPeriodeFilters')($scope.structure.typePeriodes.all, $scope.search);

            ($scope.structure.typePeriodes.all, $scope.search);
            if ($scope.selected.bfc === true) {
                let cycle = _.findWhere($scope.filteredPeriode, {isCycle: true});
                let year = _.findWhere($scope.filteredPeriode, {isCycle: false});
                if (year === undefined) {
                    $scope.filteredPeriode.push($scope.buildYear());
                }
                if (cycle === undefined) {
                    $scope.filteredPeriode.push($scope.buildCycle());
                }
                $scope.search.periode = _.findWhere($scope.filteredPeriode, {isCycle: true});
            } else {
                $scope.filteredPeriode = _.filter($scope.filteredPeriode, (periode) => {
                    return $scope.filterTrimestres(periode);
                });
                let periodeSelected = $scope.search.periode != null ?_.findWhere($scope.filteredPeriode,
                    {id_type: $scope.search.periode.id_type}) : undefined;
                if ($scope.search.periode === undefined || $scope.search.periode === "*" || periodeSelected === undefined) {
                    $scope.getCurrentPeriode($scope.search.classe).then((currentPeriode) => {
                        $scope.search.periode = currentPeriode !== null ? _.findWhere($scope.filteredPeriode,
                            {id_type: currentPeriode.id_type}) : null;
                        utils.safeApply($scope);
                    });
                }
            }
        };

        $scope.savePositionnementEleve = async (suiviDesAcquis, positionnement) => {
            await suiviDesAcquis.savePositionnementEleve(positionnement);
            utils.safeApply($scope);
        };

        $scope.openUpdateElementProgramme = (suiviDesAcquis) => {
            $scope.opened.lightboxUpdateElementProgramme = true;
            $scope.suiviDesAcquis = suiviDesAcquis;
            utils.safeApply($scope);
        }

        $scope.saveElementsProgramme = (elementProgrammeByClasse?) => {
            if($scope.suiviDesAcquis.elementsProgramme.length <= $scope.MAX_LENGTH_300){
                if(elementProgrammeByClasse != undefined) {
                    $scope.suiviDesAcquis.saveElementProgrammeByClasse(elementProgrammeByClasse);
                } else {
                    $scope.suiviDesAcquis.saveElementsProgramme();
                }
            } else {
                notify.error('evaluations.releve.elementProgramme.classe.max.length');
            }
        }

        $scope.getClasseLibelle = (idClasse) => {
            return _.find($scope.classes.all, {id: idClasse}).name;
        }

        $scope.setColonne = async function (colonne) {
            await $scope.search.eleve.setColonne(colonne, $scope.search.periode.id_type);
            await $scope.setHistoriqueEvenement();
            await utils.safeApply($scope);
        };

        $scope.updateNiveau = function (usePerso) {
            updateNiveau(usePerso, $scope);
        };

        $scope.updateColorAndLetterForSkills = async function () {
            updateColorAndLetterForSkills($scope, $location);
            let graphiqueClosed = false;
            let bfcClosed = false;
            if ($scope.selected !== undefined) {
                if ($scope.selected.graphique === true) {
                    await Utils.runMessageLoader($scope);
                    $scope.selected.graphique = false;
                    template.close('graphique');
                    graphiqueClosed = true;
                }
                if ($scope.selected.bfc === true) {
                    await Utils.runMessageLoader($scope);
                    $scope.selected.bfc = false;
                    template.close('bfc');
                    bfcClosed = true;
                }
            }
            await utils.safeApply($scope);
            if (graphiqueClosed) {
                $scope.selected.graphique = true;
                template.open('graphique', 'enseignants/bilan_periodique/display_graphiques');
                /*if (template.contains('graphMatiere', 'enseignants/bilan_periodique/graph/graph_subject')) {
                    $scope.openMatiere();
                }
                if (template.contains('graphDomaine', 'enseignants/bilan_periodique/graph/graph_domaine')) {
                    $scope.openDomaine();
                }*/
                await Utils.stopMessageLoader($scope);
            }
            if (bfcClosed) {
                $scope.selected.bfc = true;
                template.open('bfc', 'enseignants/suivi_eleve/content');
            }

            await utils.safeApply($scope);
        };
        $scope.$watch('selected', function (newValue, oldValue) {
            if ($scope.selected !== newValue) {
                angular.copy(newValue, $scope.selected);
            }
        });

        $scope.showNewOpinion = false;

        $scope.changeClassOpinion = function() {
            if($scope.search.avisClasse && $scope.search.avisClasse.type_avis === 0){
                $scope.showNewOpinion = true;
            }
            else {
                $scope.previousClassOpinion = $scope.search.avisClasse;
                if($scope.search.avisClasse)
                    $scope.elementBilanPeriodique.avisConseil.saveAvisConseil($scope.search.avisClasse.id);
                else
                    $scope.elementBilanPeriodique.avisConseil.saveAvisConseil(null);
            }
        };

        $scope.changeOrientationOpinion = function() {
            if($scope.search.avisOrientation && $scope.search.avisOrientation.type_avis === 0){
                $scope.showNewOpinion = true;
            }
            else {
                $scope.previousOrientationOpinion = $scope.search.avisOrientation;
                if($scope.search.avisOrientation)
                    $scope.elementBilanPeriodique.avisOrientation.saveAvisOrientation($scope.search.avisOrientation.id);
                else
                    $scope.elementBilanPeriodique.avisOrientation.saveAvisOrientation(null);
            }
        };

        $scope.createNewOpinion = async function (opinion) {
            if ($scope.search.avisClasse && $scope.search.avisClasse.type_avis === 0) {
                $scope.elementBilanPeriodique.avisConseil.id_avis_conseil_bilan =
                    await $scope.elementBilanPeriodique.avisConseil.createNewOpinion(opinion);
                await $scope.refreshOpinionList(opinion);
                await $scope.elementBilanPeriodique.avisConseil.saveAvisConseil($scope.search.avisClasse.id);
            } else if ($scope.search.avisOrientation && $scope.search.avisOrientation.type_avis === 0) {
                $scope.elementBilanPeriodique.avisOrientation.id_avis_conseil_bilan =
                    await $scope.elementBilanPeriodique.avisOrientation.createNewOpinion(opinion);
                await $scope.refreshOpinionList(opinion);
                $scope.elementBilanPeriodique.avisOrientation.saveAvisOrientation($scope.search.avisOrientation.id);
            }

            $scope.showNewOpinion = false;
            await utils.safeApply($scope);
        };

        $scope.refreshOpinionList = async function(opinion) {
            await $scope.elementBilanPeriodique.avisConseil.getLibelleAvis();
            $scope.search.avisClasse = _.find($scope.elementBilanPeriodique.avisConseil.avis,
                {id: $scope.elementBilanPeriodique.avisConseil.id_avis_conseil_bilan});
            if(!$scope.search.avisClasse)
                $scope.search.avisClasse = _.find($scope.elementBilanPeriodique.avisConseil.avis,
                    {libelle: opinion, type_avis:1});
            $scope.search.avisOrientation = _.find($scope.elementBilanPeriodique.avisConseil.avis,
                {id: $scope.elementBilanPeriodique.avisOrientation.id_avis_conseil_bilan});
            if(!$scope.search.avisOrientation)
                $scope.search.avisOrientation = _.find($scope.elementBilanPeriodique.avisConseil.avis,
                    {libelle: opinion, type_avis:2});
            $scope.previousClassOpinion = $scope.search.avisClasse;
            $scope.previousOrientationOpinion = $scope.search.avisOrientation;
        };

        $scope.closeNewOpinion = function() {
            if($scope.search.avisClasse.type_avis === 0){
                $scope.search.avisClasse = $scope.previousClassOpinion;
            }
            else if($scope.search.avisOrientation.type_avis === 0){
                $scope.search.avisOrientation = $scope.previousOrientationOpinion;
            }

            $scope.showNewOpinion = false;
        };

        async function changeStructurePeriodReport() {
            evaluations.structure = $scope.evaluations.structures.all.find(s => s.id ===  evaluations.structure.id);
            $scope.structure = evaluations.structure;
            $scope.opened.displayStructureLoader = true;
            try {
                await evaluations.structure.sync();
                $scope.classes = evaluations.structure.classes;
                if (Utils.isTeacher()) {
                    await evaluations.structure.syncAllClasses();
                    $scope.allClasses = evaluations.structure.allClasses;
                } else $scope.allClasses = evaluations.structure.classes.all;
                await initValidClasseGroups();
                delete $scope.informations.eleve;
            } catch (e) {
                toasts.warning('evaluations.export.etab.not.found.err');
                console.log(e);
            }
        }

        async function initValidClasseGroups(): Promise<void> {
            try {
                let audiences: any[] = [...await ClassesService.getClassesAndGroup($scope.structure.id)];
                audiences.forEach((audience: any) => {
                    let classe: Classe = $scope.allClasses.find((classe: Classe) => classe.id === audience.id_classe)
                    if (!!classe) classe.idGroups = audience.id_groupes;
                });
                $scope.filteredClassesGroups = $scope.allClasses
                    .filter((classe: Classe) => filterValidClasseGroups(classe))
            } catch (e) {
                toasts.warning('evaluations.classe.get.error');
                console.log(e);
            }
            $scope.opened.displayStructureLoader = false;
            utils.safeApply($scope);
        }

        function filterValidClasseGroups(item: Classe): boolean {
            return isValidClasse(item.id, item.id_matiere, $scope.allClasses) ||
                (item.type_groupe === Classe.type.CLASSE && item.idGroups &&
                    !!item.idGroups.find((groupId: string) => isValidClasse(groupId, item.id_matiere, $scope.allClasses)));
        }

        $scope.switchStructurePeriodReport = async (): Promise<void> => {
            $scope.critereIsEmpty = true;
            $scope.filteredClassesGroups = [];
            $scope.filteredPeriode = [];
            $scope.filteredEleves.all = [];
            $scope.search = {
                periode: null,
                classe: null
            };
            await changeStructurePeriodReport();
            utils.safeApply($scope);
        };

        $scope.switchClassePeriodReport = async (): Promise<void> => {
            await $scope.syncPeriodesBilanPeriodique();
            $scope.displayBilanPeriodique();
            await $scope.deleteStudent();
            utils.safeApply($scope);
        };

        $scope.switchPeriodPeriodReport = async (): Promise<void> => {
            $scope.displayBilanPeriodique();
            await $scope.changeContent();
            utils.safeApply($scope);
        };

        $scope.switchStudentPeriodReport = async (): Promise<void> => {
            $scope.displayBilanPeriodique();
            await $scope.changeContent();
            utils.safeApply($scope);
        };

        $scope.translateAvis = (avis) => {
            if(avis && $scope.elementBilanPeriodique.avisConseil){
                let result = _.find($scope.elementBilanPeriodique.avisConseil.avis, {id: avis.id_avis_conseil_bilan});
                if(result){
                    return result.libelle;
                }
            }
            return "";
        };
    }
]);
