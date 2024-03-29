
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

import {$, Collection, http, idiom as lang, model, ng, notify, template} from 'entcore';
import httpAxios, {AxiosResponse} from "axios";
import {evaluations, IClassReport, Matiere, SuiviCompetenceClasse} from '../models/teacher';
import * as utils from '../utils/teacher';
import {Defaultcolors} from "../models/eval_niveau_comp";
import {Utils} from "../models/teacher/";
import {FilterNotEvaluated, FilterNotEvaluatedEnseignement} from "../utils/filters/filterNotEvaluatedEnseignement";
import {updateColorAndLetterForSkills, updateNiveau} from "../models/common/Personnalisation";
import {BilanPeriodique} from "../models/teacher/BilanPeriodique";
import {getTitulairesForRemplacantsCoEnseignant, translate} from "../utils/teacher";
import {structureOptionsService} from "../services";
import {Periode} from "../models/common/Periode";

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
                    $scope.suiviCompetence.domaines.all = [];
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
            $scope.closeWarningMessages();
            if (classeHasChange === true) {
                await $scope.syncPeriode($scope.search.classe.id);
                $scope.listTeacher = getTitulairesForRemplacantsCoEnseignant($scope.me.userId, $scope.search.classe);
            }
            if ($scope.search.classe && $scope.search.classe.id_cycle === null) {
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

        $scope.switchEtablissementSuivi = (): void => {
            // Angular 1.7.9 <select> now change the reference of our $scope evaluations.structure
            // We reassign the $scope with the ng-option element evaluations.structures.all selected in order to keep the same reference
            evaluations.structure = $scope.evaluations.structures.all.find(s => s.id ===  evaluations.structure.id);

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

        $scope.getMaxOrAverageEvaluations = (idEleve) : number => {
            if ($scope.detailCompetence === undefined
                || $scope.detailCompetence === null) {
                return;
            }
            if ($scope.suiviFilter === undefined) $scope.initFilterMine();
            let evals: Array<object>;
            evals = _.where($scope.detailCompetence.competencesEvaluations, {id_eleve: idEleve});
            if($scope.suiviFilter.mine == 'true') {
                evals = _.filter(evals, (evaluation : any) => {
                    return _.findWhere($scope.listTeacher,{id_enseignant : evaluation.owner, id_matiere : evaluation.id_matiere});
                });
            }
            if (evals.length > 0) {
                // filtre sur les competences prises dans le calcul
                evals = _.filter(evals, (competence: any) => {
                    return !competence.formative; // la competence doit être reliée à un devoir ayant un type non "formative"
                });
                return (evaluations.structure.options.isSkillAverage) ?
                    Utils.getNiveauMoyOfListEval(evals,$scope.suiviCompetence.tableauConversion, false,
                        (!$scope.search.periode.id_type ) ) :
                    Utils.getNiveauMaxOfListEval(evals,$scope.suiviCompetence.tableauConversion, false,
                    !$scope.search.periode.id_type);
            }
        };
        /**
         * Retourne la classe en fonction de l'évaluation obtenue pour la compétence donnée
         * @param eleveId identifiant de l'élève
         * @returns {String} Nom de la classe
         */
        $scope.getEvaluationResultColor =  function (eleveId) {
            let moyenneMaxMats = $scope.getMaxOrAverageEvaluations(eleveId);
            if (moyenneMaxMats !== -Infinity) {
                return $scope.mapCouleurs[moyenneMaxMats];
            }
        };

        $scope.DisplayEvaluable = function (eleve) {
            return eleve.isEvaluable($scope.search.periode);
        };

        $scope.FilterColor = (item) : number => {
            let moyenneMaxMats = $scope.getMaxOrAverageEvaluations(item.id);
            if (moyenneMaxMats === undefined) {
                return;
            }
            else if (moyenneMaxMats !== -Infinity) {
                return $scope.selected.colors[moyenneMaxMats + 1];
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
            return _.findWhere($scope.listTeacher, {id_enseignant : evaluation.owner, id_matiere : evaluation.id_matiere});
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
                $scope.search.periode = null;
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
                                        withMoyGeneraleByEleve, withMoyMinMaxByMat, classReport?: IClassReport) => {
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
                        + "&usePerso=" + $scope.structure.usePerso + "&structureId=" + $scope.structure.id;
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
                    url += `&withAppreciations=${!!$scope.suiviClasse.withAppreciations}`;
                    url += "&withMoyMinMaxByMat=" + withMoyMinMaxByMat + "&text=" + !textMod;
                    url += "&idEtablissement=" + $scope.structure.id;
                    url += "&typeGroupe=" + $scope.search.classe.type_groupe;
                    url += "&name=" + $scope.search.classe.name;
                    if (idPeriode) {
                        url += "&idPeriode=" + idPeriode;
                    }

                   await httpAxios.get(url, {responseType: 'arraybuffer'}).then ((data : AxiosResponse) => {
                       delete $scope.recapEval;
                       $scope.opened.recapEval = false;
                       Utils.downloadFile(data, document);
                   }).catch((error) => {
                       if (error.response.status === 400) {
                           error.responseText = String.fromCharCode.apply(null, new Uint8Array(error.response.data));
                           $scope.errorResult(error);
                           utils.safeApply($scope);
                           throw error;
                        }
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
                        await Utils.getClasseReleve(type_periode !== undefined ? idPeriode : undefined,
                            idClasse, type_periode.type, type_periode.ordre,
                            idStructure, classeName, classReport ? classReport.classReportUriOption : null);

                        await Utils.stopMessageLoader($scope);
                    } catch (e) {
                        await Utils.stopMessageLoader($scope);
                    }
                    break;
                }
                case 'csvRecapEval': {
                    $scope.disabledExportFile = true;
                    utils.safeApply($scope);
                    if(await positioningCsvData(idPeriode, $scope.search.classe.id, $scope.structure.id)){
                        await cvsLaunch('csv');
                        $scope.opened.recapEval = false
                    } else {
                        $scope.evalNotFound = true;
                        $timeout(() => $scope.evalNotFound = false, 1000);
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
                        if (evaluation.owner !== undefined &&
                            _.findWhere($scope.listTeacher,{id_enseignant : evaluation.owner, id_matiere : evaluation.id_matiere}))
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
                } else {
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
            } else {
                $scope.search.classe = null;
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
            urlPdf = `/competences/recapEval/print/${$scope.search.classe.id}/export?text=false&usePerso=${$scope.structure.usePerso}&structureId=${$scope.structure.id}`;
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
                if(isExportFinish) {
                    notify.success('evaluations.export.bulletin.success')
                } else {
                   ($scope.noScore) ? notify.error ('competence.class.followup.export.average.error.noScore') :
                    notify.error('competance.information.noExport');
                }
            }
            utils.safeApply($scope);
        };

        const initResultPeriodic = ():any => {
            return {
                idEtablissement: evaluations.structure.id,
                idClasse: $scope.search.classe.id,
                idPeriode: $scope.search.periode.id_type,
                periodeName: null,
                periodes: $scope.periodes.all.filter((period):Boolean => period.id_type).map((period):Number => period.id_type),
                exportOptions: {
                    appreciation:$scope.suiviClasse.withAppreciations,
                    averageFinal: $scope.suiviClasse.withMoyGeneraleByEleve,
                    statistiques: $scope.suiviClasse.withMoyMinMaxByMat,
                    positionnementFinal: $scope.opened.releveNoteTotaleChoice === "pos" || $scope.opened.releveNoteTotaleChoice === "moyPos",
                    moyenneMat: $scope.opened.releveNoteTotaleChoice === "moy" || $scope.opened.releveNoteTotaleChoice === "moyPos",
                    avisConseil: $scope.suiviClasse.withAvisConseil,
                    avisOrientation: $scope.suiviClasse.withAvisOrientation
                },
                idMatieres: $scope.allMatieresSorted.filter((subject:Matiere):Boolean => subject.id).map((subject:Matiere):Boolean => subject.id),
                allMatieres: $scope.allMatieresSorted
            }
        };

        const getSubjectsNotesAppraisals = async ():Promise<void> => {
            const teacherBySubject = utils.getTeacherBySubject($scope.classes.all, $scope.search.classe.id,
                $scope.structure.enseignants.all, $scope.search.periode);
            const dataSynthesisAndAppraisals : Array<any> = await bilanPeriodic.synthesisAndAppraisals(initResultPeriodic(), $scope);
            $scope.averagesClasses = await bilanPeriodic.getAverage(dataSynthesisAndAppraisals, teacherBySubject);
        };

        const cleanScopeTabs = () => {
            $scope.contentIframe = $scope.averagesClasses =  $scope.teacherNotesAndAppraisals = dataBodyCsv = undefined;
        };

        $scope.filterPeriodLightBox = (): (item: Periode) => boolean  => {
            if($scope.suiviClasse && $scope.suiviClasse.print == 'printRecapAppreciations')
            return (item: Periode) => {
                return item.id_type != null;
            };
        }

        $scope.filterPeriode = () : (item: Periode) => boolean => {
            if($scope.displayFollowCompetencesClass == 'teacherAppraisals') {
                return (item : Periode) => {
                    return item.id != null;
                };
            }
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
                    $scope.isDataOnPage = !!$scope.averagesClasses && [...$scope.averagesClasses.footerTable]
                        .some((elem: any) => elem.length >= 2 && elem[1] != "NN");
                    $scope.isDownloadWaiting = !$scope.isDataOnPage;
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
                await positioningCsvData($scope.search.periode.id_type, $scope.search.classe.id, $scope.structure.id);
            else if ($scope.displayFollowCompetencesClass == 'teacherAppraisals')
                dataBodyCsv = prepareBodyAppraisalsForCsv($scope.dataHeaderAppraisals, $scope.teacherNotesAndAppraisals);
        };

        const positioningCsvData = async (idPeriod:number, idClass:string, idStructure: string):Promise<boolean> => {
            try {
                isManualCsvFromAngular = true;
                fileDownloadName.csv = `positioning${infoNameFileEnd}`;
                const resultRecapEval = await bilanPeriodic.getExportRecapEval(idClass, idPeriod, idStructure);
                dataBodyCsv = prepareBodyPositioningForCsv(resultRecapEval);
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
                isExportFinish = true;
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
                    bilanPeriodic.makeCsv(fileDownloadName[formatType], dataBodyCsv);
                    isExportFinish = true;
                } else {
                    $scope.suiviClasse.periode = $scope.search.periode;
                    $scope.opened.releveNoteTotaleChoice = "moy";
                    $scope.suiviClasse.withAppreciations = $scope.suiviClasse.withAvisConseil =
                        $scope.suiviClasse.withAvisOrientation = false;
                    await $scope.exportRecapEval($scope.suiviClasse.textMod, fileDownloadName[formatType],
                        $scope.search.periode.id_type,false,
                        true,true);
                    await Utils.stopMessageLoader($scope);
                }
            } catch (error) {
                console.error(error);
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

        const emptyValue:string = "";
        const emptyRow:any = (defaultValue:boolean = false):Array<string> => defaultValue? new Array(emptyValue) : new Array(0);

        const prepareBodyAppraisalsForCsv = (dataHeader:Array<string | number>, dataBody:Array<any>):Array<Array<string | number>> => {
            let result:Array<Array<string | number>> = emptyRow();
            for (let rowIndex = 0; rowIndex < dataBody.length; rowIndex++) {
                let row:Array<string | number> = emptyRow();
                let rowBody = dataBody[rowIndex];
                let appraisal = rowBody.appraisal ? rowBody.appraisal.content_text.replace(/\r?\n|\r/," ") : undefined;
                let coTeachersContent = rowBody.coTeachers.length > 0 ? ", " + rowBody.coTeachers.join(', ') : emptyValue;
                row.push(
                    `${rowBody.subjectName || emptyValue} - ${ rowBody.teacherName || emptyValue}${coTeachersContent || emptyValue}`,
                    appraisal || emptyValue,
                    rowBody.average.moyenne || emptyValue,
                    rowBody.average.minimum || emptyValue,
                    rowBody.average.maximum || emptyValue,
                );
                result.push(row);
            }
            return [dataHeader, ...result];
        };

        const prepareBodyPositioningForCsv = (resultRecapEval:any):Array<Array<string | number>> => {
            let result:Array<Array<string | number>> = emptyRow();
            let row:Array<string | number> = emptyRow(true);
            let header:Array<Array<any>> = emptyRow();
            let footer:Array<Array<any>> = emptyRow();

            header.push([
                [`${lang.translate('evaluations.classe.groupe')} : `], [`${resultRecapEval.classe}`]
            ]);
            header.push([
                [`${lang.translate('viescolaire.utils.periode')} : `], [`${resultRecapEval.periode}`]
            ]);
            result.push(...header);

            if(resultRecapEval.domaines){
                for (let rowIndex = 0; rowIndex < resultRecapEval.domaines.length; rowIndex++) {
                    let domaine = resultRecapEval.domaines[rowIndex];
                    row.push(domaine.codification || emptyValue);
                    footer.push([[`${domaine.codification || ""} /${domaine.libelle}`]]);
                }
                result.push(row);
            }

            if(resultRecapEval.eleves){
                for (let rowIndex = 0; rowIndex < resultRecapEval.eleves.length; rowIndex++) {
                    row = emptyRow();
                    let student = resultRecapEval.eleves[rowIndex];
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
