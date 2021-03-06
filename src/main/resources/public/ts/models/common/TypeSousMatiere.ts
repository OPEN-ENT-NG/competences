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
import { Model } from 'entcore';
import {Mix, Selectable,Selection} from "entcore-toolkit";

import http from "axios";

export class TypeSousMatiere extends Model implements Selectable{
    id: number;
    libelle: string;
    selected: boolean;
    id_structure: string;

    async create(){
        try{
            let {status, data} = await http.post(`/viescolaire/types/sousmatiere`, this.toJson());
            this.id = data.id;
            return status === 200;
        }catch (e){
            return false
        }
    }

    async update(){
        try{
            let {status, data} = await http.put(`/viescolaire/types/sousmatiere/${this.id}`, this.toJson());
            this.id = data.id;
            return status === 200;
        }catch (e){
            return false
        }
    }

    async save() {
        if(this.id){
            return await this.update();
        } else {
            return await this.create();
        }
    }

    private toJson() {
        return {
            ...(this.id && {id: this.id}),
            ...(this.libelle && {libelle: this.libelle}),
            ...(this.id_structure && {id_structure: this.id_structure}),
        }
    }
}

export class TypeSousMatieres extends Selection<TypeSousMatiere>{
    id: number;
    libelle: string;
    id_structure: string;

    async get(idStructure){
        let {data} = await http.get(`/viescolaire/types/sousmatieres?idStructure=` + idStructure);
        this.all = Mix.castArrayAs(TypeSousMatiere, data);
    }

    async saveTopicSubTopicRelation(topics){
        let topicsToSend = [];
        let subTopicsToSend = [];

        topics.forEach(topic =>{
            if (topic.selected){
                topic.sous_matieres = this.selected;
                topicsToSend.push(topic.id);
            }
        });

        this.selected.forEach(subTopic => {
            subTopicsToSend.push(subTopic.id);
        });

        let jsonToSend = {
            topics:topicsToSend,
            subTopics:subTopicsToSend
        };

        let {status} = await http.post(`/viescolaire/types/sousmatieres/relations`, jsonToSend);
        return status === 200 || status === 204;
    }
}