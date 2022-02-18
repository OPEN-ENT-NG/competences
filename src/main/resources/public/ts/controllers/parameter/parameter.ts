import {appPrefix, ng, template, toasts} from "entcore";
import {ParameterService} from  "../../services"
declare const window: any;

/**
 Parameter controller
 ------------------.
 **/
export const parameterController = ng.controller("parameterController", [
    "$scope", "ParameterService", async ($scope, parameterService: ParameterService) => {
        template.open("main", "parameter/parameter");
        $scope.counter = {
            value: 0
        };

    $scope.exports = await parameterService.getExports();
    console.log($scope.exports)
}]);
