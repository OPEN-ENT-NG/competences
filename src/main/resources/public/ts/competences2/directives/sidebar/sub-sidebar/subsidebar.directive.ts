import {idiom as lang, ng} from "entcore";
import {RootsConst} from "../../../core/constants/roots.const";
import {IScope} from "angular";
import {RouterLink, RouterLinkChildren} from "../../../models/router-link.model";

interface IViewModel extends ng.IController, ISidebarItemProps {
    translate(key: string): string
    hasChildren(): boolean;
}

interface ISidebarItemProps {
   children: RouterLinkChildren;
}
interface ISidebarItemScope extends IScope, ISidebarItemProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    public children: RouterLinkChildren;

    constructor(private $scope: ISidebarItemScope) {
    }


    $onInit() {
    }

    $onDestroy() {
    }

    translate(key: string): string {
        return lang.translate(key);
    }

    hasChildren(): boolean {
        return this.children && this.children.routerLinks && this.children.routerLinks.length > 0;
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/sidebar/sub-sidebar/subsidebar.html`,
        scope: {
            children: '<'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', Controller],
        /* interaction DOM/element */
        link: function ($scope: ISidebarItemScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: IViewModel) {
        }
    }
}

export const subsidebar = ng.directive('subsidebar', directive)