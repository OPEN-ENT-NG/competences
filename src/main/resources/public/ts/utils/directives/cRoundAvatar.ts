/**
 * Created by rollinq on 21/08/2017.
 */
import {ng, appPrefix, skin, http} from 'entcore';
import * as utils from '../../utils/parent';

export let cRoundAvatar = ng.directive("cRoundAvatar", [function () {
    return {
        templateUrl: "/" + appPrefix + "/public/template/directives/cRoundAvatar.html",
        restrict: "E",
        scope: {
            eleve: "=eleve"
        },
        link: function ($scope) {
            $scope.skin = skin;
            http().get('/userbook/avatar/' + $scope.eleve.id)
                .done(function(data){
                    if(typeof(data) == "string"){
                        $scope.userbook = true;
                        utils.safeApply($scope);
                    } else {
                        $scope.userbook = false;
                        utils.safeApply($scope);
                    }
                })
                .error(function () {
                    $scope.userbook = false;
                    utils.safeApply($scope);
                });
        }
    };
}]);