
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

import {ng, template, model, http, notify, Controller} from 'entcore';
import httpAxios from "axios";
import { SuiviCompetenceClasse, evaluations } from '../models/teacher';
import * as utils from '../utils/teacher';
import { Defaultcolors } from "../models/eval_niveau_comp";
import {Utils} from "../models/teacher/";
import { FilterNotEvaluated, FilterNotEvaluatedEnseignement
} from "../utils/filters/filterNotEvaluatedEnseignement";
import {updateColorAndLetterForSkills, updateNiveau} from "../models/common/Personnalisation";

declare let _: any;

export let evalSuiviCompetenceClasseCtl = ng.controller('EvalSuiviCompetenceClasseCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route', '$timeout',
    async function ($scope, route, $rootScope, $location, $filter, $route, $timeout) {

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
                await Utils.stopMessageLoader($scope);
                resolve();
            });
        }

        /**
         * Créer une suivi de compétence
         */
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
                // cas changement période (les élèves sont déjà chargés)
                if ($scope.informations.classe.eleves !== undefined && $scope.informations.classe.eleves.all.length > 0) {
                    $scope.suiviCompetence = new SuiviCompetenceClasse(
                        $scope.search.classe.filterEvaluableEleve($scope.search.periode)
                        , $scope.search.periode, $scope.structure);
                    await endSelectSuivi();
                } else {
                    // cas 1ère sélection de période : on attend le chargement des élèves avant de passer
                    // lal iste des élèves au constructeur SuiviCompetenceClasse
                    await $scope.informations.classe.eleves.sync();
                    $scope.suiviCompetence = new SuiviCompetenceClasse(
                        $scope.search.classe.filterEvaluableEleve($scope.search.periode)
                        , $scope.search.periode, $scope.structure);
                    await  endSelectSuivi();
                }
            } else {
                //cas 1ere entrée dans le suivi
                template.open('left-side', 'enseignants/suivi_competences_eleve/left_side');
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

        $scope.exportRecapEval = async (textMod, printSuiviClasse, idPeriode, exportByEnseignement,
                                        withMoyGeneraleByEleve, withMoyMinMaxByMat) => {
            switch (printSuiviClasse) {
                case 'printRecapAppreciations' : {
                    let url = "/competences/recapAppreciations/print/" + $scope.search.classe.id + "/export?text=" + !textMod;
                    if (idPeriode) {
                        url += "&idPeriode=" + idPeriode;
                    }
                    url += "&idStructure=" + $scope.structure.id;
                    http().getJson(url + "&json=true")
                        .error((result) => {
                            $scope.errorResult(result);
                            utils.safeApply($scope);
                        })
                        .done(() => {
                            delete $scope.recapEval;
                            $scope.opened.recapEval = false;
                            location.replace(url);
                            utils.safeApply($scope);
                        });
                    break;
                }
                case 'printRecapEval' : {
                    let url = "/competences/recapEval/print/" + $scope.search.classe.id + "/export?text=" + !textMod;
                    if (idPeriode) {
                        url += "&idPeriode=" + idPeriode;
                    }
                    http().getJson(url + "&json=true")
                        .error((result) => {
                            $scope.errorResult(result);
                            utils.safeApply($scope);
                        })
                        .done(() => {
                            delete $scope.recapEval;
                            $scope.opened.recapEval = false;
                            location.replace(url);
                            utils.safeApply($scope);
                        });
                    break;
                }
                case 'printReleveComp' : {
                    if ($scope.opened.recapEval) $scope.opened.recapEval = false;
                    await Utils.runMessageLoader($scope);

                    let url = "/competences/releveComp/print/export?text=" + !textMod;
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
                            await Utils.stopMessageLoader($scope);
                            $scope.opened.recapEval = true;
                            console.log(result);
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
                            $scope.errorResult(result);
                            utils.safeApply($scope);
                        })
                        .done((result) => {
                            delete $scope.recapEval;
                            $scope.opened.recapEval = false;
                            location.replace(url);
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

    }

]);
