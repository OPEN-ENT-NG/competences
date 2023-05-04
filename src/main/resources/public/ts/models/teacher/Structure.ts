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

import {_, Collection, http, idiom as lang, model, Model, angular, toasts} from 'entcore';
import * as utils from '../../utils/teacher';
import {
    Annotation,
    Classe,
    Cycle,
    Defaultcolors,
    Devoir,
    Devoirs,
    DevoirsCollection,
    Eleve,
    Enseignant,
    Enseignement,
    evaluations,
    Matiere,
    NiveauCompetence,
    ReleveNote,
    Responsable,
    Type,
    TypePeriode,
    TypeSousMatiere,
    Utils
} from './index';
import {Mix} from "entcore-toolkit";
import httpAxios from 'axios';
import {StructureOptions, structureOptionsService} from "../../services";

function castClasses(classes: any) {
    return _.map(classes, (classe) => {
        classe.type_groupe_libelle = Classe.get_type_groupe_libelle(classe);
        classe = new Classe(classe);
        return classe;
    });
}

export class Structure extends Model {
    id: string;
    libelle: string;
    eleves: Collection<Eleve>;
    enseignants: Collection<Enseignant>;
    devoirs: Devoirs;
    synchronized: any;
    classes: Collection<Classe>;
    allClasses: Collection<Classe>;
    classesBilanPeriodique: any[];
    matieres: Collection<Matiere>;
    types: Collection<Type>;
    enseignements: Collection<Enseignement>;
    annotations: Collection<Annotation>;
    annotationsfull: [Annotation];
    releveNotes: Collection<ReleveNote>;
    isSynchronized: boolean;
    cycles: Array<Cycle>;
    cycle: Cycle;
    typePeriodes: Collection<TypePeriode>;
    typeSousMatieres: TypeSousMatiere[];
    niveauCompetences: Collection<NiveauCompetence>;
    usePerso: any;
    responsables: Collection<Responsable>;
    moyenneVisible: boolean|number;
    baremeDNBvisible: number;
    detailsUser: any;
    composer: any; // Set By infra
    services: any[];
    options: StructureOptions;

    get api() {
        return {
            getEnseignants: '/competences/user/list?profile=Teacher&structureId=',
            getClassesBilanPeriodique: '/competences/elementsBilanPeriodique/classes?idStructure=' + this.id,
            TYPE: {
                synchronization: '/competences/types?idEtablissement=' + this.id
            },
            ENSEIGNEMENT: {
                synchronization: '/competences/enseignements'
            },
            MATIERE: {
                synchronization: '/viescolaire/matieres/services-filter?idEtablissement=' + this.id,
            },
            CLASSE: {
                synchronization: '/viescolaire/classes?idEtablissement=' + this.id,
                synchronizationAllClasses: '/viescolaire/classes?idEtablissement=' + this.id + "&forAdmin=true",
            },
            ELEVE: {
                synchronization: '/viescolaire/classe/eleves?idEtablissement=' + this.id
            },
            ANNOTATION: {
                synchronization: '/competences/annotations?idEtablissement=' + this.id
            },
            RESPONSABLE : {
                synchronisation : '/competences/responsablesDirection?idStructure=' + this.id
            },
            NIVEAU_COMPETENCES: {
                synchronisation: '/competences/maitrise/level/' + this.id,
                use: '/competences/maitrise/perso/use/' + model.me.userId
            },
            TYPEPERIODES : {
                synchronisation: '/viescolaire/periodes/types'
            },
            GET_TEACHER_DETAILS : {
                synchronisation : `/directory/user/${model.me.userId}?manual-groups=true`
            },
            GET_TYPE_SOUS_MATIERES: `/viescolaire/types/sousmatieres?idStructure=` + this.id,
            GET_SERVICES: `/viescolaire/services?idEtablissement=${this.id}`
        };
    }

