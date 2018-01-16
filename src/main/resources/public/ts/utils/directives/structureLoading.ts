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
        '<h4 class="content-loader"><i18n>viescolaire.structure.load</i18n> ' +
        '[[structureName]] ' +
        '<i18n>viescolaire.structure.load.end</i18n></h4>' +
        '</div>' +
        '</div>',
        replace: true
    };
});