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

import { http, model, _ } from 'entcore';

/**
 * Récupère les structures activées de l'utilisateur.
 * @param module Nom du module appelant la fonction. Permet de variabiliser la route appelante.
 * @returns {Promise<T>} Callback de retour.
 */
export function getActiveStructures (module: string): Promise<any[]> {
    return new Promise((resolve, reject) => {
        http().getJson('/viescolaire/user/structures/actives?module=' + module )
            .done((activeStructures) => {
                let structures: any[] = [];
                for (let i = 0; i < model.me.structures.length; i++) {
                    let id_structure: string = model.me.structures[i];
                    if (_.findWhere(activeStructures, {id_etablissement: id_structure})) {
                        structures.push({
                           id: id_structure,
                            name: model.me.structureNames[i]
                        });
                    }
                }
                resolve(structures);
            })
            .error(() => {
                reject();
            });
    });
}

/**
 * Active une structure de l'utilisateur.
 * @param module Nom du module appelant la fonction. Permet de variabiliser la route appelante.
 * @param id_structure
 * @returns {Promise<T>} Callback de retour.
 */
export function createActiveStructure (module: string, id_structure: string): Promise<any[]> {
    return new Promise((resolve, reject) => {
        http().postJson('/viescolaire/user/structures/actives', {structureId: id_structure, module: module})
            .done((res) => {
                resolve(res);
            })
            .error(() => {
                reject();
            });
    });
}


/**
 * Supprime l'activation d'une structure de l'utilisateur.
 * @param module Nom du module appelant la fonction. Permet de variabiliser la route appelante.
 * @param id_structure
 * @returns {Promise<T>} Callback de retour.
 */
export function deleteActiveStructure (module: string, id_structure: string): Promise<any[]> {
    return new Promise((resolve, reject) => {
        http().delete('/viescolaire/user/structures/actives', {structureId: id_structure, module: module})
            .done((res) => {
                resolve(res);
            })
            .error(() => {
                reject();
            });
    });
}
