import {appPrefix, ng} from "entcore";
import { FilterNotEvaluatedConnaissance} from "../utils/filters/filterNotEvaluatedEnseignement";


export let teachingsSkills = ng.directive ('teachingsSkills', function(){
    return {
        restrict : 'E',
        scope : {
            data : '=',
            suiviFilter : '=',
            listTeacher : '=',
            isClasse : '=',
            mapCouleurs : '=',
            mapLettres : '=',
            functionOpenDetailCompetence: '=',
            functionFilterNotEvaluated: '=',
            isCycle: '=?',
            level: '=?',
            isYear:'=?',
            trimesters:'=?'
        },

        templateUrl : " /"+appPrefix+"/public/template/directives/teachingsSkills.html ",

        controller : ['$scope','$timeout', function($scope,$timeout) {
            $scope.FilterNotEvaluatedConnaissance = function (maConnaissance) {
                return FilterNotEvaluatedConnaissance(maConnaissance);
            };

            var timer;
            // mouseenter event
            $scope.showIt = function (item) {
                timer = $timeout(function () {
                    item.hovering = true;
                }, 350);
            };

            // mouseleave event
            $scope.hideIt = function (item) {
                $timeout.cancel(timer);
                item.hovering = false;
            };

        }]
    };
});