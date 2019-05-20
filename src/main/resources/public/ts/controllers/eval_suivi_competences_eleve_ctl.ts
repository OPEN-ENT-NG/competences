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
import {ng, template, model, moment, idiom as lang} from "entcore";
import {
    SuiviCompetence, Devoir, CompetenceNote, evaluations, Structure, Classe, Eleve, Utils
} from "../models/teacher";
import * as utils from "../utils/teacher";
import {NiveauLangueCultReg, NiveauLangueCultRegs,BaremeBrevetEleve} from "../models/teacher/index";
import {Mix} from "entcore-toolkit";
import { FilterNotEvaluatedConnaissance,FilterNotEvaluatedEnseignement
} from "../utils/filters/filterNotEvaluatedEnseignement";
import {updateColorAndLetterForSkills, updateNiveau} from "../models/common/Personnalisation";



declare let _: any;
declare let Chart: any;
declare let location: any;
declare let $: any;

export let evalSuiviCompetenceEleveCtl = ng.controller('EvalSuiviCompetenceEleveCtl', [
    '$scope', 'route', '$rootScope', '$location', '$filter', '$route', '$timeout',
    async function ($scope, route, $rootScope, $location, $filter, $route, $timeout) {


        /**********************************************************************************************************
         *  FONCTION DU SCOPE
         *********************************************************************************************************/

        /**
         * show label too long
         */
            // create the timer variable
        let timer;

        // mouseenter event
        $scope.showIt = (item) => {
            timer = $timeout(function () {
                item.hovering = true;
            }, 350);
        };

        // mouseleave event
        $scope.hideIt = (item) => {
            $timeout.cancel(timer);
            item.hovering = false;
        };

        $scope.canSaveCompetenceNiveauFinal = () => {
            return Utils.canSaveCompetenceNiveauFinal();
        };

        $scope.canUpdateBFCSynthese = () => {
            return Utils.canUpdateBFCSynthese();
        };

        /**
         * Initialise d'une évaluation libre.
         */
        $scope.initEvaluationLibre = () => {
            let today = new Date();
            let evaluationLibre = new Devoir({
                date_publication: today,
                date: today,
                diviseur: 20,
                coefficient: 1,
                id_etablissement: $scope.evaluations.structure.id,
                ramener_sur: false,
                id_etat: 1,
                owner: model.me.userId,
                is_evaluated: false,
                id_classe: null,
                id_periode: $scope.search.periode.id_type,
                id_type: 1, // TODO modifier en optional foreign key
                id_matiere: "",
                id_sousmatiere: null,
                competences: [],
                controlledDate: true,
                matieres: [_.findWhere(evaluations.matieres.all, {idEtablissement: $scope.evaluations.structure.id})],
                sousmatiere: [],
            });

            let competenceEvaluee = new CompetenceNote({
                evaluation: -1,
                id_competence: $scope.detailCompetence.id,
                id_eleve: $scope.informations.eleve.id,
                owner: model.me.userId
            });
            evaluationLibre.competences.all.push($scope.detailCompetence.id);
            evaluationLibre.competenceEvaluee = competenceEvaluee;

            return evaluationLibre;
        };


        /**
         * Ouvre la fenêtre de création d'une évaluation libre
         */
        $scope.createEvaluationLibre = async () => {
            $scope.messages.successEvalLibre = false;
            $scope.evaluationLibre = $scope.initEvaluationLibre();

            if ($scope.search.classe && $scope.search.classe.periodes && $scope.search.classe.periodes.length() == 0) {
                await $scope.search.classe.periodes.sync();
            }
            $scope.evaluationLibre.periodes = $scope.search.classe.periodes.all;

            $scope.controleDate();
            $scope.opened.lightboxEvalLibre = true;
            template.open('lightboxContainerEvalLibre', 'enseignants/creation_devoir/display_creation_eval_libre');
        };

        /**
         * Evaluation de la compétence sur laquelle on est lors d'une évaluation libre
         */
        $scope.switchColor = function () {
            // recupération de la compétence (il n'y en a qu'une)
            var competenceEvaluee = $scope.evaluationLibre.competenceEvaluee;
            let niveauCompetenceMax = -1;
            for (let o in $scope.mapCouleurs) {
                niveauCompetenceMax++;
            }

            if (competenceEvaluee.evaluation === -1) {
                competenceEvaluee.evaluation = niveauCompetenceMax - 1;
            } else {
                competenceEvaluee.evaluation = competenceEvaluee.evaluation - 1;
            }
        };
        /**
         *  Sauvegarde d'une évaluation libre
         */
        $scope.saveNewEvaluationLibre = function () {
            $scope.evaluationLibre.date = $scope.getDateFormated($scope.evaluationLibre.dateDevoir);
            $scope.evaluationLibre.date_publication = $scope.getDateFormated($scope.evaluationLibre.datePublication);

            // fermeture popup
            $scope.opened.lightboxEvalLibre = false;

            // message de succes
            $scope.messages.successEvalLibre = true;
            $scope.evaluationLibre.create().then( async function () {

                // refresh du suivi élève
                $scope.suiviCompetence = new SuiviCompetence($scope.search.eleve, $scope.search.periode,
                    $scope.search.classe, $scope.currentCycle, false, $scope.evaluations.structure);

                $scope.suiviCompetence.sync().then( async () => {
                    if ($scope.searchBilan.parDomaine ===  'true') {
                        await $scope.suiviCompetence.domaines.sync();
                        $scope.suiviCompetence.setMoyenneCompetences($scope.suiviFilter.mine);
                        $scope.detailCompetence = $scope.suiviCompetence.findCompetence($scope.detailCompetence.id);
                        await utils.initChartsEval($scope);

                        await utils.safeApply($scope);
                    }
                    else if ($scope.searchBilan.parDomaine ===  'false') {
                        $scope.suiviFilter.mine = 'false';
                        await $scope.suiviCompetence.enseignements.sync();
                        await utils.safeApply($scope);
                    }
                });

                $scope.initSliderBFC();
                await utils.safeApply($scope);

            });
        };

        /**
         * Controle que la date de publication du devoir n'est pas inférieur à la date du devoir.
         * Et que la date de creation est comprise dans la période
         */
        $scope.controleDate = async () => {
            let idClasse = _.findWhere($scope.structure.eleves.all,
                {id: $scope.evaluationLibre.competenceEvaluee.id_eleve}).idClasse;
            if( ($scope.search.classe !== undefined) && (idClasse === null || idClasse === undefined)){
                idClasse = $scope.search.classe.id;
            }
            let classe = _.findWhere($scope.structure.classes.all, {id: idClasse});
            if (classe !== undefined && _.isEmpty(classe.periodes) && classe.periodes !== undefined) {
                await classe.periodes.sync();
            }
            let current_periode = _.findWhere(classe.periodes.all, {id_type: $scope.evaluationLibre.id_periode});

            let start_datePeriode = current_periode.timestamp_dt;
            let end_datePeriode = current_periode.timestamp_fn;
            let date_saisie = current_periode.date_fin_saisie;

            $scope.errDatePubliEvalFree = (moment($scope.evaluationLibre.datePublication).diff(moment($scope.evaluationLibre.dateDevoir), "days") < 0);
            $scope.errDateEvalFree = !(moment($scope.evaluationLibre.dateDevoir).isBetween(moment(start_datePeriode), moment(end_datePeriode), 'days', '[]'));
            $scope.endSaisieFree = moment($scope.evaluationLibre.dateDevoir).isAfter(moment(date_saisie), 'days', '[') || moment(new Date()).isAfter(moment(date_saisie), 'days', '[');

            $scope.evaluationLibre.controlledDate = !$scope.errDatePubliEvalFree && !$scope.errDateEvalFree && !$scope.endSaisieFree;
            await utils.safeApply($scope);
        };

        /**
         * Controle la validité du formulaire de création d'une évaluation libre.
         * @returns {boolean} Validité du formulaire
         */
        $scope.controleNewEvaluationLibreForm = function () {
            return $scope.evaluationLibre == undefined || !(
                $scope.evaluationLibre.controlledDate
                && $scope.evaluationLibre.id_matiere !== ""
                && $scope.evaluationLibre.id_matiere !== null
                && $scope.evaluationLibre.name !== undefined
                && $scope.evaluationLibre.id_periode !== null && $scope.evaluationLibre.id_periode !== undefined
                && $scope.evaluationLibre.competenceEvaluee.evaluation !== -1
            );
        };

        /**
         *
         */
        $scope.initFilterMine = function(){
            Utils.initFilterMine($scope);
        };


        /**
         *
         */
        $scope.refreshSlider = function () {
            $timeout(function () {
                $scope.$broadcast('rzSliderForceRender');
            });
        };
        /**
         * @returns {boolean | boolean}
         */
        $scope.isMyEvaluations = ( ) => {
            return ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true);
        };
        /**
         * modification et sauvegarde du niveau d'une compétence évaluée => niveau_final
         * @param competence
         */

        $scope.switchColorCompetenceNivFinal = async function(competence){
            if(!$scope.isEndSaisieNivFinal){
                let niveauCompetenceMin = 0;
                let niveauCompetenceMax = -1;
                for (let o in $scope.mapCouleurs) {
                    niveauCompetenceMax++;
                }

                if (niveauCompetenceMin <= competence.niveauFinalToShowMyEvaluations
                    && competence.niveauFinalToShowMyEvaluations < niveauCompetenceMax - 1) {
                    competence.niveauFinalToShowMyEvaluations = competence.niveauFinalToShowMyEvaluations + 1;
                } else {
                    competence.niveauFinalToShowMyEvaluations = niveauCompetenceMin;
                }
                let myEvaluations = _.filter(competence.competencesEvaluations, function (evaluation) {
                    return evaluation.owner !== undefined && evaluation.owner === model.me.userId && !evaluation.formative;
                });
                let competenceNiveauFinal = new CompetenceNote({
                    id_periode: $scope.search.periode.id_type,
                    id_eleve: $scope.search.eleve.id,
                    niveau_final: competence.niveauFinalToShowMyEvaluations,
                    id_competence: competence.id,
                    ids_matieres: _.unique(_.pluck(myEvaluations, 'id_matiere'))
                });

                _.each(competence.competencesEvaluations, (evaluation) => {
                    if (_.contains(competenceNiveauFinal.ids_matieres, evaluation.id_matiere)) {
                        evaluation.niveau_final = competence.niveauFinalToShowMyEvaluations;
                    }
                });
                await competenceNiveauFinal.saveNiveaufinal();
                Utils.setMaxCompetenceShow(competence, $scope.isCycle);
            }
        };

        /**
         * test pour checker si la moyenne ou la valeur du bfc est dans les bornes
         * de la table de conversion pour le bon libelle
         */
        $scope.hasValueInConversionTable = (domaine, Conversion, $index) => {
            return (domaine.moyenne !== -1 &&
                (($index !== 0 && domaine.moyenne >= Conversion.valmin && domaine.moyenne < Conversion.valmax) ||
                    ($index === 0 && domaine.moyenne >= Conversion.valmin && domaine.moyenne <= Conversion.valmax))) ||
                ((domaine.moyenne === -1 && domaine.bfc !== undefined) &&
                    (($index !== 0 && domaine.bfc.valeur >= Conversion.valmin && domaine.bfc.valeur < Conversion.valmax) ||
                        ($index === 0 && domaine.bfc.valeur >= Conversion.valmin && domaine.bfc.valeur <= Conversion.valmax)))
        };

        /**
         * Supprime un BFC créé par un chef d'établissement
         */
        $scope.deleteBFC = async function () {
            this.domaine.bfc.deleteBilanFinDeCycle().then(async (res) => {
                if (res.rows === 1) {
                    this.domaine.bfc = undefined;
                    // Récupération de la moyenne convertie
                    this.domaine.slider.value = utils.getMoyenneForBFC(this.domaine.moyenne,
                        $scope.suiviCompetence.tableConversions.all);
                }
                await utils.safeApply($scope);
            });
            await $scope.baremeBrevet();
        };

        $scope.switchEtablissementSuivi = () => {
            delete $scope.suiviCompetence;
            $scope.changeEtablissement();
        };

        $scope.updateNiveau = function (usePerso) {
            updateNiveau(usePerso, $scope);
        };

        $scope.updateColorAndLetterForSkills = async function () {
            updateColorAndLetterForSkills($scope, $location);
            await utils.initChartsEval($scope);
            if(template.contains('suivi-competence-content',
                    'enseignants/suivi_competences_eleve/content_vue_bilan_fin_cycle')){
                template.close('suivi-competence-content');
                await utils.safeApply($scope);
                await $scope.initSliderBFC();
                template.open('suivi-competence-content',
                    'enseignants/suivi_competences_eleve/content_vue_bilan_fin_cycle');
            }
            await utils.safeApply($scope);
        };

        /**
         *
         * @returns {Promise<void>}
         */
        $scope.getCyclesEleve = async () => {
            await $scope.search.eleve.getCycles();
            if($scope.search.eleve.cycles.length == 0) {
                $scope.currentCycle = {id_cycle: undefined, libelle: "Pas de cycle évalué"};
                $scope.selectedCycleRadio = {id_cycle: undefined};
            }else {
                $scope.currentCycle = _.findWhere($scope.search.eleve.cycles, {id_cycle: $scope.search.classe.id_cycle});
                $scope.selectedCycleRadio = {id_cycle: $scope.currentCycle.id_cycle};
            }
            await utils.safeApply($scope);
        };

        /**
         * Créer un suivi de compétence
         */
        $scope.selectSuivi = async function (classeHasChange) {
            return new Promise(async (resolve) => {
                if(classeHasChange === true){
                    if($scope.search.eleve !== undefined
                        && $scope.search.classe.eleves.findWhere({id: $scope.search.eleve.id}) === undefined){
                        $scope.search.eleve = "";
                    }
                    await $scope.syncPeriode($scope.search.classe.id);
                }
                $scope.selected.grey = true;
                if ($scope.search.classe.id_cycle === null) {
                    resolve();
                    return;
                }
                if ($scope.search.eleve !== undefined && $scope.search.classe.eleves.empty()) {
                    await $scope.search.classe.eleves.sync();
                }
                if ($scope.search.eleve !== undefined &&
                    $scope.search.classe.eleves.findWhere({id: $scope.search.eleve.id}) === undefined) {
                    $scope.search.eleve = "";
                    $scope.informations.eleve = $scope.search.eleve;
                    delete $scope.suiviCompetence;
                    await utils.safeApply($scope);
                    resolve();
                    return;
                }


                $scope.informations.eleve = $scope.search.eleve;
                if ($scope.informations.eleve !== null && $scope.search.eleve !== ""
                    && $scope.informations.eleve !== undefined) {

                    // Récupérer le suivi de l'élève
                    let eleveIsEvaluable = $scope.search.eleve.isEvaluable($scope.search.periode);
                    if (eleveIsEvaluable) {
                        if($scope.currentCycle === null || $scope.currentCycle === undefined)
                            await $scope.getCyclesEleve();

                        $scope.suiviCompetence = new SuiviCompetence($scope.search.eleve,
                            $scope.search.periode, $scope.search.classe, $scope.currentCycle,
                            $scope.isCycle, $scope.evaluations.structure);
                        await $scope.updateColorAndLetterForSkills();

                        //Enseignement de complement cycle 4
                        $scope.suiviCompetence.niveauLangueCultRegs = new NiveauLangueCultRegs();
                        await Promise.all([$scope.suiviCompetence.ensCpls.sync(),
                            $scope.suiviCompetence.niveauEnsCpls.sync(),
                            $scope.suiviCompetence.eleveEnsCpl.sync(),
                            $scope.suiviCompetence.langues.sync()]);
                        $scope.showButtonSave = true;
                        if ($scope.suiviCompetence.eleveEnsCpl.id) {
                            $scope.suiviCompetence.ensCplSelected = _.findWhere($scope.suiviCompetence.ensCpls.all,
                                {id: $scope.suiviCompetence.eleveEnsCpl.id_enscpl});
                            $scope.suiviCompetence.niveauEnsCplSelected = _.findWhere(
                                $scope.suiviCompetence.niveauEnsCpls.all, {id:
                                    $scope.suiviCompetence.eleveEnsCpl.id_niveau});
                            // si il y a une langue régionale de precisée, on la sélectionne
                            if ($scope.suiviCompetence.eleveEnsCpl.id_langue !== undefined) {
                                $scope.suiviCompetence.langueSelected = _.findWhere($scope.suiviCompetence.langues.all,
                                    {id: $scope.suiviCompetence.eleveEnsCpl.id_langue});
                                // sélection du niveau si renseigné
                                if ($scope.suiviCompetence.eleveEnsCpl.niveau_lcr !== undefined) {
                                    $scope.suiviCompetence.niveauLangueCultRegSelected = _.findWhere(
                                        $scope.suiviCompetence.niveauLangueCultRegs.all,
                                        {niveau: $scope.suiviCompetence.eleveEnsCpl.niveau_lcr});
                                }
                            }
                            await utils.safeApply($scope);
                        } else {
                            $scope.suiviCompetence.niveauEnsCplSelected = $scope.suiviCompetence.eleveEnsCpl;
                            await utils.safeApply($scope);
                        }

                        $scope.onChangeEns = () => {
                            // réinit des listes déroulantes concernant les langues régionales
                            $scope.suiviCompetence.langueSelected = undefined;
                            $scope.suiviCompetence.niveauLangueCultRegSelected = undefined;
                            $scope.onChangeObjectif();
                            //si id=1 on est sur ensCpl Aucun
                            if ($scope.suiviCompetence.ensCplSelected.id === 1) {
                                // on met à jour le niveau à 0
                                $scope.suiviCompetence.niveauEnsCplSelected = _.findWhere(
                                    $scope.suiviCompetence.niveauEnsCpls.all,
                                    {niveau: $scope.suiviCompetence.eleveEnsCpl.niveau});
                            } else {
                                //sinon on positionne sur le 1er niveau par défaut
                                $scope.suiviCompetence.niveauEnsCplSelected = _.findWhere(
                                    $scope.suiviCompetence.niveauEnsCpls.all,
                                    {niveau: $scope.suiviCompetence.niveauEnsCpls.niveau = 1});

                                // si l'enseignement sélectionné est avec le code LCR, alors, on affiche la liste déroulante
                                // de choix de la langue de culture régionale et on positionne sur la 1ère langue par défaut
                                if ($scope.suiviCompetence.ensCplSelected.code === 'LCR') {
                                    $scope.suiviCompetence.langueSelected = $scope.suiviCompetence.langues.all[0];
                                }

                            }
                        };
                        $scope.onChangeObjectif = () => {
                            ($scope.showButtonSave) ? $scope.showButtonSave = !$scope.showButtonSave :
                                $scope.showButtonSave = $scope.showButtonSave;
                        }

                        $scope.showSaveButton = () => {
                            let id_langue;
                            if ($scope.suiviCompetence.langueSelected !== undefined) {
                                id_langue = $scope.suiviCompetence.langueSelected.id;
                            }

                            let visible = $scope.suiviCompetence.ensCplSelected !== undefined &&
                                $scope.suiviCompetence.ensCplSelected.id !== undefined && // ense complement
                                $scope.suiviCompetence.niveauEnsCplSelected.niveau !== undefined && // avec un niveau
                                ($scope.suiviCompetence.langueSelected == undefined || // et pas de langue regionale
                                    ($scope.suiviCompetence.langueSelected !== undefined && // ou un langue avec le code AUC mais sans niveau
                                        $scope.suiviCompetence.langueSelected.code === 'AUC') ||
                                    ($scope.suiviCompetence.langueSelected !== undefined && // ou une langue avec un niveau
                                        $scope.suiviCompetence.niveauLangueCultRegSelected.niveau !== undefined)
                                );

                            return visible;
                        };


                        $scope.oncChangeLangue = () => {



                            $scope.onChangeObjectif();
                            if ($scope.suiviCompetence.langueSelected.code === 'AUC') {
                                // suppression du niveau
                                $scope.suiviCompetence.niveauLangueCultRegSelected = new NiveauLangueCultReg(0);
                            } else {
                                // sélection du 1er niveau par defaut
                                $scope.suiviCompetence.niveauLangueCultRegSelected = $scope.suiviCompetence.niveauLangueCultRegs.all[0];
                            }

                        };
                        let idTypePeriode = ($scope.suiviCompetence.periode.id !== null)?
                            $scope.suiviCompetence.periode.id_type : null;
                        let isCycle = ($scope.suiviCompetence.periode.libelle !== null
                            && $scope.suiviCompetence.periode.libelle !== undefined
                            && $scope.suiviCompetence.periode.id === null);
                        let idCycle = null;
                        if($scope.selectedCycleRadio!== undefined){
                            if ( $scope.selectedCycleRadio !== null) {
                                idCycle = $scope.selectedCycleRadio.id_cycle;
                            }
                            else {
                                idCycle = $scope.currentCycle.id_cycle;
                            }
                        }
                        $scope.suiviCompetence.bilanFinDeCycles.all = [];
                        $scope.suiviCompetence.domaines.all = [];
                        if(idCycle !== undefined) {
                            let allPromise = [$scope.suiviCompetence.sync(),
                                $scope.suiviCompetence.baremeBrevetEleves.sync($scope.suiviCompetence.classe.id,
                                    idTypePeriode, isCycle, idCycle),
                                $scope.suiviCompetence.bilanFinDeCycles.sync(), $scope.initSliderBFC()];
                            if ($scope.searchBilan.parDomaine === 'true') {
                                allPromise.push($scope.suiviCompetence.domaines.sync());
                            } else {
                                $scope.suiviFilter.mine = 'false';
                                allPromise.push($scope.suiviCompetence.enseignements.sync());
                            }
                            // On lance les synchronisation en paralelle
                            await Promise.all(allPromise);

                            // On récupère d'abord les bilans de fin de cycle enregistrés par le chef d'établissement
                            //on récupère la période en cours en fonction du type car quand il n'y a pas de période
                            // sélectionnée on a un type de période
                            $scope.suiviCompetence.baremeBrevetEleve = new BaremeBrevetEleve();
                            $scope.suiviCompetence.baremeBrevetEleve = Mix.castAs(BaremeBrevetEleve,
                                _.findWhere($scope.suiviCompetence.baremeBrevetEleves.all,
                                    {id_eleve: $scope.search.eleve.id}));

                            $scope.suiviCompetence.setMoyenneCompetences($scope.suiviFilter.mine);
                            $scope.suiviCompetence.on('refresh-slider', function () {
                                $scope.baremeBrevet();
                            });
                        }
                        if ($scope.opened.detailCompetenceSuivi) {
                            if ($scope.detailCompetence !== undefined) {
                                $scope.detailCompetence = $scope.suiviCompetence.findCompetence($scope.detailCompetence.id);
                                if ($scope.detailCompetence) {
                                    let detail = $scope.template.containers['suivi-competence-detail'];
                                    if (detail !== undefined) {
                                        detail = detail.split('.html?hash=')[0].split('template/')[1];
                                    }
                                    $scope.openDetailCompetence($scope.detailCompetence, detail);
                                } else {
                                    $scope.backToSuivi();
                                }
                            } else $scope.backToSuivi();
                        }

                        $scope.informations.eleve.suiviCompetences.push($scope.suiviCompetence);

                        $scope.template.close('suivi-competence-content');
                        await utils.safeApply($scope);
                        $scope.template.open('suivi-competence-content', 'enseignants/suivi_competences_eleve/content_vue_suivi_eleve');
                        if ($scope.displayFromClass) delete $scope.displayFromClass;
                        await utils.safeApply($scope);
                        resolve();
                    }
                    else {
                        delete $scope.suiviCompetence;
                        await utils.safeApply($scope);
                        resolve();
                        return;
                    }
                }
                else {
                    resolve();
                }
            });
        };

        $scope.initSliderBFC =  async function () {
            return new Promise( async resolve => {
                await $scope.suiviCompetence.getConversionTable($scope.evaluations.structure.id,
                    $scope.search.classe.id, $scope.mapCouleurs);
                resolve($scope.suiviCompetence.tableConversions);
            });
        };

        $scope.updateSuiviEleve = (Eleve) => {
            $scope.currentCycle = null;
            $scope.selected.grey = true;
            $scope.search.classe = _.findWhere(evaluations.classes.all, {'id': Eleve.idClasse});
            $scope.search.eleve = _.findWhere($scope.structure.eleves.all, {'id': Eleve.id});
            $scope.syncPeriode($scope.search.classe.id);
            //$scope.search.periode = '*';
            $scope.search.classe.eleves.sync().then(async function () {
                $scope.search.eleve = _.findWhere($scope.search.classe.eleves.all, {'id': Eleve.id});
                await $scope.selectSuivi($scope.route.current.$$route.originalPath);
                await utils.safeApply($scope);
            });
        };

        $scope.initSuivi = async () => {
            return new Promise( async (resolve, reject) => {
                try {
                    Utils.initFilterMine($scope);
                    $scope.opened.detailCompetenceSuivi = false;
                    $scope.pOFilterEval = {
                        limitTo: 2
                    };
                    $scope.successCreateSynthese = false;
                    $scope.successUpdateSynthese = false;
                    $scope.successUpdateEnseignement = false;
                    $scope.textPeriode = "Hors periode scolaire";
                    $scope.chartOptionsEval = {
                        series: ['Evaluation'],
                        tooltipLabels: [],
                        options: {
                            tooltips: {
                                callbacks: {
                                    label: function (tooltipItems, data) {
                                        return $scope.chartOptionsEval.tooltipLabels[tooltipItems.index];
                                    }
                                }
                            },
                            elements: {
                                point: {
                                    radius: 10,

                                },
                                line: {
                                    fill: false,
                                    borderDash: [0, 15]
                                }
                            },
                            maintainAspectRatio: false,
                            scales: {
                                responsive: true,
                                yAxes: [{
                                    gridLines: {
                                        display: false,
                                        color: '#000000'
                                    },
                                    pointRadius: 10,
                                    type: 'linear',
                                    display: true,
                                    ticks: {
                                        max: 6,
                                        min: 0,
                                        fontColor: 'black',
                                        stepSize: 1,
                                        padding: 20,
                                        callback: function (value, index, values) {
                                            if (value === 1) {
                                                return "Compétence non évaluée";
                                            }
                                            else if (value === 2) {
                                                return "Maîtrise insuffisante";
                                            }
                                            else if (value === 3) {
                                                return "Maîtrise fragile";
                                            }
                                            else if (value === 4) {
                                                return "Maîtrise satisfaisante";
                                            }
                                            else if (value === 5) {
                                                return "Très bonne maîtrise";
                                            }
                                            else {
                                                return " ";
                                            }
                                            // return parseFloat(value).toFixed(2) + '%';
                                        }
                                    },
                                }],
                                xAxes: [{
                                    type: 'category',
                                    display: true,
                                    responsive: false,
                                    gridLines: {
                                        display: false,
                                        offsetGridLines: false,
                                        color: '#000000'
                                    },
                                    ticks: {
                                        labelOffset: 30,
                                        minRotation: 20, // rotation des labels
                                        autoSkip: true,
                                        maxTicksLimit: 20,
                                        fontColor: 'black'
                                    }
                                }]
                            }
                        },
                        //les données des axes X: et Y:
                        datasets: {
                            labels: [],
                            data: []
                        },
                        //les couleurs des points
                        colors: []
                    };
                    $scope.selected.grey = true;
                    if ($scope.displayFromClass !== true) {
                        $scope.search.eleve = "";
                        $scope.search.classe = "";
                        delete $scope.informations.eleve;
                        delete $scope.suiviCompetence;
                    } else {
                        await $scope.selectSuivi($scope.route.current.$$route.originalPath);
                        $scope.displayFromEleve = true;
                        await utils.safeApply($scope);
                    }
                    resolve();
                } catch (e) {
                    reject(e);
                }
            });
        };

        $scope.exportBFC = (object, periode?) => {
            let url = "/competences/BFC/pdf?";
            if (object instanceof Structure) {
                url += "idStructure=" + object.id;
            } else if (object instanceof Classe) {
                url += "idClasse=" + object.id;
            } else if (object instanceof Eleve) {
                url += "idEleve=" + object.id + "&idEtablissement=" + $scope.structure.id;
            }
            if (periode && periode !== "*" && periode.id_type) {
                url += "&idPeriode=" + periode.id_type;
            } else if ($scope.suiviCompetence.isCycle === true) {
                url += "&idCycle="+$scope.suiviCompetence.cycle.id_cycle;
            }
            location.replace(url);
        };

        $scope.saveNiveauEnsCpl = async () => {
            let id_langue;
            if ($scope.suiviCompetence.langueSelected !== undefined) {
                id_langue = $scope.suiviCompetence.langueSelected.id;

                // si la langue culturelle choisie est aucun, on remet le niveau à 0 par pécaution
                if ($scope.suiviCompetence.langueSelected.code == 'AUC') {
                    $scope.suiviCompetence.niveauLangueCultRegSelected = new NiveauLangueCultReg(0);
                }
            } else {
                // si pas de langue culturelle choisie on remet le niveau à 0 par pécaution
                $scope.suiviCompetence.niveauLangueCultRegSelected = new NiveauLangueCultReg(0);
            }

            $scope.suiviCompetence.eleveEnsCpl.setAttributsEleveEnsCpl($scope.suiviCompetence.ensCplSelected.id,
                $scope.suiviCompetence.niveauEnsCplSelected.id,
                $scope.suiviCompetence.niveauLangueCultRegSelected.niveau,
                id_langue).save();
            $scope.showButtonSave = !$scope.showButtonSave;

            $scope.successUpdateEnseignement = true;
            await utils.safeApply($scope);
            $timeout(async () => {
                $scope.successUpdateEnseignement = false;
                await utils.safeApply($scope);
            }, 3000);
        };

        $scope.saveSynthese = () => {
            $scope.suiviCompetence.bfcSynthese.saveBfcSynthese().then(async (res) => {
                if (res.rows === 1) {
                    $scope.successUpdateSynthese = true;
                    await utils.safeApply($scope);
                    $timeout(async () => {
                        $scope.successUpdateSynthese = false;
                        await utils.safeApply($scope);
                    }, 3000);
                } else {
                    $scope.successCreateSynthese = true;
                    await utils.safeApply($scope);
                    $timeout(async () => {
                        $scope.successCreateSynthese = false;
                        await utils.safeApply($scope);
                    }, 3000);
                }
            });
        };
        /**
         * Lance la séquence d'ouverture du détail d'une compétence permettant d'accéder à la vue liste ou graph
         * @param competence Compétence à ouvrir
         */
        $scope.openDetailCompetence = async function (competence, detail) {
            $scope.detailCompetence = competence;
            await utils.initChartsEval($scope);
            if (detail !== undefined) {
                template.open("suivi-competence-detail", detail);
            }
            else {
                template.open("suivi-competence-detail",
                    "enseignants/suivi_competences_eleve/detail_vue_graph");
            }
            utils.scrollTo('top');
        };

        /**
         * Lance la séquence de retour à la vue globale du suivi de compétence
         */
        $scope.backToSuivi = function () {
            template.close("suivi-competence-detail");
            $scope.opened.detailCompetenceSuivi = false;
            $scope.detailCompetence = null;
            $scope.messages.successEvalLibre = false;
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

        $scope.EvaluationExiste = function (list) {
            let ListOfOwner = _.map(list, function (item) {
                if (item.owner === $scope.me.userId)
                    return item;
            });
            if (ListOfOwner.length === 0) {
                return true;
            } else {
                return false;
            }
        };

        /**
         * Remplace l'élève recherché par le nouveau suite à l'incrémentation de l'index
         * @param num pas d'incrémentation. Peut être positif ou négatif
         */
        $scope.incrementEleve = async function (num) {
            $scope.selected.grey = true;
            let index = _.findIndex($scope.search.classe.eleves.all, {id: $scope.search.eleve.id});
            if (index !== -1 && index + parseInt(num) >= 0
                && index + parseInt(num) < $scope.search.classe.eleves.all.length) {

                $scope.search.eleve = $scope.search.classe.eleves.all[index + parseInt(num)];
                await $scope.changeContent();
            }
        };


        $scope.changeContent = async function (cycle?) {
            return new Promise(async (resolve) => {
                if(template.containers['suivi-competence-content'] !== undefined) {
                    let content = $scope.template.containers['suivi-competence-content']
                        .split('.html?hash=')[0].split('template/')[1];

                    if (cycle === null || cycle === undefined) {
                        $scope.selectedCycleRadio = null;
                        if ($scope.search.periode.libelle === "cycle") {
                            $scope.currentCycle = null;
                            $scope.isCycle = true;
                            $scope.suiviFilter.mine = "false";
                        }
                        else {
                            Utils.initFilterMine($scope);
                            $scope.currentCycle = {id_cycle: $scope.search.classe.id_cycle};
                            $scope.isCycle = false;
                        }
                    }
                    else {
                        $scope.currentCycle = cycle;
                        $scope.isCycle = true;
                    }
                    Utils.runMessageLoader($scope);
                    $scope.template.close('suivi-competence-content');
                    await $scope.selectSuivi($scope.route.current.$$route.originalPath);
                    $scope.template.open('suivi-competence-content', content);
                    Utils.stopMessageLoader($scope);
                }
                resolve();
            });
        };

        /**
         *
         * @param refresh
         * @returns {Promise<any>}
         */
        $scope.refreshAndOpenBFC = async function (refresh?, init?) {
            return new Promise (async resolve => {
                try {
                    if (init === true) {
                        Utils.initFilterMine($scope);
                        await $scope.search.eleve.getCycles();
                        $scope.currentCycle = _.findWhere($scope.search.eleve.cycles,
                            {id_cycle: $scope.search.classe.id_cycle});
                        $scope.isCycle = true;
                        $scope.suiviCompetence = new SuiviCompetence($scope.search.eleve,
                            {libelle: "cycle", id: null}, $scope.search.classe, $scope.currentCycle,
                            $scope.isCycle, $scope.evaluations.structure);
                        $scope.selectedCycleRadio = $scope.currentCycle;
                        await Promise.all([$scope.suiviCompetence.bilanFinDeCycles.sync(),
                            $scope.updateColorAndLetterForSkills(), $scope.baremeBrevet()]);
                    }
                    await Utils.runMessageLoader($scope);
                    if ($scope.searchBilan.parDomaine === 'false' || refresh === true) {
                        $scope.searchBilan.parDomaine = 'true';
                        template.close('suivi-competence-content');
                        await utils.safeApply($scope);
                        await $scope.suiviCompetence.domaines.sync();
                        await $scope.initSliderBFC();
                    }
                    $scope.showRechercheBarFunction(false);
                    $scope.suiviCompetence.setMoyenneCompetences($scope.suiviFilter.mine);
                    template.open('suivi-competence-content',
                        'enseignants/suivi_competences_eleve/content_vue_bilan_fin_cycle');
                    await Utils.stopMessageLoader($scope);
                    resolve();
                }
                catch (e){
                    await Utils.stopMessageLoader($scope);
                    resolve();
                }

            });
        };

        $scope.removeBottomClass = function () {
            if($location.path() === '/conseil/de/classe' ){
                $('#bfc_dropdowns').removeClass("bottom");
            }
        };
        /**
         * MISE A JOUR POUR LA PRISE EN COMPTE DE LA PERSONNALISATION DES COULEURS DE COMPETENCES DANS LE GRAPHE
         */
        Chart.plugins.register({
            afterDatasetsDraw: function (chart, easing) {

                // Ne pas appliquer la personnalisation sur les graphs du relevé et sur le conseil de classe
                // et dans l'export de bulletin
                if ($scope.$location.$$path !== '/releve'
                    && $scope.$location.$$path !== '/conseil/de/classe'
                    && $scope.$location.$$path !== '/bulletin') {
                    // To only draw at the end of animation, check for easing === 1
                    let ctx = chart.chart.ctx;

                    chart.data.datasets.forEach(function (dataset, i) {
                        let meta = chart.getDatasetMeta(i);
                        if (!meta.hidden) {
                            meta.data.forEach(function (element, index) {
                                // Draw the text invert color of buble, with the specified font
                                let rgba = dataset.backgroundColor[index];
                                rgba = rgba.split('(')[1].split(')')[0].split(',');
                                let r = 255 - parseInt(rgba[0]);
                                let g = 255 - parseInt(rgba[1]);
                                let b = 255 - parseInt(rgba[2]);
                                let a = rgba[3];

                                ctx.fillStyle = "rgba(" + r.toString() + "," + g.toString() + ","
                                    + b.toString() + "," + a + ")";
                                let fontSize = 10.5;
                                let fontStyle = 'normal';
                                let fontFamily = 'Helvetica Neue';
                                ctx.font = Chart.helpers.fontString(fontSize, fontStyle, fontFamily);
                                // Just naively convert to string for now
                                let dataString = dataset.data[index].label;
                                // Make sure alignment settings are correct
                                ctx.textAlign = 'center';
                                ctx.textBaseline = 'middle';
                                //var padding = 5;
                                let position = element.tooltipPosition();
                                if (dataString === undefined) {
                                    dataString = " ";
                                }
                                ctx.fillText(dataString, position.x, position.y);

                            });
                        }
                    });
                }
            }
        });
        /**
         *
         */
        $scope.initChartsEval = async function () {
            utils.initChartsEval($scope);
            await utils.safeApply($scope);
        };

        $scope.saveDispenseEleve = async (domaine) => {
            //$scope.domaine = new Domaine(domaine);
            domaine.dispense_eleve = !domaine.dispense_eleve;
            await domaine.saveDispenseEleve();
            domaine.slider.options.disabled = !domaine.slider.options.disabled;
            domaine.slider.options.readOnly = !domaine.slider.options.readOnly;
            await $scope.baremeBrevet();

        };


        $scope.baremeBrevet = async() => {
            //on récupère la période en cours en fonction du type car quand il n'y a pas de période sélectionnée on a un type de période
            let idTypePeriode = ($scope.suiviCompetence.periode.id !== null) ? $scope.suiviCompetence.periode.id_type : null;
            let isCycle = ($scope.suiviCompetence.periode.libelle !== null
                && $scope.suiviCompetence.periode.libelle !== undefined
                && $scope.suiviCompetence.periode.id === null);
            let idCycle = null;
            if($scope.selectedCycleRadio!== undefined){
                if ( $scope.selectedCycleRadio !== null) {
                    idCycle = $scope.selectedCycleRadio.id_cycle;
                }
                else {
                    idCycle = $scope.currentCycle.id_cycle;
                }
            }
            await $scope.suiviCompetence.baremeBrevetEleves.sync($scope.suiviCompetence.classe.id,
                idTypePeriode, isCycle, idCycle);
            $scope.suiviCompetence.baremeBrevetEleve = _.findWhere($scope.suiviCompetence.baremeBrevetEleves.all, {id_eleve: $scope.search.eleve.id});
            await utils.safeApply($scope);
        };

        // Impression du releve de l'eleve
        $scope.getReleve = function() {
            let type_periode = _.findWhere($scope.structure.typePeriodes.all,
                {id: $scope.search.periode.id_type});
            if (type_periode !== undefined) {
                $scope.suiviCompetence.getReleve($scope.search.periode.id_type,
                    $scope.search.eleve.id, type_periode.type, type_periode.ordre);
            }
            else {
                $scope.suiviCompetence.getReleve(undefined,
                    $scope.search.eleve.id, undefined, undefined);
            }
        };

        $scope.isMaxEvaluation = function (listeEvaluations) {
            return Utils.isMaxEvaluation(listeEvaluations,$scope);
        };


        $scope.hasMaxNotFormative = function (MaCompetence) {
            return Utils.hasMaxNotFormative(MaCompetence, $scope);
        };


        /**
         * Retourne si l'utilisateur n'est pas le propriétaire de compétences
         * @param listeEvaluations Tableau d'évaluations de compétences
         * @returns {boolean} Retourne true si l'utilisateur n'est pas le propriétaire
         */
        $scope.notEvalutationOwner = function (listeEvaluations) {
            return Utils.hasMaxNotFormative(listeEvaluations, $scope);
        };


        $scope.FilterNotEvaluated = function (MaCompetence) {
            return Utils.FilterNotEvaluated(MaCompetence, $scope);
        };

        $scope.FilterNotEvaluatedEnseignement = function (monEnseignement) {
            return FilterNotEvaluatedEnseignement(monEnseignement, $scope.selected.grey);
        };

        $scope.FilterNotEvaluatedConnaissance = function (maConnaissance) {
            return FilterNotEvaluatedConnaissance(maConnaissance);
        };

        $scope.initView = async function () {
            if ($scope.searchBilan !== undefined  && $scope.searchBilan.parDomaine ===  'false') {
                $scope.searchBilan.parDomaine = 'true';
                await $scope.changeContent();
            }
        };

        $scope.showEnseignementChoice = (parDomaine?) => {
            let hideFilterMine = true;
            if (parDomaine !== undefined){

                hideFilterMine =  (parDomaine === 'false');
            }
            return hideFilterMine && $scope.template.contains('suivi-competence-content',
                'enseignants/suivi_competences_eleve/content_vue_suivi_eleve');
        };




        /**********************************************************************************************************
         *  instructions exécutées au chargement du controleur
         *********************************************************************************************************/
        $scope.searchBilan = {
            parDomaine: 'true'
        };
        if($location.path() === '/competences/eleve') {
            template.open('container', 'layouts/2_10_layout');
            template.open('left-side', 'enseignants/suivi_competences_eleve/left_side');
            template.open('content', 'enseignants/suivi_competences_eleve/content');
            template.open('suivi-competence-content', 'enseignants/suivi_competences_eleve/content_vue_suivi_eleve');

            $scope.route = $route;
            $scope.lang = lang;
            $scope.opened.lightboxEvalLibre = false;
            $scope.$watch(function () {
                if ($scope.evaluationLibre != undefined)
                    return $scope.evaluationLibre.id_matiere;
            }, function (newValue) {
                if (newValue !== "" && newValue !== undefined && newValue !== null) {
                    let mamatiere = _.findWhere($scope.evaluationLibre.matieres, {id: $scope.evaluationLibre.id_matiere});
                    if (mamatiere != undefined) {
                        $scope.evaluationLibre.sousmatiere = mamatiere.sousMatieres.all;
                        if (mamatiere.sousMatieres.all !== undefined && mamatiere.sousMatieres.all.length > 0) {
                            $scope.evaluationLibre.id_sousmatiere = mamatiere.sousMatieres.all[0].id_type_sousmatiere;
                        }
                    }
                } else if (newValue === null) {
                    $scope.evaluationLibre.sousmatiere = []
                }
            });
            await $scope.initSuivi();
            $scope.$watch($scope.displayFromClass, async function (newValue, oldValue) {
                if (newValue !== oldValue) {
                    await $scope.initSuivi();
                }
            });

            /*
             Listener sur le template suivi-competence-detail permettant la transition entre la vue détail
             et la vue globale
             */
            template.watch("suivi-competence-detail", function () {
                if (!$scope.opened.detailCompetenceSuivi) {
                    $scope.opened.detailCompetenceSuivi = true;
                }
            });
            $scope.$watch($scope.detailCompetence, async function () {
                await utils.initChartsEval($scope);
            });
            await Utils.stopMessageLoader($scope);
        }
    }
]);
