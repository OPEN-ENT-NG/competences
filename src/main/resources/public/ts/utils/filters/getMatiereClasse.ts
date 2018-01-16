/**
 * Created by ledunoiss on 20/09/2016.
 */
import { ng } from 'entcore';

export let getMatiereClasseFilter = ng.filter('getMatiereClasse', function () {
    return function (matieres, idClasse, classes, search) {
        if (idClasse === '*' || idClasse === undefined) return matieres;
        if (classes.all.length > 0) {
            let classe = classes.findWhere({id : idClasse});
            if (classe !== undefined) {
                let matieresClasse = matieres.filter((matiere) => {
                    if (matiere.hasOwnProperty('libelleClasses')){
                        return (matiere.libelleClasses.indexOf(classe.externalId) !== -1)
                    } else {
                        return false;
                    }
                });
                if (matieresClasse.length > 0) {
                     return matieresClasse;
                }
                return matieres;
            }
        }
    }
});