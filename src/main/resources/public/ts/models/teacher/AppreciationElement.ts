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

export class AppreciationElement extends Model {
    id : number;
    id_eleve : string;
    id_Element : number;
    id_appreciation : number;
    valeur : any;
    appreciation : any;
    oldAppreciation : any;

    get api () {
        return {
            createAppreciation : '/competences/appreciation',
            updateAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation,
            deleteAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation
        };
    }

    constructor (o? : any) {
        super();
        if (o) this.updateData(o, false);
    }

    toJSON () {
        let o = new AppreciationElement();
        if(this.id !== null) o.id = this.id;
        o.id_eleve  = this.id_eleve;
        o.id_Element = parseInt(this.id_Element.toString());
        o.valeur   = parseFloat(this.valeur);
        if (this.appreciation) o.appreciation = this.appreciation;
        return o;
    }

    saveAppreciation () : Promise<AppreciationElement> {
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

    createAppreciation () : Promise<AppreciationElement> {
        return new Promise((resolve) => {
            let _appreciation = {
                id_Element : this.id_Element,
                id_eleve  : this.id_eleve,
                valeur    : this.appreciation
            };
            http().postJson(this.api.createAppreciation, _appreciation).done ( function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            }) ;

        });

    }

    updateAppreciation () : Promise<AppreciationElement> {
        return new Promise((resolve) => {
            let _appreciation = {
                id : this.id_appreciation,
                id_Element : this.id_Element,
                id_eleve  : this.id_eleve,
                valeur    : this.appreciation
            };
            http().putJson(this.api.updateAppreciation, _appreciation).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
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
}
