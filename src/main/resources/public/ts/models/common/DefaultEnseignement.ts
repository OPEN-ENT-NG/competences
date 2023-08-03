/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of fr.openent.competences.model
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import {Model, Collection, _ ,angular} from 'entcore';
import { Competence } from "../parent_eleve/Competence";
import { Evaluations} from "../eval_parent_mdl";
import http, {AxiosResponse} from "axios";
import {DefaultMatiere} from "./DefaultMatiere";
import {ICompetenceResponse} from "./DefaultCompetence";

export interface ITeachingResponse {
    id: number;
    nom?: string;
    competences_1?: ICompetenceResponse[];
    ids_domaine_int?: number[];
}

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
                        let enseignements : any = await this.getAll(idClasse, idCycle, model);
                        if(withLoad === true) {
                            model.load(enseignements.data);
                        }
                    }
                    let matieresStructure = model.composer.matieres.all;

                    model.each(function (enseignement) {

                        let idsMatiereEns : string[] = [];

                        if (enseignement['competences_1'] !== undefined) {
                            enseignement.competences.load(enseignement['competences_1']);
                        }
                        _.map(enseignement.competences.all, function (competence) {
                            return competence.composer = enseignement;
                        });
                        enseignement.competences.each(function (competence) {
                            let idsMatiereComp_1 : string[]= [];
                            if (competence['competences_2']!== undefined && competence['competences_2'].length > 0) {
                                competence.competences.load(competence['competences_2']);

                                _.map(competence.competences.all, function (sousCompetence) {
                                    let idsMatiereSousComp : string[] = [];
                                    sousCompetence.competencesEvaluations = _.where(competences, {
                                        id_competence: sousCompetence.id
                                    });
                                    if(enseignement.id === 6 && sousCompetence.competencesEvaluations.length > 0){//Langues
                                        idsMatiereSousComp =  _.chain(idsMatiereSousComp).union(_.pluck(sousCompetence.competencesEvaluations,'id_matiere')).value();
                                        idsMatiereComp_1 = _.chain(idsMatiereComp_1).union( _.pluck(sousCompetence.competencesEvaluations,'id_matiere')).value();
                                        idsMatiereEns = _.chain(idsMatiereEns).union( _.pluck(sousCompetence.competencesEvaluations,'id_matiere')).value();
                                    }
                                    sousCompetence.ids_matieres =_.unique(idsMatiereSousComp);
                                    return sousCompetence.composer = competence;
                                });
                            }
                            competence.ids_matieres = _.unique(idsMatiereComp_1);
                            delete competence['competences_2'];
                        });

                        enseignement.matieres.load(enseignement.getListObjectMatEnseignement(idsMatiereEns, matieresStructure));
                        delete enseignement['competences_1'];

                        _.each(enseignement.matieres.all, (matiere) => {

                            matiere.competences = angular.copy(enseignement.competences);

                            matiere.competences.all=  _.chain(matiere.competences.all)
                                .filter((comp) => {
                                    return _.contains(comp.ids_matieres, matiere.id) || _.isEmpty(comp.ids_matieres);
                                })
                                .each((comp) => {
                                     comp.competences.all = _.chain(comp.competences.all)
                                         .filter((sousComp) => {
                                             return _.contains(sousComp.ids_matieres, matiere.id) || _.isEmpty(sousComp.ids_matieres);
                                         })
                                         .each((sousComp)=> {
                                             sousComp.competencesEvaluations = _.where(sousComp.competencesEvaluations,
                                                 { id_matiere : matiere.id}
                                             );
                                         })
                                         .value();
                                 }).value();
                        });

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

    public static async getAll(idClasse: string, idCycle: string, model: any): Promise<AxiosResponse<ITeachingResponse[]>> {
        let cycleFilter: string = !!idCycle ? `&idCycle=${idCycle}` : '';
        try {
            return !!model.all && !!model.all.length ?
                Promise.resolve({data: [...<ITeachingResponse[]>model.all.map(m => m.data)],
                    status: 200, statusText: "OK", headers: {}, config: {}}) :
                http.get(`${Evaluations.api.GET_ENSEIGNEMENT}?idClasse=${idClasse}${cycleFilter}`)
        } catch (err) {
            console.error(err);
            throw err;
        }
    }
    getListObjectMatEnseignement (idsMatiereEns, matieresStructure) : DefaultMatiere[]{

        let matEnseignement : Array<DefaultMatiere> = [];

        _.each(idsMatiereEns, (idMat)=> {
            if(_.findWhere(matieresStructure,{id: idMat}) != undefined ){
                let matiere = _.findWhere(matieresStructure,{id: idMat});
                matiere.composer = this;
                matEnseignement.push(matiere);
            }

        });
        return matEnseignement
    }
}