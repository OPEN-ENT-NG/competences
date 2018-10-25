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

import { Periode } from "./Periode";
import { DefaultClasse } from "../common/DefaultClasse";
import { Collection, http } from 'entcore';
import { translate } from "../../utils/functions/translate";
import {TypePeriode} from "../common/TypePeriode";

export class Classe extends DefaultClasse {
    id: string;
    periodes: Collection<Periode>;
    typePeriodes: Collection<TypePeriode>;
    id_cycle: number;


    get api() {
        return {
            getCycle: '/viescolaire/cycle/eleve/' + this.id,
            syncGroupe: '/viescolaire/groupe/enseignement/users/' + this.id + '?type=Student',
            syncPeriode: '/viescolaire/periodes?idGroupe=' + this.id,

            TYPEPERIODES: {
                synchronisation: '/viescolaire/periodes/types'
            }
        };
    }

    constructor(o?: any) {
        super();
        if (o !== undefined) this.updateData(o);
    }

    async sync(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            this.collection(Periode, {
                sync: async (): Promise<any> => {
                    return new Promise((resolve, reject) => {
                        http().getJson(this.api.syncPeriode).done((res) => {
                            res.push({libelle: translate('viescolaire.utils.annee'), id: null, id_type: -1});
                            res.push({libelle: "cycle", id: null, id_type: -2});
                            this.periodes.load(res);
                            http().getJson(this.api.getCycle).done( async (res) => {
                                this.id_cycle = res[0].id_cycle;
                                resolve();
                            }).bind(this);
                        }).error(function () {
                            if (reject && typeof reject === 'function') {
                                reject();
                            }
                        });
                    });
                }
            });
            this.collection(TypePeriode, {
                sync: async (): Promise<any> => {
                    return await http().getJson(this.api.TYPEPERIODES.synchronisation).done((res) => {
                        this.typePeriodes.load(res);
                    })
                    .error(function () {
                        if (reject && typeof reject === 'function') {
                            reject();
                        }
                    });
                }
            });
            await this.periodes.sync();
            await this.typePeriodes.sync();
            resolve();
        });
    }
}