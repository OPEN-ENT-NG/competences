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
 * Created by Samuel JOLLOIS on 09/06/2020
 */
import http from 'axios';
import { notify,toasts} from 'entcore';

export class Archives {
    idStructure : string;

    constructor (structureId : string){
        this.idStructure = structureId ;
    }

    async export(params: any): Promise<any> {
        return new Promise(async (resolve, reject) => {
            try {
                let url = '/competences/archive/' + params.type + '?idStructure=' + params.idStructure
                    + "&idYear=" + params.year;
                if(params.periodes_type) {
                    url += "&idsPeriode=" + params.periodes_type.join(",");
                }
                http.get(url, {responseType: 'arraybuffer'}).then(function (data) {
                    if(data.status === 200)
                        if (resolve && typeof(resolve) === 'function') {
                            resolve(data);
                        }
                    if (data.status === 202) {
                        reject();
                    }
                    if(data.status === 204){
                        toasts.info("no.data.to.export")
                        reject();
                    }
                }).catch(() => {
                    reject();
                });
            } catch (e){
                notify.error('evaluation.archives.generation.error');
                reject(e);
            }
        });
    }}