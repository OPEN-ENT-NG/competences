import {idiom as lang, ng} from "entcore";
import {RootsConst} from "../../../core/constants/roots.const";
import {IScope} from "angular";
import {RouterLink} from "../../../models/router-link.model";

interface IViewModel extends ng.IController, ISidebarItemProps {
    translate(key: string): string
    hasChildren(): boolean;
}

interface ISidebarItemProps {
    routerLink: RouterLink;
}
interface ISidebarItemScope extends IScope, ISidebarItemProps {
    vm: IViewModel;
}

class Controller implements IViewModel {
    public routerLink: RouterLink;

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
        return this.routerLink.children.routerLinks && this.routerLink.children.routerLinks.length > 0;
    }
}

function directive() {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/sidebar/sidebar-item/sidebar-item.html`,
        scope: {
            routerLink: '<'
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

export const sidebarItem = ng.directive('sidebarItem', directive)