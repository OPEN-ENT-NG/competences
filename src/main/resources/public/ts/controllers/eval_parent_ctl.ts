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
 * Created by anabah on 29/11/2017.
 */

import { model, ng, idiom as lang, template, skin, moment } from 'entcore';
import { evaluations } from '../models/eval_parent_mdl';
import * as utils from '../utils/parent';
import { Classe } from '../models/parent_eleve/Classe';
import { Defaultcolors } from '../models/eval_niveau_comp';
import {
    FilterNotEvaluated, FilterNotEvaluatedConnaissance,
    FilterNotEvaluatedEnseignement
} from "../utils/filters/filterNotEvaluatedEnseignement";
import {Utils} from "../models/teacher";
import {updateNiveau} from "../models/common/Personnalisation";

declare let _: any;
declare let location: any;
declare let window: any;
declare let Chart: any;

export let evaluationsController = ng.controller('EvaluationsController', [
    '$scope', 'route', '$location', '$filter', '$sce', '$compile', '$timeout', '$route',
     function ($scope, route, $location, $filter, $sce, $compile, $timeout, $route) {
        route({
            accueil : async function (params) {
                await $scope.init(true);
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('menu', 'parent_enfant/accueil/eval_parent_menu');
                template.open('main', 'parent_enfant/accueil/eval_parent_acu');
                utils.safeApply($scope);
            },
            displayReleveNotes : async function(params) {
                await $scope.init(true);
                template.close('main');
                template.close('menu');
                utils.safeApply($scope);
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('main', 'parent_enfant/releve/eval_parent_dispreleve');
                utils.safeApply($scope);
            },
            displayBulletin : async function(params) {
                await $scope.init(true);
                template.close('main');
                template.close('menu');
                utils.safeApply($scope);
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('main', 'parent_enfant/bulletin/eval_parent_dispbulletin');
                utils.safeApply($scope);
            },
            listDevoirs : async function (params) {
                template.close('main');
                template.close('menu');
                utils.safeApply($scope);
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('main', 'parent_enfant/liste_devoirs/display_devoirs_structure');
                template.open('evaluations', 'parent_enfant/liste_devoirs/list_view');
                utils.safeApply($scope);
            },
            displayBilanDeCompetence : async function (params) {
                await $scope.init();
                template.close('main');
                template.close('menu');
                await $scope.initBilan();
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('main', 'parent_enfant/bilan_competences/content_vue_bilan_eleve');
                template.open('menu', 'parent_enfant/bilan_competences/left-side');
                await utils.safeApply($scope);
            },
            viewDevoir : async function (params) {
                await $scope.init(true);
                template.close('menu');
                template.close('main');
                utils.safeApply($scope);
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('main', 'parent_enfant/liste_devoirs/display_devoir');
                utils.safeApply($scope);
            },
            displayBilanPeriodique: async () => {
                // $scope.opened.lightbox = false;
                await $scope.init(true);
                template.close('menu');
                template.close('main');
                if (evaluations !== undefined ) {
                    //$scope.cleanRoot();
                    //$scope.filteredPeriode = $filter('customClassPeriodeFilters')($scope.structure.typePeriodes.all, $scope.search);
                    // delete $scope.informations.eleve;
                    utils.safeApply($scope);
                    template.open('main', 'enseignants/bilan_periodique/display_bilan_periodique');

                }
            }
        });

        let initialise = (withSyncDevoir?) => {
            return new Promise( async (resolve) => {
                if (model.me.type === 'ELEVE') {
                    $scope.eleve = evaluations.eleve;
                }
                else if (model.me.type === 'PERSRELELEVE') {
                    $scope.eleves = evaluations.eleves.all;
                }
                $scope.searchBilan = {
                    // periode: evaluations.periode,
                    parDomaine: 'false'
                };
                $scope.search = {
                    periode: evaluations.periode,
                    classe : null,
                    matiere: evaluations.matiere,
                    enseignant: null,
                    sousmatiere: null,
                    type: null,
                    eleve: evaluations.eleve,
                    name: ''
                };

                $scope.opened = {
                    lightbox : false,
                    displayMessageLoader: false
                };
                await $scope.chooseChild(evaluations.eleve, withSyncDevoir);
                resolve();
            });
        };

        /**
         *
         * @returns {Promise<void>}
         */
        $scope.init = async function (withSyncDevoir?) {
            return new Promise( async (resolve) => {
                if ($scope.eleve === undefined) {
                    await evaluations.sync();
                    await initialise(withSyncDevoir);
                }
                else {
                    await initialise(withSyncDevoir);
                }
                resolve();
            });
        };

        $scope.getI18nPeriode = (periode) => {
            let result;
            if (periode.libelle === "cycle") {
                result = lang.translate('viescolaire.utils.cycle');
            } else if (periode.id === null ) {
                result = lang.translate('viescolaire.utils.annee');
            }
            else {
                let type_periode = _.findWhere(evaluations.eleve.classe.typePeriodes.all, {id: periode.id_type});

                result = type_periode ?
                    lang.translate('viescolaire.periode.' + type_periode.type) + ' ' + type_periode.ordre
                    : lang.translate('viescolaire.utils.periodeError');
            }
            return result;
        };



        $scope.getFormatedDate = function(date) {
            return moment(date).format("DD/MM/YYYY");
        };

        /**
         * Ouvre la fenêtre détail des compétences sur un devoir
         */
        $scope.getInfoCompetencesDevoir = function () {
            $scope.opened.lightbox = true;
            template.open('lightboxContainer', 'parent_enfant/accueil/display_competences');
        };

        $scope.noteMatiereEleve = function(idMatiere) {
            return $scope.dataReleve.devoirs.findWhere({ id_matiere : idMatiere });
        };

         $scope.getMoyenneClasse = function(devoirReleveNotes) {
             return +(parseFloat(devoirReleveNotes.sum_notes)/devoirReleveNotes.nbr_eleves).toFixed(2);
         };

        // Fonction de sélection d'un enfant par le parent
        $scope.chooseChild = async function(eleve, withSyncDevoir?) {
            return new Promise( async (resolve, reject) => {
                await Utils.runMessageLoader($scope);
                try {
                    evaluations.eleve = eleve;
                    $scope.eleve = evaluations.eleve;
                    $scope.selectedEleve = $scope.eleve;
                    eleve.classe = new Classe({id: eleve.idClasse});
                    await eleve.classe.sync();
                    await $scope.setCurrentPeriode();
                    if (withSyncDevoir === true && $location.path() !== "/releve") {
                        await evaluations.devoirs.sync(eleve.idStructure, eleve.id, undefined);
                    }
                    $scope.search.periode = evaluations.periode;
                    $scope.displayCycles($scope.search.periode);
                    $scope.devoirs = evaluations.devoirs;
                    $scope.matieres = evaluations.matieres;
                    $scope.enseignants = evaluations.enseignants;
                    await $scope.updateNiveau(evaluations.usePerso);
                    await $scope.getCyclesEleve();
                    if ($location.path() === "/bilan/periodique") {
                        $scope.informations = {
                            eleve: $scope.eleve
                        };
                        $scope.search.classe = $scope.eleve.classe;
                        $scope.search.eleve = $scope.eleve;
                        $scope.structure = {
                            id: model.me.structures[0],
                            cycle: {
                                niveauCompetencesArray: evaluations.arrayCompetences
                            }
                        };
                    }
                    if($location.path() === "/competences/eleve") {
                        template.close('main');
                        await utils.safeApply($scope);
                        await $scope.initBilan();
                        template.open('main', 'parent_enfant/bilan_competences/content_vue_bilan_eleve');
                        await utils.safeApply($scope);
                    }

                    if ($location.path() === "/") {
                        template.close('main');
                        template.close('menu');
                        await utils.safeApply($scope);
                        template.open('menu', 'parent_enfant/accueil/eval_parent_menu');
                        template.open('main', 'parent_enfant/accueil/eval_parent_acu');
                        await utils.safeApply($scope);
                    }

                    if ($location.path().split('/')[1] === "devoir") {
                        template.close('main');
                        await utils.safeApply($scope);
                        template.open('main', 'parent_enfant/liste_devoirs/display_devoir');
                        await utils.safeApply($scope);
                    }
                    if($location.path() !== "/releve") {
                        await utils.safeApply($scope);
                        await Utils.stopMessageLoader($scope);
                    }
                    $scope.update = false;
                    resolve();
                }
                catch (e) {
                    console.error(e);
                    await Utils.stopMessageLoader($scope);
                    reject(e);
                }
            });
        };


        /**
         * Charge la liste des periodes dans $scope.periodes et détermine la période en cours et positionne
         * son id dans $scope.currentPeriodeId
         */
        $scope.setCurrentPeriode = async function() {
            // récupération des périodes et filtre sur celle en cours
            return new Promise(async (resolve, reject)=> {
                let periodes = evaluations.eleve.classe.periodes;
                try {
                    await periodes.sync();
                    let formatStr = "DD/MM/YYYY";
                    let momentCurrDate = moment(moment().format(formatStr), formatStr);
                    $scope.currentPeriodeId = null;
                    let foundedPeriode = false;

                    for (let i = 0; i < periodes.all.length && !(foundedPeriode); i++) {
                        let momentCurrPeriodeDebut = moment(moment(periodes.all[i].timestamp_dt).format(formatStr),
                            formatStr);
                        let momentCurrPeriodeFin = moment(moment(periodes.all[i].timestamp_fn).format(formatStr),
                            formatStr);


                        if ($scope.searchBilan.periode !== undefined && !foundedPeriode
                            && $location.path() === "/competences/eleve"){
                            $scope.periode = periodes.findWhere({id_type: $scope.searchBilan.periode.id_type});
                            foundedPeriode = true;
                        }
                        else if ($scope.search.periode !== undefined && !foundedPeriode){
                            $scope.periode = periodes.findWhere({id_type: $scope.search.periode.id_type});
                            foundedPeriode = true;
                        }
                        else if(!foundedPeriode){
                            if (momentCurrPeriodeDebut.diff(momentCurrDate) <= 0
                                && momentCurrDate.diff(momentCurrPeriodeFin) <= 0) {
                                $scope.periode = periodes.findWhere({id: periodes.all[i].id});
                                foundedPeriode = true;
                            }
                        }
                        if (foundedPeriode){
                            $scope.search.periode = $scope.periode;
                            evaluations.periode = $scope.periode;
                            $scope.searchBilan.periode = $scope.periode;
                            $scope.currentPeriodeId = $scope.periode.id;
                        }
                    }
                    $scope.$broadcast('loadPeriode');
                    await utils.safeApply($scope);
                    resolve();
                }
                catch (e) {
                    console.error(e);
                }
            });
        };
        $scope.template = template;
        $scope.me = {
            type : model.me.type
        };
        $scope.suiviFilter = {
            mine : false
        };

        $scope.skin = skin;
        $scope.translate = lang.translate;
        $scope.isCurrentPeriode = function(periode) {
            return (periode.id === $scope.currentPeriodeId);
        };

        /**
         * Retourne le libelle de la matière correspondant à l'identifiant passé en paramètre
         * @param idMatiere identifiant de la matière
         * @returns {any} libelle de la matière
         */
        $scope.getLibelleMatiere = function (idMatiere) {
            if (idMatiere === undefined || idMatiere == null || idMatiere === "") return "";
            let matiere = _.findWhere($scope.matieres.all, {id: idMatiere});
            if (matiere !== undefined && matiere.hasOwnProperty('name')) {
                return matiere.name;
            } else {
                return '';
            }
        };

         $scope.getLibelleSousMatiere = function (currentDevoir) {

             let idMatiere = currentDevoir.id_matiere;
             if (idMatiere === undefined || idMatiere == null || idMatiere === "") return "";
             let matiere = _.findWhere($scope.matieres.all, {id: idMatiere});
             let idSousmatiere = currentDevoir.id_sousmatiere;
             if (matiere === undefined || idSousmatiere === undefined || idSousmatiere === null || idSousmatiere === ""  )
                 return ""
             let sousmatiere = _.findWhere(matiere.sousMatieres.all, {id_type_sousmatiere: parseInt(idSousmatiere)})
             if(sousmatiere !== undefined && sousmatiere.hasOwnProperty('libelle')){
                 return sousmatiere.libelle;
             }else{
                 return "";
             }
         };

        $scope.getTeacherDisplayName = function (owner) {
            if (owner === undefined || owner === null || owner === "") return "";
            let ensenseignant = _.findWhere(evaluations.enseignants.all, {id: owner});
            if (ensenseignant !== undefined && ensenseignant.hasOwnProperty('name')) {
                return ensenseignant.firstName[0] + '.' + ensenseignant.name;
            } else {
                return '';
            }
        };

        /**
         * Format la date passée en paramètre
         * @param date Date à formatter
         * @returns {any|string} date formattée
         */
        $scope.getDateFormated = function (date) {
            return utils.getFormatedDate(date, "DD/MM/YYYY");
        };

        $scope.saveTheme = function () {
            $scope.chooseTheme();
        };

        $scope.updateColorAndLetterForSkills = async function () {
            $scope.niveauCompetences = evaluations.niveauCompetences;
            $scope.arrayCompetences = _.groupBy(evaluations.niveauCompetences.all,
                {id_cycle : evaluations.eleve.classe.id_cycle}).true;
            $scope.structure = {
                usePerso: evaluations.usePerso
            };
            // chargement dynamique des couleurs du niveau de compétences
            // et de la valeur max (maxOrdre)
            $scope.mapCouleurs = {"-1": Defaultcolors.unevaluated};
            $scope.mapLettres = {"-1": " "};
            _.forEach($scope.arrayCompetences, function (niv) {
                $scope.mapCouleurs[niv.ordre - 1] = niv.couleur;
                $scope.mapLettres[niv.ordre - 1] = niv.lettre;
            });
            utils.safeApply($scope);
        };
        $scope.updateColorArray = async function () {
            evaluations.arrayCompetences =
                _.groupBy(evaluations.niveauCompetences.all,{id_cycle : evaluations.eleve.classe.id_cycle}).true;
        };


        $scope.updateNiveau =  async function (usePerso) {
            if(usePerso == 'true') {
                evaluations.niveauCompetences.sync().then(async () => {
                    $scope.syncColorAndLetter();
                });
            }
        };

        $scope.syncColorAndLetter = async function () {
            await $scope.updateColorArray();
            await $scope.updateColorAndLetterForSkills();
            await utils.safeApply($scope);
        };
        $scope.initLimit = function () {
            $scope.limits = [5,10,15,20];
            $scope.limitSelected = $scope.limits[0];
        };

        $scope.getLibelleLimit = function (limit) {
            return limit + " " + lang.translate('last');
        };

        $scope.update = true;
        $scope.reload = function () {
            window.location.hash='#/';
            location.reload();
        };

        $scope.FilterNotEvaluated = function (maCompetence) {
            return FilterNotEvaluated(maCompetence);
        };

        $scope.FilterNotEvaluatedDomaine = function (monDomaineCompetence) {
            if (monDomaineCompetence.domaines.all.length > 0 ) {
                for (let i = 0; i < monDomaineCompetence.domaines.all.length; i++) {
                    if($scope.FilterNotEvaluated(monDomaineCompetence.domaines.all[i])){
                        return true;
                    }
                }
            }
            else {
                for (let i = 0; i < monDomaineCompetence.competences.all.length; i++) {
                    let maCompetence = monDomaineCompetence.competences.all[i];
                    if ($scope.FilterNotEvaluated(maCompetence)) {
                        return true;
                    }
                }
            }
            return false;
        };

        $scope.FilterNotEvaluatedEnseignement = function (monEnseignement) {
            return FilterNotEvaluatedEnseignement(monEnseignement);
        };

        $scope.getCyclesEleve = async () => {
            await evaluations.eleve.getCycles();
            if (model.me.type === 'ELEVE'
                && evaluations.eleve.classe !== undefined) {
                $scope.currentCycle = _.findWhere(evaluations.eleve.cycles, {id_cycle: evaluations.eleve.classe.id_cycle});
            }
            else {
                $scope.currentCycle = _.findWhere(evaluations.eleve.cycles, {id_cycle: evaluations.eleve.id_cycle});
            }
            utils.safeApply($scope);
        }


        $scope.changePeriode = async function (cycle?) {
            await Utils.runMessageLoader($scope);
            if (cycle === null || cycle === undefined){
                let historise = false;
                if( $scope.searchBilan !== undefined
                    && $scope.searchBilan.periode.id_type > 0
                    && $scope.searchBilan.periode !== undefined
                    && $scope.searchBilan.periode.id_type !== undefined){
                    // On récupère les devoirs de la période sélectionnée

                    await evaluations.devoirs.sync(evaluations.eleve.idStructure,evaluations.eleve.id, undefined,
                        $scope.searchBilan.periode.id_type, undefined, historise);
                    $scope.getCompetences(evaluations);
                    await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve,
                        $scope.competences, undefined);
                    await evaluations.enseignements.sync(evaluations.eleve.idClasse, $scope.competences, undefined);
                } else {
                    if ($scope.searchBilan !== undefined
                        && $scope.searchBilan.periode.id_type == -2
                        && $scope.searchBilan.id_cycle !== undefined){
                        // On récupère les devoirs du cycle selectionné
                        historise = true;

                        await evaluations.devoirs.sync(this.eleve.idStructure, this.eleve.id, undefined, undefined,
                            $scope.currentCycle.id_cycle, historise);
                        $scope.getCompetences(evaluations);
                        await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve,
                            $scope.competences, $scope.currentCycle.id_cycle);
                        await evaluations.enseignements.sync(evaluations.eleve.idClasse, $scope.competences,
                            $scope.currentCycle.id_cycle);
                    }  else {
                        // On récupère les devoirs de l'année

                        await evaluations.devoirs.sync(this.eleve.idStructure, this.eleve.id, undefined, undefined,
                            undefined, historise);
                        $scope.getCompetences(evaluations);
                        await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve, $scope.competences,
                            undefined);
                        await evaluations.enseignements.sync(evaluations.eleve.idClasse, $scope.competences, undefined);
                    }
                }

            }
            else {

                await evaluations.devoirs.sync(this.eleve.idStructure, this.eleve.id, undefined, undefined,
                    cycle.id_cycle, true);
                $scope.getCompetences(evaluations);
                await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve, $scope.competences,
                    cycle.id_cycle);
                await evaluations.enseignements.sync(evaluations.eleve.idClasse, $scope.competences, cycle.id_cycle);
            }

            $scope.evaluations =  evaluations;
            template.close('main');
            await utils.safeApply($scope);
            template.open('main',  'parent_enfant/bilan_competences/content_vue_bilan_eleve');
            await Utils.stopMessageLoader($scope);
        };


        $scope.displayCycles = (periode) => {
            if(periode !== null && periode !== undefined){
                if(periode.libelle === "cycle"){
                    $scope.displayCycle = true;
                } else {
                    $scope.displayCycle = false;
                }
            }
            else {
                $scope.displayCycle = false;
            }
        };

        /**
         * Récupère les compétences des évaluations
         * @param evaluations
         */
        $scope.getCompetences = function (evaluations) {
            $scope.competences = [];
            _.forEach(evaluations.devoirs.all, function (evaluation) {
                _.forEach(evaluation.competences, function (competence) {
                    if(!_.contains($scope.competences,competence)){
                        $scope.competences.push(competence);
                    }
                });
            });
        };

        // Initialisation des variables du Bilan
        $scope.initBilan = async function () {

            if($scope.searchBilan.parDomaine === undefined) {
                $scope.searchBilan.parDomaine = 'false';
            }

            if($scope.me === undefined){
                $scope.me = {
                    type: model.me.type
                };
            }

            if($scope.suiviFilter === undefined || $scope.suiviFilter.mine) {
                $scope.suiviFilter = {
                    mine: false
                };
            }

            await $scope.changePeriode();
            if($scope.currentCycle !== undefined
                && $scope.currentCycle.id_cycle !== undefined) {
                $scope.searchBilan.id_cycle =  $scope.currentCycle.id_cycle;
            }

            $scope.evaluations = evaluations;

            await utils.safeApply($scope);
        };
        /**
         * show label too long
         * @type {boolean}
         */
        // start with the div hidden
        $scope.hovering = false;

        // create the timer variable
        var timer;

        // mouseenter event
        $scope.showIt = function (item) {
            timer = $timeout(function () {
                item.hovering = true;
            }, 350);
        };

        // mouseleave event
        $scope.hideIt = function (item) {
            $timeout.cancel(timer);
            item.hovering = false;
        };

        /**
         * Lance la séquence d'ouverture du détail d'une compétence permettant d'accéder à la vue liste ou graph
         * @param competence Compétence à ouvrir
         */
        $scope.openDetailCompetence = async function (competence) {
            $scope.detailCompetence = competence;
            await utils.initChartsEvalParents($scope);
            template.open("main", "parent_enfant/bilan_competences/detail_vue_graph");
            utils.scrollTo('top');
        };


        $scope.chartOptionsEval = {
            series : ['Evaluation'],
            tooltipLabels : [],
            options: {
                tooltips: {
                    callbacks: {
                        label: function(tooltipItems, data) {
                            return $scope.chartOptionsEval.tooltipLabels[tooltipItems.index];
                        }
                    }
                },
                elements:{
                    point :{
                        radius : 10,

                    },
                    line : {
                        fill: false,
                        borderDash : [0, 15]
                    }
                },
                maintainAspectRatio : false,
                scales: {
                    responsive: true,
                    yAxes: [{
                        gridLines : {display : false,
                            color : '#000000'},
                        pointRadius: 10,
                        type: 'linear',
                        display: true,
                        ticks: {
                            max: 6,
                            min: 0,
                            fontColor : 'black',
                            stepSize: 1,
                            padding : 20,
                            callback: function (value, index, values) {
                                if(value === 1) {
                                    return "Compétence non évaluée" ;
                                }
                                else if(value === 2) {
                                    return "Maîtrise insuffisante" ;
                                }
                                else if(value === 3) {
                                    return "Maîtrise fragile" ;
                                }
                                else if(value === 4) {
                                    return "Maîtrise satisfaisante" ;
                                }
                                else if(value === 5){
                                    return "Très bonne maîtrise" ;
                                }
                                else{
                                    return " " ;
                                }
                                // return parseFloat(value).toFixed(2) + '%';
                            }
                        },
                    }],
                    xAxes: [{
                        type: 'category',
                        display:true,
                        responsive: false,
                        gridLines:{
                            display : false,
                            offsetGridLines : false,
                            color : '#000000'
                        },
                        ticks: {
                            labelOffset : 30,
                            minRotation : 20, // rotation des labels
                            autoSkip: true,
                            maxTicksLimit: 20,
                            fontColor : 'black'
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
        /**
         * MISE A JOUR POUR LA PRISE EN COMPTE DE LA PERSONNALISATION DES COULEURS DE COMPETENCES DANS LE GRAPHE
         */
        Chart.plugins.register({
            afterDatasetsDraw: function(chart, easing) {
                // To only draw at the end of animation, check for easing === 1
                let ctx = chart.chart.ctx;

                chart.data.datasets.forEach(function (dataset, i) {
                    let meta = chart.getDatasetMeta(i);
                    if (!meta.hidden) {
                        meta.data.forEach(function(element, index) {
                            // Draw the text invert color of buble, with the specified font
                            let rgba = dataset.backgroundColor[index];
                            if(rgba && rgba.includes('(') && rgba.includes(')') && rgba.includes(',')) {
                                rgba = rgba.split('(')[1].split(')')[0].split(',');
                                let r = 255 - parseInt(rgba[0]);
                                let g = 255 - parseInt(rgba[1]);
                                let b = 255 - parseInt(rgba[2]);
                                let a = rgba[3];

                                ctx.fillStyle = "rgba(" + r.toString() + "," + g.toString() + "," + b.toString() + "," + a + ")";
                            }
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
                            if (dataString === undefined){
                                dataString = " ";
                            }
                            ctx.fillText(dataString, position.x, position.y);

                        });
                    }
                });
            }
        });

        $scope.initChartsEval = async function () {
            await utils.initChartsEvalParents($scope);
        };

        /**
         * Retourne si l'utilisateur est le propriétaire de l'évaluation
         * @param evaluation Evaluation à afficher
         * @returns {boolean} Retourne true si l'utilisateur est le propriétaire de l'évaluation
         */
        $scope.filterOwnerSuivi = function (evaluation) {
            if ($scope.suiviFilter.mine === 'false' || $scope.suiviFilter.mine === false) {
                return true;
            }
            return evaluation.owner === $scope.me.userId;
        };
        /**
         * Lance la séquence de retour à la vue globale du bilan de compétence
         */
        $scope.backToSuivi = function () {
            template.open('main', 'parent_enfant/bilan_competences/content_vue_bilan_eleve');
            $scope.detailCompetence = null;
        };

        $scope.redirectToDevoir = function (idDevoir) {
            let pathToDevoir = '/devoir/' + idDevoir;
            $scope.goTo(pathToDevoir);
        };

        $scope.goTo = function (path, id) {
            $location.path(path);
            if (id != undefined)
                $location.search(id);
            $location.replace();
            utils.safeApply($scope);
        };
        $scope.initDefaultMatiere = function () {
            if($scope.matieres !== undefined && $scope.matieres.all !== undefined && $scope.matieres.all.length === 1) {
                $scope.search.matiere = $scope.matieres.all[0];
            }
            return $scope.search.matiere;
        };

        $scope.filterCycle = () => {
            return (item) => {
                return item.id_type > -2;
            };
        };
         $scope.filterCycleAndYear = () => {
             return (item) => {
                 return item.id_type > -1;
             };
         };
    }
]);