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

import {Model, IModel, Collection, http, notify} from 'entcore';
import { CompetenceNote } from './index';

export class Evaluation extends Model implements IModel {
    id : number;
    id_eleve : string;
    data:any;
    valid: boolean;
    id_devoir : number;
    id_appreciation : number;
    valeur : any;
    appreciation : any;
    coefficient : number;
    ramener_sur : boolean;
    competenceNotes : Collection<CompetenceNote>;
    oldValeur : any;
    is_evaluated  : boolean;
    oldAppreciation : any;
    oldId_annotation : number;
    id_annotation : number;

    get api () {
        return {
            create : '/competences/note',
           // update : '/competences/note?idNote=' + this.id,
            delete : '/competences/note?idNote=' + this.id,
            createAppreciation : '/competences/appreciation',
            updateAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation,
            deleteAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation,
            updateAnnotation : '/competences/annotation?idDevoir=' + this.id_devoir,
            deleteAnnotation : '/competences/annotation?idDevoir=' + this.id_devoir+'&idEleve='+this.id_eleve,
            createAnnotation : '/competences/annotation'
        };
    }

    constructor (o? : any) {
        super();
        if (o) this.updateData(o, false);
        this.collection(CompetenceNote);
    }

    toJSON () {
        let o = new Evaluation();
        if(this.id !== null) o.id = this.id;
        o.id_eleve = this.id_eleve;
        o.id_devoir = parseInt(this.id_devoir.toString());
        o.valeur = parseFloat(this.valeur);
        if (this.appreciation) o.appreciation = this.appreciation;
        delete o.competenceNotes;
        return o;
    }


    save () : Promise<Evaluation> {
        return new Promise((resolve) => {
            if (this.id_annotation !== undefined && this.id_annotation !== -1){
                this.deleteAnnotationDevoir().then(()=>{
                    delete this.oldId_annotation;
                    delete this.id_annotation;
                    this.oldValeur = "";
                    this.create().then((data) => {
                        resolve(data);
                    });
                });
            } else {
                this.create().then((data) => {
                    resolve(data);
                });
            }
        });
    }

    saveAppreciation () : Promise<Evaluation> {
        return new Promise((resolve) => {
            if (!this.id_appreciation) {
                this.createAppreciation().then((data) => {
                    resolve(data);
                });
            } else {
                this.updateAppreciation().then((data) =>  {
                    resolve(data);
                });
            }
        });
    }
    create () : Promise<Evaluation> {
        return new Promise((resolve) => {
            let _noteData = this.toJSON();
            delete _noteData.appreciation;
            delete _noteData.id_appreciation;
            http().postJson(this.api.create, _noteData).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    delete () : Promise<any> {
        return new Promise((resolve) => {
            http().delete(this.api.delete).done(function (data) {
                if(resolve && typeof(resolve) === 'function'){
                    resolve(data);
                }
            });
        });
    }

    createAppreciation () : Promise<Evaluation> {
        return new Promise((resolve) => {
            let _appreciation = {
                id_devoir : this.id_devoir,
                id_eleve : this.id_eleve,
                valeur : this.appreciation
            };
            http().postJson(this.api.createAppreciation, _appreciation).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            }) ;
        });
    }

    updateAppreciation () : Promise<Evaluation> {
        return new Promise((resolve) => {
            let _appreciation = {
                id : this.id_appreciation,
                id_devoir : this.id_devoir,
                id_eleve : this.id_eleve,
                valeur : this.appreciation
            };
            http().putJson(this.api.updateAppreciation, _appreciation).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });

        });
    }

    saveAnnotation (): Promise<Evaluation> {
        return new Promise((resolve) => {
            this.createAnnotationDevoir().then((data) =>  {
                resolve(data);
            });
        });
    }

    createAnnotationDevoir (): Promise<Evaluation> {
        return new Promise((resolve) => {
            let _annotation = {
                id_devoir : this.id_devoir,
                id_annotation : this.id_annotation,
                id_eleve : this.id_eleve
            };
            http().postJson(this.api.createAnnotation, _annotation).done(function (data) {
                if (resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    updateAnnotationDevoir (): Promise<Evaluation> {
        return new Promise((resolve) => {
            let _annotation = {
                id_devoir : this.id_devoir,
                id_annotation : parseInt(this.id_annotation.toString()),
                id_eleve : this.id_eleve
            };
            http().putJson(this.api.updateAnnotation, _annotation).done(function (data) {
                if (resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    deleteAnnotationDevoir () : Promise<any> {
        return new Promise((resolve) => {
            let _annotation = {
                id_devoir : this.id_devoir,
                id_eleve : this.id_eleve
            };
            http().delete(this.api.deleteAnnotation, _annotation).done(function (data) {
                if(resolve && typeof(resolve) === 'function') {
                    resolve(data);
                }
            });
        });
    }

    deleteAppreciation () : Promise<any> {
        return new Promise((resolve) => {
            http().delete(this.api.deleteAppreciation).done(function (data) {
                if(resolve && typeof(resolve) === 'function') {
                    resolve(data);
                }
            });
        });
    }

    formatMoyenne() {
        return {
            valeur : parseFloat(this.valeur),
            coefficient : this.coefficient,
            ramenersur : this.ramener_sur
        }
    }


}
