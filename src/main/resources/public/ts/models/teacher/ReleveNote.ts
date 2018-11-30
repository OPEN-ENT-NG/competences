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

import { Model, IModel, _, moment, Collection, http, idiom as lang } from 'entcore';
import {
    AppreciationClasse,
    Periode,
    Matiere,
    Evaluation,
    Classe,
    Devoir,
    Structure,
    evaluations, TableConversion, Domaine, Utils
} from './index';
import {getNN} from "../../utils/functions/utilsNN";
import * as utils from "../../utils/teacher";
import {Defaultcolors} from "../eval_niveau_comp";
import {Graph} from "../common/Graph";
declare  let Chart: any;
export class ReleveNote extends  Model implements IModel {
    synchronized: any;
    elementProgramme: any;
    periode: Periode;
    matiere: Matiere;
    classe: Classe;
    devoirs: Collection<Devoir>;
    tableConversions: Collection<TableConversion>;
    structure: Structure;
    ennseignantsNames;
    idClasse: string;
    idMatiere: string;
    idPeriode: number;
    idEtablissement: string;
    appreciationClasse: AppreciationClasse;
    hasEvaluatedDevoirs: boolean;
    toogle: boolean = false;
    _tmp: any;
    isNN: boolean = false;
    openedLightboxEleve: boolean = false;

    get api() {
        return {
            get: `/competences/releve?idEtablissement=${this.structure.id}&idClasse=${this.idClasse}&idMatiere=${
                this.idMatiere}&typeClasse=${this.classe.type_groupe}`,
            GET_MOYENNE_ANNEE: `/competences/releve/annee/classe?idEtablissement=${this.structure.id}&idClasse=${
                this.idClasse}&idMatiere=${this.idMatiere}&typeClasse=${this.classe.type_groupe}`,
            GET_INFO_PERIODIQUE: `/competences/releve/periodique?idEtablissement=${this.structure.id}&idClasse=${
                this.idClasse}&idMatiere=${this.idMatiere}&idPeriode=${this.idPeriode}&typeClasse=${
                this.classe.type_groupe}`,
            GET_ELEMENT_PROGRAMME_DOMAINES: `/competences/element/programme/domaines`,
            GET_ELEMENT_PROGRAMME_SOUS_DOMAINES: `/competences/element/programme/sous/domaines`,
            GET_ELEMENT_PROGRAMME_PROPOSITIONS: `/competences/element/programme/propositions`,
            GET_CONVERSION_TABLE: `/competences/competence/notes/bilan/conversion?idEtab=${
                this.idEtablissement}&idClasse=${this.idClasse}`,
            GET_ARBRE_DOMAINE: `/competences/domaines?idClasse=${this.idClasse}`,

            POST_DATA_RELEVE_PERIODIQUE: `/competences/releve/periodique`,
            POST_DATA_ELEMENT_PROGRAMME: `/competences/releve/element/programme`,
            GET_DATA_FOR_GRAPH: `/competences/releve/datas/graph?idEtablissement=${this.structure.id}&idClasse=${
                this.idClasse}&idMatiere=${this.idMatiere}&typeClasse=${this.classe.type_groupe}`,
            GET_DATA_FOR_GRAPH_DOMAINE: `/competences/releve/datas/graph/domaine?idEtablissement=${
                this.structure.id}&idClasse=${this.idClasse}&idMatiere=${this.idMatiere}&typeClasse=${
                this.classe.type_groupe}`

        }
    }

