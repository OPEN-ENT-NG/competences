import { ng } from 'entcore';

export let mobilePanel = ng.directive('mobilePanel', function(){
    return{
        restrict: 'E',
        transclude: true,
        scope : {
            displayed : "="
        },
        template: '<div class="mobile-panel [[side]]" ng-class="{displayed : displayed}">'+
        '<div class="close-mobile-panel"><i class="close"></i></div>'+
        '<div class="content" ng-transclude></div>'+
        '</div>',
        link: function($scope, $elem, $attrs){
            $elem.children('.mobile-panel').children('.close-mobile-panel').on('click', function(e){
                setTimeout(function(){
                    $scope.displayed = false;
                    $scope.$apply();
                }, 0);
            });

            $scope.$watch(function(){return $scope.$eval($attrs.side);}, function(){
                $scope.side = $attrs.side;
            });
        }
    };
});