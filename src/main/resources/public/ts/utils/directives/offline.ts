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