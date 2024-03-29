import {_} from "entcore";
import {Utils} from "../../models/teacher";

export let setHistoriqueEvenement = function ($scope, eleve, filteredPeriode) {
    let year = {
        retard: 0,
        abs_just: 0,
        abs_non_just: 0,
        abs_totale_heure: 0,
        ordre: 0,
        periode: $scope.getI18nPeriode({id: null})
    };

    let periodes = _.filter(filteredPeriode, (p) => {
        return p.id_type > -1
    });

    eleve.evenements = _.filter(eleve.evenements, (evenement) => {
        return _.contains(_.pluck(periodes, 'id_type'), evenement.id_periode);
    });

    _.forEach(periodes, (periode) => {
        let evenement = _.findWhere(eleve.evenements, {id_periode: periode.id_type});
        let pushIt = false;
        if (evenement === undefined) {
            evenement = {id_periode: periode.id_type};
            if(Utils.isNotNull($scope.search.classe) && Utils.isNotNull($scope.search.classe.periodes) &&
                !_.isEmpty(_.where($scope.search.classe.periodes.all, {id_type : periode.id_type}))) {
                pushIt = true;
            }
        }

        evenement.periode = $scope.getI18nPeriode(periode);

        // initialisation des retards et absences
        evenement.retard = (evenement.retard !== undefined) ? evenement.retard : 0;
        evenement.abs_just = (evenement.abs_just !== undefined) ? evenement.abs_just : 0;
        evenement.abs_non_just = (evenement.abs_non_just !== undefined) ? evenement.abs_non_just : 0;
        evenement.abs_totale_heure = (evenement.abs_totale_heure !== undefined) ?
            evenement.abs_totale_heure : 0;
        evenement.ordre = (evenement.ordre !== undefined) ? evenement.ordre : 0;

        // Remplissage de la ligne pour l'année
        year.retard += evenement.retard;
        year.abs_just += evenement.abs_just;
        year.abs_non_just += evenement.abs_non_just;
        year.abs_totale_heure += evenement.abs_totale_heure;
        year.ordre += evenement.ordre;

        if (periode.id_type === $scope.search.periode.id_type) {
            eleve.evenement = evenement;
        }
        if (pushIt) {
            eleve.evenements.push(evenement);
        }
    });

    if(Utils.isNull($scope.search.periode.id_type)){
        eleve.evenement = year;
        eleve.appreciationCPE = {};
    }
    eleve.evenements.push(year);
};