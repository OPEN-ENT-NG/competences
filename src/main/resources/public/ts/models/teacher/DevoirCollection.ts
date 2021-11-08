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

import { http, model, _ } from 'entcore';
import { Devoir, evaluations } from './index';

export class DevoirsCollection {
    all : Devoir[];
    sync : any;
    percentDone : boolean;
    idEtablissement: string;

    get api () {
        return {
            get : '/competences/devoirs?idEtablissement=' + this.idEtablissement,
            areEvaluatedDevoirs : '/competences/devoirs/evaluations/informations?',
            done : '/competences/devoirs/done'
        }
    }

    constructor (idEtablissement : string) {
        this.idEtablissement = idEtablissement;
        this.sync = function (limit?) {
            return new Promise((resolve) => {
                let urlGet = this.api.get;
                if(limit !== undefined) {
                    urlGet += "&limit=" + limit;
                }

                http().getJson(urlGet).done(function (res) {
                    this.load(res);
                    if (evaluations.synchronized.matieres) {
                        DevoirsCollection.synchronizeDevoirMatiere();
                    } else {
                        evaluations.matieres.on('sync', function () {
                            DevoirsCollection.synchronizeDevoirMatiere();
                        });
                    }
                    if (evaluations.synchronized.types) {
                        evaluations.devoirs.synchronizedDevoirType();
                    } else {
                        evaluations.types.on('sync', function () {
                            evaluations.devoirs.synchronizedDevoirType();
                        });
                    }
                    evaluations.devoirs.trigger('sync');
                    if (resolve && (typeof(resolve) === 'function')) {
                        resolve(res);
                    }
                }.bind(this));
            });
        };
    }

    static synchronizeDevoirMatiere () {
        for (let i = 0; i < evaluations.devoirs.all.length; i++) {
            let matiere = evaluations.matieres.findWhere({id : evaluations.devoirs.all[i].id_matiere});
            if (matiere) evaluations.devoirs.all[i].matiere = matiere;
        }
    }

    static synchronizedDevoirType () {
        for (let i = 0 ; i < evaluations.devoirs.all.length; i++) {
            let type = evaluations.types.findWhere({id : evaluations.devoirs.all[i].id_type});
            if (type) evaluations.devoirs.all[i].type = type;
        }
    }

    areEvaluatedDevoirs (idDevoirs) : Promise<any> {
        return new Promise((resolve) => {
            let URLBuilder = "";
            for (let i=0; i<idDevoirs.length; i++){
                if(i==0)
                    URLBuilder = "idDevoir="+idDevoirs[i];
                else
                    URLBuilder += "&idDevoir="+idDevoirs[i];
            }
            http().getJson(this.api.areEvaluatedDevoirs + URLBuilder  ).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    getPercentDone(devoir?) : Promise<any> {
        return new Promise(async (resolve, reject) => {
            if(devoir && evaluations.structure.synchronized.devoirs) {
                if(devoir.eleves.length() === 0) {
                    await devoir.eleves.sync();
                }

                let url = this.api.done + "?idDevoir=" + devoir.id + "&nbStudents=" + devoir.eleves.length();
                http().getJson(url).done((res) => {
                    let _devoir = _.findWhere(this.all, {id : res.id});
                    if (_devoir !== undefined) {
                        _devoir.percent = res.percent;
                    }
                    model.trigger('apply');
                    resolve();
                }).error(() => {
                    reject();
                });
            }
        });
    }
}