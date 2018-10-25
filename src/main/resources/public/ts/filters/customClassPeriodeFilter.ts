/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import {ng} from 'entcore';

declare let _:any;

export let customClassFilters = ng.filter('customClassFilters', function(){
    return function(classes, devoirs, searchParams){
        let output = devoirs;
        let tempTable = [];
        let result = classes;

        /*if (searchParams.periode !== undefined && searchParams.periode !== '*' && searchParams.periode !== null &&
            searchParams.periode.id !== null) {
            if(searchParams.periode.id_type === undefined) {
                searchParams.periode.id_type = searchParams.periode.id;
            }
            tempTable = _.where(output, {id_periode : parseInt(searchParams.periode.id_type )});
            output = tempTable;
        }*/
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

export let customClassPeriodeFilters = ng.filter('customClassPeriodeFilters', function(){
    return  function(periodes, search){

        let result = periodes;

        if (search.classe !== '*' && search.classe !== null) {

            result =_.filter(periodes, function (periode) {
                if(periode.id_type === undefined) {
                    periode.id_type = periode.id;
                }
                return _.findWhere(search.classe.periodes.all, {id_type: periode.id_type})
            });
        }
        return result;
    };
});