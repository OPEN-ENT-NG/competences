/**
 * Created by ledunoiss on 21/09/2016.
 */
import { ng } from 'entcore';

export let pane = ng.directive('pane', function() {
    return {
        require: '^tabs',
        restrict: 'E',
        transclude: true,
        scope: {
            template: '='
        },
        link: function(scope, element, attrs, tabsCtrl) {
            tabsCtrl.addPane(scope);
        },
        template:
        '<div class="tab-pane twelve-mobile" ng-class="{active: selected}" ng-transclude>' +
        '</div>',
        replace: true
    };
});