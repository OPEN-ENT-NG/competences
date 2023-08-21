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

import { Model, Collection } from 'entcore';
import { Competence } from "../teacher";
import {Matiere} from "../parent_eleve/Matiere";

export interface ICompetenceResponse {
    id: number;
    nom?: string;
    index?: number;
    code_domaine?: string;
    hasnameperso?: boolean;
    ismanuelle?: boolean;
    masque?: boolean;
    competences_2?: ICompetenceResponse[];
    id_cycle?: number;
    id_enseignement?: number;
    id_parent?: number;
    id_type?: number;
    ids_domaine?: string;
    ids_domaine_int?: number[];
}

export class DefaultCompetence extends Model {
    competences: Collection<Competence>;
    selected: boolean;
    id: number;
    id_competence: number;
    nom: string;
    code_domaine: string;
    ids_domaine: string;
    composer: any;
   ids_matiere: string[];

}