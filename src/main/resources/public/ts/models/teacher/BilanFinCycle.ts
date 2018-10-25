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

import { Model, http } from 'entcore';

export class BilanFinDeCycle extends Model {
    id: number;
    id_eleve: string;
    id_domaine: number;
    id_etablissement: string;
    owner: string;
    valeur: number;

    constructor(p?: any) {
        super();
        if (p !== undefined) {
            this.id = p.id;
            this.id_eleve = p.id_eleve;
            this.id_domaine = p.id_domaine;
            this.id_etablissement = p.id_etablissement;
            this.owner = p.owner;
            this.valeur = p.valeur;
        }
    }

    get api () {
        return {
            createBFC : '/competences/bfc',
            updateBFC : `/competences/bfc?idDomaine=${this.id_domaine}&idEleve=${this.id_eleve}`,
            deleteBFC : `/competences/bfc?idDomaine=${this.id_domaine}&idEleve=${this.id_eleve}`
        };
    }

    saveBilanFinDeCycle (): Promise<BilanFinDeCycle> {
        return new Promise((resolve) => {
            if (!this.id) {
                this.createBilanFinDeCycle().then((data) => {
                    resolve(data);
                });
            } else {
                this.updateBilanFinDeCycle().then((data) =>  {
                    resolve(data);
                });
            }
        });
    }

    createBilanFinDeCycle (): Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            let _bilanFinDeCycle = {
                id_eleve : this.id_eleve,
                id_domaine : this.id_domaine,
                id_etablissement : this.id_etablissement,
                owner : this.owner,
                valeur : this.valeur
            };
            http().postJson(this.api.createBFC, _bilanFinDeCycle)
                .done ( function (data) {
                    if (resolve && (typeof(resolve) === 'function')) {
                        resolve(data);
                    }
                })
                .error(function () {
                    reject();
                });
        });
    }

    updateBilanFinDeCycle (): Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            let _bilanFinDeCycle = {
                id : this.id,
                id_eleve : this.id_eleve,
                id_domaine : this.id_domaine,
                id_etablissement : this.id_etablissement,
                owner : this.owner,
                valeur : this.valeur
            };
            http().putJson(this.api.updateBFC, _bilanFinDeCycle)
                .done(function (data) {
                    if (resolve && (typeof(resolve) === 'function')) {
                        resolve(data);
                    }
                })
                .error(function () {
                    reject();
                });
        });
    }

    deleteBilanFinDeCycle (): Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.deleteBFC)
                .done(function (data) {
                    if (resolve && typeof(resolve) === 'function') {
                        resolve(data);
                    }
                })
                .error(function () {
                    reject();
                });
        });
    }
}