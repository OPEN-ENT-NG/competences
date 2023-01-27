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

import {IModel, Model} from "entcore";
import http from "axios";
import {Mix} from "entcore-toolkit";
import {evaluations, Structure} from "./teacher";
export class LangueCultReg extends Model  {
    id : number;
    libelle : string;
    code : string;


    constructor(){
        super();
    }
}

export class LanguesCultRegs extends Model implements IModel{
    all : LangueCultReg[];
    structure: Structure;
    constructor(){
        super();
        this.all=[];
        this.structure = evaluations.structure
    }
    get api() {
        return {
            get: `/competences/langues/culture/regionale/list`
        }
    }

    async sync() {
        let { data } = await http.get(this.api.get + `?idEtablissement=${this.structure.id}`);
        this.all = Mix.castArrayAs(LangueCultReg, data);
    }
}

export class NiveauLangueCultReg extends Model  {
    id : number;
    libelle : string;
    niveau : number;

    constructor(niveau : number){
        super();
        this.niveau = niveau
    }
}

export class NiveauLangueCultRegs extends Model {
    all : NiveauLangueCultReg[];

    constructor(){
        super();
        this.all=[];
        this.all[0] = new NiveauLangueCultReg(1);
        this.all[0].libelle = "Objectif atteint";
        this.all[1] = new NiveauLangueCultReg(2);
        this.all[1].libelle ="Objectif dépassé";
    }
}