    constructor(o?: any) {
        super();
        if (o && o !== undefined) this.updateData(o, false);
        this.synchronized = {
            classe: false,
            devoirs: false,
            evaluations: false,
            releve: false,
            appreciationClasse: false
        };
        this.structure = evaluations.structure;
        this.matiere = _.findWhere(evaluations.structure.matieres.all, {id: this.idMatiere});
        let c = _.findWhere(evaluations.structure.classes.all, {id: this.idClasse});
        this.classe = new Classe({id: c.id, name: c.name, type_groupe: c.type_groupe, externalId: c.externalId});
        let graphData = {
            idPeriode: this.idPeriode,
            structure: this.structure,
            classe: this.classe,
            idMatiere: this.idMatiere,
            idClasse : this.idClasse
        }
        this.collection(Devoir, {
            sync: () => {
                if (evaluations.structure.synchronized.devoirs) {
                    let _devoirs = evaluations.devoirs.where({
                        id_groupe: this.idClasse,
                        id_matiere: this.idMatiere,
                        id_etablissement: this.idEtablissement
                    });
                    if (this.idPeriode) {
                        _devoirs = _.where(_devoirs, {id_periode: this.idPeriode});
                    }
                    if (_devoirs.length > 0) {
                        this.devoirs.load(_devoirs, null, false);
                        this.ennseignantsNames = "";

                        for (let i = 0; i < this.devoirs.all.length; i++) {
                            let teacher = this.devoirs.all[i].teacher;
                            if (!utils.containsIgnoreCase(this.ennseignantsNames, teacher)) {
                                this.ennseignantsNames += teacher + " "
                            }
                        }
                    }
                }
            }
        });
    }

    toJson() {
        return {
            idMatiere: this.idMatiere,
            idClasse: this.idClasse,
            idEtablissement: this.idEtablissement,
            idPeriode: this.idPeriode
        };
    }

