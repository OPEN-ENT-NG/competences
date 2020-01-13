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

/**
 * Created by ledunoiss on 20/09/2016.
 */
import { ng, moment } from 'entcore';

declare let _:any;

//TODO Rajouter le filtre sur les établissements dans le cas multi-étabs
export let customSearchFilter = ng.filter('customSearchFilters', function(){
    return function(devoirs, searchParams){
        var output = devoirs;
        var tempTable = [];
        if (searchParams.classe !== '*' && searchParams.classe !== null && searchParams.classe !== undefined) {
            tempTable = _.where(output, {id_groupe : searchParams.classe.id});
            output = tempTable;
        }
        if (searchParams.matiere !== '*' && searchParams.matiere !== null &&  searchParams.matiere !== undefined) {
            tempTable = _.where(output, {id_matiere : searchParams.matiere.id});
            output = tempTable;
        }
        if (searchParams.sousmatiere !== '*' && searchParams.sousmatiere !== null
            && searchParams.sousmatiere !== undefined) {
            tempTable = _.where(output, {id_sousmatiere : parseInt(searchParams.sousmatiere.id_type_sousmatiere)});
            output = tempTable;
        }
        if (searchParams.type !== '*' && searchParams.type !== null && searchParams.type !== undefined) {
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
        if (searchParams.name !== "" && searchParams.name !== null && searchParams.name !== undefined) {
            tempTable = _.filter(output, function (devoir){
                var  reg = new RegExp(searchParams.name.toUpperCase());
                return devoir.name.toUpperCase().match(reg) !== null;
            });
            output = tempTable;
        }

        if (searchParams.enseignant !== undefined && searchParams.enseignant !== '*'
            && searchParams.enseignant !== null) {
            tempTable = _.where(output, {owner : searchParams.enseignant.id});
            output = tempTable;
        }

        return output;
    };
});