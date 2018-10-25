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
 * Created by anabah on 31/08/2017.
 */

import { Model, model, http } from 'entcore';

export class NiveauCompetence extends Model {
    id: number;
    default: string;
    couleur: string;
    id_etablissement: string;
    libelle: string;
    ordre: string;
    lettre: string;
    id_niveau: number;
    id_cycle: number;
    cycle: string;

    constructor (o?: any) {
        super();
        if (o && typeof o === 'object') { this.updateData(o, false); }
    }

    get api () {
        return {
            POST: '/competences/maitrise/level',
            UPDATE: '/competences/maitrise/level/' + this.id,
            DELETE:'/competences/maitrise/level/' + this.id,
            MARK_USER: '/competences/maitrise/perso/use',
            UNMARK_USER:'/competences/maitrise/perso/use/' + model.me.userId
        };
    }

    toJson () {
        return {
            lettre : this.lettre,
            couleur: this.couleur,
            id: this.id,
            id_niveau: this.id_niveau,
            id_etablissement: this.id_etablissement };
    }

    create(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.POST, this.toJson())
                .done((data) => {
                    data = data [0];
                    if (!data.hasOwnProperty('id')) {
                        reject();
                    }
                    this.id = data.id;
                    if (resolve && typeof resolve === 'function') {
                        resolve();
                    }
                })
                .error(function () {
                    if (reject && typeof reject === 'function') {
                        reject();
                    }
                });
        });
    }

    markUser(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.MARK_USER, {id_user: model.me.userId})
                .done((data) => {
                    data = data [0];
                    if (!data.hasOwnProperty('id')) {
                        reject();
                    }
                    this.id = data.id;
                    if (resolve && typeof resolve === 'function') {
                        resolve();
                    }
                })
                .error(function () {
                    if (reject && typeof reject === 'function') {
                        reject();
                    }
                });
        });
    }

    update(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().putJson(this.api.UPDATE, this.toJson())
                .done((data) => {
                    if (resolve && typeof resolve === 'function') {
                        resolve();
                    }
                })
                .error(function () {
                    if (reject && typeof reject === 'function') {
                        reject();
                    }
                });
        });
    }

    delete(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.DELETE)
                .done((data) => {
                    if (resolve && typeof resolve === 'function') {
                        resolve();
                    }
                })
                .error(function () {
                    if (reject && typeof reject === 'function') {
                        reject();
                    }
                });
        });
    }

    unMarkUser (): Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.UNMARK_USER)
                .done((data) => {
                    if (resolve && typeof resolve === 'function') {
                        resolve();
                    }
                })
                .error(function () {
                    if (reject && typeof reject === 'function') {
                        reject();
                    }
                });
        });
    }

    save(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (this.id === null) {
                delete this.id;
                this.create()
                    .then(resolve)
                    .catch(reject);
            } else {
                this.update()
                    .then(resolve)
                    .catch(reject);
            }
        });
    }
}

export let Defaultcolors = {
    blue: '#97BBCD', // blue
    red: '#e13a3a', // red
    green: '#46bfaf', // green
    yellow: '#ecbe30', // yellow
    grey: '#5f5f5f', // grey
    orange: '#FF8500',
    unevaluated: '#5f5f5f'
};
