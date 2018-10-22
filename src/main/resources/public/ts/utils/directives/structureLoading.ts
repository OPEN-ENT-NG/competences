import { ng } from 'entcore';

export let structureLoader = ng.directive('structureLoader', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            structureName: '=',
            display: '='
        },
        template: '<div id="structure-loader" class="overlay" ng-show="display">' +
        '<div>' +
        '<h4 class="content-loader"> ' +
        '[[structureName]] ' +
        '</h4>' +
        '</div>' +
        '</div>',
        replace: true
    };
});