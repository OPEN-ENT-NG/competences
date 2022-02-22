import {appPrefix, ng, template, toasts} from "entcore";
import {ParameterService} from  "../../services"
declare const window: any;

/**
 Parameter controller
 ------------------.
 **/
export const adminParameterController = ng.controller("adminParameterController", ["$scope", "ParameterService",
    function ($scope, parameterService: ParameterService) {
        console.log(" controller")
        $scope.exports = [];
        this.$onInit  = async function () {
            console.log("init")
            $scope.exports = await parameterService.getExports();
        }

        $scope.counter = {
            value: 0
        };
}]);