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
 * Created by Samuel Jollois on 27/01/2020.
 */
import {_, moment, ng} from 'entcore';

export let getEnseignantClasseFilter = ng.filter('getEnseignantClasse', function () {
    return function (enseignants, idClasse, classes) {
        if (idClasse === '*' || idClasse === undefined) return enseignants;
        if (classes.all.length > 0) {
            let classe = classes.findWhere({id : idClasse});
            if (classe !== undefined) {
                let enseignantsClasse = enseignants.filter((enseignant) => {
                    if (classe.services){
                        let evaluables =  _.findWhere(classe.services, {
                            id_enseignant: enseignant.id,
                            evaluable: true
                        });
                        if(evaluables == undefined){
                            evaluables = _.filter(classe.services, service => {
                                let substituteTeacher = _.findWhere(service.substituteTeachers, {second_teacher_id : enseignant.id});
                                let correctDateSubstituteTeacher = substituteTeacher &&
                                    moment(new Date()).isBetween(moment(substituteTeacher.start_date),
                                        moment(substituteTeacher.entered_end_date), 'days', '[]');
                                let coTeachers = _.findWhere(service.coTeachers, {second_teacher_id : enseignant.id});
                                return service.evaluable && (coTeachers || correctDateSubstituteTeacher);
                            });
                            if(evaluables.length == 0)
                                evaluables = undefined;
                        }
                        return evaluables !== undefined;
                    } else if (enseignant.hasOwnProperty('allClasses')) {
                        return (_.pluck(enseignant.allClasses, 'id').indexOf(classe.id) !== -1)
                    } else {
                        return false;
                    }
                });
                if (enseignantsClasse.length > 0) {
                    return enseignantsClasse;
                } else {
                    return enseignants;
                }
            }
        }
    }
});