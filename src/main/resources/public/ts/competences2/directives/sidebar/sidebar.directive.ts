import {ng} from "entcore";
import {RootsConst} from "../../core/constants/roots.const";
import {ILocationService, IParseService, IScope, IWindowService} from "angular";
import {RouterLink} from "../../models/router-link.model";
import {routerLinks} from "../../core/constants/router-link.const";
import {competencesLink} from "../../core/constants/competences-link.const";

interface IViewModel {
    routerLinks: Array<RouterLink>;
}

interface ISidebarScope extends IScope {
    vm: IViewModel;
}

class Controller implements ng.IController, IViewModel {

    routerLinks: Array<RouterLink>;

    constructor(private $scope: ISidebarScope,
                private $location: ILocationService,
                private $window: IWindowService) {
        // init all competences router
        this.routerLinks = routerLinks;
        this.manageAccessRoute();
        this.filterAccessRoute();
    }

    $onInit() {
    }
    private manageAccessRoute(): void {
        this.routerLinks.forEach((routerLink: RouterLink) => {
            if (routerLink.children && (routerLink.children.routerLinks && routerLink.children.routerLinks.length > 0)) {
                routerLink.children.routerLinks.forEach((subRouterLink: RouterLink) => this.checkRouteAccess(subRouterLink));
            }
            this.checkRouteAccess(routerLink);
        });
    }

    private checkRouteAccess(routerLink: RouterLink): void {
        switch (routerLink.link) {
            case competencesLink.HOME: {
                routerLink.canAccess = true;
                break;
            }
            case competencesLink.NOTES: {
                routerLink.canAccess = true;
                break;
            }
            case competencesLink.FOLLOW: {
                routerLink.canAccess = true;
                break;
            }
            case competencesLink.ORIENTATIONS: {
                routerLink.canAccess = true;
                break;
            }
            case competencesLink.EXPORTS: {
                routerLink.canAccess = false;
                break;
            }
        }
    }
    private filterAccessRoute(): void {
        this.routerLinks = this.routerLinks.filter((routerLink: RouterLink) => {
            if (routerLink.children && (routerLink.children.routerLinks && routerLink.children.routerLinks.length > 0)) {
                routerLink.children.routerLinks = routerLink.children.routerLinks.filter((subRouterLink: RouterLink) => subRouterLink.canAccess);
            }
            return routerLink.canAccess;
        });
    }

    $onDestroy() {
    }

}

function directive($parse: IParseService) {
    return {
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/sidebar/sidebar.html`,
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', '$parse', Controller],
        /* interaction DOM/element */
        link: function ($scope: ISidebarScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: ng.IController) {
        }
    }
}

export const sidebar = ng.directive('sidebar', directive)