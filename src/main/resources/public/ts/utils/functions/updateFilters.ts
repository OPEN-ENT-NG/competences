import {Classe} from "../../models/teacher";
import {_} from "entcore";
import {safeApply} from "./safeApply";

export function updateFilters ($scope, withStudent) {
    return new Promise(async (resolve, reject) => {
        try {
            let selectedClasses = _.where($scope.printClasses.all, {selected: true});
            $scope.filteredPeriodes = [];
            if (selectedClasses.length === 0) {
                $scope.allElevesClasses = [];
                $scope.filteredPeriodes = [];
                safeApply($scope);
            } else {
                // synchronisation de toutes les périodes et les élèves des classes sélectionnées
                let allPromise = [];
                _.forEach(selectedClasses, (classe: Classe) => {
                    if(withStudent === true && classe.eleves.length() === 0) {
                        allPromise.push(Promise.all([classe.eleves.sync()]));
                    }
                    if(classe.periodes.length() === 0) {
                        allPromise.push(Promise.all([classe.periodes.sync()]));
                    }
                });

                await Promise.all(allPromise);
                $scope.allElevesClasses = [];
                let periodes = [];

                _.forEach(selectedClasses, (classe) => {
                    $scope.allElevesClasses = $scope.allElevesClasses.concat(classe.eleves.all);
                    periodes = _.union(periodes, classe.periodes.all);
                });

                $scope.filteredPeriodes = [];
                _.forEach(periodes, (periode) => {
                    if (periode.id_type !== undefined) {
                        let periodeToset = _.findWhere($scope.filteredPeriodes, {id_type: periode.id_type});
                        if (periodeToset === undefined) {
                            let classe = [];
                            classe.push(periode.id_classe);
                            $scope.filteredPeriodes.push({
                                id_type: periode.id_type,
                                type: periode.type,
                                periode: periode,
                                classes: classe
                            });
                        } else {
                            periodeToset.classes.push(periode.id_classe);
                        }
                    }
                });

                if ($scope.selected.periode !== undefined) {
                    $scope.selected.periode = _.findWhere($scope.filteredPeriodes, {
                        id_type: $scope.selected.periode.id_type
                    });
                }
                if (!_.isEmpty($scope.allElevesClasses)) {
                    $scope.allElevesClasses = _.sortBy($scope.allElevesClasses, function (eleve) {
                        return eleve.displayName ? eleve.displayName : eleve.lastName + ' ' + eleve.firstName;
                    })
                }
            }
            resolve($scope.filteredPeriodes);
        }
        catch (e) {
            reject(e);
        }
    });
}
