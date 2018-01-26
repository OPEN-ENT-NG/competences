/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

/**
 * Created by ledunoiss on 08/08/2016.
 */
import { model, http, Model, Collection, moment, _ } from 'entcore';
import { Classe } from './parent_eleve/Classe';
import { Devoir } from './parent_eleve/Devoir';
import { Matiere } from './parent_eleve/Matiere';
import { Eleve } from './parent_eleve/Eleve';
import { Enseignant } from './parent_eleve/Enseignant';
import { Periode } from './parent_eleve/Periode';
import { Domaine, Structure } from './teacher';
import { NiveauCompetence } from './eval_niveau_comp';
import { Enseignement } from './parent_eleve/Enseignement';

declare let location: any;
declare let console: any;
declare let require: any;

export class Evaluations extends Model {
    eleves: Collection<Eleve>;
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

    static get api() {
        return {
            EVAL_ENFANTS: `/competences/enfants?userId=${model.me.userId}`,
            GET_EVALUATIONS : '/competences/devoirs?idEtablissement=',
            GET_MATIERES : '/viescolaire/matieres/infos?',
            GET_ENSEIGNANTS : '/viescolaire/enseignants?',
            GET_COMPETENCES : '/viescolaire/competences/eleve/',
            GET_ANNOTATION : '/viescolaire/annotations/eleve/',
            GET_ARBRE_DOMAINE : '/competences/domaines?idClasse=',
            GET_ENSEIGNEMENT: '/competences/enseignements'
        };
    }

    constructor (o?: any) {
        super(o);
    }

    async sync  (): Promise<any> {
        return new Promise(async (resolve) => {
            // await this.classes.sync(model.me.structures[0]);
            this.collection(Eleve, {
                sync: async () => {
                    return new Promise((resolve, reject) => {
                        http().get(Evaluations.api.EVAL_ENFANTS)
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
                        http().get(uri).done((enseignants) => {
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
                        http().get(uri).done((matieres) => {
                            this.matieres.load(matieres);
                            resolve();
                        }).bind(this);
                    });
                }
            });
            this.collection(Devoir, {
                sync: async(structureId, userId, classeId, idPeriode) => {
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
                        http().getJson(uri).done((devoirs) => {

                            // RECUPERATION DES COMPETENCES
                            let uriCompetences = Evaluations.api.GET_COMPETENCES + userId;
                            uriCompetences = uriCompetences + '?idClasse=' + this.eleve.classe.id;
                            if (idPeriode !== undefined) {
                                uriCompetences = uriCompetences + '&idPeriode=' + idPeriode;
                            }
                            http().getJson(uriCompetences).done((competences) => {
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

                                // RECUPERATION DES ANNOTIONS
                                let uriAnnotations = Evaluations.api.GET_ANNOTATION  + userId;
                                if (idPeriode !== undefined) {
                                    uriAnnotations = uriAnnotations + '?idPeriode=' + idPeriode;
                                }
                                http().getJson(uriAnnotations).done((annotations) => {
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
                                    let matieresDevoirs = _.groupBy(devoirs, 'id_matiere');
                                    let enseignants = _.groupBy(devoirs, 'owner');
                                    this.enseignants.sync(enseignants).then(() => {
                                        this.matieres.sync(matieresDevoirs).then(() => {
                                            for (let o in matieresDevoirs) {
                                                matieresDevoirs[o].forEach(function (element) {
                                                    let devoir = element;
                                                    let _matiere = that.matieres.findWhere({id: devoir.id_matiere});
                                                    let enseignant = that.enseignants.findWhere({id: devoir.owner});
                                                    if ( enseignant !== undefined && _matiere !== undefined
                                                        && _.filter(_matiere.ens, {id: enseignant.id}).length === 0) {
                                                        _matiere.ens.push(enseignant);
                                                    }
                                                });
                                            }
                                            resolve();
                                        });
                                    });
                                }).bind(this);
                            }).bind(this);
                        }).bind(this);
                    });
                }
            });
            this.collection(Domaine, {
                sync: async function (classe, eleve, competences) {
                    let that = this.composer;
                    return new Promise((resolve, reject) => {
                        let url = that.api.GET_ARBRE_DOMAINE + classe.id;
                        http().getJson(url).done((resDomaines) => {
                            if (resDomaines) {
                                let _res = [];
                                for (let i = 0; i < resDomaines.length; i++) {

                                    let domaine = new Domaine(resDomaines[i]);
                                    that.setCompetenceNotes(domaine, competences);
                                    _res.push(domaine);
                                }
                                that.domaines.load(_res);
                            }
                            if (resolve && typeof (resolve) === 'function') {
                                resolve();
                            }
                        }).bind(this);
                    });
                }
            });
            this.collection(Enseignement, {
                sync: async function (idClasse: string, competences) {
                    let that = this.composer;
                    return new Promise((resolve, reject) => {
                        let uri = that.api.GET_ENSEIGNEMENT;
                        if (idClasse !== undefined) {
                            uri += '?idClasse=' + idClasse;
                            http().getJson(uri).done(function (res) {
                                this.load(res);
                                this.each(function (enseignement) {
                                    enseignement.competences.load(enseignement['competences_1']);
                                    _.map(enseignement.competences.all, function (competence) {
                                        return competence.composer = enseignement;
                                    });
                                    enseignement.competences.each(function (competence) {
                                        if (competence['competences_2'].length > 0) {
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
                            }.bind(this));
                        } else {
                            console.error('idClasse must be defined');
                        }
                    });
                }
            });
            // Synchronisation de la collection d'élèves pour les parents
            if (model.me.type === 'PERSRELELEVE') {
                await this.eleves.sync();
                this.eleve = this.eleves.first();
                await this.updateUsePerso();
                resolve ();
            }
            // Synchronisation des matières, enseignants, devoirs et de l'élève.
            else {
                this.eleve = new Eleve({
                    id: model.me.userId,
                    idClasse: model.me.classes[0],
                    displayName: model.me.username,
                    firstName: model.me.firstName,
                    lastName: model.me.lastName,
                    idStructure: model.me.structures[0],
                    classe: new Classe({id: model.me.classes[0], name: model.me.classNames[0].split('$')[1]})
                });

                await this.eleve.classe.sync();
                await this.devoirs.sync(this.eleve.idStructure, this.eleve.id, null);
                await this.updateUsePerso();
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
    setCompetenceNotes(poDomaine, poCompetencesNotes) {
        if (poDomaine.competences) {
            _.map(poDomaine.competences.all, function (competence) {
                competence.competencesEvaluations = _.where(poCompetencesNotes, {
                    id_competence: competence.id
                });

            });
        }

        if ( poDomaine.domaines) {
            for (let i = 0; i < poDomaine.domaines.all.length; i++) {
                this.setCompetenceNotes(poDomaine.domaines.all[i], poCompetencesNotes);
            }
        }
    }

    async updateUsePerso () {
        // Recup du ...
        let s = new Structure({id:  model.me.structures[0]});
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
    await evaluations.sync();
};

