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
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
import {IModel, Model, notify, _} from "entcore";
import http from "axios";
import {Mix} from "entcore-toolkit";
import {evaluations, Structure} from "./teacher";
export class EnsCpl extends Model  {
    id : number;
    libelle : string;
    code : string;

    constructor(){
        super();
    }
}

export class EnsCpls extends Model implements IModel{
    all : EnsCpl[];
    structure : Structure
    constructor(structure){
        super();
        this.all=[];
        this.structure = evaluations.structure;
    }
    get api() {
        return {
            get: `/competences/ListEnseignementComplement`
        }
    }

    async sync() {
        return new Promise( async (resolve)=> {
            let { data } = await http.get(this.api.get + "?idEtablissement=" + this.structure.id);
            this.all = Mix.castArrayAs(EnsCpl, data);
            resolve();
        });
    }
}

export class NiveauEnseignementCpl extends Model  {
    id : number;
    libelle : string;
    niveau : number;
    bareme_brevet : number;


    constructor(niveau: number){
        super();
        this.niveau = niveau;
    }
}

export class NiveauEnseignementCpls extends Model {
    all : NiveauEnseignementCpl[];

    constructor(){
        super();
    }

    async sync(){
        return new Promise( async (resolve) => {
            let {data} = await http.get(`/competences/niveaux/enseignement/complement/list`);
            this.all = Mix.castArrayAs(NiveauEnseignementCpl, data);
            resolve();
        });
    }
}

export class EleveEnseignementCpl extends Model implements IModel{
    id : number;
    id_eleve : string;
    id_cycle : number;
    id_enscpl : number;
    id_langue : number;
    id_niveau : number;
    niveau : number;
    libelle : string;
    niveau_lcr : number;
    libelle_lcr : string;

    constructor(id_eleve : string, id_cycle : number){
        super();
        this.id_eleve = id_eleve;
        this.id_cycle = id_cycle;
        this.niveau = 0;
    }

    setAttributsEleveEnsCpl(id_enscpl : number, id_niveau : number, niveau_lcr : number, id_langue : number)  {
        this.id_enscpl = id_enscpl;
        this.id_niveau = id_niveau;
        this.id_langue = id_langue;
        this.niveau_lcr = niveau_lcr;
        return this;
    }

    get api(){
        return {
            create :`/competences/CreateNiveauEnsCpl`,
            update : `/competences/UpdateNiveauEnsCpl?id=${this.id}`,
            get :`/competences/GetNiveauEnsCpl?idEleve=${this.id_eleve}`
        }
    }

    toJSON(){
        return{
            id_eleve : this.id_eleve,
            id_enscpl : this.id_enscpl,
            id_langue : this.id_langue,
            id_niveau : this.id_niveau,
            niveau_lcr : this.niveau_lcr,
            id_cycle : this.id_cycle
        }
    }

    async create (): Promise<number> {
        try {
            let res = await http.post(this.api.create, this.toJSON());
            this.id = res.data.id;
            return this.id;
        } catch (e) {
            notify.error(e);
            console.error(e);
            throw e;
        }
    }

    async update (): Promise<void> {
        await http.put(this.api.update, this.toJSON());
    }

    async save() {
        if(this.id){
            await this.update();
        }else{
            await this.create();
        }
    }

    async sync(){
        return new Promise( async (resolve) => {
            let url = this.api.get;
            if(this.id_cycle !== null && this.id_cycle !== undefined){
                url = url + `&idCycle=${this.id_cycle}`
            }
            let {data} = await http.get(url);

            if(data.id != undefined){
                Mix.extend(this, Mix.castAs(EleveEnseignementCpl, data));
            }

            resolve(data);
        });
    }
}