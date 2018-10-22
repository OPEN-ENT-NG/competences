import { ng } from 'entcore';

export let messageLoader = ng.directive('messageLoader', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {
            message: '=',
            display: '='
        },
        template: '<div id="message-loader" class="overlay" ng-show="display">' +
        '<div>' +
        '<h4 class="content-loader"> ' +
        '[[message]] ' +
        '</h4>' +
        '</div>' +
        '</div>',
        replace: true
    };
});