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
 * Created by agnes.lapeyronnie on 19/09/2017.
 */
import { Collection, _ } from 'entcore';
import { Responsable, Classe } from '../teacher';

export class LSU {
    responsables : Collection<Responsable>;
    classes : Array<Classe>;//sans les groupes
    structureId : string;


    constructor (structureId : string, classes : Array<Classe>, responsables : Collection<Responsable>){
        this.structureId = structureId ;
        this.classes = classes;
        this.responsables =_.clone(responsables) ;

    }

    export () {
        let url = "/competences/exportLSU/lsu?idStructure=" + this.structureId;

            for(var i=0; i<this.classes.length;i++) {
                if(this.classes[i].selected){
                    url += "&idClasse=" + this.classes[i].id;
                }
            }

          /*  _.each(_.where(this.classes.all, {selected: true}), (classe) => {
                    url += "&idClasse=" + this.classes.all[i].id;

            });*/

            for(let i=0 ; i < this.responsables.all.length ; i++){
                if(this.responsables.all[i].selected){
                url+="&idResponsable=" + this.responsables.all[i].id;
                }
            }

        location.replace(url);
    }

}