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
import {_, model, moment} from 'entcore';

export function getTitulairesForRemplacantsCoEnseignant(meUserId, classe) {
    let listTeacher = [];
    if(classe && classe.services) {
        classe.services.forEach(service => {
            let substituteTeacher = _.findWhere(service.substituteTeachers, {second_teacher_id : meUserId});
            if ((substituteTeacher && moment(new Date()).isBetween(moment(substituteTeacher.start_date),
                moment(substituteTeacher.entered_end_date), 'days', '[]'))
                || _.findWhere(service.coTeachers, {second_teacher_id: meUserId})
                || service.id_enseignant == meUserId) {

                listTeacher.push({id_enseignant : service.id_enseignant, id_matiere : service.id_matiere});

                service.coTeachers.forEach(coTeacher => {
                    listTeacher.push({
                        id_enseignant: coTeacher.second_teacher_id,
                        id_matiere: coTeacher.subject_id
                    });
                });

                service.substituteTeachers.forEach(substituteTeacher => {
                    listTeacher.push({
                        id_enseignant: substituteTeacher.second_teacher_id,
                        id_matiere: substituteTeacher.subject_id
                    });
                });
            }
        });
    }
    return listTeacher;
}