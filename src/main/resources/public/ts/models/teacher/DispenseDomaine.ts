
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

import {notify} from 'entcore';
import http from 'axios';


export class DispenseDomaine {
    id_domaine : number;
    id_eleve : string;
    dispense : boolean;
    id_etablissement : string;

    constructor(id_domaine : number,id_eleve: string,dispense_eleve:boolean , id_etablissement: string){
        this. id_domaine = id_domaine;
        this.id_eleve = id_eleve;
        this.dispense = dispense_eleve;
        this.id_etablissement = id_etablissement;
    }

    toJson(){
        return {
            id_eleve: this.id_eleve,
            id_domaine: this.id_domaine,
            dispense: this.dispense
        };
    }

    async save(){
        if(this.dispense){
            await this.create();
        }else{
            await this.delete();
        }
    }
   async create(){
        try{
            await http.post(`/competences/domaine/dispense/eleve`, this.toJson());
        }catch(e){
            notify.error('evaluations.dispense.domaine.eleve.create.err');
            this.dispense = false;
        }
   }
    async delete(){
        try{
            await http.delete(`/competences/domaine/dispense/eleve/${this.id_domaine}/${
                this.id_eleve}/${this.id_etablissement}`);
        }catch(e){
            notify.error('evaluations.dispense.domaine.eleve.delete.err');
            this.dispense = true;
        }
    }

}
