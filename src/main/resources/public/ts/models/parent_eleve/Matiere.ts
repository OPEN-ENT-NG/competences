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

import {Collection} from 'entcore';
import {DefaultMatiere} from "../common/DefaultMatiere";
import {Competence} from "./Competence";
import {SousMatiere} from "./TypeSousMatiere";

export interface IOverrideAverageResponse {
    id_classe: string;
    id_eleve: string;
    id_matiere: string;
    id_periode: number;
    moyenne: string;
}

export class Matiere extends DefaultMatiere {
    id: string;
    name: string;
    externalId: string;
    ens: any = [];
    ens_is_visible: boolean;
    moyenne: number | string;
    overrideAverage: number | string;
    hasDevoirWithNote: boolean;
    sousMatieres: Collection<SousMatiere>;

    constructor(data?: any) {
        super(data);
        this.collection(Competence);
        this.collection(SousMatiere);
    }

    getAverage(): string {
        let res: number | string = !!this.overrideAverage ? this.overrideAverage.toString() : this.moyenne
        return !!res || (typeof res == 'string' && res.trim() == "") ? res.toString() : "NN";
    }

}