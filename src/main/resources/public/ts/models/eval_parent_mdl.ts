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

declare let location: any;
declare let require: any;

export class Evaluations extends Model {
    eleves: Collection<Eleve>;
    matiere: Matiere;
    matieres: Collection<Matiere>;
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
            GET_MATIERES : '/viescolaire/matieres/infos?',
            GET_ENSEIGNANTS : '/viescolaire/enseignants?',
            GET_COMPETENCES : '/viescolaire/competences/eleve/',
            GET_ANNOTATION : '/viescolaire/annotations/eleve/',
            GET_ARBRE_DOMAINE : '/competences/domaines?idClasse=',
            GET_ENSEIGNEMENT: '/competences/enseignements',
            calculMoyenne: '/competences/eleve/'
        };
    }

    constructor (o?: any) {
        super(o);
    }

    async sync  (): Promise<any> {
        return new Promise(async (resolve) => {
            this.collection(Eleve, {
                sync: async () => {
                    return new Promise((resolve, reject) => {
                        HTTP().get(Evaluations.api.EVAL_ENFANTS)
                            .done((enfants) => {
                                this.eleves.load(enfants);
                                resolve();
                            })
                            .error(function () {
                                reject();
                            });
                    });
                }
            });
            this.collection(Enseignant, {
                sync: async (mapEnseignant) => {
                    return new Promise((resolve) => {
                        let uri = Evaluations.api.GET_ENSEIGNANTS;
                        for (let enseignant in mapEnseignant) {
                            uri += '&idUser=' + enseignant;
                        }
                        HTTP().get(uri).done((enseignants) => {
                            this.enseignants.load(enseignants);
                            resolve();
                        }).bind(this);
                    });
                }
            });
            this.collection(Matiere, {
                sync: async (mapMatiere) => {
                    return new Promise((resolve) => {
                        let uri = Evaluations.api.GET_MATIERES;
                        for (let matiere in mapMatiere) {
                            uri = uri + '&idMatiere=' + matiere;
                        }
                        HTTP().get(uri).done( (matieresResult) => {
                            this.matieres.load(matieresResult);
                            this.matieres.all.forEach( (matiere) =>{
                                if (matiere.hasOwnProperty('sous_matieres')) {
                                    matiere.sousMatieres.load(_.find(matieresResult,{id : matiere.id}).sous_matieres);
                                }
                            })
                            resolve();
                        }).bind(this);
                    });
                }
            });
            this.collection(Devoir, {
                sync: async(structureId, userId, classeId, idPeriode, idCycle, historise) => {
                    return new Promise( (resolve) => {
                        let that = this;
                        let uri = Evaluations.api.GET_EVALUATIONS
                            + structureId + '&idEleve=' + userId;
                        if (classeId !== undefined) {
                            uri = uri + '&idClasse=' + classeId;
                        }
                        if (idPeriode !== undefined) {
                            uri = uri + '&idPeriode=' + idPeriode;
                        }
                        if (historise !== undefined) {
                            uri = uri + '&historise=' + historise;
                        }
                        uri = uri + '&forStudentReleve=true';
                        HTTP().getJson(uri).done((devoirs) => {

                            // RECUPERATION DES COMPETENCES
                            let uriCompetences = Evaluations.api.GET_COMPETENCES + userId;
                            if(this.eleve && this.eleve.classe)
                                uriCompetences = uriCompetences + '?idClasse=' + this.eleve.classe.id;
                            else if(classeId)
                                uriCompetences = uriCompetences + '?idClasse=' + classeId;
                            if (idPeriode !== undefined) {
                                uriCompetences = uriCompetences + '&idPeriode=' + idPeriode;
                            }
                            if (idCycle !== undefined) {
                                uriCompetences = uriCompetences + '&idCycle=' + idCycle;
                            }
                            HTTP().getJson(uriCompetences).done((competences) => {
                                competences.forEach(function (competence) {
                                    let devoir = _.findWhere(devoirs, {id: competence.id_devoir});
                                    if (devoir !== undefined) {
                                        if (!devoir.competences) {
                                            devoir.competences = [];
                                        }
                                        devoir.competences.push(competence);
                                    }
                                    else {
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
                                let uriAnnotations = Evaluations.api.GET_ANNOTATION  + userId;
                                if (idPeriode !== undefined) {
                                    uriAnnotations = uriAnnotations + '?idPeriode=' + idPeriode ;
                                    if(this.eleve && this.eleve.classe)
                                        uriAnnotations = uriAnnotations + '&idClasse=' + this.eleve.classe.id;
                                    else if(classeId)
                                        uriAnnotations = uriAnnotations + '&idClasse=' + classeId;
                                }
                                else {
                                    if(this.eleve && this.eleve.classe)
                                        uriAnnotations = uriAnnotations + '?idClasse=' + this.eleve.classe.id;
                                    else if(classeId)
                                        uriAnnotations = uriAnnotations + '?idClasse=' + classeId;
                                }

                                HTTP().getJson(uriAnnotations).done((annotations) => {
                                    annotations.forEach(function () {
                                        annotations.forEach(function (annotation) {
                                            let devoir = _.findWhere(devoirs, {id: annotation.id_devoir});
                                            if (devoir !== undefined) {
                                                devoir.annotation = {
                                                    id : annotation.id,
                                                    libelle: annotation.libelle,
                                                    libelle_court : annotation.libelle_court
                                                };
                                            }
                                            else {

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
                                                    _type_libelle : annotation._type_libelle
                                                });
                                            }
                                        });
                                    });

                                    this.devoirs.load(devoirs);
                                    let matieresDevoirs = _.omit(_.groupBy(devoirs, 'id_matiere'),null);
                                    let enseignants = _.omit(_.groupBy(devoirs, 'owner'), "id-user-transition-annee");
                                    if(!_.isEmpty(enseignants)) {

                                        this.enseignants.sync(enseignants).then(() => {
                                            if (!_.isEmpty(matieresDevoirs)) {
                                                this.matieres.sync(matieresDevoirs).then(() => {
                                                    for (let o in matieresDevoirs) {
                                                        matieresDevoirs[o].forEach(function (element) {
                                                            let devoir = element;
                                                            let _matiere = that.matieres.findWhere({id: devoir.id_matiere});
                                                            let enseignant = that.enseignants.findWhere({id: devoir.owner});
                                                            if (enseignant !== undefined && _matiere !== undefined
                                                                && _.filter(_matiere.ens, {id: enseignant.id}).length === 0) {
                                                                _matiere.ens.push(enseignant);
                                                            }
                                                        });
                                                    }
                                                    resolve();
                                                });
                                            } else {
                                                resolve();
                                            }
                                        });
                                    }else{
                                        resolve();
                                    }
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
            }
            // Synchronisation des matières, enseignants, devoirs et de l'élève.
            else if(model.me.classNames && model.me.classNames.length>0 && model.me.classes && model.me.structures) {
                this.eleve = new Eleve({
                    id: model.me.userId,
                    idClasse: model.me.classes[0],
                    displayName: model.me.username,
                    firstName: model.me.firstName,
                    lastName: model.me.lastName,
                    idStructure: model.me.structures[0],
                    classe: new Classe({id: model.me.classes[0], name: model.me.classNames[0].split('$')[1]})
                });

                await Promise.all([this.eleve.classe.sync(), this.updateUsePerso()]);
                // await this.devoirs.sync(this.eleve.idStructure, this.eleve.id, null);
                this.synchronised = true;
                resolve();
            }
            else{
                resolve();
            }
        });
    }

    getReleve (idPeriode, idUser, idTypePeriode, ordrePeriode) {
        let uri = '/competences/releve/pdf?idEtablissement=' +
            model.me.structures[0] + '&idUser=' + idUser;
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
        Utils.setCompetenceNotes(poDomaine, poCompetencesNotes,tableauConversion);
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

