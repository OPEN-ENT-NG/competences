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

import { Model,toasts } from 'entcore';
import {Mix, Selectable,Selection} from "entcore-toolkit";
import http from "axios";

export class TypeSousMatiere extends Model implements Selectable{
    id: number;
    libelle: string;
    selected:boolean;

    async save() {
        try{
            let {status,data} = await http.post(`/viescolaire/types/sousmatiere`,this.toJson());
            console.log(status)
            console.log(status === 200)
            this.id = data.id;
            return status === 200;
        }catch (e){
            return false
        }
    }

    private toJson() {
        return {
            ...(this.id && {id: this.id}),
            ...(this.libelle && {libelle: this.libelle}),
        }
    }
}
//TODO faire le CRUD et faire les controllers backs

export class TypeSousMatieres extends Selection<TypeSousMatiere>{
    id: number;
    libelle: string;

    async get(){
        let {data} = await http.get(`/viescolaire/types/sousmatieres`);
        this.all = Mix.castArrayAs(TypeSousMatiere, data);

    }
}