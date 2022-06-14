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
 * Created by ledunoiss on 08/08/2016.
 */
import {model, Model, http as HTTP, Collection, _, Behaviours} from 'entcore';
import { Classe } from './parent_eleve/Classe';
import { Devoir } from './parent_eleve/Devoir';
import { Matiere } from './parent_eleve/Matiere';
import { Eleve } from './parent_eleve/Eleve';
import { Enseignant } from './parent_eleve/Enseignant';
import { Periode } from './parent_eleve/Periode';
import {Domaine, Structure, SuiviCompetence, Utils} from './teacher';
import { NiveauCompetence } from './eval_niveau_comp';
import { Enseignement } from './parent_eleve/Enseignement';
import http from 'axios';
import {getTitulairesForRemplacantsCoEnseignant} from "../utils/functions/getTitulairesForRemplacantsCoEnseignant";
import httpAxios from "axios";
import {Service} from "./common/ServiceSnipplet";

declare let location: any;
declare let require: any;

export class Evaluations extends Model {
    eleves: Collection<Eleve>;
    matiere: Matiere;
    matieres: Collection<Matiere>;
    services: Collection<Service>;
    classes: Collection<Classe>;
    enseignants: Collection<Enseignant>;
    enseignements: Collection<Enseignement>;
    devoirs: Collection<Devoir>;
    eleve: Eleve; // Elève courant
    periode: Periode; // Période courante
    niveauCompetences: Collection<NiveauCompetence>;
    domaines: Collection<Domaine>;
    arrayCompetences: any;
    usePerso: any;
    composer: any;
    synchronised: boolean = false;

    static get api() {
        return {
            EVAL_ENFANTS: `/competences/enfants?userId=${model.me.userId}`,
            GET_EVALUATIONS : '/competences/devoirs?idEtablissement=',
            GET_MATIERES : '/viescolaire/matieres/services-filter?idEtablissement=',
            GET_ENSEIGNANTS : '/competences/user/list?profile=Teacher&structureId=',
            GET_COMPETENCES : '/viescolaire/competences/eleve',
            GET_ANNOTATION : '/viescolaire/annotations/eleve',
            GET_ARBRE_DOMAINE : '/competences/domaines?idClasse=',
            GET_ENSEIGNEMENT: '/competences/enseignements',
            GET_SERVICES: '/viescolaire/services?idEtablissement=',
            calculMoyenne: '/competences/eleve/'
        };
    }

    constructor (o?: any) {
        super(o);
    }

