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

import { ng, idiom as lang } from 'entcore';
import { evaluations } from '../models/eval_parent_mdl';
import * as utils from '../utils/parent';

declare let _: any;
declare let window: any;
declare let $: any;

export let listController = ng.controller('ListController', [
    '$scope','$location','$filter',
    async  function ($scope,  $location, $filter) {

        // Initialisation des variables
        $scope.initListDevoirs = async function () {
            if (evaluations.devoirs === undefined || evaluations.synchronised !== true) {
                await $scope.init(true);
            }
            $scope.propertyName =  'date';
            $scope.reverse = true;
            $scope.sortBy = function(propertyName) {
                $scope.reverse = ($scope.propertyName === propertyName) ? !$scope.reverse : false;
                $scope.propertyName = propertyName;
            };
            $scope.openedDevoir = -1;
            $scope.devoirs =  evaluations.devoirs;
            $scope.devoirs.all = _.map($scope.devoirs.all, function (devoir) {
                if(devoir.competences === undefined){
                    devoir.competences = [];
                }
                if(devoir.note === undefined) {
                    devoir.note = -1;
                }
                devoir.noteAnnotation = devoir.note;
                if(devoir.annotation !== undefined) {
                    devoir.noteAnnotation = devoir.annotation;
                }
                devoir.enseignant = $scope.getTeacherDisplayName(devoir.owner);
                devoir.matiere = $scope.getLibelleMatiere(devoir.id_matiere);
                return devoir;
            });

            $scope.matieres = evaluations.matieres;
            $scope.enseignants = evaluations.enseignants;
            $scope.translate = lang.translate;
            if($location.path().split('/')[2] !== "list" && $location.path() !== '/') {
                let devoirId = $location.path().split('/')[2];
                $scope.currentDevoir = _.findWhere(evaluations.devoirs.all, {id: parseInt(devoirId)});
                if ($scope.currentDevoir !== undefined) {
                    await evaluations.domaines.sync(evaluations.eleve.classe, evaluations.eleve,
                        $scope.currentDevoir.competences);
                    await $scope.currentDevoir.getAppreciation(evaluations.eleve.id);

                    $scope.suiviCompetence = {
                        domaines: evaluations.domaines
                    };
                    utils.safeApply($scope);
                }
                else {
                    if(evaluations.devoirs.all.length > 0 ) {
                        $scope.goToDevoir( evaluations.devoirs.all[0].id);
                    }
                    else {
                        window.location.hash = '#/';
                    }
                }
            }
            utils.safeApply($scope);
        };
        // Au changement de la période courante par le parent
        await $scope.initListDevoirs();
        utils.safeApply($scope);

        $scope.goToDevoir = (idDevoir) => {
            window.location.hash = '#/devoir/' + idDevoir;
        };

        $scope.checkHaveResult = function () {
            let custom = $filter('customSearchFilters');
            let filter = $filter('filter');
            let res =  custom(evaluations.devoirs.all, $scope.search);
            res = filter(res, $scope.search.name);

            return (res.length > 0);
        };
        /**
         * Ouvre le détail du devoir correspondant à l'index passé en paramètre
         * @param index index du devoir
         * @param bool état du détail
         */
        $scope.expand = function (index, bool) {
            if ($scope.openedDevoir !== index) {
                $scope.openedDevoir = index;
            } else {
                if (bool === true) {
                    $scope.openedDevoir = -1;
                }
            }
        };

        $scope.incrementDevoir = function (num) {
            let index = _.findIndex(evaluations.devoirs.all, {id: $scope.currentDevoir.id});
            if (index !== -1 && (index + parseInt(num)) >= 0
                && (index + parseInt(num)) < evaluations.devoirs.all.length) {
                let target = evaluations.devoirs.all[index + parseInt(num)];
                $scope.goToDevoir(target.id);
                $scope.currentDevoir = target;
                utils.safeApply($scope);
            }
        };

    }
]);