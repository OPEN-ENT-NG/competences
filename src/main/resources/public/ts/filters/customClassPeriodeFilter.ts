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
import {isValidClasse} from "../utils/functions/isValidClasse";

declare let _:any;

export let customClassFilters = ng.filter('customClassFilters', function(){
    return function(classes, devoirs){
        let output = _.filter(devoirs, devoir => {
            return isValidClasse(devoir.id_groupe, devoir.id_matiere,classes);
        });

        return _.filter(classes, function (classe) { return _.findWhere(output, {id_groupe: classe.id});
        });
    };
});

export let customPeriodeTypeFilter = ng.filter('customPeriodeTypeFilter', function(){
    return function(typePeriodes,searchParams){
        if (searchParams.classe !== '*' && searchParams.classe !== null) {
            let id_typeClasse = _.map(searchParams.classe.periodes.all, (pc) => {
                return (pc.id !== undefined && pc.id === null) ? pc.id : pc.id_type;
            });

            return _.reject(typePeriodes, function (periode) {
                return !_.contains(id_typeClasse, periode.id_type);
            });
        } else {
            return typePeriodes;
        }
    };
});

export let customPeriodeFilters = ng.filter('customPeriodeFilters', function(){
    return function(periodes, devoirs, searchParams){
        let output = devoirs;
        let tempTable = [];
        if (searchParams.classe !== '*' && searchParams.classe !== null) {
            tempTable = _.where(output, {id_groupe : searchParams.classe.id});
            output = tempTable;
        }
        let types =_.filter(periodes, function (periode) {
            if(periode.id_type === undefined) {
                periode.id_type = periode.id;
            }
            return _.findWhere(output,{id_periode: parseInt(periode.id_type)});
        });

        return _.reject(periodes, function (periode) {
            let _t = _.groupBy(types, 'type');
            return !((_t !== undefined && _t[parseInt(periode.type)] !== undefined) || periode.id === null);
        });
    };
});

export let customClassPeriodeFilters = ng.filter('customClassPeriodeFilters', function(){
    return function(periodes, search){
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