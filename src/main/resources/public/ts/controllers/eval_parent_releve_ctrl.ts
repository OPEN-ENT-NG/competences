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

import { model, ng, idiom as lang } from 'entcore';
import { evaluations } from '../models/eval_parent_mdl';
import * as utils from '../utils/parent';
import {Utils} from "../models/teacher";
import http from "axios";

declare let _: any;

// @ts-ignore
export let releveController = ng.controller('ReleveController', [
    '$scope', '$location',
    async  function ($scope, $location) {
        /**
         * Calcul la moyenne pour chaque matière
         * contenue dans $scope.matieres
         */
        $scope.calculMoyenneMatieres = async function () {
            if ($scope.dataReleve === undefined) {
                return;
            }
            let id_eleve;
            if (model.me.type === 'PERSRELELEVE') {
                id_eleve = $scope.searchReleve.eleve.id;
            } else {
                id_eleve = $scope.eleve.id;
            }
            await utils.calculMoyennes($scope.search.periode.id_type,id_eleve,$scope.matieresReleve.all,$scope.dataReleve.devoirs);
            await utils.safeApply($scope);
        };
        /**
         * chargement d'un
         * @returns {Promise<void>}
         */
        $scope.loadReleveNote = async function () {
            await Utils.runMessageLoader($scope);
            let eleve = $scope.searchReleve.eleve;
            let idPeriode = undefined;
            if ($scope.searchReleve.periode !== null && $scope.searchReleve.periode.id_type !== null && $scope.searchReleve.periode.id_type !== -1) {
                idPeriode = $scope.searchReleve.periode.id_type;
            }
            //let idClasse = (eleve.classe!==undefined)?eleve.classe.id : undefined;
            await evaluations.devoirs.sync(eleve.idStructure, eleve.id, undefined, idPeriode);
            $scope.dataReleve = {
                devoirs: evaluations.devoirs
            };
            $scope.matieresReleve = evaluations.matieres;
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
            $scope.matieresReleve = evaluations.matieres;
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

        // Filter
        $scope.hasEvaluatedDevoir = (matiere) => {
            if($scope.dataReleve) {
                let devoirWithNote = $scope.dataReleve.devoirs.filter((devoir) => {
                    return (devoir.note !== undefined || devoir.annotation !== undefined)
                });
                return _.findWhere(devoirWithNote, {id_matiere: matiere.id, is_evaluated: true}) !== undefined;
            }else{
                return false;
            }
        };
        $scope.isEvaluated = (devoir) => {
            return devoir.is_evaluated && (devoir.note !== undefined || devoir.annotation !== undefined);
        };

        $scope.hasDevoirWithUnderSubject = (sousMat) => {
            let devoirWithNote = $scope.dataReleve.devoirs.filter((devoir) => {
                return (devoir.note !== undefined || devoir.annotation !== undefined)
            });
            return _.some(devoirWithNote,
                {id_matiere: sousMat.id_matiere, id_sousmatiere: sousMat.id_type_sousmatiere, is_evaluated: true});
        };

        $scope.checkHaveResult = function () {
            let res = false;
            _.forEach(evaluations.matieres.all, (matiere) => {
                if ($scope.hasEvaluatedDevoir(matiere)) {
                    res = true;
                }
            });
            return res;
        };

    }]);