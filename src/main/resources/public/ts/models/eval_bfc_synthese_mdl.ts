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

import {model, http, IModel, Model, notify} from 'entcore';

export class BfcSynthese extends Model {
    id: number;
    id_eleve: string;
    id_classe : string;
    id_cycle: string;
    texte: string;

    get api() {
        return {
            create: '/competences/BfcSynthese',
            update: '/competences/BfcSynthese?id=' + this.id,
            getBfcSynthese: '/competences/BfcSynthese?idEleve='
        }
    }

    constructor(id_eleve : string, id_cycle : string) {
        super();
        this.texte = "";
        this.id_eleve = id_eleve;
        this.id_cycle = id_cycle;
    }

    toJSON() {
        return {
            id_eleve: this.id_eleve,
            id_cycle : this.id_cycle,
            texte: this.texte
        }
    }

    createBfcSynthese(): Promise<BfcSynthese> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.create, this.toJSON())
                .done((data) =>{
                    this.id= data.id;
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve(data);
                    }
                })
                .error(()=> {
                    if (reject && typeof reject === 'function') {
                        reject();
                    }
                });
        });
    }
    updateBfcSynthese(): Promise<BfcSynthese> {
        return new Promise((resolve,reject)=>{
            http().putJson(this.api.update,this.toJSON())
                .done((data)=>{
                    if(resolve&&(typeof(resolve)==='function')){
                        resolve(data);
                    }
                })
                .error(()=> {
                    if (reject && typeof reject === 'function') {
                        reject();
                    }
                });
        })
    }

    saveBfcSynthese():Promise<BfcSynthese> {
        return new Promise((resolve, reject) => {
            if(this.texte != undefined){
                try{
                    if(!this.id && this.texte != ''){
                        this.createBfcSynthese().then((data)=>{
                            resolve(data);
                        });
                    }else{
                        this.updateBfcSynthese().then((data)=>{
                            resolve(data);
                        });
                    }
                }catch (e){
                    notify.error('evaluation.bilan.fin.cycle.synthese.save.error')
                    console.error(e);
                    reject(e);
                }

            }else{
                notify.error('evaluation.bilan.fin.cycle.synthes.max.length');
            }

        });
    }

    syncBfcSynthese(): Promise<any> {
        return new Promise((resolve, reject) => {
            // var that = this;
            let url = this.api.getBfcSynthese  + this.id_eleve;
            if(this.id_cycle !== null && this.id_cycle !== undefined){
                url = url  + "&idCycle=" + this.id_cycle;
            }
            http().getJson(url).done((data) => {
                if(data != {}){
                    this.updateData(data,false);
                }
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }
}