    async sync (): Promise<any> {
        return new Promise(async (resolve) => {
            this.collection(Eleve, {
                sync: async () => {
                    return new Promise((resolve, reject) => {
                        HTTP().get(Evaluations.api.EVAL_ENFANTS).done((enfants) => {
                            this.eleves.load(enfants);
                            resolve();
                        }).error(function () {
                            reject();
                        });
                    });
                }
            });
            this.collection(Enseignant, {
                sync: async (idEtablissement) => {
                    return new Promise((resolve) => {
                        let uri = Evaluations.api.GET_ENSEIGNANTS + idEtablissement;
                        HTTP().get(uri).done((enseignants) => {
                            this.enseignants.load(enseignants);
                            resolve();
                        }).bind(this);
                    });
                }
            });
            this.collection(Service, {
                sync: async () => {
                    return new Promise((resolve) => {
                        let uri = Evaluations.api.GET_SERVICES + model.me.structures[0];
                        HTTP().get(uri).done((services) => {
                            this.services.all = services;
                            resolve();
                        }).bind(this);
                    });
                }
            });

            this.collection(Matiere, {
                sync: async () => {
                    return new Promise((resolve) => {
                        let uri = Evaluations.api.GET_MATIERES + model.me.structures[0];
                        HTTP().get(uri).done((matieres) => {
                            this.matieres.load(matieres);
                            this.matieres.all.forEach((matiere) => {
                                if (matiere.hasOwnProperty('sous_matieres')) {
                                    matiere.sousMatieres.load(_.find(matieres, {id : matiere.id}).sous_matieres);
                                }
                            });
                            resolve();
                        }).bind(this);
                    });
                }
            });
            this.collection(Devoir, {
                sync: async(structureId, userId, classeId, periode?, idCycle?, historise?, classe?) => {
                    return new Promise((resolve) => {
                        let that = this;
                        let idPeriode = periode != undefined ? periode.id_type : undefined;

                        let uri = Evaluations.api.GET_EVALUATIONS + structureId + '&forStudentReleve=true'
                            + '&idEleve=' + userId;

                        if (classeId !== undefined) {
                            uri += '&idClasse=' + classeId;
                        }

                        if (idPeriode !== undefined) {
                            uri += '&idPeriode=' + idPeriode;
                        }

                        if (historise !== undefined) {
                            uri += '&historise=' + historise;
                        }

                        HTTP().getJson(uri).done((devoirs) => {
                            let uriCompetences = Evaluations.api.GET_COMPETENCES + '?idEleve=' + userId;

                            if(this.eleve && this.eleve.classe)
                                uriCompetences += '&idClasse=' + this.eleve.classe.id;
                            else if(classeId)
                                uriCompetences += '&idClasse=' + classeId;

                            if (idPeriode !== undefined) {
                                uriCompetences += '&idPeriode=' + idPeriode;
                            }

                            if (idCycle !== undefined) {
                                uriCompetences += '&idCycle=' + idCycle;
                            }

                            HTTP().getJson(uriCompetences).done((competences) => {
                                competences.forEach(function (competence) {
                                    let devoir = _.findWhere(devoirs, {id: competence.id_devoir});
                                    if (devoir !== undefined) {
                                        if (!devoir.competences) {
                                            devoir.competences = [];
                                        }
                                        devoir.competences.push(competence);
                                    } else {
                                        let _c = [];
                                        _c.push(competence);
                                        devoirs.push({
                                            id : competence.id_devoir,
                                            id_matiere: competence.id_matiere,
                                            id_sousmatiere: competence.id_sousmatiere,
                                            owner : competence.owner,
                                            competences : _c,
                                            apprec_visible : competence.apprec_visible,
                                            coefficient : competence.coefficient,
                                            created : competence.created,
                                            date : competence.date,
                                            date_publication : competence.date_publication,
                                            diviseur : competence.diviseur,
                                            id_etat : competence.id_etat,
                                            id_periode : competence.id_periode,
                                            id_type : competence.id_type,
                                            is_evaluated : competence.is_evaluated,
                                            libelle : competence.libelle,
                                            name : competence.name,
                                            _type_libelle : competence._type_libelle
                                        });
                                    }
                                });

                                // RECUPERATION DES ANNOTATIONS
                                let uriAnnotations = Evaluations.api.GET_ANNOTATION + '?idEleve=' + userId;
                                if (idPeriode !== undefined) {
                                    uriAnnotations += '&idPeriode=' + idPeriode;
                                }

                                if(this.eleve && this.eleve.classe)
                                    uriAnnotations += '&idClasse=' + this.eleve.classe.id;
                                else if(classeId)
                                    uriAnnotations += '&idClasse=' + classeId;

                                HTTP().getJson(uriAnnotations).done((annotations) => {
                                    annotations.forEach(function (annotation) {
                                        let devoir = _.findWhere(devoirs, {id: annotation.id_devoir});
                                        if (devoir !== undefined) {
                                            devoir.annotation = {
                                                id : annotation.id,
                                                libelle: annotation.libelle,
                                                libelle_court : annotation.libelle_court
                                            };
                                        } else {
                                            devoirs.push({
                                                id : annotation.id_devoir,
                                                id_matiere: annotation.id_matiere,
                                                id_sousmatiere: annotation.id_sousmatiere,
                                                owner : annotation.owner,
                                                annotation : {
                                                    id : annotation.id,
                                                    libelle: annotation.libelle,
                                                    libelle_court : annotation.libelle_court
                                                },
                                                apprec_visible : annotation.apprec_visible,
                                                coefficient : annotation.coefficient,
                                                created : annotation.created,
                                                date : annotation.date,
                                                date_publication : annotation.date_publication,
                                                diviseur : annotation.diviseur,
                                                id_etat : annotation.id_etat,
                                                id_periode : annotation.id_periode,
                                                id_type : annotation.id_type,
                                                is_evaluated : annotation.is_evaluated,
                                                libelle : annotation.lib,
                                                name : annotation.name,
                                                competences : [],
                                                _type_libelle : annotation._type_libelle,
                                                sum_notes : annotation.sum_notes,
                                                nbr_eleves : annotation.nbr_eleves
                                            });
                                        }
                                    });

                                    this.devoirs.load(devoirs);
                                    this.enseignants.sync(structureId);
                                    //releve note
                                    let devoirsWithNote = _.filter(devoirs, (d) => { return d.note !== undefined ; });
                                    let matieresDevoirs = _.pluck(devoirsWithNote, 'id_matiere');
                                    let groupesDevoirs = _.pluck(devoirsWithNote, 'id_groupe');
                                    let homeworksOwner = _.pluck(devoirs, 'owner');
                                    this.enseignants.sync(structureId).then(() => {
                                        this.matieres.sync().then(() => {
                                            if(this.eleve != undefined && this.eleve.classe != undefined && classe == undefined) {
                                                classe = this.eleve.classe;
                                            }

                                            this.services.sync().then(() => {
                                                let filteredServices = this.services.filter((service) => {
                                                    return _.contains(groupesDevoirs, service.id_groupe) && service.evaluable;
                                                });

                                                _.forEach(filteredServices, service => {
                                                    let _matiere = that.matieres.findWhere({id: service.id_matiere});
                                                    if(_matiere !== undefined) {

                                                        let enseignant = that.enseignants.findWhere({id: service.id_enseignant});
                                                        if(enseignant !== undefined && service.is_visible && _.contains(homeworksOwner, enseignant.id)) {
                                                            _matiere.ens.push(enseignant);
                                                        }

                                                        _.forEach(service.coTeachers, coTeacher => {
                                                            let enseignant = that.enseignants.findWhere({id: coTeacher.second_teacher_id});
                                                            if(coTeacher.is_visible && enseignant != undefined && !_.contains(_matiere.ens, enseignant)) {
                                                                _matiere.ens.push(enseignant);
                                                            }
                                                        });

                                                        _.forEach(service.substituteTeachers, substituteTeacher => {
                                                            let enseignant = that.enseignants.findWhere({id: substituteTeacher.second_teacher_id});
                                                            let conditionForDate = periode != undefined ? Utils.checkDateForSubTeacher(substituteTeacher, periode) : true;

                                                            if(substituteTeacher.is_visible && enseignant != undefined && !_.contains(_matiere.ens, enseignant) && conditionForDate) {
                                                                _matiere.ens.push(enseignant);
                                                            }
                                                        });
                                                        _matiere.hasDevoirWithNote = _.contains(matieresDevoirs, _matiere.id);
                                                    }
                                                });
                                                resolve();
                                            })
                                        });
                                    });
                                }).bind(this);
                            }).bind(this);
                        }).bind(this);
                    });
                }
            });
            this.collection(Domaine, {
                sync: async function (classe, eleve, competences, idCycle) {
                    let that = this.composer;
                    return new Promise(async (resolve, reject) => {
                        try {
                            var url = Evaluations.api.GET_ARBRE_DOMAINE + classe.id;
                            if (idCycle !== undefined) {
                                url = url + '&idCycle=' + idCycle;
                            }
                            let uriGetConversionTable = SuiviCompetence.api.getCompetenceNoteConverssion + '?idEtab=' + model.me.structures[0] + '&idClasse=' + classe.id;
                            let response = await Promise.all([
                                http.get(url),
                                http.get(uriGetConversionTable)
                            ]);
                            let resDomaines = response[0].data;
                            let resGetConversionTable = {all:response[1].data};

                            if (resDomaines) {
                                let _res = [];
                                for (let i = 0; i < resDomaines.length; i++) {
                                    let domaine = new Domaine(resDomaines[i]);
                                    that.setCompetenceNotes(domaine, competences,resGetConversionTable);
                                    _res.push(domaine);
                                }
                                that.domaines.load(_res);
                            }
                            if (resolve && typeof (resolve) === 'function') {
                                resolve();
                            }
                        } catch (e) {
                            reject(e);
                        }
                    });
                }
            });
            this.collection(Enseignement, {
                sync: async (idClasse: string, competences, idCycle: string) => {
                    this.enseignements.all = [];
                    await Enseignement.loadCompetences(idClasse, competences, idCycle, this.enseignements,
                        true);
                }
            });

            // Synchronisation de la collection d'élèves pour les parents
            if (model.me.type === 'PERSRELELEVE') {
                await this.eleves.sync();
                this.eleve = this.eleves.first();
                await this.updateUsePerso();
                this.synchronised = true;
                resolve ();
            } else if(model.me.classNames && model.me.classNames.length > 0 && model.me.classes && model.me.structures) {
                // Synchronisation des matières, enseignants, devoirs de l'élève.

                this.eleve = new Eleve({
                    id: model.me.userId,
                    idClasse: model.me.classes[0],
                    displayName: model.me.username,
                    firstName: model.me.firstName,
                    lastName: model.me.lastName,
                    idStructure: model.me.structures[0],
                    classe: new Classe({id: model.me.classes[0], name: model.me.classNames[0].split('$')[1]})
                });

                await Promise.all([this.eleve.classe.sync(),
                    this.updateUsePerso()]);

                this.synchronised = true;
                resolve();
            } else {
                resolve();
            }
        });
    }