    constructor(o?: any) {
        super();
        if (o) this.updateData(o, false);
        this.isSynchronized = false;
        this.synchronized = {
            devoirs: false,
            classes: false,
            classesBilanPeriodique: false,
            matieres: false,
            typePeriodes: false,
            types: false,
            annotations: false,
            enseignements: false,
            niveauCompetences: false
        };
        if (Utils.isChefEtabOrHeadTeacher()) {
            this.synchronized.enseignants = false;
        }
        let that: Structure = this;
        http().get(`/competences/bfc/visibility/structures/${that.id}/1`)
            .done(function (res) {
                that.moyenneVisible = res[0].visible;
            }.bind(this));
        http().get(`/competences/bfc/visibility/structures/${that.id}/2`)
            .done(function (res) {
                that.baremeDNBvisible = res[0].visible;
            }.bind(this));
        structureOptionsService.getStructureOptionsIsAverageSkills(that.id).then(res =>{
            that.options = res;
        }).catch((err) => {
            toasts.warning('competences.structure.options.error.get');
            console.log(err);
        });

        this.collection(NiveauCompetence, {
            sync: async function () {
                // Récupération (sous forme d'arbre) des niveaux de compétences de l'établissement en cours
                return new Promise((resolve, reject) => {
                    that.usePerso = 'true';
                    http().getJson(that.api.NIVEAU_COMPETENCES.synchronisation).done(function (niveauCompetences) {
                        if (_.filter(niveauCompetences, {couleur: null}).length > 0 ||
                            _.filter(niveauCompetences, {libelle: null}).length > 0) {
                            niveauCompetences.forEach((niveauCompetence) => {
                                if (niveauCompetence.couleur === null) {
                                    niveauCompetence.couleur = Defaultcolors[niveauCompetence.default];
                                }
                                if (niveauCompetence.lettre === null) {
                                    niveauCompetence.lettre = " ";
                                }
                                if(niveauCompetence.libelle === null) {
                                    niveauCompetence.libelle = niveauCompetence.default_lib;
                                }
                                niveauCompetence.id_etablissement = this.composer.id;
                            });
                        }

                        that.niveauCompetences.load(niveauCompetences);
                        let cycles = [];
                        let tree = _.groupBy(niveauCompetences, "id_cycle");

                        _.map(tree, function (node) {
                            let cycleNode = {
                                id_cycle: node[0].id_cycle,
                                libelle: node[0].cycle,
                                selected: false,
                                niveauCompetencesArray: _.sortBy(node, function (niv) {
                                    return niv.ordre;
                                })
                            };
                            cycleNode.niveauCompetencesArray = cycleNode.niveauCompetencesArray.reverse();
                            cycles.push(cycleNode);
                        });
                        that.cycles = cycles;
                        if (that.cycles.length > 0) {
                            that.cycle = cycles[0];
                        }
                        that.synchronized.niveauCompetences = true;

                        if (resolve && typeof resolve === 'function') {
                            resolve();
                        }
                    }.bind(this)).error(function () {
                        if (reject && typeof reject === 'function') {
                            reject();
                        }
                    });
                });
            }
        });
        this.collection(Enseignant);
        this.collection(Responsable, {// responsable de Direction
            sync :  function(){
                return new Promise ((resolve) => {
                    http().getJson(that.api.RESPONSABLE.synchronisation).done(function (res) {
                        that.responsables.load(res);
                        resolve();
                    });
                });
            }
        });
        this.collection(Eleve, {
            sync: function () {
                return new Promise((resolve) => {
                    // chargement des élèves Pour les enseignants ou personnel de l'établissement
                    let url = that.api.ELEVE.synchronization;
                    // filtre par classe pour les enseignants
                    if ((model.me.type === 'ENSEIGNANT')) {
                        that.classes.forEach((classe) => {
                            url += '&idClasse=' + classe.id;
                        });
                    }
                    if (model.me.type === 'PERSEDUCNAT' || model.me.type === 'ENSEIGNANT') {
                        http().getJson(url).done((res) => {
                            // On tri les élèves par leur lastName en ignorant les accents
                            utils.sortByLastnameWithAccentIgnored(res);
                            that.eleves.load(res);
                            that.synchronized.eleves = true;
                            resolve();
                        });
                    }
                });
            }
        });
        this.collection(Type, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.TYPE.synchronization).done(function (res) {
                        this.load(res);
                        that.synchronized.types = true;
                        resolve();
                    }.bind(this));
                });
            }
        });
        this.collection(Devoir, new DevoirsCollection(that.id));
        this.collection(Enseignement, {
            sync: function (idClasse: string) {
                return new Promise((resolve, reject) => {
                    let uri = that.api.ENSEIGNEMENT.synchronization;
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
                                            return sousCompetence.composer = competence;
                                        });
                                    }
                                    delete competence['competences_2'];
                                });
                                delete enseignement['competences_1'];
                            });
                            that.synchronized.enseignements = true;
                            resolve();
                        }.bind(this));
                    } else {
                        console.error('idClasse must be defined');
                    }
                });
            }
        });
        this.collection(Matiere, {
            sync: function () {
                return new Promise((resolve) => {
                    let uri = that.api.MATIERE.synchronization;
                    http().getJson(uri).done(function (res) {
                        this.load(res);
                        this.each(function (matiere) {
                            if (matiere.hasOwnProperty('sous_matieres')) {
                                matiere.sousMatieres.load(matiere.sous_matieres);
                                delete matiere.sous_matieres;
                            }
                        });
                        that.synchronized.matieres = true;
                        resolve();
                    }.bind(this));
                });
            }
        })
        this.collection(Annotation, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.ANNOTATION.synchronization).done(function (res) {
                        this.load(res);
                        evaluations.annotations = this;
                        evaluations.annotationsfull = _.clone(evaluations.annotations.all);
                        evaluations.annotationsfull.push(new Annotation({
                            id: 0, libelle: lang.translate('no.annotation'), id_etablissement: this.id,
                            libelle_court: ""
                        }));
                        that.synchronized.annotations = true;
                        this.trigger('sync');
                        resolve();
                    }.bind(this));
                });
            }
        });
        this.collection(ReleveNote);

        this.collection(Classe, {
            sync: () => {
                return new Promise(async (resolve) => {
                    let allPromise = await Promise.all([httpAxios.get(this.api.CLASSE.synchronization),
                        httpAxios.get(this.api.GET_SERVICES)]);

                    let classes = allPromise[0].data;
                    let services = allPromise[1].data;
                    this.services = services;
                    _.map(classes, (classe) => {
                        let servicesClasse = _.filter(services, service =>{
                            return service.id_groups ? service.id_groups.includes(classe.id)
                                : service.id_groupe === classe.id;
                        });
                        classe.services = !_.isEmpty(servicesClasse) ? servicesClasse : null;

                    });

                    this.classes.addRange(castClasses(classes));

                    this.eleves.sync().then(() => {
                        model.trigger('apply');
                        if (Utils.isChefEtabOrHeadTeacher())
                            resolve();
                    });
                    this.synchronized.classes = true;
                });
            }
        });
        this.collection(TypePeriode, {
            sync: async () : Promise<any> => {
                return await http().getJson(this.api.TYPEPERIODES.synchronisation).done((res) => {
                    res.push({id: null, type: 0 });
                    this.typePeriodes.load(res);
                    this.synchronized.typePeriodes = true;
                });
            }
        });

        this.devoirs.on('sync', function () {
            that.synchronized.devoirs = true;
        });
    }

    sync() {
        return new Promise((resolve, reject) => {
            let isSynced = () => {
                let b =
                    this.synchronized.matieres &&
                    this.synchronized.types &&
                    this.synchronized.typeSousMatieres &&
                    this.synchronized.classes &&
                    this.synchronized.annotations &&
                    this.synchronized.niveauCompetences &&
                    this.synchronized.typePeriodes &&
                    this.synchronized.devoirs &&
                    this.synchronized.detailsUser;
                if (Utils.isChefEtabOrHeadTeacher()) {
                    b = b && this.synchronized.enseignants;
                }

                if (Utils.canCreateElementBilanPeriodique() || Utils.canSaisieProjet()) {
                    b = b && this.synchronized.classesBilanPeriodique;
                }

                if (b) {
                    this.isSynchronized = true;
                    resolve();
                }
            };
            this.matieres.sync().then(isSynced);
            this.annotations.sync().then(isSynced);
            this.types.sync().then(isSynced);
            this.classes.sync().then(isSynced);
            /*this.usePersoFun(model.me.userId).then((res) => {
                let useDefautTheme = !res;
                this.usePerso = res;
                this.niveauCompetences.sync(useDefautTheme).then(isSynced);
            });*/
            this.niveauCompetences.sync().then(isSynced);

            this.syncDevoirs(25).then(isSynced);

            this.getDetailsOfUser().then(isSynced);
            this.syncEnseignants().then(isSynced);

            this.typePeriodes.sync().then(isSynced);

            if (Utils.canCreateElementBilanPeriodique() || Utils.canSaisieProjet()) {
                this.syncClassesBilanPeriodique().then(isSynced);
            }

            this.syncTypeSousMatieres().then(isSynced);
        });
    }

    syncDevoirs(limit?:number): Promise<any> {
        return new Promise((resolve, reject) => {
            this.devoirs.sync().then((data) => {
                this.synchronized.devoirs = true;
                this.devoirs.trigger('devoirs-sync');
                resolve();
            });
        });
    }

    syncEnseignants(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().getJson(this.api.getEnseignants + this.id).done(function (res) {
                this.enseignants.load(res);
                this.synchronized.enseignants = true;
                if (resolve && (typeof(resolve) === 'function')) {
                    resolve();
                }
            }.bind(this));
        });
    }

    syncClassesBilanPeriodique(): Promise<any> {
        return new Promise((resolve, reject) => {
            var that = this;
            http().getJson(this.api.getClassesBilanPeriodique).done((res) => {
                _.map(res, (classe) => {
                    classe.type_groupe_libelle = Classe.get_type_groupe_libelle(classe);
                    return classe;
                });
                this.classesBilanPeriodique =  Mix.castArrayAs(Classe, res);
                this.synchronized.classesBilanPeriodique = true;
                resolve();
            }) .error(() => {
                reject();
            });
        });
    }

    usePersoFun(idUser): Promise<boolean> {
        return new Promise((resolve, reject) => {
            http().getJson(this.api.NIVEAU_COMPETENCES.use).done((res) => {
                if (!res) {
                    reject();
                }
                if (res.length > 0) {
                    resolve(true);
                }
                else {
                    resolve(false);
                }
            });
        });
    }

    getDetailsOfUser(): Promise<any> {
        return new Promise ( ((resolve, reject) => {
            http().getJson(this.api.GET_TEACHER_DETAILS.synchronisation)
                .done((res)=> {
                    this.detailsUser = res;
                    this.synchronized.detailsUser = true;
                    resolve();
                })
                .error(() => {
                    reject();
                });
        }));
    }

    async syncTypeSousMatieres() {
        let {data} = await httpAxios.get(this.api.GET_TYPE_SOUS_MATIERES);
        if (!data.error) {
            this.typeSousMatieres = data;
        }
        this.synchronized.typeSousMatieres = true;
    }

    async syncAllClasses() {

        return new Promise ((resolve, reject) => {
            http().getJson(this.api.CLASSE.synchronizationAllClasses)
                .done((classesRep)=> {
                    _.map(classesRep, (classe) => {
                        let servicesClasse = _.filter(this.services, service =>{
                            return service.id_groups ? service.id_groups.includes(classe.id)
                                : service.id_groupe === classe.id;
                        });
                        classe.services = !_.isEmpty(servicesClasse) ? servicesClasse : null;
                    });
                    this.allClasses = castClasses(classesRep);
                    resolve();
                })
                .error(() => {
                    reject();
                });
        });
    }
}