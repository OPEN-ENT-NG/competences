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

import {ng, _, template} from 'entcore';
import { evaluations } from '../models/teacher';
import * as utils from '../utils/teacher';
import {Utils} from "../models/teacher";
import http from "axios";

export let evalAcuTeacherController = ng.controller('EvalAcuTeacherController', [
    '$scope', 'route', 'model', '$rootScope',
    async function ($scope, route, model, $rootScope) {

        await model.me.workflow.load(['viescolaire']);

        $scope.initControler = function () {

            $scope.evaluations = evaluations;
            $scope.chartOptions = {
                classes: {},
                options: {
                    tooltips: {
                        callbacks: {
                            label: function (tooltipItems, data) {
                                return tooltipItems.yLabel + "%";
                            }
                        }
                    },
                    scales: {
                        yAxes: [{
                            ticks: {
                                size: 0,
                                max: 100,
                                min: 0,
                                stepSize: 20,
                            },
                        }],
                        xAxes: [{
                            display: false,
                        }]
                    }
                },
                colors: ['#4bafd5', '#46bfaf', '#ecbe30', '#FF8500', '#e13a3a', '#b930a2', '#763294', '#1a22a2']
            };
            $scope.showAutocomplete = false;
            $scope.devoirsNotDone = [];
            $scope.devoirsClasses = [];

            // $scope.periodes = evaluations.periodes;

            // Récupération des structures
            $scope.structures = evaluations.structures;
            $scope.usePerso = evaluations.structure.usePerso;

            // $scope.getDefaultPeriode = function () {
            //     return utils.getDefaultPeriode($scope.periodes.all);
            // };

            $scope.getDevoirsNotDone = function (idDevoirs?) {
                return new Promise((resolve, reject) => {
                    let calcPercent = () => {
                        if (!idDevoirs) {
                            idDevoirs = _.pluck(_.filter($scope.devoirs.all, (devoir) => {
                                return _.contains(_.pluck($scope.classes.all, 'id'),
                                        devoir.id_groupe);
                            }), 'id');
                        }
                        resolve($scope.devoirs.filter((devoir) => {
                            return (devoir.percent < 100 && _.contains(idDevoirs, devoir.id));
                         }));
                    };
                    if (!evaluations.structure.synchronized.devoirs) {
                        evaluations.structure.devoirs.one('sync', function () {
                            calcPercent();
                        });
                    } else {
                        calcPercent();
                    }

                });
            };

            $scope.initChartListNotDone = function () {
                $scope.getDevoirsNotDone().then(async function(devoirs){
                    $scope.devoirsNotDone = _.filter(devoirs, (devoir) => {
                        devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
                        return $scope.filterValidDevoir(devoir);
                    });
                    $scope.devoirsClasses = _.filter(evaluations.structure.classes.all, (classe) => {
                        return _.contains(_.uniq(_.pluck($scope.devoirsNotDone, 'id_groupe')), classe.id)
                            && classe.remplacement !== true;
                    });
                    if ($scope.devoirsClasses.length > 0 ) {
                        $scope.chartOptions.selectedClasse = _.first(_.sortBy($scope.devoirsClasses, 'name')).id;
                        $scope.loadChart($scope.chartOptions.selectedClasse);
                    }
                    await utils.safeApply($scope);
                });
            };
        };

        // Initialisation du Controler
        if (evaluations.structure !== undefined) {
            $scope.initControler(false);
        }else {
            console.log("Aucun établissement actif pour l'utilisateur");
        }

        $scope.loadChart = function (idClasse) {
            let idDevoirs = _.pluck(_.where($scope.devoirsNotDone, {id_groupe: idClasse}), 'id');
            $scope.getDevoirsNotDone(idDevoirs).then((devoirs) => {
                if (devoirs) {
                    $scope.chartOptions.classes[idClasse] = {
                        names: _.pluck(devoirs, 'name'),
                        percents: _.pluck(devoirs, 'percent'),
                        id: _.pluck(devoirs, 'id')
                    };
                } else {
                    $scope.chartOptions.classes[idClasse] = {
                        names: [],
                        percents: [],
                        id: []
                    };
                }
                utils.safeApply($scope);
            });
        };

        /**
         * ouvrir le suivi d'un eleve (utilisé dans la barre de recherche)
         * @param Eleve
         */
        $scope.openSuiviEleve = (Eleve) => {
            let path = '/competences/eleve';
            let idOfpath = {idEleve : Eleve.id, idClasse: Eleve.idClasse};
            $scope.goTo(path, idOfpath);
        };

        $scope.changeEtablissementAccueil =  async function() {
            // Angular 1.7.9 <select> now change the reference of our $scope evaluations.structures
            // We reassign the $scope with the ng-option element structures.all selected in order to keep the same reference
            evaluations.structure = $scope.structures.all.find(s => s.id ===  evaluations.structure.id);

            let switchEtab = async () => {
                await $scope.initControler();
                await $scope.$parent.initReferences();
                $scope.search = $scope.initSearch();
                $scope.devoirs = evaluations.structure.devoirs;
                $scope.filteredDevoirs = _.filter($scope.devoirs.all, devoir => {
                    devoir.nameClass = $scope.getClasseData(devoir.id_groupe, 'name');
                    return $scope.filterValidDevoir(devoir);
                });
                $scope.usePerso = evaluations.structure.usePerso;
                $scope.classes = evaluations.structure.classes;
                $scope.filteredClasses = _.filter($scope.classes.all, classe => {
                    return $scope.filterValidClasse(classe);
                });
                $scope.initChartListNotDone();
                await utils.safeApply($scope);
            };
            if (evaluations.structure === undefined || !evaluations.structure.isSynchronized) {
                $scope.$parent.opened.displayStructureLoader = true;
                evaluations.structure.sync().then(async function() {
                    await switchEtab();
                    $scope.$parent.opened.displayStructureLoader = false;
                    utils.safeApply($scope);
                });
            } else {
               await switchEtab();
            }
        };

        /**
         * ouvrir la page de création devoir
         */
        $scope.openCreateEval = async function() {
            let formData = new FormData();
            formData.append('file', $scope.testI.files[0], $scope.testI.files[0].name);
            console.log($scope.testI.files[0]);
            let response;
            try {
                response = await http.post(`competences/csv/1/exercizer/import`,
                    formData, {'headers' : { 'Content-Type': 'multipart/form-data' }});
            } catch (err) {
                throw err.response.data;
            }
            return response;
        };

        $scope.FilterGroupEmpty = (item) => {
            let nameofclasse = $scope.getClasseData(item.id_groupe, 'name');
            if ( item.id_groupe !== '' && nameofclasse !== undefined && nameofclasse !== '') {
                return item;
            }
        };

        evaluations.devoirs.on('sync', function () {
            $scope.initChartListNotDone();
        });

        if (evaluations.structure !== undefined && evaluations.structure.isSynchronized) {
            $scope.initChartListNotDone();
            $scope.initSearch();
        }

        // permet de basculer sur l' écran de saisie de note en cliquant sur le diagramme
        $scope.SaisieNote = (points, evt) => {
            if ( points.length > 0 && points !== undefined ) {
                let path = '/devoir/' +
                    $scope.chartOptions.classes[$scope.chartOptions.selectedClasse].id[points[0]._index];
                $scope.goTo(path);
            }

        };

        $scope.syncAllDevoirs =  async function(){
            if(evaluations.structure !== undefined && evaluations.structure.devoirs !== undefined
                && evaluations.structure.devoirs.length() < 51 && evaluations.structure.devoirs.lock === undefined) {

                console.log("syncAllDevoirs...");
                Utils.runMessageLoader($scope);

                evaluations.structure.devoirs.lock = true;
                await evaluations.structure.syncDevoirs();
                evaluations.structure.devoirs.lock = undefined;

                Utils.stopMessageLoader($scope);
                console.log("syncAllDevoirs done !");
            } else {
                console.log("syncAllDevoirs already running or devoirs already sync : " + evaluations.structure.devoirs.length());
            }


        };

        $scope.openTm = function(){
            $scope.testI = new TestImporter();
            $scope.opened.test = true;
            template.open('lightboxTest', 'enseignants/test');
        };


    }
]);

export class TestImporter {
    files: File[];
    id_campaign: number;
    message: string;

    constructor () {
        this.files = [];
    }

    isValid(): boolean {
        return this.files.length > 0
            ? this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== ''
            : false;
    }

    async validate(): Promise<any> {
        try {
            await this.postFile();
        } catch (err) {
            throw err;
        }
    }

    private async postFile(): Promise<any> {
        let formData = new FormData();
        formData.append('file', this.files[0], this.files[0].name);
        let response;
        try {
            response = await http.post(`/lystore/campaign/${this.id_campaign}/purses/import`,
                formData, {'headers' : { 'Content-Type': 'multipart/form-data' }});
        } catch (err) {
            throw err.response.data;
        }
        return response;
    }
}