/**
 * Created by ledunoiss on 21/09/2016.
 */
import {ng, appPrefix, idiom as lang, _} from 'entcore';

export let cFilAriane = ng.directive("cFilAriane", ["$location", "route", "$rootScope", "$route", function($location, routes, $rootScope, $route){
    return {
        templateUrl: "/"+appPrefix+"/public/template/directives/ariane.html",
        restrict : "E",
        link : function($scope, element, attrs){
            /**boolean si vrai alors l'état d'avant était création de devoir
             * @type {boolean}
             */
            $scope.checkIfIsTodelete ;
            /**
             * les URLS suprimé du fil d'ariane just après les avoir quitter
             * @type {[string]}
             */
            $scope.$location = $location;
            $scope.ToDelete = [
                "/devoir/create"
            ];
            if($scope.ariane === undefined && $route.current.originalPath !== $route.routes.null.redirectTo ){
                var initFilNonAcceuil = true;
            }
            $scope.ariane = [];

            let getSize = function(obj): number {
                var size = 0, key;
                for (key in obj) {
                    if (obj.hasOwnProperty(key)) size++;
                }
                return size;
            };

            $scope.ariane.push({stateName : appPrefix+".title", url : $route.routes.null.redirectTo });
            if(initFilNonAcceuil === true){
                var state = {
                    stateName : "ariane."+appPrefix+"."+$route.current.action,
                    url : ""
                };
                let url = $route.originalPath;
                if (getSize($route.current.params) > 0 && url !== undefined) {
                    let params = $route.current.params;
                    _.map(params, (param) => {
                        if (typeof param !== 'string'){
                            param.done = false;
                        }
                    });
                    for (let p in params) {
                            if (url.indexOf(':' + p)) {
                                url.replace(':' + p, params[p]);
                            } else {
                                if (url.indexOf('?') === -1) url = url + '?';
                                url = url + p + '=' + params[p] + '&';
                            }
                    }
                    state.url = url.slice(0, -1);
                }
                $scope.ariane.push(state);
            }
            $rootScope.$on("$routeChangeSuccess", function($currentRoute, $previousRoute, $location){
                if($route.current.originalPath === $route.routes.null.redirectTo || $route.current.action === undefined){
                    $scope.ariane.splice(1, $scope.ariane.length-1);
                }else{
                    /**
                     * si l'état précédent est à suprimé
                     */
                    if( $scope.checkIfIsTodelete === true && $scope.ariane.length > 1 ){
                        $scope.ariane.splice($scope.ariane.length-1, 1);
                        $scope.checkIfIsTodelete = undefined;
                    }
                    /**
                     * si l'état actuelle est à suprimé
                     */
                    if( _.contains($scope.ToDelete, $route.current.originalPath) ){
                        $scope.checkIfIsTodelete = true;
                    }

                    /**
                     * si l'état existe déja
                     * @type {Eleve|T|any}
                     */
                    var o = _.findWhere($scope.ariane, {stateName: "ariane."+appPrefix+"."+$route.current.action,});
                    if(o!== undefined){
                        var i = $scope.ariane.indexOf(o);
                    }else{
                        i=-1;
                    }
                    if(i !== -1){
                        $scope.ariane.splice(i+1, $scope.ariane.length-1);
                    }else{
                        var state = {
                            stateName : "ariane."+appPrefix+"."+$route.current.action,
                            url : ""
                        };
                        let url = $route.current.$$route.originalPath;
                        let params = $route.current.params;
                        for (let p in params) {
                            if (url.indexOf(':' + p) !== -1) {
                                url = url.replace(':' + p, params[p]);
                            } else {
                                if (url.indexOf('?') === -1) url = url + '?';
                                url = url + p + '=' + params[p] + '&';
                            }
                        }
                        if (url.indexOf('=') !== -1) url.slice(0, -1);
                        state.url = url;
                        $scope.ariane.push(state);
                    }
                }
            });

            $scope.isLast = function(state){
                return $scope.ariane.indexOf(state)+1 === $scope.ariane.length;
            };

            $scope.getI18nValue = function(i18nKey){
                return lang.translate(i18nKey);
            };

            $scope.$on('change-params', function (event, updatedUrl) {
                var o = _.findWhere($scope.ariane, {stateName: "ariane."+appPrefix+"."+$route.current.action,});
                if(o!== undefined){
                    var i = $scope.ariane.indexOf(o);
                }else{
                    i=-1;
                }
                if(i !== -1) {
                    o.url = updatedUrl;
                }
                });

        }

    };
}]);