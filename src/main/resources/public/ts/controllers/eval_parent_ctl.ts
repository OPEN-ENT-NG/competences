/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

/**
 * Created by anabah on 29/11/2017.
 */

import { model, ng, idiom as lang, template, skin, moment } from 'entcore';
import { evaluations } from '../models/eval_parent_mdl';
import * as utils from '../utils/parent';
import { Classe } from '../models/parent_eleve/Classe';
import { Defaultcolors } from '../models/eval_niveau_comp';

declare let _: any;
declare let location: any;
declare let window: any;
declare let Chart: any;

export let evaluationsController = ng.controller('EvaluationsController', [
    '$scope', 'route', '$location', '$filter', '$sce', '$compile', '$timeout', '$route',
    function ($scope, route, $location, $filter, $sce, $compile, $timeout, $route) {

        model.me.workflow.load(['viescolaire']);
        route({
            accueil : async function (params) {
                await $scope.init();
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('menu', 'parent_enfant/accueil/eval_parent_menu');
                template.open('main', 'parent_enfant/accueil/eval_parent_acu');
                utils.safeApply($scope);
            },
            displayReleveNotes : async function(params) {
                await $scope.init();
                template.close('main');
                template.close('menu');
                utils.safeApply($scope);
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('main', 'parent_enfant/releve/eval_parent_dispreleve');
                utils.safeApply($scope);
            },
            listDevoirs : async function (params) {
                await $scope.init();
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
                utils.safeApply($scope);
            },
            viewDevoir : async function (params) {
                await $scope.init();
                template.close('menu');
                template.close('main');
                utils.safeApply($scope);
                template.open('header', 'parent_enfant/accueil/eval_parent_selectEnfants');
                template.open('main', 'parent_enfant/liste_devoirs/display_devoir');
                utils.safeApply($scope);
            }
        });

        /**
         *
         * @returns {Promise<void>}
         */
        $scope.init = async function () {
            let initialise = async () => {
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
                    matiere: null,
                    enseignant: null,
                    sousmatiere: null,
                    type: null,
                    name: ''
                };
                await $scope.chooseChild (evaluations.eleve);
            };
            if ($scope.eleve === undefined) {
                await evaluations.sync();
                initialise();
            }
            else {
                initialise();
            }
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
            $scope.opened = {
                lightbox : true
            };
            template.open('lightboxContainer', 'parent_enfant/accueil/display_competences');
        };

        $scope.noteMatiereEleve = function(idMatiere) {
            return $scope.dataReleve.devoirs.findWhere({ id_matiere : idMatiere });
        };

        // Fonction de sélection d'un enfant par le parent
        $scope.chooseChild = async function(eleve) {
            evaluations.eleve = eleve;
            $scope.eleve = evaluations.eleve;
            $scope.selectedEleve = $scope.eleve;
            eleve.classe = new Classe({id: eleve.idClasse});
            await eleve.classe.sync();
            await evaluations.devoirs.sync( eleve.idStructure, eleve.id, undefined );
            $scope.devoirs = evaluations.devoirs;
            $scope.matieres = evaluations.matieres;
            $scope.search.periode = evaluations.periode;
            $scope.enseignants = evaluations.enseignants;
            $scope.setCurrentPeriode();
            await $scope.updateNiveau(evaluations.usePerso);
            await $scope.getCyclesEleve();

            if ($location.path() === "/competences/eleve") {
               await $scope.initBilan();
            }
            if ($location.path() === "/") {
                template.close('main');
                template.close('menu');
                utils.safeApply($scope);
                template.open('menu', 'parent_enfant/accueil/eval_parent_menu');
                template.open('main', 'parent_enfant/accueil/eval_parent_acu');
                utils.safeApply($scope);
            }
            $scope.update = false;
            utils.safeApply($scope);
        };


        /**
         * Charge la liste des periodes dans $scope.periodes et détermine la période en cours et positionne
         * son id dans $scope.currentPeriodeId
         */
        $scope.setCurrentPeriode = function() {
            // récupération des périodes et filtre sur celle en cours
            let periodes = evaluations.eleve.classe.periodes;
            periodes.sync().then(() => {
                let formatStr = "DD/MM/YYYY";
                let momentCurrDate = moment(moment().format(formatStr), formatStr);
                let foundPeriode = false;
                $scope.currentPeriodeId = null;

                for (let i = 0; i < periodes.all.length && !foundPeriode; i++) {
                    let momentCurrPeriodeDebut = moment(moment(periodes.all[i].timestamp_dt).format(formatStr),
                        formatStr);
                    let momentCurrPeriodeFin = moment(moment(periodes.all[i].timestamp_fn).format(formatStr),
                        formatStr);

                    if ( momentCurrPeriodeDebut.diff(momentCurrDate) <= 0
                        && momentCurrDate.diff(momentCurrPeriodeFin) <= 0) {
                        foundPeriode = true;
                        $scope.periode = periodes.findWhere({id : periodes.all[i].id});
                        $scope.search.periode = $scope.periode;
                        evaluations.periode = $scope.periode;
                        $scope.searchBilan.periode = $scope.periode;
                        $scope.currentPeriodeId = $scope.periode.id;
                    }

                }
                $scope.$broadcast('loadPeriode');
                utils.safeApply($scope);

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
            return utils.getFormatedDate(date, "DD/MM");
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
            if (usePerso === 'true') {
                evaluations.usePerso = 'true';
                evaluations.niveauCompetences.sync(false).then(async () => {
                    if ($scope.update){
                        await $scope.syncColorAndLetter();

                    }
                    else {
                        evaluations.niveauCompetences.first().markUser().then(async () => {
                            await $scope.syncColorAndLetter();
                        });
                    }
                });

            }
            else if (usePerso === 'false') {
                evaluations.usePerso = 'false';
                evaluations.niveauCompetences.sync(true).then( async () => {
                    if($scope.update) {
                        await $scope.syncColorAndLetter();
                    }
                    else {
                        evaluations.niveauCompetences.first().unMarkUser().then(async () => {
                            await $scope.syncColorAndLetter();
                        });
                    }
                });
            }
        };

        $scope.syncColorAndLetter = async function () {
            await $scope.updateColorArray();
            $scope.updateColorAndLetterForSkills();
            utils.safeApply($scope);
        };
        $scope.initLimit = function () {
            $scope.limits = [5,10,15,20];
            $scope.limitSelected = $scope.limits[0];
        };

        $scope.getLibelleLimit = function (limit) {
            return limit +" " + lang.translate('last');
        };

        $scope.update = true;
        $scope.reload = function () {
            window.location.hash='#/';
            location.reload();
        }

        $scope.FilterNotEvaluated = function (maCompetence) {
            var _t = maCompetence.competencesEvaluations;
            var max = _.max(_t, function (evaluation) {
                return evaluation.evaluation;
            });
            if (typeof max === 'object') {
                return true;
            }
            else {
                return false;
            }
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

        $scope.FilterNotEvaluatedConnaissance = function (maConnaissance) {            
            for (let i = 0; i < maConnaissance.competences.all.length; i++) {
                let maCompetence = maConnaissance.competences.all[i];
                if($scope.FilterNotEvaluated(maCompetence)){                   
                    return true;
                }
            };
            return false;
        };


        $scope.FilterNotEvaluatedEnseignement = function (monEnseignement) {
            for (let i = 0; i < monEnseignement.competences.all.length; i++) {
                let maConnaissance = monEnseignement.competences.all[i];
                if($scope.FilterNotEvaluatedConnaissance(maConnaissance)){
                    return true;
                }
            };
            return false;
        };

        $scope.getCyclesEleve = async () => {
            await evaluations.eleve.getCycles();
            if (model.me.type === 'ELEVE'
                && evaluations.eleve.classe !== undefined) {
                $scope.currentCycle = _.findWhere(evaluations.eleve.cycles, {id_cycle: evaluations.eleve.classe.id_cycle});
            } else {
                $scope.currentCycle = _.findWhere(evaluations.eleve.cycles, {id_cycle: evaluations.eleve.id_cycle});
            }
            utils.safeApply($scope);
        }


        $scope.changePeriode = async function (cycle?) {
            if (cycle === null || cycle === undefined){
                let historise = false;
                if( $scope.searchBilan !== undefined
                    && $scope.searchBilan.periode.id_type > 0
                    && $scope.searchBilan.periode !== undefined
                    && $scope.searchBilan.periode.id_type !== undefined){
                    // On récupère les devoirs de la période sélectionnée
                    await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve, $scope.competences, undefined);
                    await evaluations.devoirs.sync(evaluations.eleve.idStructure,evaluations.eleve.id, undefined, $scope.searchBilan.periode.id_type, undefined, historise);
                } else if ($scope.searchBilan !== undefined
                    && $scope.searchBilan.periode.id_type == -2
                    && $scope.searchBilan.id_cycle !== undefined){
                    // On récupère les devoirs du cycle selectionné
                    if ($scope.currentCycle.id_cycle !== $scope.searchBilan.id_cycle){
                        historise = true;
                    }
                    await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve, $scope.competences, $scope.searchBilan.id_cycle);
                    await evaluations.devoirs.sync(this.eleve.idStructure, this.eleve.id, undefined, undefined, $scope.searchBilan.id_cycle, historise);
                }  else {
                    // On récupère les devoirs de l'année
                    await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve, $scope.competences, undefined);
                    await evaluations.devoirs.sync(this.eleve.idStructure, this.eleve.id, undefined, undefined, undefined, historise);
                }

            }
            else {
                $scope.currentCycle = cycle;
            }

            $scope.getCompetences(evaluations);

            await evaluations.enseignements.sync(evaluations.eleve.idClasse, $scope.competences);
            $scope.evaluations =  evaluations;
            template.close('main');
            utils.safeApply($scope);
            template.open('main',  'parent_enfant/bilan_competences/content_vue_bilan_eleve');
            utils.safeApply($scope);
        };


        $scope.displayCycles = (periode) => {
            if(periode !== null && periode !== undefined){
                if(periode.libelle === "cycle"){
                    $scope.displayCycle = true;
                } else {
                    $scope.displayCycle = false;
                }
            }
        }

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

            $scope.getCompetences(evaluations);
            $scope.searchBilan.parDomaine =  'false';

            if($scope.currentCycle !== undefined
                    && $scope.currentCycle.id_cycle !== undefined) {
                $scope.searchBilan.id_cycle =  $scope.currentCycle.id_cycle;
            }

            if($scope.competences
                && $scope.competences.length > 0){
                await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve,$scope.competences);
                await evaluations.enseignements.sync(evaluations.eleve.idClasse,$scope.competences);
                $scope.evaluations =  evaluations;
            }

            $scope.me = {
                type: model.me.type
            };

            $scope.suiviFilter = {
                mine: false
            };
            utils.safeApply($scope);
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
        $scope.openDetailCompetence = function (competence) {
            $scope.detailCompetence = competence;
            $scope.initChartsEval();
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
                            rgba = rgba.split('(')[1].split(')')[0].split(',');
                            let r = 255 - parseInt(rgba[0]);
                            let g = 255 - parseInt(rgba[1]);
                            let b = 255 - parseInt(rgba[2]);
                            let a = rgba[3];

                            ctx.fillStyle = "rgba(" + r.toString()+ ","+ g.toString() +","+ b.toString() +"," + a + ")";
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

        $scope.initChartsEval = function () {
            if ($scope.detailCompetence !== undefined && $scope.detailCompetence !== null) {
                let ListEval = _.filter($scope.detailCompetence.competencesEvaluations, function (evalu) {
                    return $scope.filterOwnerSuivi(evalu);
                });
                //initialisation et rajout de la 1er colomn vide
                $scope.chartOptionsEval.tooltipLabels = [];
                $scope.chartOptionsEval.tooltipLabels.push(' ');
                $scope.chartOptionsEval.datasets.data = [];
                $scope.chartOptionsEval.datasets.data.push(-10);
                $scope.chartOptionsEval.datasets.labels = [];
                $scope.chartOptionsEval.datasets.labels.push(" ");
                $scope.chartOptionsEval.colors = [];
                $scope.chartOptionsEval.colors.push('#FFFFFF');
                ListEval =  _.sortBy(ListEval, function(evalu){ return evalu.evaluation_date; });

                for (let i = 0; i < ListEval.length; i++) {

                    let fontText = $scope.mapLettres[ListEval[i].evaluation];
                    if (!fontText) {
                        fontText = " ";
                    }
                    $scope.chartOptionsEval.datasets.data.push({y :ListEval[i].evaluation + 2,
                        x: $scope.getDateFormated(ListEval[i].evaluation_date),
                        r: 10,
                        label: fontText});
                    $scope.chartOptionsEval.datasets.labels.push($scope.getDateFormated(ListEval[i].evaluation_date));
                    let colorValue;
                    if(ListEval[i].evaluation !== -1){colorValue = $scope.mapCouleurs[ListEval[i].evaluation];}
                    else{colorValue = Defaultcolors.unevaluated;}
                    $scope.chartOptionsEval.colors.push(colorValue);
                    $scope.chartOptionsEval.tooltipLabels.push(ListEval[i].name+' : '+ListEval[i].owner_name);

                }

                //rajout de la dernière colomn vide
                $scope.chartOptionsEval.datasets.data.push(-10);
                $scope.chartOptionsEval.datasets.labels.push(" ");
                $scope.chartOptionsEval.colors.push('#FFFFFF');
                $scope.chartOptionsEval.tooltipLabels.push(' ');
            }
            utils.safeApply($scope);
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
    }


]);