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

import { ng, idiom as lang } from 'entcore';
import { safeApply } from '../functions/safeApply';

export let offline = ng.directive('offline', () => {
    return {
        restrict : 'E',
        transclude : true,
        template: '<div class="offline-message card {{notification.status}}" ng-if="notification !== undefined">' +
        '<p><i class="horizontal-spacing"></i>[[getI18nMessage()]]</p>' +
        '</div>',
        controller : ['$scope', '$rootScope', '$window', ($scope, $rootScope, $window) => {
            $rootScope.online = navigator.onLine;

            /**
             * Return the i18n value based on notification status
             */
            $scope.getI18nMessage = function () {
                return lang.translate('viescolaire.connection.' + $scope.notification.status);
            };

            $window.addEventListener('offline', () => {
                $rootScope.$apply(() => {
                    $scope.notification = {
                        status: 'offline',
                    };
                    setTimeout(function () {
                        if ($scope.hasOwnProperty('notification')) {
                            $scope.notification.status = 'attempting';
                            safeApply($scope);
                        }
                    }, 3000);
                }, false);
            });
            $window.addEventListener('online', () => {
                $rootScope.$apply(() => {
                    $scope.notification.status = 'online';
                    setTimeout(function () {
                        delete $scope.notification;
                        safeApply($scope);
                    }, 3000);
                }, false);
            }, false);
        }]
    };
});