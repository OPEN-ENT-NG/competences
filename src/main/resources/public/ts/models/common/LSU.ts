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
 * Created by agnes.lapeyronnie on 19/09/2017.
 */
import http from 'axios';
import {angular, _} from 'entcore';
import { Responsable, Classe } from '../teacher';
import {ErrorLSU, ErrorsLSU} from './ErrorLSU';
import { Mix } from 'entcore-toolkit';

export class LSU {
    responsables: Array<Responsable>;
    periodes_type: any[];
    classes : Array<Classe>;//sans les groupes
    idStructure : string;
    errorsLSU : ErrorsLSU;


    constructor (structureId : string, classes : Array<Classe>, responsables : Array<Responsable>){
        this.idStructure = structureId ;
        this.classes = classes;
        this.responsables = responsables ;
        this.periodes_type = [];
        this.errorsLSU = new ErrorsLSU();

    }

    async export(params: any): Promise<any> {
       return new Promise((resolve, reject) => {
            http.post('/competences/exportLSU/lsu', params, {responseType: 'arraybuffer'})
                .then(function (data) {
                    if (resolve && typeof(resolve) === 'function') {
                        resolve(data);
                    }
                })
                .catch( (data) => {
                    if(data.response != undefined && data.response.status === 400 ){
                        this.errorsLSU.setErrorsLSU(data.response.data);
                    }
                    reject();
                });
        });
    }

}