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
import {LSU_TYPE_EXPORT} from "../models/common/LSU";
export let exportControleur = ng.controller('ExportController',['$scope',
    async function($scope) {

        async function initparams(type, stsFile?, errorResponse?) {
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
            await utils.safeApply($scope);
        }


        $scope.changeType = async function (type: String){
            await initparams(type);
        };

        $scope.dropComboModel = async function (el: any, table: any){
            table = _.without(table, el);
            await utils.safeApply($scope);
        };

        $scope.toggleperiode = async function toggleperiode(periode_type) {
            if(periode_type.selected){
                let idx = $scope.params.periodes_type.indexOf(periode_type);
                if (idx > -1) {
                    $scope.params.periodes_type.splice(idx, 1);
                    await utils.safeApply($scope);
                }
                else {
                    $scope.params.periodes_type.push(periode_type);
                    await utils.safeApply($scope);
                }
                if(periode_type.libelle === undefined){
                    periode_type.libelle = $scope.getI18nPeriode(periode_type.periode);
                }
            }else{
                $scope.params.periodes_type = _.without($scope.params.periodes_type, periode_type);
            }

        };

        // Créer une fonction dans le $scope qui lance la récupération des responsables
        $scope.getResponsables = function () {
            $scope.structure.responsables.sync().then(async function(){
                $scope.lsu.responsable = $scope.structure.responsables.all[0].displayName;
                await utils.safeApply($scope);
            });
        };


        /**
         * Controle la validité des selections avant l'exportLSU
         */
        $scope.controleExportLSU = function(){

            $scope.inProgress = !(
                ($scope.params.type == LSU_TYPE_EXPORT.BFC
                    && $scope.params.classes.length > 0
                    && $scope.params.responsables.length > 0
                    && $scope.params.stsFile !== null)
                || ($scope.params.type == LSU_TYPE_EXPORT.BILAN_PERIODIQUE
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

        $scope.exportLSU = async function(getUnheededStudents) {
            await Utils.runMessageLoader($scope);
            $scope.inProgress = true;
            $scope.params.type = ""+ $scope.params.type;
            $scope.noStudent = false;
            $scope.lsu.export($scope.params, getUnheededStudents)
                .then(async function(res){
                    if(!$scope.lsu.hasUnheededStudents) {
                        let blob = new Blob([res.data]);
                        let link = document.createElement('a');
                        link.href = window.URL.createObjectURL(blob);
                        link.download = res.headers['content-disposition'].split('filename=')[1];
                        document.body.appendChild(link);
                        link.click();
                        $scope.errorResponse = null;
                    }
                    await Utils.stopMessageLoader($scope);
                })

                .catch(async (error) => {
                    if($scope.lsu.errorsLSU !== null && $scope.lsu.errorsLSU !== undefined
                        && ($scope.lsu.errorsLSU.all.length > 0
                            || $scope.lsu.errorsLSU.errorCode.length > 0
                            || $scope.lsu.errorsLSU.emptyDiscipline)
                    ){
                        $scope.opened.lightboxErrorsLSU = true;
                    }
                    else {
                        if ($scope.lsu.errorsLSU !== null && $scope.lsu.errorsLSU !== undefined
                            && !_.isEmpty($scope.lsu.errorsLSU.errorMessageBadRequest)) {
                            if (_.contains($scope.lsu.errorsLSU.errorMessageBadRequest, "getEleves : no student")) {
                                $scope.noStudent = true;
                                notify.info('evaluation.lsu.error.getEleves.no.student');
                            }
                            console.error(error);
                            $scope.errorResponse = true;
                        }
                    }
                    await Utils.stopMessageLoader($scope);
                });
        };

        $scope.chooseClasse = async function (classe) {
            await Utils.chooseClasse(classe,$scope, false);
            await utils.safeApply($scope);
        };
        $scope.controleAllIgnored = function (students, periodes): boolean {
            let allStudentChoose = $scope.controleExportLSU();
            if(allStudentChoose === false) {
                for(let i =0; i < students.length; i++){
                    let student = students[i];
                    if (student.hasOwnProperty("choose")) {
                        if ($scope.params.type === LSU_TYPE_EXPORT.BILAN_PERIODIQUE) {
                            for (let index = 0; index < periodes.length; index++) {
                                let currentPeriode = periodes[index];
                                if (currentPeriode.selected === true && student.choose[index] !== true) {
                                    allStudentChoose = true;
                                    break;
                                }
                            }
                        }
                        else if ($scope.params.type === LSU_TYPE_EXPORT.BFC) {
                            if (student.choose[0] !== true) {
                                allStudentChoose = true;
                                break;
                            }
                        }
                    }
                }
            }
            return allStudentChoose;
        };
        $scope.updateFilters = async function(classes){
            if( !_.isEmpty(classes) ) {
                _.map( classes, (classe) => {
                    classe.selected = true;
                });
                $scope.printClasses = {
                    all: classes
                };
                await utils.updateFilters($scope, false);
                if( !_.isEmpty( $scope.params.periodes_type) &&  !_.isEmpty( $scope.filteredPeriodes )){
                    _.forEach( $scope.filteredPeriodes, (filteredPeriode) => {
                        let periodeToSelected =_.findWhere($scope.params.periodes_type, {id_type: filteredPeriode.id_type});
                        if(periodeToSelected !== undefined){
                            if(periodeToSelected.selected !== undefined ){
                                filteredPeriode.selected = periodeToSelected.selected;
                            }
                            periodeToSelected.periode.id_classe = filteredPeriode.periode.id_classe;
                            periodeToSelected.classes = filteredPeriode.classes;
                        }
                    });
                }

                if ( _.isEmpty($scope.filteredPeriodes )) {
                    notify.info('evaluations.classes.are.not.initialized');
                }
                await utils.safeApply($scope);
            }else{
                if( !_.isEmpty($scope.params.periodes_type )){
                    _.each($scope.params.periodes_type, ( periode_type ) => {
                        periode_type.classes =[];
                    });
                }
            }
        };

        $scope.changeUnheededStudents = async function (students, changeTo?, periode?, index?){
            if(changeTo === undefined) {
                if(students.hasOwnProperty("choose")){
                    let choose = (index === undefined)? students.choose : students.choose[index];
                    let idperiode = null;
                    if(periode!== null && periode === undefined){
                        idperiode = students.ignoredInfos
                    } else if (periode!== null && periode !== undefined){
                        idperiode = periode.id_type;
                    }
                    let idClasse = (students.idClasse !== undefined)?students.idClasse : students.id_classe;
                    let idStudents = [students.idEleve];
                    let doChange = async (periodeId) => {
                        if(choose) {
                            await $scope.lsu.addUnheededStudents(idStudents, periodeId, idClasse);
                        }
                        else{
                            await $scope.lsu.remUnheededStudents(idStudents, periodeId, idClasse);
                        }
                    };
                    if(idperiode instanceof Array){
                        let allPromise = [];
                        _.forEach(idperiode, (ignoredInfo) => {
                            allPromise.push(doChange(ignoredInfo.id_periode));
                        });
                        await Promise.all(allPromise);
                    }
                    else {
                        await doChange(idperiode);
                    }
                }
            }
            else {
                await Utils.runMessageLoader($scope);
                try {
                    let allPromise = [];
                    _.forEach(students, (student) => {
                        if (student.hasOwnProperty("choose")) {
                            if (periode === undefined) {
                                if (student.choose !== changeTo) {
                                    student.choose = changeTo;
                                    allPromise.push($scope.changeUnheededStudents(student));
                                }
                            }
                            else{
                                if($scope.params.type === LSU_TYPE_EXPORT.BILAN_PERIODIQUE) {
                                    for (let index = 0; index < periode.length; index++) {
                                        let currentPeriode = periode[index];
                                        if (currentPeriode.selected === true && student.choose[index] !== changeTo) {
                                            student.choose[index] = changeTo;
                                            allPromise.push($scope.changeUnheededStudents(student, undefined,
                                                currentPeriode, index));
                                        }
                                    }
                                }
                                else if ($scope.params.type === LSU_TYPE_EXPORT.BFC){
                                    if (student.choose[0] !== changeTo) {
                                        student.choose[0] = changeTo;
                                        allPromise.push($scope.changeUnheededStudents(student, undefined, null, 0));
                                    }
                                }
                            }
                        }
                    });
                    if (!_.isEmpty(allPromise)) {
                        await Promise.all(allPromise);
                    }
                    await Utils.stopMessageLoader($scope);
                }
                catch (e){
                    await Utils.stopMessageLoader($scope);
                }
            }
        };


        /*********************************************************************************************
         * Séquence exécuté au chargement du controleur
         *********************************************************************************************/
        await initparams("1");
        $scope.getResponsables();
    }
]);