    getReleve (idPeriode, idEleve, idTypePeriode, ordrePeriode) {
        let uri = '/competences/releve/pdf?idEtablissement=' +
            model.me.structures[0] + '&idEleve=' + idEleve;
        if (idPeriode !== undefined && idPeriode !== null) {
            uri += '&idPeriode=' + idPeriode;
            if (idTypePeriode !== undefined) {
                uri += '&idTypePeriode=' + idTypePeriode;
                if (ordrePeriode !== undefined) {
                    uri += '&ordrePeriode=' + ordrePeriode;
                }
            }
        }

        location.replace(uri);
    }
    setCompetenceNotes(poDomaine, poCompetencesNotes, tableauConversion) {
        let listTeacher = getTitulairesForRemplacantsCoEnseignant(model.me.userId, this.eleve.classe);
        Utils.setCompetenceNotes(poDomaine, poCompetencesNotes,tableauConversion, undefined,undefined,
            undefined,undefined,listTeacher);
    }

    async updateUsePerso () {
        // Recup du ...
        if(Behaviours.applicationsBehaviours.viescolaire === undefined){
            await model.me.workflow.load(['viescolaire']);
        }
        let s = new Structure({id:  model.me.structures[0]});
        //let s = evaluations.structure;
        s.usePersoFun(model.me.userId).then(async(res) => {
            this.niveauCompetences = s.niveauCompetences;
            if (res) {
                this.usePerso = 'true';
            }
            else {
                this.usePerso = 'false';
            }
        });
    }
}


export let evaluations = new Evaluations();

model.build = async function () {
    require('angular-chart.js');
    (this as any).evaluations = evaluations;
    if(Behaviours.applicationsBehaviours.viescolaire === undefined){
        await model.me.workflow.load(['viescolaire']);
    }
    await evaluations.sync();
};

