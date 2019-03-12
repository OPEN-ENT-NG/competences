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
 * Created by agnes.lapeyronnie on 15/09/2017.
 */
import {ng, _, notify} from "entcore";
import {Classe, LSU, Utils} from '../models/teacher';
import * as utils from '../utils/teacher';
export let exportControleur = ng.controller('ExportController',['$scope',
    function($scope) {

        function initparams(type, stsFile?, errorResponse?) {
            $scope.lsu = new LSU($scope.structure.id,
                $scope.evaluations.classes.where({type_groupe : Classe.type.CLASSE}),
                $scope.structure.responsables.all);
            $scope.allClasses = $scope.evaluations.classes.where({type_groupe: Classe.type.CLASSE});
            $scope.errorResponse = (errorResponse!== undefined)? errorResponse: null;
            $scope.inProgress = false;
            $scope.filteredPeriodes = [];

            $scope.params = {
                type: type,
                idStructure: $scope.structure.id,
                classes: [],
                responsables: [],
                periodes_type: [],
                stsFile: (stsFile!== undefined)?stsFile:null
            };
            utils.safeApply($scope);
        }
        initparams("1");

        $scope.changeType = (type: String): void => {
            initparams(type);
        };

        $scope.dropComboModel = (el: any, table: any): void => {
            table = _.without(table, el);
            utils.safeApply($scope);
        };

        $scope.toggleperiode = function toggleperiode(periode_type) {
            let idx = $scope.params.periodes_type.indexOf(periode_type);
            if (idx > -1) {
                $scope.params.periodes_type.splice(idx, 1);
                utils.safeApply($scope);
            }
            else {
                $scope.params.periodes_type.push(periode_type);
                utils.safeApply($scope);
            }
        };

        // Créer une fonction dans le $scope qui lance la récupération des responsables
        $scope.getResponsables = function () {
            $scope.structure.responsables.sync().then(() => {
                $scope.lsu.responsable = $scope.structure.responsables.all[0].displayName
            });
        };
        $scope.getResponsables();

        /**
         * Controle la validité des selections avant l'exportLSU
         */
        $scope.controleExportLSU = function(){

            $scope.inProgress = !(
                ($scope.params.type == "1"
                    && $scope.params.classes.length > 0
                    && $scope.params.responsables.length > 0)
                || ($scope.params.type == "2"
                    && $scope.params.stsFile !== null
                    && $scope.params.periodes_type.length > 0
                    && $scope.params.classes.length > 0
                    && $scope.params.responsables.length > 0)
            );
            utils.safeApply($scope);
            return $scope.inProgress;
        };

        $scope.uploadFile = function (files) {
            let file = files[0],
                reader = new FileReader();
            reader.onload = () => {
                let text = reader.result;
                let parser = new DOMParser();
                let doc = parser.parseFromString(text, "application/xml");
                // let individus = ((((utilsJson.xmlToJson(doc) || {})['STS_EDT'] || {}).DONNEES || {}).INDIVIDUS || {}).INDIVIDU;
                //$scope.params.stsFile = utilsJson.cleanJson(individus);
                let individus = ((((utils.xmlToJson(doc) || {})['STS_EDT'] || {}).DONNEES || {}).INDIVIDUS || {}).INDIVIDU;
                $scope.params.stsFile = utils.cleanJson(individus);
                $scope.inProgress = $scope.controleExportLSU();
                utils.safeApply($scope);
            };
            reader.readAsText(file);
        };

        $scope.exportLSU = ()=> {
            Utils.runMessageLoader($scope);
            $scope.inProgress = true;
            $scope.params.type = ""+ $scope.params.type;
            $scope.lsu.export($scope.params)
                .then((res) => {
                    let blob = new Blob([res.data]);
                    let link = document.createElement('a');
                    link.href = window.URL.createObjectURL(blob);
                    link.download = res.headers['content-disposition'].split('filename=')[1];
                    document.body.appendChild(link);
                    link.click();
                    $scope.errorResponse = null;
                    Utils.stopMessageLoader($scope);
                }).catch((error) => {
                console.error(error);
                $scope.errorResponse = true;
                Utils.stopMessageLoader($scope);
            });
        };
        $scope.chooseClasse = async function (classe) {
            await Utils.chooseClasse(classe,$scope, false);
            utils.safeApply($scope);
        };
        $scope.updateFilters = async (classes) => {
            if(!_.isEmpty(classes)) {
                _.map(classes, (classe) => {
                    classe.selected = true;
                });
                $scope.printClasses = {
                    all: classes
                };
                await utils.updateFilters($scope, false);
                if (_.isEmpty($scope.filteredPeriodes)) {
                    notify.info('evaluations.classes.are.not.initialized');
                }
                utils.safeApply($scope);
            }
        };
    }
]);




// Si 1 structure =>  Initialiser lsu.structureId à l'id de la structure
// if($scope.evaluations.structures.all.length == 1){
//$scope.lsu.idStructure = $scope.structure.id;
// }