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
import {_, moment, ng} from 'entcore';
import {Utils} from "../../models/teacher";

export let getMatiereClasseFilter = ng.filter('getMatiereClasse', function () {
    function getEvaluables(classe, matiere, idTeacher) {
        if (Utils.isChefEtab()) {
            return _.where(classe.services, {
                id_matiere: matiere.id,
                evaluable: true
            });
        } else {
            return _.filter(classe.services, service => {
                let substituteTeacher = _.findWhere(service.substituteTeachers,
                    {second_teacher_id: idTeacher, subject_id: matiere.id});
                let correctDateSubstituteTeacher = substituteTeacher &&
                    moment(new Date()).isBetween(moment(substituteTeacher.start_date),
                        moment(substituteTeacher.entered_end_date), 'days', '[]');

                let coTeachers = _.findWhere(service.coTeachers,
                    {second_teacher_id: idTeacher, subject_id: matiere.id});

                let mainTeacher = service.id_enseignant == idTeacher && service.id_matiere == matiere.id;
                if (matiere.hasOwnProperty('libelleClasses')) {
                    mainTeacher = mainTeacher && (matiere.libelleClasses.indexOf(classe.externalId) !== -1)
                }

                return service.evaluable && (coTeachers || correctDateSubstituteTeacher || mainTeacher);
            });
        }
    }

    return function (matieres, idClasse, classes, idTeacher) {
        if (classes) {
            let classe = classes.findWhere({id : idClasse});
            if (classe) {
                return matieres.filter((matiere) => {
                    let evaluables = [];
                    if (classe.services) {
                        evaluables = getEvaluables(classe, matiere, idTeacher);
                    }
                    return evaluables.length > 0;
                });
            }
            else {
                return matieres.filter((matiere) => {
                    let evaluables;
                    for(let c of classes.all){
                        evaluables = getEvaluables(c, matiere, idTeacher);
                        if(evaluables.length > 0){
                            break;
                        }
                    }
                    return evaluables.length > 0;
                });
            }
        }
        return false;
    }
});