/**
 * Created by ledunoiss on 26/10/2016.
 */

import { ng } from 'entcore';

export let autofocus = ng.directive('autofocus', ['$timeout',
    function ($timeout) {
        return {
            restrict: 'A',
            link: function ($scope, $element) {
                $timeout(function () {
                    $element[0].focus();
                });
            }
        };
    }
]);