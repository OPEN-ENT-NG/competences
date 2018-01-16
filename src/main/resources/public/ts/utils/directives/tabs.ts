/**
 * Created by ledunoiss on 21/09/2016.
 */
import { ng, angular } from 'entcore';

export let tabs = ng.directive('tabs', function() {
    return {
        restrict: 'E',
        transclude: true,
        scope: {},
        controller: [ "$scope", function($scope) {
            var panes = $scope.panes = [];

            $scope.select = function(pane) {
                angular.forEach(panes, function(pane) {
                    pane.selected = false;
                });
                pane.selected = true;
            };

            this.addPane = function(pane) {
                if (panes.length === 0) $scope.select(pane);
                panes.push(pane);
            };
        }],
        template:
        '<div class="tabbable">' +
        '<ul class="nav nav-tabs">' +
        '<li ng-repeat="pane in panes" ng-click="select(pane)" ng-class="{active:pane.selected}" class="six">'+
        '<a href=""">{{pane.title}}</a>' +
        '</li>' +
        '</ul>' +
        '<div class="tab-content twelve-mobile" ng-transclude></div>' +
        '</div>',
        replace: true
    };
})