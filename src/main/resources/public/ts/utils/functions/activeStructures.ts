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
