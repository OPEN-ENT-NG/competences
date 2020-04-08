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

import {_} from 'entcore';
import * as utils from "../teacher";
import http from "axios";
/**
 * @param arr liste de nombres
 * @returns la moyenne si la liste n'est pas vide
 */
export function average (arr) {
    return _.reduce(arr, function(memo, num) {
            return memo + num;
        }, 0) / (arr.length === 0 ? 1 : arr.length);
}

function   getMoyenne (devoirs) {
    if(devoirs.length == 0){
        return "NN";
    }else {
        let diviseurM = 20;

        // (SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
        // avec d : diviseurs, n : note, c : coefficient, m = 20 : si ramené sur
        // avec i les notes ramenées sur m, et j les notes non ramenées sur m

        let sumCI = 0;
        let sumCJDJParM = 0;
        let sumCJDJ = 0;
        let sumNIMCIParD = 0;

        let hasNote = false;

        devoirs.forEach(devoir => {
            if(devoir.note && devoir.coefficient && devoir.diviseur) {
                hasNote = true;
                let currNote = parseFloat(devoir.note);
                let currCoefficient = parseFloat(devoir.coefficient);
                let currDiviseur = devoir.diviseur;

                if (!devoir.ramener_sur) {
                    sumCJDJParM += (currCoefficient * currDiviseur / diviseurM);
                    sumCJDJ += (currNote * currCoefficient);
                } else if (currCoefficient != 0) {
                    sumNIMCIParD += ((currNote * diviseurM * currCoefficient) / currDiviseur);
                    sumCI += currCoefficient;
                }
            }
        });
        if(hasNote) {

            let moyenne = ((sumNIMCIParD + sumCJDJ) / (sumCI + sumCJDJParM));

            if (null == moyenne) moyenne = 0.0;

            return +(moyenne).toFixed(2);
        }else{
            return "NN";
        }
    }
}

export async function calculMoyennes (periode_idType,id_eleve,matieresReleve,dataReleveDevoirs) {
    return new Promise( async (resolve, reject) => {
        try {
            let id_typePeriode = "";
            if (periode_idType)
                id_typePeriode = "idPeriode=" + periode_idType.toString();
            http.get('/competences/eleve/' + id_eleve + "/moyenneFinale?" + id_typePeriode).then(res => {
                let moyennesFinales = res.data;
                for (let matiere of matieresReleve) {
                    let devoirsMatieres = dataReleveDevoirs.where({id_matiere: matiere.id});
                    if (devoirsMatieres !== undefined) {
                        let moyenneFinale = _.findWhere(moyennesFinales, {id_matiere: matiere.id});
                        if (moyenneFinale) {
                            if (moyenneFinale.moyenne == null) {
                                matiere.moyenne = "NN";
                            } else {
                                matiere.moyenne = moyenneFinale.moyenne;
                            }
                        } else {
                            matiere.moyenne = getMoyenne(devoirsMatieres);
                        }
                        if (matiere.sousMatieres != undefined && matiere.sousMatieres.all.length > 0) {
                            for (let sousMat of matiere.sousMatieres.all) {
                                let devoirsSousMat = _.where(devoirsMatieres, {id_sousmatiere: sousMat.id_type_sousmatiere});
                                if (devoirsSousMat.length > 0) {
                                    let moyenneFinale = _.findWhere(moyennesFinales, {id_matiere: sousMat.id_type_sousmatiere});
                                    if (moyenneFinale) {
                                        if (moyenneFinale.moyenne == null) {
                                            matiere.moyenne = "NN";
                                        } else {
                                            matiere.moyenne = moyenneFinale.moyenne;
                                        }
                                    } else {
                                        sousMat.moyenne = getMoyenne(devoirsSousMat);
                                    }
                                } else {
                                    sousMat.moyenne = "";
                                }
                            }
                        }
                    }
                }
                resolve();
            });
        }
        catch (e) {
                console.error(e);
                reject(e);
            }
        });
}