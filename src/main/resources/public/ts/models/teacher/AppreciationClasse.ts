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

import http from 'axios';
import {Mix} from "entcore-toolkit";
import {notify} from "entcore";

export class AppreciationClasse  {
    appreciation: string;
    id_classe: string;
    id_periode: number;
    id_matiere: string;
    endSaisie: boolean;
    idEtablissement: string;

    constructor(idClasse:string, idMatiere:string, idPeriode:number,
                endSaisie:boolean, idEtablissement:string, appreciation?:string) {
        // super();
        this.id_classe = idClasse;
        this.id_matiere = idMatiere;
        this.id_periode = idPeriode;
        this.endSaisie = endSaisie;
        this.idEtablissement = idEtablissement;
        if (appreciation !== undefined) {
            this.appreciation = appreciation;
        }
    }

    async sync() {
        try {
            let {data} = await http.get(`/competences/appreciation/classe?id_matiere=${this.id_matiere}
            &id_classe=${this.id_classe}&id_periode=${this.id_periode}`);
            // Mix.extend(this, Mix.castAs(AppreciationClasse, data) );
            if(data.appreciation !== undefined) {
                this.appreciation = data.appreciation;
            }
        } catch (e) {
            notify.error('evaluations.releve.appreciation.classe.get.error');
        }
    }

    toJSON() {
        return {
            appreciation: this.appreciation,
            id_classe: this.id_classe,
            id_periode: this.id_periode,
            id_matiere: this.id_matiere,
            idEtablissement: this.idEtablissement
        }
    }

    async save () {
        try {
            if(this.appreciation !== undefined){
             await http.post(`/competences/appreciation/classe`, this.toJSON());
            }
        } catch (e) {
            notify.error('evaluations.releve.appreciation.classe.save.error');
        }
    }

}