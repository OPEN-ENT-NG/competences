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
 * Created by Samuel Jollois on 29/11/2017.
 */

import {model, ng,idiom as lang} from 'entcore';
import { evaluations } from '../models/eval_parent_mdl';
import * as utils from '../utils/parent';
import {Structure, Utils} from "../models/teacher";
import http from "axios";
import {ExportBulletins} from "../models/common/ExportBulletins";

declare let _: any;

// @ts-ignore
export let bulletinController = ng.controller('BulletinController', [
    '$scope', '$sce',
    async  function ($scope,$sce) {
        /**
         * chargement d'un releve
         * @returns {Promise<void>}
         */
        $scope.loadBulletin = async function () {
            try {
                /*let url = "/competences/student/bulletin/parameters?idEleve=" + $scope.searchBulletin.eleve.id
                    + "&idPeriode=" + $scope.searchBulletin.periode.id_type
                    + "&idStructure=" + $scope.searchBulletin.periode.id_etablissement;
                let data = await http.get(url);
                if(data.status == 204){
                    //empty result, le bulletin n'a pas encore été généré
                    $scope.content = undefined;
                }else{
                    let options = data.data;
                    options.images = {}; // contiendra les id des images par élève
                    options.idImagesFiles = []; // contiendra tous les ids d'images à supprimer après l'export

                    options.students = [];

                    _.forEach( options.idStudents, (id) => {
                        let student = {id:id,idClasse:$scope.searchBulletin.eleve.idClasse};
                        options.students.push(student);
                    });

                    if (options.showBilanPerDomaines === true) {
                        $scope.niveauCompetences = options.niveauCompetences;
                    }

                    if(!($scope.structure && $scope.structure.id))
                        $scope.structure = new Structure({id:  model.me.structures[0]});

                    if (options.showBilanPerDomaines === true && options.simple !== true) {
                        // Si on choisit de déssiner les graphes
                        await ExportBulletins.createCanvas(options, $scope);
                    }
                }*/
                // lancement de l'export et récupération du fichier généré
                let data = await http.post(`/competences/see/bulletins`, {
                    idEleve: $scope.searchBulletin.eleve.id,
                    idPeriode: $scope.searchBulletin.periode.id_type,
                    idStructure: $scope.searchBulletin.eleve.idStructure,
                    idClasse: $scope.searchBulletin.eleve.idClasse,
                    idParent: $scope.me.type === 'PERSRELELEVE' ? $scope.me.externalId : null
                },{responseType: 'arraybuffer'});
                if(data.status == 204){
                    //empty result, le bulletin n'a pas encore été généré
                    $scope.content = undefined;
                } else {
                    var file = new Blob([data.data], {type: 'application/pdf'});
                    var fileURL = window.URL.createObjectURL(file);
                    $scope.content = $sce.trustAsResourceUrl(fileURL);
                }
                await utils.safeApply($scope);
            } catch (data) {
                console.error(data);
                if(data.response != undefined && data.response.status === 500){
                    this.manageError(data.response.data, $scope);
                }
            }
        };

        let initSearchBulletin = (periode) => {
            $scope.searchBulletin = {
                eleve: evaluations.eleve,
                periode: periode,
                enseignants: evaluations.enseignants
            };
        };
        // Initialisation des variables du bulletin
        $scope.initBulletin = async function () {
            if ($scope.searchBulletin !== undefined
                && $scope.searchBulletin.periode !== undefined
                && $scope.searchBulletin.periode.id_type !== undefined) {
                initSearchBulletin(_.findWhere(evaluations.eleve.classe.periodes.all,
                    {id_type: $scope.searchBulletin.periode.id_type}));
            } else {
                initSearchBulletin(evaluations.periode);
            }

            $scope.me = {
                type: model.me.type,
                externalId: model.me.externalId
            };
            await $scope.loadBulletin();
            await utils.safeApply($scope);
        };

        await $scope.init();
        $scope.initBulletin();
        $scope.translate = lang.translate;
        // Au changement de la période par le parent
        $scope.$on('loadPeriode', async function () {
            $scope.initBulletin();
            await utils.safeApply($scope);
        });

        $scope.checkHaveResult = function () {
            if ($scope.searchBulletin.periode !== null && $scope.searchBulletin.periode.id_type !== null)
                return $scope.searchBulletin.periode.publication_bulletin && $scope.content;
            else
                return false;
        };

    }]);