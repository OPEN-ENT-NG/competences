import { ng } from 'entcore';

export let messageLoader = ng.directive('messageLoader', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            display: '='
        },
        template: '<div id="message-loader" class="overlay" ng-show="display">' +
        '<div>' +
        '<h4 class="content-loader"> ' +
        '<i18n>evaluations.loading</i18n>' +
        '</h4>' +
        '</div>' +
        '</div>',
        replace: true
    };
});