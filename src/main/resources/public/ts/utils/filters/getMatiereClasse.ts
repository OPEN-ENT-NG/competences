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
import {_, ng} from 'entcore';

export let getMatiereClasseFilter = ng.filter('getMatiereClasse', function () {
    return function (matieres, idClasse, classes, search) {
        let hasService = false;
        if (idClasse === '*' || idClasse === undefined) return matieres;
        if (classes.all.length > 0) {
            let classe = classes.findWhere({id : idClasse});
            if (classe !== undefined) {
                let matieresClasse = matieres.filter((matiere) => {
                    if (classe.hasOwnProperty('services')) {
                        let services = classe.services;
                        let evaluables = _.findWhere(services, {id_matiere: matiere.id, evaluable: true});
                        if (services !== null) {
                            hasService = true;
                        }
                        return evaluables !== undefined;
                    }
                    else {
                        if (matiere.hasOwnProperty('libelleClasses')) {
                            return (matiere.libelleClasses.indexOf(classe.externalId) !== -1)
                        } else {
                            return false;
                        }
                    }
                });
                if (matieresClasse.length > 0) {
                     return matieresClasse;
                }
                let matieresList = (hasService)? matieresClasse : matieres;
                return matieresList;
            }
        }
    }
});