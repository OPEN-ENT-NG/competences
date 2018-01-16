/**
 * Created by ledunoiss on 20/09/2016.
 */
import { ng, _ } from 'entcore';

export let customSearchCompetencesFilter = ng.filter('customSearchCompetencesFilter', function(){
    return function(enseignements, enseignementsAfiltrer){
        var output = enseignements;
        var tempTable = [];

        // on ne prend que le enseignements selectionnes
        var enseignementsAfiltrer = _.where(enseignementsAfiltrer.all, {selected : true});


        // filtre sur la liste des enseignements dans l'arbre
        if (enseignementsAfiltrer != undefined && enseignementsAfiltrer.length > 0) {
            for (var i = 0; i < enseignementsAfiltrer.length; i++) {

               var enseignementAajouter =  _.findWhere(enseignements.all, {id : enseignementsAfiltrer[i].id});

                if(enseignementAajouter != undefined) {
                    tempTable.push(enseignementAajouter);
                }
            }
            output = tempTable;
        }
        return output;
    };
});