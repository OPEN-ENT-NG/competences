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
import {ng, _, notify, moment, idiom as lang, toasts} from "entcore";
import {Classe, LSU, Utils} from '../models/teacher';
import * as utils from '../utils/teacher';
import {LSU_TYPE_EXPORT} from "../models/common/LSU";
import {STSFile, STSFiles} from "../models/common/STSFile";
import {Archives} from "../models/common/Archives";
import http from "axios";
import {archivesService} from "../services/archives.service";

export let exportControleur = ng.controller('ExportController', ['$scope',
    async function ($scope) {

        async function initparams(typeLSU, typeArchive?, stsFile?, errorResponse?) {
            getResponsablesAndClasses();
            $scope.lsu = new LSU($scope.structure.id,
                $scope.structure.classes.where({type_groupe: Classe.type.CLASSE}),
                $scope.structure.responsables.all);
            $scope.archive = new Archives($scope.structure.id);
            $scope.allClasses = $scope.structure.classes.where({type_groupe: Classe.type.CLASSE});
            $scope.errorResponse = (errorResponse !== undefined) ? errorResponse : null;
            $scope.inProgress = false;
            $scope.showMessageBulletin = false;
            $scope.filteredPeriodes = [];

            $scope.paramsLSU = {
                type: typeLSU,
                idStructure: $scope.structure.id,
                classes: [],
                responsables: [],
                periodes_type: [],
                stsFile: (stsFile !== undefined) ? stsFile : null
            };

            $scope.LSU_TYPE_EXPORT = LSU_TYPE_EXPORT;
            if(typeArchive !== undefined) {
                $scope.paramsArchive = {
                    type: typeArchive,
                    idStructure: $scope.structure.id
                };
            }
            await $scope.getYearsAndPeriodes();
            await $scope.getArchives();
            await utils.safeApply($scope);
        }

       async function initSelectStsFiles() {
            $scope.selectStsFiles = new STSFiles();
            await $scope.selectStsFiles.sync($scope.structure.id);
            if ($scope.selectStsFiles.selected != null) {
                $scope.paramsLSU.stsFile = $scope.selectStsFiles.selected.content;
            }
            await utils.safeApply($scope);
        }

        $scope.changeType = async function (type: String) {
            await initparams(type);
        };

        $scope.dropComboModel = async function (el: any, table: any) {
            table.splice(table.indexOf(el), 1);
            await utils.safeApply($scope);
        };

        $scope.togglePeriode = async function (periode_type) {
            if (periode_type.libelle === undefined) {
                periode_type.libelle = $scope.getI18nPeriode(periode_type.periode);
            }

            let idx = $scope.paramsLSU.periodes_type.indexOf(_.findWhere($scope.paramsLSU.periodes_type,
                {id_type : periode_type.id_type}));
            if (idx > -1) {
                $scope.paramsLSU.periodes_type.splice(idx, 1);
            } else {
                $scope.paramsLSU.periodes_type.push(periode_type);
            }
            await utils.safeApply($scope);
        };

         $scope.changeStructure = async () => {
             $scope.structure = $scope.evaluations.structure =
                 _.findWhere($scope.evaluations.structures.all, {id: $scope.lsu.idStructure})
             await initparams($scope.paramsLSU.type, $scope.paramsArchive.type);
             await initSelectStsFiles();
             await utils.safeApply($scope);
        }

        function getResponsablesAndClasses() {
           if($scope.structure.responsables.length() === 0){
                $scope.structure.responsables.sync().then(async function () {
                    if($scope.structure.responsables.length() > 0)
                        $scope.lsu.responsable = $scope.structure.responsables.all[0].displayName;
                    await utils.safeApply($scope);
                });
            }

           if($scope.structure.classes.length() === 0){
               $scope.structure.classes.sync().then(async function () {
                    $scope.lsu.classes = $scope.structure.classes.where({type_groupe: Classe.type.CLASSE});
                    await utils.safeApply($scope);
                });
           }
        }

        $scope.setParamsContentFile = () => {
            $scope.paramsLSU.stsFile = $scope.selectStsFiles.selected.content;
            utils.safeApply($scope);
        };

        /**
         * Controle la validité des selections avant l'exportLSU
         */
        $scope.controleExportLSU = function () {
            $scope.inProgress = !(
                ($scope.paramsLSU.type == LSU_TYPE_EXPORT.BFC
                    && $scope.paramsLSU.classes.length > 0
                    && $scope.paramsLSU.responsables.length > 0
                    && $scope.paramsLSU.stsFile !== null)
                || ($scope.paramsLSU.type == LSU_TYPE_EXPORT.BILAN_PERIODIQUE
                    && $scope.paramsLSU.stsFile !== null
                    && $scope.paramsLSU.periodes_type.length > 0
                    && $scope.paramsLSU.classes.length > 0
                    //&& $scope.paramsLSU.classes.length < 4
                    && $scope.paramsLSU.responsables.length > 0)
            );
            utils.safeApply($scope);
            return $scope.inProgress;
        };

        $scope.uploadFile = function (files) {
            let file = files[0],
                reader = new FileReader();
            reader.onload = async () => {
                let text : any = reader.result;
                let parser = new DOMParser();
                let doc = parser.parseFromString(text, "application/xml");
                let individus = ((((utils.xmlToJson(doc) || {})['STS_EDT'] || {}).DONNEES || {}).INDIVIDUS || {}).INDIVIDU;
                let contentStsFile = utils.cleanJson(individus);
                if (contentStsFile !== null && contentStsFile !== undefined) {
                    $scope.stsFile = new STSFile($scope.lsu.idStructure, file.name, contentStsFile);
                    await $scope.stsFile.create();
                    if ($scope.stsFile.id) {
                        if ($scope.selectStsFiles === undefined) {
                            $scope.selectStsFiles = new STSFiles();
                        }
                        $scope.selectStsFiles.all.unshift($scope.stsFile);
                        $scope.selectStsFiles.selected = $scope.selectStsFiles.all[0];
                    }
                    $scope.paramsLSU.stsFile = contentStsFile;
                }
                $scope.inProgress = $scope.controleExportLSU();
                await utils.safeApply($scope);
            };
            reader.readAsText(file);
        };

        $scope.exportLSU = async function (getUnheededStudents) {
            /*if( $scope.paramsLSU.classes.length > 3 && $scope.paramsLSU.type === $scope.LSU_TYPE_EXPORT.BILAN_PERIODIQUE) {
                notify.info("Veuillez sélectionner trois classes maximum");
            } else {*/
                await Utils.runMessageLoader($scope);
                $scope.inProgress = true;
                $scope.paramsLSU.type = "" + $scope.paramsLSU.type;
                $scope.noStudent = false;
                $scope.lsu.export($scope.paramsLSU, getUnheededStudents).then(async function (res) {
                    if (!$scope.lsu.hasUnheededStudents) {
                        let blob = new Blob([res.data]);
                        let link = document.createElement('a');
                        link.href = window.URL.createObjectURL(blob);
                        link.download = res.headers['content-disposition'].split('filename=')[1];
                        document.body.appendChild(link);
                        link.click();
                        $scope.errorResponse = null;
                    }
                    await Utils.stopMessageLoader($scope);
                }).catch(async (error) => {
                    if ($scope.lsu.errorsLSU !== null && $scope.lsu.errorsLSU !== undefined
                        && ($scope.lsu.errorsLSU.all.length > 0
                            || $scope.lsu.errorsLSU.errorCode.length > 0
                            || $scope.lsu.errorsLSU.emptyDiscipline
                            || $scope.lsu.errorsLSU.errorEPITeachers.length > 0)) {
                        $scope.opened.lightboxErrorsLSU = true;
                    } else {
                        if ($scope.lsu.errorsLSU !== null && $scope.lsu.errorsLSU !== undefined
                            && !_.isEmpty($scope.lsu.errorsLSU.errorMessageBadRequest)) {
                            if (_.contains($scope.lsu.errorsLSU.errorMessageBadRequest, "getEleves : no student")) {
                                $scope.noStudent = true;
                                notify.info('evaluation.lsu.error.getEleves.no.student');
                            }else {
                                $scope.errorResponse = true;
                            }
                            console.log(error);
                        }
                    }
                    await Utils.stopMessageLoader($scope);
                });
            //}
        };

        $scope.chooseClasse = async function (classe) {
            await Utils.chooseClasse(classe, $scope, false);
            await utils.safeApply($scope);
        };

        $scope.controleAllIgnored = function (students, periodes): boolean {
            let allStudentChoose = $scope.controleExportLSU();
            if (allStudentChoose === false) {
                for (let i = 0; i < students.length; i++) {
                    let student = students[i];
                    if (student.hasOwnProperty("choose")) {
                        if ($scope.paramsLSU.type === LSU_TYPE_EXPORT.BILAN_PERIODIQUE) {
                            for (let index = 0; index < periodes.length; index++) {
                                let currentPeriode = periodes[index];
                                if (currentPeriode.selected === true && student.choose[index] !== true) {
                                    allStudentChoose = true;
                                    break;
                                }
                            }
                        } else if ($scope.paramsLSU.type === LSU_TYPE_EXPORT.BFC) {
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

        $scope.updateFilters = async function (classes) {
            if (!_.isEmpty(classes)) {
                _.map(classes, (classe) => {
                    classe.selected = true;
                });
                $scope.printClasses = {
                    all: classes
                };
                await utils.updateFilters($scope, false);
                if (!_.isEmpty($scope.paramsLSU.periodes_type) && !_.isEmpty($scope.filteredPeriodes)) {
                    _.forEach($scope.filteredPeriodes, (filteredPeriode) => {
                        let periodeToSelected = _.findWhere($scope.paramsLSU.periodes_type, {id_type: filteredPeriode.id_type});
                        if (periodeToSelected !== undefined) {
                            if (periodeToSelected.selected !== undefined) {
                                filteredPeriode.selected = periodeToSelected.selected;
                            }
                            periodeToSelected.periode.id_classe = filteredPeriode.periode.id_classe;
                            periodeToSelected.classes = filteredPeriode.classes;
                        }
                    });
                }

                if (_.isEmpty($scope.filteredPeriodes)) {
                    notify.info('evaluations.classes.are.not.initialized');
                }
                await utils.safeApply($scope);
            } else {
                if (!_.isEmpty($scope.paramsLSU.periodes_type)) {
                    _.each($scope.paramsLSU.periodes_type, (periode_type) => {
                        periode_type.classes = [];
                    });
                }
            }
        };

        $scope.changeUnheededStudents = async function (students, changeTo?, periode?, index?) {
            if (changeTo === undefined) {
                if (students.hasOwnProperty("choose")) {
                    let choose = (index === undefined) ? students.choose : students.choose[index];
                    let idperiode = null;
                    if (periode !== null && periode === undefined) {
                        idperiode = students.ignoredInfos
                    } else if (periode !== null && periode !== undefined) {
                        idperiode = periode.id_type;
                    }
                    let idClasse = (students.idClasse !== undefined) ? students.idClasse : students.id_classe;
                    let idStudents = [students.idEleve];
                    let doChange = async (periodeId) => {
                        if (choose) {
                            await $scope.lsu.addUnheededStudents(idStudents, periodeId, idClasse);
                        } else {
                            await $scope.lsu.remUnheededStudents(idStudents, periodeId, idClasse);
                        }
                    };
                    if (idperiode instanceof Array) {
                        let allPromise = [];
                        _.forEach(idperiode, (ignoredInfo) => {
                            allPromise.push(doChange(ignoredInfo.id_periode));
                        });
                        await Promise.all(allPromise);
                    } else {
                        await doChange(idperiode);
                    }
                }
            } else {
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
                            } else {
                                if ($scope.paramsLSU.type === LSU_TYPE_EXPORT.BILAN_PERIODIQUE) {
                                    for (let index = 0; index < periode.length; index++) {
                                        let currentPeriode = periode[index];
                                        if (currentPeriode.selected === true && student.choose[index] !== changeTo) {
                                            student.choose[index] = changeTo;
                                            allPromise.push($scope.changeUnheededStudents(student, undefined,
                                                currentPeriode, index));
                                        }
                                    }
                                } else if ($scope.paramsLSU.type === LSU_TYPE_EXPORT.BFC) {
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
                } catch (e) {
                    await Utils.stopMessageLoader($scope);
                }
            }
        };

        $scope.generateArchives = async function () {
            await Utils.runMessageLoader($scope);
            $scope.inProgress = true;
            $scope.showMessageBulletin = $scope.paramsArchive.type === 'bulletins';

            if($scope.paramsArchive.year === $scope.years[0].id){
                $scope.paramsArchive.periodes_type = _.pluck(_.filter($scope.currentYearTypesPeriodes, (type_periode) => {
                    return type_periode.selected;
                }), "id");
            }
            $scope.archive.export($scope.paramsArchive).then(async function (res) {
                let blob = new Blob([res.data]);
                let link = document.createElement('a');
                link.href = window.URL.createObjectURL(blob);
                link.download = res.headers['content-disposition'].split('filename=')[1];
                link.download = link.download.split("\"").join("");
                document.body.appendChild(link);
                link.click();
                await Utils.stopMessageLoader($scope);
            }).catch(async (error) => {
                console.error(error);
                await Utils.stopMessageLoader($scope);
            });
        };

        $scope.getYearsAndPeriodes = async function () {
            $scope.currentYearTypesPeriodes = [];
            $scope.cycles = $scope.structure.cycles;
            $scope.years = [];

            let url = '/competences/archive/years?idStructure=' + $scope.structure.id +
                '&type=' + $scope.paramsArchive.type;
            await http.get(url).then(function (data) {
                if(data.status === 200){
                    let res = data.data;

                     let active_year = res.active_year;
                     $scope.currentYearTypesPeriodes = _.filter($scope.structure.typePeriodes.all , (type) => {
                         type.selected = true;
                         return active_year.periodes.includes(type.id);
                     });

                    let years = res.years;
                    let start_active_year = active_year.start_date.substring(0, 4);
                    let end_active_year = active_year.end_date.substring(0, 4);
                    if(years && years.length > 0) {
                        years.forEach(year => {
                            let nextYear = (parseInt(year.id_annee) + 1);
                            let libelle = year.id_annee + " - " + nextYear;
                            if(start_active_year === year.id_annee && end_active_year === nextYear.toString()) {
                                libelle += " (Année en cours)";
                            }

                            $scope.years.push({
                                id: year.id_annee,
                                libelle: libelle
                            });
                        });
                        $scope.years = _.sortBy($scope.years, (year) =>  year.id * -1); // sort descending
                    } else {
                        $scope.years.push({
                            id: start_active_year,
                            libelle: start_active_year + " - " + end_active_year + " (Année en cours)"
                        });
                    }
                    $scope.paramsArchive.year = $scope.years[0].id;

                    utils.safeApply($scope);
                } else if(data.status === 204){
                    toasts.info("competence.error.results.eleve");
                }
            });
        }

        $scope.getPeriodeLibelle = function (type_periode) {
            return lang.translate("viescolaire.periode." + type_periode.type) + " " + type_periode.ordre
        }

        $scope.getArchives = async function () {
            $scope.archivesBFC = [];
            $scope.archivesBulletins = [];

            $scope.archivesBFC = await archivesService.getArchivesBFC($scope.structure.id);
            $scope.archivesBulletins = await archivesService.getArchivesBulletins($scope.structure.id);

            $scope.archivesBFCActualYear = _.filter($scope.archivesBFC , (bfc) => {
                return bfc.id_annee === $scope.paramsArchive.year;
            });
            $scope.archivesBulletinsActualYear = _.filter($scope.archivesBulletins , (bulletin) => {
                return bulletin.id_annee === $scope.paramsArchive.year;
            });

        }

        $scope.loadArchiveBFCPerCycle = function (cycle): String {
            let count: number = $scope.archivesBFCActualYear.filter(function(archive){
                return archive.id_cycle === cycle.id_cycle;
            }).length;

            return cycle.libelle + " : " + count + lang.translate("evaluations.archives.bfc.archived");
        }

        $scope.loadArchiveBulletinsPerPeriode = function (type_periode): String {
            let count: number = $scope.archivesBulletinsActualYear.filter(function(archive){
                return archive.id_periode === type_periode.id;
            }).length;

            return $scope.getPeriodeLibelle(type_periode) + " : " + count + lang.translate("evaluations.archives.bulletins.archived");
        }

        $scope.filter
        /*********************************************************************************************
         * Séquence exécuté au chargement du controleur
         *********************************************************************************************/
        await initparams("2","bfc");
        await initSelectStsFiles();
    }
]);

