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

import {model, ng, idiom as lang, moment} from 'entcore';
import {evaluations as evaluationsParentFormat, evaluations} from '../models/eval_parent_mdl';
import * as utils from '../utils/parent';
import {Utils} from "../models/teacher";
import http from "axios";
import {SubTopicsServiceService} from "../services/SubTopicServiceService";
import {ClassesService} from "../services/classes.service";
import {SubTopicsServices} from "../models/sniplets";

declare let _: any;

// @ts-ignore
export let releveController = ng.controller('ReleveController', [
    '$scope', '$location',
    async  function ($scope, $location) {
        /**
         * Calcul la moyenne pour chaque matière contenue dans $scope.matieres
         */
        $scope.calculMoyenneMatieres = async function () {
            if ($scope.dataReleve === undefined) {
                return;
            }
            let subTopicsServiceService = new SubTopicsServiceService();
            let {data} =  await subTopicsServiceService.get($scope.eleve.idStructure)
            let classAndGroups = await ClassesService.getClassesAndGroup($scope.eleve.idStructure);
            let subTopicsServicesStruct = new SubTopicsServices([],data);
            let classe = _.findWhere(classAndGroups, {id_classe : $scope.eleve.classe.id});
            let subTopicsServices = subTopicsServicesStruct.filter(subTopic =>
                subTopic.id_group === $scope.eleve.classe.id || (classe != undefined && _.contains(classe.id_groupes, subTopic.id_group))
            );

            await utils.calculMoyennesWithSubTopic($scope.searchReleve.periode.id_type, $scope.eleve.id, $scope.matieresReleve,
                $scope.matieres, $scope.dataReleve.devoirs, subTopicsServices, $scope.eleve.classe);

            await utils.safeApply($scope);
        };
        /**
         * chargement d'un releve
         * @returns {Promise<void>}
         */
        $scope.loadReleveNote = async function () {
            await Utils.runMessageLoader($scope);
            let eleve = $scope.searchReleve.eleve;

            await evaluations.devoirs.sync(eleve.idStructure, eleve.id, undefined, $scope.searchReleve.periode);
            $scope.dataReleve = {
                devoirs: evaluations.devoirs
            };

            $scope.matieresReleve = _.filter(evaluations.matieres.all, (m) => m.hasDevoirWithNote);
            await $scope.calculMoyenneMatieres();

            await Utils.stopMessageLoader($scope);
        };

        // Impression du releve de l'eleve
        $scope.getReleve = async function () {
            Utils.runMessageLoader($scope);
            let type_periode = _.findWhere(evaluations.eleve.classe.typePeriodes.all,
                {id: $scope.searchReleve.periode.id_type});
            if (type_periode !== undefined) {
                await evaluations.getReleve($scope.searchReleve.periode.id_type,
                    $scope.searchReleve.eleve.id, type_periode.type, type_periode.ordre);
            } else {
                await evaluations.getReleve(undefined,
                    $scope.searchReleve.eleve.id, undefined, undefined);
            }
            Utils.stopMessageLoader($scope);
        };


        let initSearchReleve = (periode) => {
            $scope.searchReleve = {
                eleve: evaluations.eleve,
                periode: periode,
                enseignants: evaluations.enseignants
            };
        };
        // Initialisation des variables du relevé
        $scope.initReleve = async function () {
            $scope.dataReleve = {
                devoirs: evaluations.devoirs
            };
            if ($scope.searchReleve !== undefined
                && $scope.searchReleve.periode !== undefined
                && $scope.searchReleve.periode.id_type !== undefined) {
                initSearchReleve(_.findWhere(evaluations.eleve.classe.periodes.all,
                    {id_type: $scope.searchReleve.periode.id_type}));
            } else {
                initSearchReleve(evaluations.periode);
            }

            $scope.me = {
                type: model.me.type
            };
            $scope.matieresReleve = _.filter(evaluations.matieres.all, (m) => m.hasDevoirWithNote);

            await $scope.loadReleveNote();
            await Utils.stopMessageLoader($scope);
            await utils.safeApply($scope);
        };

        await $scope.init();
        $scope.initReleve();
        $scope.translate = lang.translate;
        // Au changement de la période par le parent
        $scope.$on('loadPeriode', async function () {
            $scope.initReleve();
            await utils.safeApply($scope);
        });

        /*$scope.hasEvaluatedDevoir = (matiere) => {
            if($scope.dataReleve) {
                let devoirWithNote = $scope.dataReleve.devoirs.filter((devoir) => {
                    return (devoir.note !== undefined || devoir.annotation !== undefined)
                });
                return _.findWhere(devoirWithNote, {id_matiere: matiere.id, is_evaluated: true}) !== undefined;
            }else{
                return false;
            }
        };*/

        $scope.isEvaluated = (devoir) => {
            return devoir.is_evaluated && (devoir.note !== undefined || devoir.annotation !== undefined);
        };

        $scope.hasDevoirWithUnderSubject = (sousMat) => {
            let devoirWithNote = $scope.dataReleve.devoirs.all.filter((devoir) => {
                return (devoir.note !== undefined || devoir.annotation !== undefined)
            });
            return _.some(devoirWithNote,
                {id_matiere: sousMat.id_matiere, id_sousmatiere: sousMat.id_type_sousmatiere, is_evaluated: true});
        };

        $scope.checkHaveResult = function () {
            return $scope.matieresReleve.length > 0;
        };

    }]);