    syncClasse(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            if (this.classe.eleves.length() === 0) {
                await this.classe.eleves.sync();
            }
            this.synchronized.classe = true;
            resolve();
        });
    }

    syncEvaluations(): Promise<any> {
        return new Promise((resolve, reject) => {
            let url = this.api.get;
            let _p = _.findWhere(this.classe.periodes.all, {id_type: this.idPeriode});
            url += (this.idPeriode !== null) ? ('&idPeriode=' + this.idPeriode) : '';
            if (_p !== undefined || this.idPeriode === null) {
                http().getJson(url)
                    .done((res) => {
                        this._tmp = res;
                        this.synchronized.evaluations = true;
                        resolve();
                    });
            }
        });
    }

    syncDevoirs(): Promise<any> {
        return new Promise((resolve, reject) => {
            this.devoirs.sync();
            this.synchronized.devoirs = true;
            resolve();
        });
    }

    syncAppreciationClasse() {
        return new Promise((resolve, reject) => {
            if (this.idPeriode != null) {
                let periode = _.findWhere(this.classe.periodes.all, {id_type: this.idPeriode});

                let endSaisie = moment(periode.date_fin_saisie).isBefore(moment(), "days");

                this.appreciationClasse = new AppreciationClasse(this.idClasse, this.idMatiere, this.idPeriode,
                    endSaisie, this.structure.id);

                this.appreciationClasse.sync();
            }
            resolve();
        });
    }

    syncMoyenneAnnee(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            if (this.idPeriode === null) {
                http().getJson(this.api.GET_MOYENNE_ANNEE)
                    .done((res) => {

                        _.forEach(this.classe.eleves.all, (eleve) => {
                            let moyennes = _.where(res.moyennes, {id_eleve: eleve.id});

                            let moyennesFinales = _.where(res.moyennes_finales, {id_eleve: eleve.id});


                            let nbMoyenneAnnee = 0;
                            let moyennesFinalesNumber = 0;
                            let moyennesFinalesAnnee = [];
                            // Si une moyenne a été modifiée on recalcule la moyenne à l'année
                            _.forEach(moyennesFinales, (moyenneFinale) => {
                                moyennesFinalesAnnee.push(moyenneFinale);
                            });
                            _.forEach(moyennes, (moyenne) => {
                                if (moyenne.id_periode !== null) {
                                    let _moyenne = _.findWhere(moyennesFinalesAnnee, {id_periode: moyenne.id_periode});
                                    if (_moyenne === undefined) {
                                        moyennesFinalesAnnee.push(moyenne);
                                    }
                                }
                            });
                            _.forEach(moyennesFinalesAnnee, (moyenneFinaleAnnee) => {
                                nbMoyenneAnnee++;
                                moyennesFinalesNumber += parseFloat(moyenneFinaleAnnee.moyenne);
                            });
                            if (nbMoyenneAnnee > 0) {
                                let _moyenneFinaleAnnee = {
                                    id_eleve: eleve.id,
                                    id_periode: null,
                                    moyenne: (moyennesFinalesNumber / nbMoyenneAnnee).toFixed(2)
                                }
                                if (moyennesFinales !== undefined && moyennesFinales !== null
                                    && moyennesFinales.length > 0) {
                                    moyennesFinales.push(_moyenneFinaleAnnee);
                                } else {
                                    moyennes = _.without(moyennes, _.findWhere(moyennes, {id_periode: null}));
                                    moyennes.push(_moyenneFinaleAnnee);
                                }
                            }

                            if (moyennes !== undefined && moyennes !== null) {

                                eleve.moyennes = moyennes;
                            }
                            eleve.moyennesFinales = moyennesFinales;


                        });
                        resolve();
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            }
            else {
                resolve();
            }
        });
    }

    syncMoyenneFinale(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (this.idPeriode !== null) {
                http().getJson(this.api.GET_INFO_PERIODIQUE + '&colonne=moyenne')
                    .done((res) => {
                        console.log(res);
                        _.forEach(this.classe.eleves.all, (eleve) => {
                            let _eleve = _.findWhere(res, {id_eleve: eleve.id});
                            if (_eleve !== undefined && _eleve.moyenne !== null) {
                                eleve.moyenneFinale = _eleve.moyenne;
                            }
                        });
                        resolve();
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            }
            else {
                resolve();
            }
        });
    }

    syncPositionnement(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (this.idPeriode !== null) {
                http().getJson(this.api.GET_INFO_PERIODIQUE + '&colonne=positionnement')
                    .done((res) => {
                        console.log(res);
                        _.forEach(this.classe.eleves.all, (eleve) => {
                            let _eleve = _.findWhere(res, {id_eleve: eleve.id});
                            if (_eleve !== undefined && _eleve.positionnement !== null) {
                                eleve.positionnement = _eleve.positionnement;
                            }
                        });
                        resolve();
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            }
            else {
                resolve();
            }
        });
    }

    syncDomainesEnseignement(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (evaluations.domainesEnseignements === undefined || evaluations.domainesEnseignements.length == 0) {
                http().getJson(this.api.GET_ELEMENT_PROGRAMME_DOMAINES)
                    .done((res) => {
                        evaluations.domainesEnseignements = res;
                        resolve();
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            } else {
                resolve();
            }
        });
    }

    syncSousDomainesEnseignement(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (evaluations.sousDomainesEnseignements === undefined || evaluations.sousDomainesEnseignements.length == 0) {
                http().getJson(this.api.GET_ELEMENT_PROGRAMME_SOUS_DOMAINES)
                    .done((res) => {
                        evaluations.sousDomainesEnseignements = res;
                        http().getJson(this.api.GET_ELEMENT_PROGRAMME_PROPOSITIONS)
                            .done((propositions) => {
                                _.forEach(evaluations.sousDomainesEnseignements, (sousDomaine) => {
                                    let _propositions = _.where(propositions, {id_sous_domaine: sousDomaine.id});
                                    if (_propositions !== undefined && _propositions.length > 0) {
                                        sousDomaine.propositions = _propositions;
                                    }
                                });
                                resolve();
                            })
                            .error((propositions) => {
                                console.dir(propositions);
                                reject();
                            })
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            } else {
                resolve();
            }
        });
    }

    sync(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            if (this.classe.periodes.length() === 0) {
                await this.classe.periodes.sync();
            }
            await Promise.all([this.syncEvaluations(), this.syncDevoirs(), this.syncClasse(),
                this.getConversionTable(), this.syncMoyenneAnnee()]);
            this.syncAppreciationClasse();
            this.periode = _.findWhere(this.classe.periodes.all, {id_type: this.idPeriode});
            this.classe = this.classe.filterEvaluableEleve(this.periode);
            let _notes, _devoirs, _eleves, _moyennesFinales, _appreciations, _competencesNotes;
            if (this._tmp) {
                _notes = this._tmp.notes;
                _devoirs = this._tmp.devoirs;
                _eleves = this._tmp.eleves;
                _moyennesFinales = this._tmp.moyennes;
                _appreciations = this._tmp.appreciations;
                _competencesNotes = this._tmp.competencesNotes;
                this.elementProgramme = this._tmp.elementProgramme;
            }
            this.hasEvaluatedDevoirs = _.findWhere(this.devoirs.all, {is_evaluated: true});
            this.hasEvaluatedDevoirs = (this.hasEvaluatedDevoirs === undefined) ? false : true;
            _.each(this.classe.eleves.all, (eleve) => {
                // chargement des  competencesNotes de l'élève
                let competencesNotesEleve = _.where(_competencesNotes, {id_eleve: eleve.id});
                eleve.competencesNotes = (competencesNotesEleve !== undefined) ? competencesNotesEleve : [];

                // chargement de la  moyenne finale de l'élève
                let _eleve = _.findWhere(_moyennesFinales, {id_eleve: eleve.id});
                if (_eleve !== undefined && _eleve.moyenne !== null) {
                    eleve.moyenneFinale = _eleve.moyenne;

                    // if (this.hasEvaluatedDevoirs) {
                    //     eleve.moyenneFinale = _eleve.moyenne;
                    // }
                    // else {
                        // suppression de la moyenne finale lorsqu'il n'y a pas de devoir avec l'évaluation numérique
                        // eleve.moyenneFinale = "";
                        // this.saveAppreciationMatierePeriodeEleve(eleve);
                    // }

                }
                // load appreciation
                let _eleve_appreciation = _.findWhere(_appreciations, {id_eleve: eleve.id});
                if (_eleve_appreciation !== undefined && _eleve_appreciation !== null) {
                    eleve.appreciation_matiere_periode = _eleve_appreciation.appreciation_matiere_periode;
                }
                let _evals = [];
                let _t = _.where(_notes, {id_eleve: eleve.id});
                _.each(this.devoirs.all, async (devoir) => {
                    let periode = _.findWhere(this.classe.periodes.all, {id_type: devoir.id_periode});
                    let endSaisie = moment(periode.date_fin_saisie).isBefore(moment(), "days");
                    let _e;
                    if (_t && _t.length !== 0) {
                        _e = _.findWhere(_t, {id_devoir: devoir.id});

                        if (_e) {
                            _e.oldValeur = _e.valeur;
                            _e.oldAppreciation = _e.appreciation !== undefined ? _e.appreciation : '';
                            if (_e.annotation !== undefined
                                && _e.annotation !== null
                                && _e.annotation > 0) {
                                _e.oldAnnotation = _e.annotation;
                                _e.annotation_libelle_court = evaluations.structure.annotations.findWhere(
                                    {id: _e.annotation}).libelle_court;
                                _e.is_annotation = true;
                            }
                            _e.endSaisie = endSaisie;
                            _evals.push(_e);
                        }
                        else {
                            _evals.push(new Evaluation({
                                valeur: "", oldValeur: "", appreciation: "",
                                oldAppreciation: "", id_devoir: devoir.id, id_eleve: eleve.id,
                                ramener_sur: devoir.ramener_sur, coefficient: devoir.coefficient,
                                is_evaluated: devoir.is_evaluated, endSaisie: endSaisie
                            }));
                        }
                    } else {
                        _evals.push(new Evaluation({
                            valeur: "", oldValeur: "", appreciation: "",
                            oldAppreciation: "", id_devoir: devoir.id, id_eleve: eleve.id,
                            ramener_sur: devoir.ramener_sur, coefficient: devoir.coefficient,
                            is_evaluated: devoir.is_evaluated, endSaisie: endSaisie
                        }));
                    }
                });
                eleve.evaluations.load(_evals, null, false);

            });
            _.each(_devoirs, (devoir) => {
                let d = _.findWhere(this.devoirs.all, {id: devoir.id});
                if (d) {
                    d.statistiques = devoir;
                    if (!d.percent) {
                        evaluations.devoirs.getPercentDone(d).then(() => {
                            d.statistiques.percentDone = d.percent;
                        });
                    } else {
                        d.statistiques.percentDone = d.percent;
                    }
                }
            });

            if (this.hasEvaluatedDevoirs) {
                _.each(_eleves, (eleve) => {
                    let e = _.findWhere(this.classe.eleves.all, {id: eleve.id});
                    if (e) {
                        e.moyenne = eleve.moyenne;
                    }
                });
            }
            else {
                this.isNN = true;
                _.each(this.classe.eleves.all, (eleve) => {
                    eleve.moyenne = getNN();
                })
            }
            resolve();
        });
    }

    saveMoyenneFinaleEleve(eleve): any {
        return new Promise((resolve, reject) => {
            let _data = _.extend(this.toJson(), {
                idEleve: eleve.id,
                colonne: 'moyenne',
                moyenne: parseFloat(eleve.moyenneFinale),
                delete: eleve.moyenneFinale === "" || eleve.moyenneFinale === "NN"
            });

            http().postJson(this.api.POST_DATA_RELEVE_PERIODIQUE, _data)
                .done((res) => {
                    resolve(res);
                })
                .error((err) => {
                    reject(err);
                });
        });
    }

    saveElementProgramme(texte): Promise<any> {
        return new Promise((resolve, reject) => {
            var that = this;
            let _data = _.extend(this.toJson(), {
                texte: texte
            });

            http().postJson(this.api.POST_DATA_ELEMENT_PROGRAMME, _data)
                .done((res) => {
                    if (resolve && typeof(resolve) === 'function') {
                        resolve();
                    }
                })
                .error((err) => {

                });
        });
    }


    savePositionnementEleve(eleve): any {
        return new Promise((resolve, reject) => {
            let _data = _.extend(this.toJson(), {
                idEleve: eleve.id,
                colonne: 'positionnement',
                positionnement: parseInt(eleve.positionnement),
                delete: eleve.positionnement === ""
            });
            if (_data.idPeriode !== null) {
                http().postJson(this.api.POST_DATA_RELEVE_PERIODIQUE, _data)
                    .done((res) => {
                        resolve(res);
                    })
                    .error((err) => {
                        reject(err);
                    });
            }
        });
    }

    saveAppreciationMatierePeriodeEleve(eleve): any {
        return new Promise((resolve, reject) => {
            let _data = _.extend(this.toJson(), {
                idEleve: eleve.id,
                appreciation_matiere_periode: eleve.appreciation_matiere_periode,
                colonne: 'appreciation_matiere_periode',
                delete: eleve.appreciation_matiere_periode === ""
            });

            http().postJson(this.api.POST_DATA_RELEVE_PERIODIQUE, _data)
                .done((res) => {
                    resolve(res);
                })
                .error((err) => {
                    reject(err);
                });
        });
    }

    getConversionTable(): Promise<any> {
        this.collection(TableConversion, {
            sync: async (): Promise<any> => {
                return new Promise((resolve) => {
                    http().getJson(this.api.GET_CONVERSION_TABLE).done((data) => {

                        this.tableConversions.load(data);

                        if (resolve && (typeof (resolve) === 'function')) {
                            resolve(data);
                        }
                    });
                });
            }
        });
        return this.tableConversions.sync();
    }

    getArbreDomaine(eleve): any {
        return new Promise((resolve, reject) => {
            let uri = this.api.GET_ARBRE_DOMAINE + '&idEleve=' + eleve.id;
            eleve.domaines = {
                all: []
            };
            http().getJson(uri)
                .done((res) => {
                    if (res) {
                        for (let i = 0; i < res.length; i++) {
                            let domaine = new Domaine(res[i], eleve.id);
                            eleve.domaines.all.push(domaine);
                            eleve.tabDomaine = [];
                            Utils.setCompetenceNotes(domaine, eleve.competencesNotes,
                                undefined, undefined, eleve.tabDomaine);
                        }
                    }
                    resolve();
                })
                .error((err) => {
                    reject(err);
                });
        });
    }

    getDataForGraph(eleve, forDomaine?): any {
      return Graph.getDataForGraph(this, eleve, forDomaine);
    }

}