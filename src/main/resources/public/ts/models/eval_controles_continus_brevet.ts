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

import { notify,Model }from 'entcore';
import http from 'axios';
import {Mix} from 'entcore-toolkit';

export class BaremeBrevetEleve {


    id_eleve: string;
    controlesContinus_brevet: number;
    totalMaxBaremeBrevet: number;

    constructor() {

    }

}

export class BaremeBrevetEleves {
    all: BaremeBrevetEleve[];

    constructor()   {
        this.all = [];
    }

    async sync(id_classe: string,idTypePeriode: number){

        try{
            let { data } = await http.get(`/competences/bfc/bareme/brevet/eleves?idClasse=${id_classe}&idTypePeriode=${idTypePeriode}`);
            this.all = Mix.castArrayAs(BaremeBrevetEleve,data);
        }catch (e){
            notify.error('evaluation.bfc.controle.continu.eleves.err');
        }
    }

}