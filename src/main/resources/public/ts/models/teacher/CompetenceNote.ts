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

import {http, IModel, Model, notify} from 'entcore';
import httpAxios from 'axios';

export interface CompetenceNote {
    id: number;
    id_devoir: number;
    id_competence: number;
    evaluation: number;
    id_eleve: string;
    ids_matieres: string[];
    id_periode: number;
    niveau_final: number;
    id_classe: string;
    nom?: string;
}

export class CompetenceNote extends Model implements IModel {
    id: number;
    id_devoir: number;
    id_competence: number;
    evaluation: number;
    id_eleve: string;
    ids_matieres: string[];
    id_periode: number;
    niveau_final: number;
    id_classe: string;

    get api() {
        return {
            create: '/competences/competence/note',
            update: '/competences/competence/note?id=' + this.id,
            delete: '/competences/competence/note?id=' + this.id
        }
    }

    constructor(o?: any) {
        super();
        if (o !== undefined) this.updateData(o, false);
    }

    toJSON() {
        if (this.niveau_final !== undefined) {
            return {
                id_periode: this.id_periode,
                id_eleve: this.id_eleve,
                niveau_final: this.niveau_final,
                id_competence: this.id_competence,
                ids_matieres: this.ids_matieres
            }
        } else {
            return {
                id: this.id,
                id_devoir: this.id_devoir,
                id_competence: this.id_competence,
                evaluation: this.evaluation,
                id_eleve: this.id_eleve
            }
        }
    }

    create(): Promise<number> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.create, this.toJSON()).done((data) => {
                this.id = data.id;
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data.id);
                }
            });
        });
    }

    update(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().putJson(this.api.update, this.toJSON()).done(function (data) {
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            });
        });
    }

    delete(): Promise<any> {
        return new Promise((resolve, reject) => {
            let that = this;
            http().delete(this.api.delete).done(function (data) {
                delete that.id;
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            });
        });
    }

    save(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (this.id && this.evaluation === -1) {
                this.delete().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve();
                    }
                });
            } else {
                this.create().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve(data);
                    }
                });
            }
        });
    }

    async saveNiveaufinal(): Promise<void> {
        try {
            await httpAxios.post(`/competences/competence/note/niveaufinal`, this.toJSON());
        } catch (e) {
            notify.error('competences.competence.niveau.final.save.err');
        }
    }

}