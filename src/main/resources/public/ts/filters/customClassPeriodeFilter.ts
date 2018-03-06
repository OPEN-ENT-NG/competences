import {ng} from 'entcore';

declare let _:any;

export let customClassFilters = ng.filter('customClassFilters', function(){
    return function(classes, devoirs, searchParams){
        let output = devoirs;
        let tempTable = [];
        let result = classes;

        if (searchParams.periode !== undefined && searchParams.periode !== '*' && searchParams.periode !== null &&
            searchParams.periode.id !== null) {
            if(searchParams.periode.id_type === undefined) {
                searchParams.periode.id_type = searchParams.periode.id;
            }
            tempTable = _.where(output, {id_periode : parseInt(searchParams.periode.id_type )});
            output = tempTable;
        }
        result =_.filter(classes, function (classe) { return _.findWhere(output, {id_groupe: classe.id});
        });
        return result;
    };
});

export let customPeriodeFilters = ng.filter('customPeriodeFilters', function(){
    return function(periodes, devoirs, searchParams){
        let output = devoirs;
        let tempTable = [];
        let result = [];
        if (searchParams.classe !== '*' && searchParams.classe !== null) {
            tempTable = _.where(output, {id_groupe : searchParams.classe.id});
            output = tempTable;
        }
        let types =_.filter(periodes, function (periode) {
            if(periode.id_type === undefined) {
                periode.id_type = periode.id;
            }
            return _.findWhere(output,{id_periode:  parseInt(periode.id_type)});
        });



        return _.reject(periodes, function (periode) {
            let _t = _.groupBy(types, 'type');
            return !((_t!== undefined && _t[parseInt(periode.type)]!== undefined) || periode.id === null);
        });
    };
});