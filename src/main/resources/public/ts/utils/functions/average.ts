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

import {_, workspace} from 'entcore';
import http from "axios";
import service = workspace.v2.service;

/**
 * @param arr liste de nombres
 * @returns la moyenne si la liste n'est pas vide
 */
export function average (arr) {
    return _.reduce(arr, function(memo, num) {
        return memo + num;
    }, 0) / (arr.length === 0 ? 1 : arr.length);
}

function getMoyenne (devoirs) {
    if(devoirs.length == 0){
        return "NN";
    } else {
        let diviseurM = 20;

        // (SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
        // avec d : diviseurs, n : note, c : coefficient, m = 20 : si ramené sur
        // avec i les notes ramenées sur m, et j les notes non ramenées sur m

        let sumCI = 0;
        let sumCJDJParM = 0;
        let sumCJDJ = 0;
        let sumNIMCIParD = 0;

        let hasNote = false;

        let coefficientTotalHomework = 0;
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
                coefficientTotalHomework += currCoefficient;
            }
        });
        if(hasNote && coefficientTotalHomework !== 0) {
            let moyenne = ((sumNIMCIParD + sumCJDJ) / (sumCI + sumCJDJParM));

            if (null == moyenne) moyenne = 0.0;

            return +(moyenne).toFixed(1);
        }else{
            return "NN";
        }
    }
}

function addMatieresWithoutDevoirs(matieresReleve, matieres, moyennesFinales) {
    for(let moyenneFinale of moyennesFinales) {
        if(!_.contains(_.pluck(matieresReleve, 'id'), moyenneFinale.id_matiere)) {
            let matiere = _.findWhere(matieres.all, {id : moyenneFinale.id_matiere});
            if (moyenneFinale.moyenne == null) {
                matiere.moyenne = "NN";
            } else {
                matiere.moyenne = moyenneFinale.moyenne;
            }
            matieresReleve.push(matiere);
        }
    }
}

function getMoyenneSubTopic(matiere: any, devoirsMatieres: any[], subTopicsServices, moyennesFinales) {
    let coefficientTotal = 0;
    let sumMoyenneSubTopic = 0
    for (let sousMat of matiere.sousMatieres.all) {
        let devoirsSousMat = _.where(devoirsMatieres, {id_sousmatiere: sousMat.id_type_sousmatiere});
        let mapTeacherDevoirs = new Map();

        devoirsSousMat.forEach(devoir => {
            if (mapTeacherDevoirs.get(devoir.owner) === undefined) {
                let devoirArray = []
                devoirArray.push(devoir);
                mapTeacherDevoirs.set(devoir.owner, devoirArray)
            } else {
                let devoirArray = mapTeacherDevoirs.get(devoir.owner)
                devoirArray.push(devoir);
                mapTeacherDevoirs.set(devoir.owner, devoirArray)
            }
        })
        if (devoirsSousMat.length > 0) {
            mapTeacherDevoirs.forEach((devoirArray, key) => {
                let coefficient = 1;
                let subTopicsService = subTopicsServices.find(subTopic => subTopic.id_subtopic === sousMat.id_type_sousmatiere
                    && subTopic.id_topic === matiere.id && subTopic.id_teacher === key)
                if (subTopicsService)
                    coefficient = subTopicsService.coefficient
                let moyenneFinale = _.findWhere(moyennesFinales, {id_matiere: sousMat.id_type_sousmatiere});
                if (moyenneFinale) {
                    if (moyenneFinale.moyenne == null) {
                        matiere.moyenne = "NN";
                    } else {
                        matiere.moyenne = moyenneFinale.moyenne;
                        sumMoyenneSubTopic += matiere.moyenne * coefficient;
                        coefficientTotal += coefficient;
                    }
                } else {
                    sousMat.moyenne = getMoyenne(devoirsSousMat);
                    sumMoyenneSubTopic += sousMat.moyenne * coefficient;
                    coefficientTotal += coefficient;
                }
            })
        } else {
            sousMat.moyenne = "";
        }
    }

    if (coefficientTotal != 0)
        matiere.moyenne = (sumMoyenneSubTopic / coefficientTotal).toFixed(2);
    else
        matiere.moyenne = ""
}

export async function calculMoyennesWithSubTopic(periode_idType, id_eleve, matieresReleve, matieres, dataReleveDevoirs,subTopicsServices) {
    return new Promise(async (resolve, reject) => {
        try {
            let url = '/competences/eleve/' + id_eleve + "/moyenneFinale?";
            if (periode_idType)
                url += "idPeriode=" + periode_idType.toString();
            http.get(url).then(res => {
                let moyennesFinales = res.data;
                addMatieresWithoutDevoirs(matieresReleve, matieres, moyennesFinales);
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
                            if(!(matiere.sousMatieres != undefined && matiere.sousMatieres.all.length > 0))
                                matiere.moyenne = getMoyenne(devoirsMatieres);

                        }
                        if (matiere.sousMatieres != undefined && matiere.sousMatieres.all.length > 0) {
                             getMoyenneSubTopic(matiere, devoirsMatieres, subTopicsServices, moyennesFinales);
                        }
                    }
                }
                resolve();
            });
        } catch (e) {
            console.error(e);
            reject(e);
        }
    });
}

//DELTE AFTER and replace with previous
export async function calculMoyennes(periode_idType, id_eleve, matieresReleve, matieres, dataReleveDevoirs) {
    return new Promise(async (resolve, reject) => {
        try {
            let url = '/competences/eleve/' + id_eleve + "/moyenneFinale?";
            if (periode_idType)
                url += "idPeriode=" + periode_idType.toString();
            http.get(url).then(res => {
                let moyennesFinales = res.data;
                addMatieresWithoutDevoirs(matieresReleve, matieres, moyennesFinales);
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
        } catch (e) {
            console.error(e);
            reject(e);
        }
    });
}