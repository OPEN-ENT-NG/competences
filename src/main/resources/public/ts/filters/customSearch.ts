/**
 * Created by ledunoiss on 20/09/2016.
 */
import { ng, moment } from 'entcore';

declare let _:any;

export let customSearchFilter = ng.filter('customSearchFilters', function(){
    return function(devoirs, searchParams){
        var output = devoirs;
        var tempTable = [];
        if (searchParams.classe !== '*' && searchParams.classe !== null) {
            tempTable = _.where(output, {id_groupe : searchParams.classe.id});
            output = tempTable;
        }
        if (searchParams.matiere !== '*' && searchParams.matiere !== null) {
            tempTable = _.where(output, {id_matiere : searchParams.matiere.id});
            output = tempTable;
        }
        if (searchParams.sousmatiere !== '*' && searchParams.sousmatiere !== null) {
            tempTable = _.where(output, {id_sousmatiere : parseInt(searchParams.sousmatiere.id)});
            output = tempTable;
        }
        if (searchParams.type !== '*' && searchParams.type !== null) {
            tempTable = _.where(output, {id_type : parseInt(searchParams.type.id)});
            output = tempTable;
        }
        if (searchParams.periode !== undefined && searchParams.periode !== '*' && searchParams.periode !== null &&
            searchParams.periode.id !== null) {
            if(searchParams.periode.id_type === undefined) {
                searchParams.periode.id_type = searchParams.periode.id;
            }
            tempTable = _.where(output, {id_periode : parseInt(searchParams.periode.id_type )});
            output = tempTable;
        }
        if (searchParams.name !== "" && searchParams.name !== null) {
            tempTable = _.filter(output, function (devoir){
                var  reg = new RegExp(searchParams.name.toUpperCase());
                return devoir.name.toUpperCase().match(reg) !== null;
            });
            output = tempTable;
        }

        if (searchParams.enseignant !== undefined && searchParams.enseignant !== '*' && searchParams.enseignant !== null) {
            tempTable = _.where(output, {owner : searchParams.enseignant.id});
            output = tempTable;
        }

        return output;
    };
});