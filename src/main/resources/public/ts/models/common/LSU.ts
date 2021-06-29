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
import { notify, idiom as lang} from 'entcore';
import { Responsable, Classe } from '../teacher';
import {ErrorsLSU} from './ErrorLSU';


declare let _ :any;
export const LSU_UNHEEDED_STUDENT_ACTION = {
    GET : 'get',
    ADD: 'add',
    REM: 'rem'
};

export const LSU_TYPE_EXPORT = {
    BFC : '1',
    BILAN_PERIODIQUE: '2'
}
export class LSU {
    responsables: Array<Responsable>;
    periodes_type: any[];
    classes : Array<Classe>;//sans les groupes
    idStructure : string;
    errorsLSU : ErrorsLSU;
    unheededStudents : any[];
    hasUnheededStudents : boolean;

    constructor (structureId : string, classes : Array<Classe>, responsables : Array<Responsable>){
        this.idStructure = structureId ;
        this.classes = classes;
        this.responsables = responsables ;
        this.periodes_type = [];
        this.errorsLSU = new ErrorsLSU();
        this.unheededStudents = [];
        this.hasUnheededStudents = false;
    }

    private initializeExport () {
        this.errorsLSU = new ErrorsLSU();
        this.unheededStudents = [];
        this.hasUnheededStudents = false;
    }
    async export(params: any, getUnheededStudents): Promise<any> {
        return new Promise(async (resolve, reject) => {
            this.initializeExport();
            let idPeriodes = _.map(params.periodes_type, (periode) => {
                return periode.id_type;
            });
            let idClasses = _.map(params.classes, (classe) => {
                return classe.id;
            });
            try {

                if(getUnheededStudents) {
                    let unheededPeriodes = (params.type === LSU_TYPE_EXPORT.BFC)? null : idPeriodes;
                    await this.getUnheededStudents(unheededPeriodes, idClasses, this.idStructure, params.periodes_type);
                }

                if(!_.isEmpty(this.unheededStudents) && getUnheededStudents) {
                    this.hasUnheededStudents = true;
                    resolve();
                }
                else {
                    this.errorsLSU = new ErrorsLSU(); http.post('/competences/exportLSU/lsu', params, {responseType: 'arraybuffer'})
                        .then(function (data) {
                            if (resolve && typeof(resolve) === 'function') {
                                resolve(data);
                            }
                        })
                        .catch( (data) => {
                            if(data.response != undefined && data.response.status === 400){
                                this.errorsLSU.setErrorsLSU(data.response.data);
                            }
                            reject();
                            if(data.response.data.byteLength === 0)
                            notify.error('evaluation.lsu.export.error');
                        });}
            }
            catch (e){
                notify.error('evaluation.lsu.export.error');
                reject(e);
            }
        });
    }

    async getUnheededStudents(idPeriodes, idClasses, idStructure, periodes) {
        let params = {
            action: LSU_UNHEEDED_STUDENT_ACTION.GET,
            idPeriodes: idPeriodes,
            idClasses: idClasses,
            idStructure: idStructure
        };
        await this.postUnheededStudents(params, periodes);
    }

    async addUnheededStudents(idsStudents, idPeriode, idClasse) {
        let params = {
            action: LSU_UNHEEDED_STUDENT_ACTION.ADD,
            idPeriode: idPeriode !== -1? idPeriode: null,
            idClasse: idClasse,
            idsStudents: idsStudents
        };
        await this.postUnheededStudents(params);
    }

    async remUnheededStudents(idsStudents, idPeriode, idClasse) {
        let params = {
            action: LSU_UNHEEDED_STUDENT_ACTION.REM,
            idPeriode: idPeriode !== -1? idPeriode: null,
            idClasse: idClasse,
            idsStudents: idsStudents
        };
        await this.postUnheededStudents(params);
    }

    private async postUnheededStudents(params : any, setPeriode?) {
        try {
            let {data} = await http.post('/competences/lsu/unheeded/students', params);
            if(params.action === 'get'){
                _.forEach(data, (student) => {
                    if(params.idPeriodes === null){
                        student.periodes = [{libelle: lang.translate('viescolaire.utils.cycle')}];
                    } else{
                        student.periodes = _.filter(setPeriode, (periode) => {
                            return _.findWhere(student.ignoredInfos, {id_periode : periode.id_type}) !== undefined;
                        });
                    }
                });
                this.unheededStudents = data;
            }
            return data;
        } catch (e){
            throw e;
        }
    }
}