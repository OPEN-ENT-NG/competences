/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of model
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import {Model, Collection, _} from 'entcore';
import { Competence } from "../parent_eleve/Competence";
import { Evaluations} from "../eval_parent_mdl";
import http from "axios";

export class DefaultEnseignement extends Model {
    id;
    competences : Collection<Competence>;

    public static async loadCompetences (idClasse: string, competences, idCycle: string, model: any,
                                         withLoad? : boolean) {
        return new Promise(async (resolve, reject) => {
            if (idClasse === undefined) {
                console.error('idClasse must be defined');
                reject();
            }
            else {
                try {
                    if(_.isEmpty(model.all)) {
                        let enseignements : any = await this.getAll(idClasse,idCycle,model);
                        if(withLoad === true) {
                            model.load(enseignements.data);
                        }
                    }
                    model.each(function (enseignement) {
                        if (enseignement['competences_1'] !== undefined) {
                            enseignement.competences.load(enseignement['competences_1']);
                        }
                        _.map(enseignement.competences.all, function (competence) {
                            return competence.composer = enseignement;
                        });
                        enseignement.competences.each(function (competence) {
                            if (competence['competences_2']!== undefined && competence['competences_2'].length > 0) {
                                competence.competences.load(competence['competences_2']);
                                _.map(competence.competences.all, function (sousCompetence) {
                                    sousCompetence.competencesEvaluations = _.where(competences, {
                                        id_competence: sousCompetence.id
                                    });
                                    return sousCompetence.composer = competence;
                                });
                            }
                            delete competence['competences_2'];
                        });
                        delete enseignement['competences_1'];
                    });
                    if (resolve && typeof (resolve) === 'function') {
                        resolve();
                    }
                }
                catch (e) {
                    console.error(e);
                    reject(e);
                }
            }
        });
    }

    public static async getAll(idClasse: string, idCycle: string, model: any){
        return new Promise(async (resolve,reject) => {
            let uri = Evaluations.api.GET_ENSEIGNEMENT;
            uri += '?idClasse=' + idClasse;
            if (idCycle !== undefined) {
                uri += '&idCycle=' + idCycle;
            }
            try {
                if (_.isEmpty(model.all)) {
                    let res = await http.get(uri);
                    resolve(res);
                }
                else {
                    resolve({data: model.all});
                }
            }
            catch (e){
                console.error(e);
                reject(e);
            }
        });
    }
}