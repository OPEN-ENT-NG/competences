import {ng, template} from 'entcore';
import {IScope, IWindowService} from "angular";

declare let window: any;

interface IViewModel extends ng.IController {
}

interface IMainScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {

    constructor(private $scope: IMainScope,
                private $route: any,
                private $window: IWindowService) {
        this.$scope.vm = this;
    }

    $onInit() {

        this.$route({
            home: () => {
                template.open('main', `competences2/main`);
            }
        });

    }

    $onDestroy() {
    }

}

export const mainController = ng.controller('MainController',
    ['$scope', 'route', '$window', Controller]);
