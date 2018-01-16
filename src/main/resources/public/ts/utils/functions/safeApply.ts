/**
 * Lance un $scope.$apply() si un $apply ou un $diggest n'est pas déjà en cours. Permet de
 * supprimer l'erreur de < $scope.$diggest already in progress >
 * @param that $scope
 * @returns {Promise<T>} Promesse
 */
export function safeApply(that) {
    return new Promise((resolve, reject) => {
        var phase = that.$root.$$phase;
        if(phase === '$apply' || phase === '$digest') {
            if(resolve && (typeof(resolve) === 'function')) resolve();
        } else {
            if (resolve && (typeof(resolve) === 'function')) that.$apply(resolve);
            else that.$apply();
        }
    });
}