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

import {_, Collection} from 'entcore';
import http from "axios";
import {SubtopicserviceService} from "../../models/sniplets";
import {IOverrideAverageResponse, Matiere} from "../../models/parent_eleve/Matiere";
import {Classe, Devoir} from "../../models/teacher";
import {Service} from "../../models/common/ServiceSnipplet";
import {MultiTeaching} from "../../models/common/MultiTeaching";

/**
 * @param arr liste de nombres
 * @returns la moyenne si la liste n'est pas vide
 */
export function average(arr): number {
    return _.reduce(arr, function (memo, num) {
        return memo + num;
    }, 0) / (arr.length === 0 ? 1 : arr.length);
}

function getMoyenne(devoirs): number | string {
    if (devoirs.length == 0) {
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
            if (devoir.note && devoir.coefficient && devoir.diviseur && !devoir.formative) {
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
        if (hasNote && coefficientTotalHomework !== 0) {
            let moyenne = ((sumNIMCIParD + sumCJDJ) / (sumCI + sumCJDJParM));

            if (null == moyenne) moyenne = 0.0;

            return +(moyenne).toFixed(1);
        } else {
            return "NN";
        }
    }
}

function getSumAndCoeff(devoirs: Devoir[]): [number, number] {
    if (devoirs.length == 0) {
        return [null, null];
    } else {
        let diviseurM: number = 20;

        // (SUM ( ni *m *ci /di)  + SUM ( nj *cj)  ) / (S ( ci)  + SUM ( cj  *dj /m)  )
        // avec d : diviseurs, n : note, c : coefficient, m = 20 : si ramené sur
        // avec i les notes ramenées sur m, et j les notes non ramenées sur m

        let sumCI: number = 0;
        let sumCJDJParM: number = 0;
        let sumCJDJ: number = 0;
        let sumNIMCIParD: number = 0;

        let hasNote: boolean = false;

        let coefficientTotalHomework: number = 0;
        devoirs.forEach((devoir: Devoir) => {
            if (devoir.note && devoir.coefficient && devoir.diviseur) {
                hasNote = true;
                let currNote: number = parseFloat(devoir.note);
                let currCoefficient: number = parseFloat(devoir.coefficient.toString());
                let currDiviseur: number = devoir.diviseur;

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
        if (hasNote && coefficientTotalHomework !== 0) {
            let sum: number = sumNIMCIParD + sumCJDJ;
            let coeff: number = sumCI + sumCJDJParM;

            return [sum, coeff];
        } else {
            return [null, null];
        }
    }
}

function setOverrideAverage(matieresReleve: Matiere[], matieres: Collection<Matiere>, moyennesFinales: IOverrideAverageResponse[]): void {
    for (let moyenneFinale of moyennesFinales) {
        let subject: Matiere = matieresReleve.find((matiereReleve: Matiere) => matiereReleve.id == moyenneFinale.id_matiere);
        if (!subject) {
            subject = matieres.all.find((matiere: Matiere) => matiere.id == moyenneFinale.id_matiere);
            if (!!subject) {
                subject = new Matiere(subject);
                matieresReleve.push(subject);
            }
        }
        if (!!subject) subject.overrideAverage = !!moyenneFinale.moyenne ? moyenneFinale.moyenne : "NN";
    }
}

function setSubSubjectAndSubjectAverages(matiere: any, devoirsMatieres: Devoir[], subTopicsServices: SubtopicserviceService[],
                                         moyennesFinales: IOverrideAverageResponse[], classe: Classe): void {
    let coefficientTemp: number;
    let sumMoyenneTemp: number;
    let sumMoyenne: number = 0;
    let coefficientMoyenne: number = 0;
    for (let sousMat of matiere.sousMatieres.all) {
        let coefficientSubTopic: number = 0;
        let sumMoyenneSubTopic: number = 0;
        let devoirsSousMat: Devoir[] = devoirsMatieres.filter((evaluationSubject: Devoir) =>
            evaluationSubject.id_sousmatiere == sousMat.id_type_sousmatiere);
        let mapTeacherDevoirs: Map<string, Devoir[]> = new Map();
        devoirsSousMat.forEach(devoir => {
            let devoirArray: Devoir[] = mapTeacherDevoirs.get(devoir.owner);
            devoirArray = !!devoirArray ? devoirArray : []
            devoirArray.push(devoir);
            mapTeacherDevoirs.set(devoir.owner, devoirArray);
        });

        if (devoirsSousMat.length > 0) {
            let coefficient: number = 1;
            mapTeacherDevoirs.forEach((devoirArray: Devoir[], ownerId: string) => {
                let subTopicsService: SubtopicserviceService = subTopicsServices.find((subTopic: SubtopicserviceService) => {
                    return subTopic.id_subtopic === sousMat.id_type_sousmatiere && subTopic.id_topic === matiere.id &&
                        (subTopic.id_teacher === ownerId ||

                            !!classe.services.find((service: Service) =>
                                (!!service.coTeachers && !!service.coTeachers.find((coTeacher: MultiTeaching) =>
                                    coTeacher.group_id === classe.id &&
                                    coTeacher.subject_id == matiere.id &&
                                    coTeacher.second_teacher_id === ownerId &&
                                    coTeacher.main_teacher_id === subTopic.id_teacher
                                )) ||

                                (!!service.substituteTeachers && !!service.substituteTeachers
                                    .find((substituteTeacher: MultiTeaching) =>
                                        substituteTeacher.group_id === classe.id &&
                                        substituteTeacher.subject_id == matiere.id &&
                                        substituteTeacher.second_teacher_id === ownerId &&
                                        substituteTeacher.main_teacher_id === subTopic.id_teacher
                                    ))
                            ))
                });

                if (subTopicsService) coefficient = subTopicsService.coefficient;

                let [sumMoyenneTemp, coefficientTemp]: [number, number] = getSumAndCoeff(devoirArray);
                if (sumMoyenneTemp != null && coefficientTemp != null) {
                    sumMoyenneSubTopic += sumMoyenneTemp;
                    coefficientSubTopic += coefficientTemp;
                }
            });
            if (coefficientSubTopic == 0) {
                sousMat.moyenne = "NN";
            } else {
                sousMat.moyenne = (sumMoyenneSubTopic / coefficientSubTopic).toFixed(1);
                sumMoyenne += sousMat.moyenne * coefficient;
                coefficientMoyenne += coefficient;
            }
        } else {
            sousMat.moyenne = "";
        }
    }

    if (!matiere.overrideAverage) {
        if (coefficientMoyenne != 0)
            matiere.moyenne = (sumMoyenne / coefficientMoyenne).toFixed(1);
        else
            matiere.moyenne = "";
    }
}

export async function calculMoyennesWithSubTopic(periode_idType: number, id_eleve: string, matieresReleve: Matiere[],
                                                 matieres: Collection<Matiere>, dataReleveDevoirs: Collection<Devoir>,
                                                 subTopicsServices: SubtopicserviceService[], classe: Classe): Promise<{}> {
    return new Promise(async (resolve, reject) => {
        try {
            let url = '/competences/eleve/' + id_eleve + "/moyenneFinale?";
            if (periode_idType)
                url += "idPeriode=" + periode_idType.toString();
            http.get(url).then(res => {
                let moyennesFinales: IOverrideAverageResponse[] = res.data;
                setOverrideAverage(matieresReleve, matieres, moyennesFinales);
                for (let matiere of matieresReleve) {
                    let devoirsMatieres = dataReleveDevoirs.where({id_matiere: matiere.id, formative: false});
                    if (matiere.sousMatieres != undefined && matiere.sousMatieres.all.length > 0)
                        setSubSubjectAndSubjectAverages(matiere, devoirsMatieres, subTopicsServices, moyennesFinales, classe);
                    else if (devoirsMatieres !== undefined && !matiere.overrideAverage)
                        matiere.moyenne = getMoyenne(devoirsMatieres);
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
export async function calculMoyennes(periode_idType, id_eleve, matieresReleve, matieres, dataReleveDevoirs): Promise<{}> {
    return new Promise(async (resolve, reject) => {
        try {
            let url = '/competences/eleve/' + id_eleve + "/moyenneFinale?";
            if (periode_idType)
                url += "idPeriode=" + periode_idType.toString();
            http.get(url).then(res => {
                let moyennesFinales = res.data;
                setOverrideAverage(matieresReleve, matieres, moyennesFinales);
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