
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
 * Created by ledunoiss on 27/10/2016.
 */

import {ng, template, model, http, notify, idiom as lang, $} from 'entcore';
import httpAxios from "axios";
import {SuiviCompetenceClasse, evaluations, Matiere} from '../models/teacher';
import * as utils from '../utils/teacher';
import { Defaultcolors } from "../models/eval_niveau_comp";
import {Utils} from "../models/teacher/";
import { FilterNotEvaluated, FilterNotEvaluatedEnseignement
} from "../utils/filters/filterNotEvaluatedEnseignement";
import {updateColorAndLetterForSkills, updateNiveau} from "../models/common/Personnalisation";
import {BilanPeriodique} from "../models/teacher/BilanPeriodique";
import {translate} from "../utils/teacher";

declare let _: any;

export let evalSuiviCompetenceClasseCtl = ng.controller('EvalSuiviCompetenceClasseCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route', '$timeout', '$sce',
    async function ($scope, route, $rootScope, $location, $filter, $route, $timeout,$sce) {

        /** --------------------------------------  Functions définies dans le scope  --------------        */

        $scope.hasMaxNotFormative = function (MaCompetence) {
            return Utils.hasMaxNotFormative(MaCompetence, $scope);
        };

        // mouseenter event
        $scope.showIt = function (item) {
            $scope.timer = $timeout(function () {
                item.hovering = true;
            }, 350);
        };

        // mouseleave event
        $scope.hideIt = function (item) {
            $timeout.cancel($scope.timer);
            item.hovering = false;
        };

        async function endSelectSuivi() {

            return new Promise(async (resolve) => {
                updateColorAndLetterForSkills($scope, $location);

                // on met à jour le fil d'ariane
                let updatedUrl = '/competences/classe?idClasse=' + $scope.search.classe.id;
                if ($scope.search.periode)
                    updatedUrl += '&idPeriode=' + $scope.search.periode.id_type;

                $rootScope.$broadcast('change-params', updatedUrl);
                if ($scope.searchBilan.parDomaine === 'true') {
                    await $scope.suiviCompetence.domaines.sync();
                }
                if ($scope.searchBilan.parDomaine === 'false') {
                    await $scope.suiviCompetence.enseignements.sync();
                    $scope.suiviFilter.mine = 'false';
                }
                if ($scope.opened.detailCompetenceSuivi) {
                    if ($scope.detailCompetence !== undefined) {
                        $scope.detailCompetence = $scope.suiviCompetence.findCompetence($scope.detailCompetence.id);
                        if (!$scope.detailCompetence) $scope.backToSuivi();
                    } else {
                        $scope.backToSuivi();
                    }
                }

                // On stocke l'ensemble des élèves de la classe dan une MapsuiviCompetence.enseignements.sync
                let mapEleves = {};
                for (let i = 0; i < $scope.search.classe.eleves.all.length; i++) {
                    mapEleves[$scope.search.classe.eleves.all[i].id] = $scope.search.classe.eleves.all[i];
                }
                $scope.search.classe.mapEleves = mapEleves;
                await utils.safeApply($scope);
                if ($scope.displayFromEleve) delete $scope.displayFromEleve;

                template.close('suivi-competence-content');
                await utils.safeApply($scope);
                template.open('suivi-competence-content',
                    'enseignants/suivi_competences_classe/content_vue_suivi_classe');

                resolve();
            });
        }

        /**
         * Créer une suivi de compétence
         */
        $scope.displayFollowCompetencesClass = 'followItems';
        $scope.selectSuivi = async function (classeHasChange) {
            await Utils.runMessageLoader($scope);
            if (classeHasChange === true) {
                await $scope.syncPeriode($scope.search.classe.id);
            }
            if ($scope.search.classe.id_cycle === null) {
                await Utils.stopMessageLoader($scope);
                return;
            }
            $scope.Display = {EvaluatedCompetences: true};
            $scope.informations.classe = $scope.search.classe;
            if ($scope.informations.classe !== null && $scope.search.classe !== '' && $scope.search.classe !== '*') {
                if (!($scope.informations.classe.eleves !== undefined && $scope.informations.classe.eleves.all.length > 0)) {
                    await $scope.informations.classe.eleves.sync();
                }
                $scope.suiviCompetence = new SuiviCompetenceClasse(
                    $scope.search.classe.filterEvaluableEleve($scope.search.periode)
                    , $scope.search.periode, $scope.structure);
                $scope.isShowDownloadButton = true;
                cleanScopeTabs();
                await $scope.selectDisplayClassTabs($scope.displayFollowCompetencesClass);
                await Utils.stopMessageLoader($scope);
            } else {
                //cas 1ere entrée dans le suivi
                template.open('left-side', 'enseignants/suivi_eleve/left_side');
                await Utils.stopMessageLoader($scope);
            }
        };

        $scope.switchEtablissementSuivi = () => {
            delete $scope.suiviCompetence;
            delete $scope.informations.classe;
            $scope.changeEtablissement();
        };

        $scope.updateColorAndLetterForSkills = function () {
            updateColorAndLetterForSkills($scope, $location);
        };

        $scope.updateNiveau = function (usePerso) {
            updateNiveau(usePerso, $scope);
        };

        $scope.getMaxEvaluations = function (idEleve) {
            if ($scope.detailCompetence === undefined
                || $scope.detailCompetence === null) {
                return;
            }
            if ($scope.suiviFilter === undefined) $scope.initFilterMine();
            let evaluations = $scope.suiviFilter.mine == 'true'
                ? _.where($scope.detailCompetence.competencesEvaluations, {id_eleve: idEleve, owner: model.me.userId})
                : _.where($scope.detailCompetence.competencesEvaluations, {id_eleve: idEleve});
            if (evaluations.length > 0) {
                // filtre sur les competences prises dans le calcul
                evaluations = _.filter(evaluations, function (competence) {
                    return !competence.formative; // la competence doit être reliée à un devoir ayant un type non "formative"
                });
                return _.max(evaluations, function (evaluation) {
                    return evaluation.evaluation;
                });
            }
        };
        /**
         * Retourne la classe en fonction de l'évaluation obtenue pour la compétence donnée
         * @param eleveId identifiant de l'élève
         * @returns {String} Nom de la classe
         */
        $scope.getEvaluationResultColor = function (eleveId) {
            let evaluation = $scope.getMaxEvaluations(eleveId);
            if (evaluation !== -Infinity) {
                return $scope.mapCouleurs[evaluation.evaluation];
            }
        };

        $scope.DisplayEvaluable = function (eleve) {
            return eleve.isEvaluable($scope.search.periode);
        };

        $scope.FilterColor = function (item) {
            let evaluation = $scope.getMaxEvaluations(item.id);
            if (evaluation === undefined) {
                return;
            }
            else if (evaluation !== -Infinity) {
                return $scope.selected.colors[evaluation.evaluation + 1];
            }
        };

        /**
         * Retourne si l'utilisateur n'est pas le propriétaire de compétences
         * @param listeEvaluations Tableau d'évaluations de compétences
         * @returns {boolean} Retourne true si l'utilisateur n'est pas le propriétaire
         */
        $scope.notEvalutationOwner = function (listeEvaluations) {
            return Utils.notEvalutationOwner(listeEvaluations, $scope);
        };

        /**
         * Lance la séquence d'ouverture du détail d'une compétence
         * @param competence Compétence à ouvrir
         */
        $scope.openDetailCompetence = function (competence) {
            $scope.detailCompetence = competence;
            template.open("suivi-competence-detail", "enseignants/suivi_competences_classe/detail_vue_classe");
            utils.scrollTo('top');
        };

        /**
         * Lance la séquence de retour à la vue globale du suivi de compétence
         */
        $scope.backToSuivi = function () {
            template.close("suivi-competence-detail");
            $scope.opened.detailCompetenceSuivi = false;
            $scope.detailCompetence = null;
        };

        /**
         * Retourne si l'utilisateur est le propriétaire de l'évaluation
         * @param evaluation Evaluation à afficher
         * @returns {boolean} Retourne true si l'utilisateur est le propriétaire de l'évaluation
         */
        $scope.filterOwnerSuivi = function (evaluation) {
            if ($scope.suiviFilter === undefined) $scope.initFilterMine();
            if ($scope.suiviFilter.mine === 'false' || $scope.suiviFilter.mine === false) {
                return true;
            }
            return evaluation.owner === $scope.me.userId;
        };

        /**
         * Remplace l'élève recherché par le nouveau suite à l'incrémentation de l'index
         * @param num pas d'incrémentation. Peut être positif ou négatif
         */
        $scope.incrementClasse = async function (num) {
            $scope.Display = {EvaluatedCompetences: true};
            let index = _.findIndex(_.sortBy(_.sortBy($scope.classes.all, 'name'), 'type_groupe_libelle'), {id: $scope.search.classe.id});
            if (index !== -1 && (index + parseInt(num)) >= 0
                && (index + parseInt(num)) < $scope.classes.all.length) {
                $scope.search.classe = _.sortBy(_.sortBy($scope.classes.all, 'name'), 'type_groupe_libelle')[index + parseInt(num)];
                $scope.syncPeriode($scope.search.classe.id);
                $scope.search.periode = '*';
                $scope.synchronizeStudents($scope.search.classe.id);
                await $scope.selectSuivi(true);
                utils.safeApply($scope);
            }
        };

        $scope.hideArrow = function (num) {
            let index = _.findIndex(_.sortBy(_.sortBy($scope.classes.all, 'name'), 'type_groupe_libelle'), {id: $scope.search.classe.id});
            return !(index !== -1 && (index + parseInt(num)) >= 0
                && (index + parseInt(num)) < $scope.classes.all.length);
        };

        $scope.changeContent = async function () {
            if (template.containers['suivi-competence-content'] !== undefined) {
                let content = $scope.template.containers['suivi-competence-content']
                    .split('.html?hash=')[0].split('template/')[1];
                await Utils.runMessageLoader($scope);
                await $scope.selectSuivi($scope.route.current.$$route.originalPath);
                $scope.template.open('suivi-competence-content', content);
                await Utils.stopMessageLoader($scope);
            }
        };

        const launchDownloadInNewWindows = (url:string):void => {
            const link = document.createElement('a');
            link.href = (url);
            link.setAttribute("target", "_blank");
            document.body.appendChild(link);
            link.click();
        };

        let infoNameFileEnd;
        $scope.disabledExportFile = false;
        $scope.exportRecapEval = async (textMod, printSuiviClasse, idPeriode, exportByEnseignement,
                                        withMoyGeneraleByEleve, withMoyMinMaxByMat) => {
            infoNameFileEnd = `_${$scope.search.classe.name}`;
            $scope.errorWhenExportPdf = false;
            switch (printSuiviClasse) {
                case 'printRecapAppreciations' : {
                    let url = "/competences/recapAppreciations/print/" + $scope.search.classe.id + "/export?text=" + !textMod;
                    if (idPeriode) {
                        url += "&idPeriode=" + idPeriode;
                    }
                    url += "&idStructure=" + $scope.structure.id;
                    http().getJson(url + "&json=true")
                        .error((result) => {
                            $scope.errorWhenExportPdf = true;
                            $scope.errorResult(result);
                            utils.safeApply($scope);
                        })
                        .done(() => {
                            delete $scope.recapEval;
                            $scope.opened.recapEval = false;
                            launchDownloadInNewWindows(url);
                            utils.safeApply($scope);
                        });
                    break;
                }
                case 'printRecapEval' : {
                    let url = "/competences/recapEval/print/" + $scope.search.classe.id + "/export?text=" + !textMod
                        + "&usePerso=" + $scope.structure.usePerso;
                    if (idPeriode) {
                        url += "&idPeriode=" + idPeriode;
                    }
                    http().getJson(url + "&json=true")
                        .error((result) => {
                            $scope.errorWhenExportPdf = true;
                            $scope.errorResult(result);
                            utils.safeApply($scope);
                        })
                        .done(() => {
                            delete $scope.recapEval;
                            $scope.opened.recapEval = false;
                            launchDownloadInNewWindows(url);
                            utils.safeApply($scope);
                        });
                    break;
                }
                case 'printReleveComp' : {
                    if ($scope.opened.recapEval) $scope.opened.recapEval = false;
                    await Utils.runMessageLoader($scope);

                    let url = "/competences/releveComp/print/export?text=" + !textMod
                        + "&usePerso=" + $scope.structure.usePerso;
                    url += "&idEtablissement=" + $scope.structure.id;

                    for (let m = 0; m < $scope.allMatieresSorted.length; m++) {
                        if ($scope.allMatieresSorted[m].select) {
                            url += "&idMatiere=" + $scope.allMatieresSorted[m].id;
                        }
                    }
                    if (idPeriode) {
                        url += "&idPeriode=" + idPeriode;
                    }
                    url += "&idClasse=" + $scope.search.classe.id;
                    url += "&byEnseignement=" + exportByEnseignement;
                    http().getJson(url + "&json=true")
                        .error(async (result) => {
                            $scope.errorWhenExportPdf = true;
                            await Utils.stopMessageLoader($scope);
                            $scope.opened.recapEval = true;
                            $scope.errorResult(result);
                            utils.safeApply($scope);
                        })
                        .done(async () => {

                            delete $scope.releveComp;
                            $scope.releveComp = {
                                textMod: true
                            };
                            await httpAxios.get(url, {responseType: 'arraybuffer'}).then(async (data) => {
                                let blob;
                                let link = document.createElement('a');
                                let response = data.data;
                                blob = new Blob([response]);
                                link = document.createElement('a');
                                link.href = window.URL.createObjectURL(blob);
                                link.download = data.headers['content-disposition'].split('filename=')[1];
                                document.body.appendChild(link);
                                link.click();
                                await Utils.stopMessageLoader($scope);
                                notify.success('evaluations.export.bulletin.success');
                            });

                        });

                    break;
                }
                case 'printTabMoys': {
                    let url = "/competences/suiviClasse/tableau/moyenne/" + $scope.search.classe.id;
                    url += "/export?withMoyGeneraleByEleve=" + withMoyGeneraleByEleve;
                    url += "&withMoyMinMaxByMat=" + withMoyMinMaxByMat + "&text=" + !textMod;
                    if (idPeriode) {
                        url += "&idPeriode=" + idPeriode;
                    }
                    http().getJson(url)
                        .error((result) => {
                            $scope.errorWhenExportPdf = true;
                            $scope.errorResult(result);
                            utils.safeApply($scope);
                        })
                        .done((result) => {
                            delete $scope.recapEval;
                            $scope.opened.recapEval = false;
                            launchDownloadInNewWindows(url);
                            utils.safeApply($scope);
                        });
                    break;
                }
                case 'printTabMoyPosAppr': {
                    $scope.exportMoyennesMatieres();
                    break;
                }

                case 'printClasseReleve': {

                    let type_periode = _.findWhere($scope.structure.typePeriodes.all, {id: idPeriode});
                    let idStructure = $scope.structure.id;
                    let idClasse = $scope.search.classe.id;
                    let classeName = $scope.search.classe.name;
                    $scope.opened.recapEval = false;
                    $scope.exportRecapEvalObj.errExport = false;
                    await Utils.runMessageLoader($scope);
                    try {
                        if (type_periode !== undefined) {
                            await Utils.getClasseReleve(idPeriode, idClasse, type_periode.type, type_periode.ordre,
                                idStructure, classeName);
                        }
                        else {
                            await Utils.getClasseReleve(undefined, $scope.search.classe.id, undefined, undefined,
                                idStructure, classeName);
                        }
                        await Utils.stopMessageLoader($scope);
                    }
                    catch (e) {
                        await Utils.stopMessageLoader($scope);
                    }
                    break;
                }
                case 'csvRecapEval': {
                    $scope.disabledExportFile = true;
                    utils.safeApply($scope);
                    if(await positioningCsvData(idPeriode, $scope.search.classe.id)){
                        await cvsLaunch('csv');
                        $scope.opened.recapEval = false
                    } else {
                        $scope.evalNotFound = true;
                        $timeout( () => $scope.evalNotFound = false, 1000);
                    }
                    $scope.disabledExportFile = false;
                    break;
                }
                case 'csvTableAverages': {
                    $scope.disabledExportFile = true;
                    utils.safeApply($scope);
                    $scope.opened.releveNoteTotaleChoice = "moy";
                    $scope.suiviClasse.withAppreciations =
                        $scope.suiviClasse.withAvisConseil =
                            $scope.suiviClasse.withAvisOrientation =
                                false;
                    await $scope.exportMoyennesMatieres();
                    $scope.disabledExportFile = false;
                    break;
                }
            }
            utils.safeApply($scope);
        };

        $scope.ClasseFilterNotEvaluated = function (MaCompetence) {
            if ($scope.Display.EvaluatedCompetences === true || ($scope.Display.EvaluatedCompetences === false && MaCompetence.masque)) {
                let _t = MaCompetence.competencesEvaluations;
                if ($scope.suiviFilter === undefined) $scope.initFilterMine();
                if ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true) {
                    _t = _.filter(MaCompetence.competencesEvaluations, function (evaluation) {
                        if (evaluation.owner !== undefined && evaluation.owner === $scope.me.userId)
                            return evaluation;
                    });
                }
                let EvaluatedOK = false;
                _.map(_t, function (competenceNote) {
                    if (competenceNote.evaluation != -1) {
                        EvaluatedOK = true;
                    }
                });
                return EvaluatedOK;
            } else {
                return true;
            }
        };

        $scope.FilterNotEvaluated = function (maCompetence) {
            return FilterNotEvaluated(maCompetence);
        };

        $scope.FilterNotEvaluatedEnseignement = function (monEnseignement) {
            return FilterNotEvaluatedEnseignement(monEnseignement, $scope.Display.EvaluatedCompetences);
        };

        $scope.initController = async () => {
            template.open('container', 'layouts/2_10_layout');

            // create the timer variable
            $scope.timer = undefined;


            $scope.searchBilan = {
                parDomaine: 'true'
            };

            //sélection de la classe du suivi
            if ($route.current.params.idClasse !== undefined) {
                if ($scope.classes !== undefined) {
                    $scope.search.classe = $scope.classes.findWhere({id: $route.current.params.idClasse});
                }
                else {
                    $scope.classes.sync();
                    $scope.classes.on('classes-sync', function () {
                        $scope.search.classe = $scope.classes.findWhere({id: $route.current.params.idClasse});
                        let niveauCompetence = _.findWhere(evaluations.structure.cycles, {
                            id_cycle: $scope.search.classe.id_cycle
                        });
                        if (niveauCompetence !== undefined) {
                            niveauCompetence = niveauCompetence.niveauCompetencesArray;
                        }
                        else {
                            niveauCompetence = evaluations.structure.cycles[0].niveauCompetencesArray;
                        }
                        $scope.mapCouleurs = {"-1": Defaultcolors.unevaluated};
                        $scope.mapLettres = {"-1": " "};
                        _.forEach(niveauCompetence, function (niv) {
                            $scope.mapCouleurs[niv.ordre - 1] = niv.couleur;
                            $scope.mapLettres[niv.ordre - 1] = niv.lettre;
                        });
                    });
                }
            }
            else {
                $scope.search.classe = "";
            }
            delete $scope.informations.eleve;
            $scope.route = $route;


            $scope.initFilterMine = function () {
                Utils.initFilterMine($scope);
            };

            $scope.initFilterMine();

            utils.safeApply($scope);
            $scope.pOFilterEval = {
                limitTo: 2
            };

            // Listener sur le template suivi-competence-detail permettant
            // la transition entre la vue détail et la vue globale
            template.watch("suivi-competence-detail", function () {
                if (!$scope.opened.detailCompetenceSuivi) {
                    $scope.opened.detailCompetenceSuivi = true;
                }
            });

            await $scope.selectSuivi($scope.route.current.$$route.originalPath);
            template.open('content', 'enseignants/suivi_competences_classe/content');
            await utils.safeApply($scope);
        };

        $scope.showEnseignementChoice = (parDomaine?) => {
            if (parDomaine !== undefined) {

                return parDomaine === 'false';
            }
            else {
                return true;
            }
        };
        /** --------------------------------------  Fin définition des fonctions usuelles  --------------        */

        const bilanPeriodic:BilanPeriodique = new BilanPeriodique($scope.search.periode, $scope.search.classe);
        const fileDownloadName:{pdf:string, csv:string} = {pdf:undefined, csv :undefined};
        let isManualCsvFromAngular:Boolean  = $scope.openLighBoxChosePdfCsv = $scope.loadingTab = $scope.isUseLinkForPdf = false;
        $scope.downloadContent = () => $scope.openedRecapEval();
        let dataBodyCsv:Array<Array<any>>;

        const openTemplateFollowCompetance = (nameOfPageHtml:string):void => {
            template.close('followCompetencesClass');
            template.open('followCompetencesClass', `enseignants/suivi_competences_classe/tabs_follow_competences_class/${nameOfPageHtml}`);
        };

        let urlPdf:string = undefined;
        const getPdfPositioning = async ():Promise<void> => {
            urlPdf = `/competences/recapEval/print/${$scope.search.classe.id}/export?text=false&usePerso=${$scope.structure.usePerso}`;
            if ($scope.search.periode.id_type) {
                urlPdf += `&idPeriode=${$scope.search.periode.id_type}`;
            }
            $scope.urlPdfSrc = urlPdf;
            $scope.contentIframe = await utils.getIframeFromPdfDownload(urlPdf, $sce);
            $scope.loadingTab = false;
        };

        const defaultFinallyDownload = (isExportFinish:boolean | undefined):void => {
            $scope.loadingTab = false;
            if(isExportFinish !== undefined){
                isExportFinish?
                    notify.success('evaluations.export.bulletin.success'):
                    notify.error('competance.information.noExport');
            }
        };

        const initResultPeriodic = ():any => {
            return {
                idEtablissement: evaluations.structure.id,
                idClasse: $scope.search.classe.id,
                idPeriode: $scope.search.periode.id_type,
                periodeName: null,
                periodes:[],
                exportOptions: {
                    appreciation:$scope.suiviClasse.withAppreciations,
                    averageFinal: $scope.suiviClasse.withMoyGeneraleByEleve,
                    statistiques: $scope.suiviClasse.withMoyMinMaxByMat,
                    positionnementFinal: $scope.opened.releveNoteTotaleChoice === "pos" || $scope.opened.releveNoteTotaleChoice === "moyPos",
                    moyenneMat: $scope.opened.releveNoteTotaleChoice === "moy" || $scope.opened.releveNoteTotaleChoice === "moyPos",
                    avisConseil: $scope.suiviClasse.withAvisConseil,
                    avisOrientation: $scope.suiviClasse.withAvisOrientation
                },
                idMatieres: $scope.matieres.all.filter((subject:Matiere):Boolean => subject.id).map((subject:Matiere):Boolean => subject.id),
                allMatieres: $scope.allMatieresSorted
            }
        };

        const getSubjectsNotesAppraisals = async ():Promise<void> => {
            const teacherBySubject = utils.getTeacherBySubject($scope.classes.all,
                $scope.search.classe.id,
                $scope.structure.enseignants.all);
            const dataSynthesisAndAppraisals:Array<any> = await bilanPeriodic.synthesisAndAppraisals( initResultPeriodic(), $scope, $scope.matieres.all);
            $scope.averagesClasses = await bilanPeriodic.getAverage(dataSynthesisAndAppraisals, teacherBySubject);
        };

        const cleanScopeTabs = () => {
            $scope.contentIframe = $scope.averagesClasses =  $scope.teacherNotesAndAppraisals = dataBodyCsv = undefined;
        };

        $scope.selectDisplayClassTabs = async (tabsSelected:string):Promise<void> => {
            $scope.displayFollowCompetencesClass = tabsSelected;
            $scope.loadingTab = true;
            $scope.isDataOnPage = $scope.errorWhenExportPdf = $scope.isUseLinkForPdf = false;
            infoNameFileEnd = `_${$scope.search.classe.name}`;
            switch ($scope.displayFollowCompetencesClass) {
                case ('followItems'):
                    await endSelectSuivi();
                    $scope.isDataOnPage = true;
                    fileDownloadName.pdf = undefined;
                    fileDownloadName.csv = undefined;
                    await initTabClass('follow_items');
                    break;
                case ('positioning'):
                    if(!$scope.contentIframe) await getPdfPositioning();
                    $scope.isDataOnPage = ($scope.contentIframe.status === 200
                        && $scope.displayFollowCompetencesClass === 'positioning');
                    fileDownloadName.pdf = 'printRecapEval';
                    $scope.isUseLinkForPdf = true;
                    isManualCsvFromAngular = true;
                    await initTabClass('positioning');
                    await Utils.stopMessageLoader($scope);
                    break;
                case ('average'):
                    if(!$scope.averagesClasses) await getSubjectsNotesAppraisals();
                    $scope.isDataOnPage = Object.keys($scope.averagesClasses)
                        .map( element => $scope.averagesClasses[element])
                        .some(array => array.length > 0);
                    isManualCsvFromAngular = false;
                    fileDownloadName.pdf = 'printTabMoys';
                    fileDownloadName.csv = 'printTabMoyPosAppr';
                    await initTabClass('average');
                    break;
                case ('teacherAppraisals'):
                    if(!$scope.teacherNotesAndAppraisals) $scope.teacherNotesAndAppraisals = await bilanPeriodic
                    .getAppraisalsAndNotesByClassAndPeriod($scope.search.classe.id, $scope.search.periode.id_type, evaluations.structure.id);
                    isManualCsvFromAngular = true;
                    fileDownloadName.pdf = 'printRecapAppreciations';
                    fileDownloadName.csv = `teacher_appraisals${infoNameFileEnd}`;
                    $scope.isDataOnPage = $scope.teacherNotesAndAppraisals.length !== 0;
                    await initTabClass('teacher_appraisals');
                    await Utils.stopMessageLoader($scope);
                    break;
                default:
                    defaultSwitch();
            }
            $scope.loadingTab = false;
            await utils.safeApply($scope);
            if($scope.displayFollowCompetencesClass == 'positioning')
                if(!dataBodyCsv)
                    await positioningCsvData($scope.search.periode.id_type, $scope.search.classe.id);
            else if ($scope.displayFollowCompetencesClass == 'teacherAppraisals')
                    if(!dataBodyCsv)
                        dataBodyCsv = prepareBodyAppraisalsForCsv($scope.dataHeaderAppraisals, $scope.teacherNotesAndAppraisals);
        };

        const positioningCsvData = async (idPeriod:number, idClass:string):Promise<boolean> => {
            try {
                isManualCsvFromAngular = true;
                fileDownloadName.csv = `positioning${infoNameFileEnd}`;
                const resultSummaryEvaluations = await bilanPeriodic.summaryEvaluations( idClass, idPeriod);
                dataBodyCsv = prepareBodyPositioningForCsv(resultSummaryEvaluations);
                return true
            } catch (e) {
                return false
            }
        };

        const initTabClass:Function = async (htmlTab:string):Promise<any> => {
            if($scope.isDataOnPage && htmlTab !== 'follow_items'){
                $scope.downloadContent = async function () {
                    $("#urlPdfSrc").on("click", () => {
                        $( '.close-2x' ).click ();
                    });
                    $scope.openLighBoxChosePdfCsv = true;
                };
            } else {
                $scope.downloadContent = () => $scope.openedRecapEval();
            }
            openTemplateFollowCompetance(htmlTab);
        };

        $scope.downloadFileWithType = async (formatType:string):Promise<void> => {
            $scope.openLighBoxChosePdfCsv = false;
            $scope.loadingTab = true;
            switch(formatType) {
                case 'csv':
                    await cvsLaunch(formatType);
                    break;
                case 'pdf':
                    await pdfLaunch(formatType);
                    break;
                default:
                    defaultSwitch();
            }
        };

        const defaultSwitch = ():void => {
            cleanScopeTabs();
            fileDownloadName.pdf = undefined;
            fileDownloadName.csv = undefined;
            $location.path('/competences');
        };

        const pdfLaunch  = async (formatType:string):Promise<void> => {
            let isExportFinish:boolean | undefined = undefined;
            try {
                await $scope.exportRecapEval(
                    $scope.suiviClasse.textMod,
                    fileDownloadName[formatType],
                    $scope.search.periode.id_type,
                    false,
                    true,
                    true
                );
            } catch (error) {
                isExportFinish = false;
            } finally {
                defaultFinallyDownload(isExportFinish);
            }
        };

        const cvsLaunch = async (formatType:string):Promise<void> => {
            let isExportFinish:boolean | undefined = undefined;
            try {
                if(isManualCsvFromAngular){
                    bilanPeriodic.makeCsv(fileDownloadName[formatType], makeHeaderCsv(), dataBodyCsv);
                    isExportFinish = true;
                } else {
                    $scope.suiviClasse.periode = $scope.search.periode;
                    $scope.opened.releveNoteTotaleChoice = "moy";
                    $scope.suiviClasse.withAppreciations =
                        $scope.suiviClasse.withAvisConseil =
                            $scope.suiviClasse.withAvisOrientation =
                                false;
                    await $scope.exportRecapEval(
                        $scope.suiviClasse.textMod,
                        fileDownloadName[formatType],
                        $scope.search.periode.id_type,
                        false,
                        true,
                        true
                    );
                    await Utils.stopMessageLoader($scope);
                }
            } catch (error) {
                isExportFinish = false;
            } finally {
                defaultFinallyDownload(isExportFinish);
            }
        };

        $scope.closeLightBox = ():void => {
            $scope.openLighBoxChosePdfCsv = false;
            $("#urlPdfSrc").off("click");
        };

        $scope.dataHeaderAppraisals = [
            lang.translate('matieres'),
            lang.translate('appreciation'),
            lang.translate('average.min'),
            lang.translate('classaverage.min'),
            lang.translate('classaverage.max'),
        ];

        const makeHeaderCsv = ():Array<Array<string>> => {
            return  [[
                `${lang.translate('evaluations.classe.groupe')}:`,
                ($scope.search.classe? $scope.search.classe.name : "")

            ], [
                `${lang.translate('viescolaire.periode.3')}:`,
                (`${lang.translate('viescolaire.utils.periode')}&nbsp;
                ${$scope.search.periode
                    ? $scope.search.periode.ordre || lang.translate("viescolaire.utils.annee") 
                    : ""}`)
            ]]
        };

        const emptyValue:string = "";
        const emptyRow:any = (defaultValue:boolean = false):Array<string> => defaultValue? new Array(emptyValue) : new Array(0);

        const prepareBodyAppraisalsForCsv = (dataHeader:Array<string | number>, dataBody:Array<any>):Array<Array<string | number>> => {
            let result:Array<Array<string | number>> = emptyRow();
            for (let rowIndex = 0; rowIndex < dataBody.length; rowIndex++) {
                let row:Array<string | number> = emptyRow();
                let rowBody = dataBody[rowIndex];
                let appraisal = rowBody.appraisal? rowBody.appraisal.content_text.replace(/\r?\n|\r/," ") : undefined;
                row.push(
                    `${rowBody.subjectName || emptyValue} - ${ rowBody.teacherName || emptyValue}`,
                    appraisal || emptyValue,
                    rowBody.average.moyenne || emptyValue,
                    rowBody.average.minimum || emptyValue,
                    rowBody.average.maximum || emptyValue,
                );
                result.push(row);
            }
            return [dataHeader, ...result];
        };

        const prepareBodyPositioningForCsv = (dataBody:any):Array<Array<string | number>> => {
            let result:Array<Array<string | number>> = emptyRow();
            let row:Array<string | number> = emptyRow(true);
            let footer:Array<Array<any>> = emptyRow();
            if(dataBody.domaines){
                for (let rowIndex = 0; rowIndex < dataBody.domaines.length; rowIndex++) {
                    let domaine = dataBody.domaines[rowIndex];
                    row.push(domaine.codification || emptyValue);
                    footer.push([[`${domaine.codification || ""} /${domaine.libelle}`]]);
                }
                result.push(row);
            }
            if(dataBody.eleves){
                for (let rowIndex = 0; rowIndex < dataBody.eleves.length; rowIndex++) {
                    row = emptyRow();
                    let student = dataBody.eleves[rowIndex];
                    let notesByStudent:Array<string> = emptyRow();
                    row.push(student.nom || emptyValue,);
                    if(student.notes){
                        for (let indexNote = 0; indexNote < student.notes.length; indexNote++) {
                            let note = student.notes [indexNote];
                            notesByStudent.push(note.moyenne || emptyValue);
                        }
                        row.push(...notesByStudent);
                    }
                    result.push(row);
                }
                result.push(emptyRow());
                result.push(...footer);
            }
            return result;
        };
    }
]);
