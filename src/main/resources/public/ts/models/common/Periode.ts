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

import {Model} from 'entcore';

export interface Period {
    id?: number;
    id_etablissement?: string;
    id_classe?: string;
    timestamp_dt: Date;
    timestamp_fn: Date;
    date_fin_saisie?: Date;
    id_type?: number;
    date_conseil_classe?: Date;
    publication_bulletin?: boolean;

    type?: number;
    ordre?: number;
    label?: string;
}

/**
 * Created by rahnir on 10/08/2017.
 */
export class Periode extends Model {
    id: number;
    id_etablissement: string;
    id_classe: string;
    timestamp_dt: Date;
    timestamp_fn: Date;
    date_fin_saisie: Date;
    id_type: number;
    date_conseil_classe: Date;
    publication_bulletin: boolean;

    toString = function () {
        return String(this.id_type);
    }
}