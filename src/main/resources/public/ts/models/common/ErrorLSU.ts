/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
import {_, Model} from 'entcore';
import {Mix} from "entcore-toolkit";

declare let TextDecoder : any;

export class ErrorLSU extends Model {
    idEleve : string;
    lastName : string;
    firstName : string;
    nameClass : string;
    errorsMessages : string[];

}
export class ErrorsLSU {

    errorCode : any[];
    all : ErrorLSU[];
    emptyDiscipline : boolean;
    errorMessageBadRequest : String;

    constructor () {
        this.all = [];
        this.errorCode = [];
        this.emptyDiscipline = false;
    }

    setErrorsLSU(data: any){
        if(data instanceof ArrayBuffer && data.byteLength !== 0){
           let obj : string;
           let decodedString : any;
           if('TextDecoder' in window){
                    let dataView = new DataView(data);
                    decodedString = new TextDecoder ('utf8');
                    obj = JSON.parse(decodedString.decode(dataView));
                }else{
                     decodedString = String.fromCharCode.apply(null, new Uint8Array(data));
                     obj = JSON.parse(decodedString);
                }

            let errorCode = _.values(_.pick(obj, 'errorCode'));
            if(!_.isEmpty(errorCode)){
                errorCode = errorCode[0];
            }
            this.errorCode =  errorCode;
            let emptyDiscipline = _.values(_.pick(obj, 'emptyDiscipline'));
            if(!_.isEmpty(emptyDiscipline)){
                this.emptyDiscipline = emptyDiscipline[0];
            }
            else{
                this.emptyDiscipline = false;
            }
            let errorBadRequest = _.values(_.pick(obj, 'error'));
            if(!_.isEmpty(errorBadRequest)){
               this.errorMessageBadRequest = errorBadRequest;
            }
            this.all = Mix.castArrayAs(ErrorLSU,_.values(_.omit(obj, 'errorCode', 'emptyDiscipline','error')));
        }
    }

}