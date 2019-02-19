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
 * Created by anabah on 09/11/2016.
 */

import {ng, appPrefix} from 'entcore';
import * as utils from '../utils/teacher';

/**
 * Directive d'affichage de la boule des compétences
 * @param color {string}: Contient le code couleur de la boulle.
 * @param text {string}: Contient le text contenu dans la boulle.
 * @param classes {string}: Contient l'ensemble des classes css supplémentaires à appliquer à la boulle.
 * @param selectCond {boolean}: Contient la condition pour marquer une boule comme sélectionnée
 *                                          (rajouter la classe selected).
 */
export let cSkillsBubble = ng.directive('cSkillsBubble', function () {
    return {
        restrict : 'E',
        scope : {
            color : '=',
            text : '=?',
            classes : '=?',
            selectCond : '=?',
            onClick: '=?'
        },
        templateUrl : '/' + appPrefix + '/public/template/directives/cSkillsBubble.html',
        controller : ['$scope', function ($scope) {
            $scope.activeClick = false;
            if ($scope.classes === undefined) {
                $scope.classes = ' ';
            }
            if ($scope.selectCond === undefined) {
                $scope.selected = false;
            }
            if ($scope.onClick !== undefined) {
                $scope.activeClick = true;
            }
            $scope.text = $scope.text || '';
            $scope.$watch('color', function (newValue, oldValue) {
                if (newValue !== oldValue) {
                    utils.safeApply($scope);
                }
            }, true);
            $scope.$watch('text', function (newValue, oldValue) {
                if (newValue !== oldValue) {
                    utils.safeApply($scope);
                }
            });
        }]
    };
});