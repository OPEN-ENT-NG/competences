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
 * Created by ledunoiss on 09/11/2016.
 */
/**
 * Created by ledunoiss on 20/09/2016.
 */
import { ng, moment, _ } from 'entcore';
import {CompetenceNote} from "../models/teacher";

export let fitlerCompetenceSuivi = ng.filter('fitlerCompetenceSuivi', function(){
    return function(competences, mine, listTeacher){
        var _t = competences;
        if (mine === 'true' || mine === true) {
            _t = _.filter(competences, function (competence) {
                return competence.owner === undefined ||
                    _.findWhere(listTeacher,{id_enseignant : competence.owner, id_matiere : competence.id_matiere});
            });
        }
        var output = [];
        var max = null;
        if (_t.length > 0) {
            max = _.max(_t, function (competence) {
                return competence.evaluation;
            });
        }
        max === -Infinity || max === null ? output.push(new CompetenceNote({evaluation : -1})) : output.push(max);
        return output;
    };
});
