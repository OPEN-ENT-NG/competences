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

import {Model, IModel, _, moment, Collection, http, idiom as lang, notify, model} from 'entcore';
import httpAxios from 'axios';
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
import {Graph} from "../common/Graph";
import {SousMatiere} from "./SousMatiere";
import {getTitulairesForRemplacantsCoEnseignant} from "../../utils/teacher";


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
    exportOptions : any;

    get api() {
        return {
            get: `/competences/releve?idEtablissement=${this.structure.id}&idClasse=${this.idClasse}&idMatiere=${
                this.idMatiere}&typeClasse=${this.classe.type_groupe}`,
            GET_MOYENNE_ANNEE: `/competences/releve/annee/classe?idEtablissement=${this.structure.id}&idClasse=${
                this.idClasse}&idMatiere=${this.idMatiere}&typeClasse=${this.classe.type_groupe}`,
            GET_ELEMENT_PROGRAMME_DOMAINES: `/competences/element/programme/domaines?idCycle=`,
            GET_ELEMENT_PROGRAMME_SOUS_DOMAINES: `/competences/element/programme/sous/domaines?idDomaine=`,
            GET_ELEMENT_PROGRAMME_PROPOSITIONS: `/competences/element/programme/propositions?idEtablissement=${
                this.structure.id}&idSousDomaine=`,
            GET_CONVERSION_TABLE: `/competences/competence/notes/bilan/conversion?idEtab=${
                this.idEtablissement}&idClasse=${this.idClasse}`,
            GET_ARBRE_DOMAINE: `/competences/domaines?idClasse=${this.idClasse}`,
            POST_DATA_RELEVE_PERIODIQUE: `/competences/releve/periodique`,
            POST_DATA_ELEMENT_PROGRAMME: `/competences/releve/element/programme`,
            GET_DATA_FOR_GRAPH: `/competences/releve/datas/graph?idEtablissement=${this.structure.id}&idClasse=${
                this.idClasse}&idMatiere=${this.idMatiere}&typeClasse=${this.classe.type_groupe}`,
            GET_DATA_FOR_GRAPH_DOMAINE: `/competences/releve/datas/graph/domaine?idEtablissement=${
                this.structure.id}&idClasse=${this.idClasse}&idMatiere=${this.idMatiere}&typeClasse=${
                this.classe.type_groupe}`,
            EXPORT: `/competences/releve/export`
        }
    }


    constructor(o?: any) {
        super();
        if (o) this.updateData(o, false);
        this.synchronized = {
            classe: false,
            devoirs: false,
            evaluations: false,
            releve: false,
            appreciationClasse: false,
            moyenneClasse: false
        };
        this.structure = evaluations.structure;
        this.matiere = _.findWhere(evaluations.structure.matieres.all, {id: this.idMatiere});
        let c = _.findWhere(evaluations.structure.classes.all, {id: this.idClasse});
        this.classe = new Classe({id: c.id, name: c.name, type_groupe: c.type_groupe, externalId: c.externalId});
        this.exportOptions = {
            show : false,
            fileType: 'pdf',
            sousMatieres: {moyennes : {}, positionnements_auto:{}},
            appreciation: true,
            averageFinal: true,
            averageAuto: true,
            positionnementFinal: true,
            appreciationClasse: true,
            moyenneClasse: true
        };
        _.forEach(this.matiere.sousMatieres.all, (sousMatiere) => {
            this.exportOptions.sousMatieres.moyennes[sousMatiere.id_type_sousmatiere] = true;
            this.exportOptions.sousMatieres.positionnements_auto[sousMatiere.id_type_sousmatiere] = true;
        });
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
                http().getJson(url).done((res) => {
                    this._tmp = res;
                    utils.sortByLastnameWithAccentIgnored(this._tmp.eleves);
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

    syncAppreciationClasse(appreciation) {
        if (this.idPeriode != null) {
            let periode = _.findWhere(this.classe.periodes.all, {id_type: this.idPeriode});

            let endSaisie = moment(periode.date_fin_saisie).isBefore(moment(), "days");

            this.appreciationClasse = new AppreciationClasse(this.idClasse, this.idMatiere, this.idPeriode,
                endSaisie, this.structure.id, appreciation);

        }
    }

    syncMoyenneAnnee(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            http().getJson(this.api.GET_MOYENNE_ANNEE).done((res) => {
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

                    let moyenneOfPeriode = _.findWhere(eleve.moyennes, {id_periode: this.idPeriode});
                    if(moyenneOfPeriode){
                        eleve.moyenne = moyenneOfPeriode.moyenne;
                        if(!eleve.moyenneFinaleIsSet){
                            eleve.moyenneFinale = eleve.moyenne;
                            eleve.oldMoyenneFinale = eleve.moyenne;
                        }
                    }

                });
                resolve();
            }).error((res) => {
                console.error(res);
                reject();
            });
        });
    }

    syncDomainesEnseignement(cycle): Promise<any> {
        return new Promise(async (resolve, reject) => {
            http().getJson(this.api.GET_ELEMENT_PROGRAMME_DOMAINES + cycle.id_cycle)
                .done((res) => {
                    evaluations.domainesEnseignements = res;
                    evaluations.sousDomainesEnseignements = [];
                    resolve();
                })
                .error((res) => {
                    console.error(res);
                    reject();
                })
        });
    }

    syncSousDomainesEnseignement(domaine): Promise<any> {
        evaluations.sousDomainesEnseignements = [];
        return new Promise(async (resolve, reject) => {
            http().getJson(this.api.GET_ELEMENT_PROGRAMME_SOUS_DOMAINES + domaine.id)
                .done((res) => {
                    evaluations.sousDomainesEnseignements = res;
                    _.forEach(evaluations.sousDomainesEnseignements, async (sousDomaine) => {
                        sousDomaine.propositions = await this.syncPropositions(sousDomaine);
                    });
                    resolve();
                })
                .error((res) => {
                    console.error(res);
                    reject();
                })
        });
    }

    syncPropositions(sousDomaine) : Promise<any> {
        return new Promise(async (resolve, reject) => {
            http().getJson(this.api.GET_ELEMENT_PROGRAMME_PROPOSITIONS + sousDomaine.id
                + "&idEtablissement=" + this.structure.id)
                .done(async (res) => {
                    resolve(res);
                }).error(async () => {
                reject();
            })
        });
    }

    sync(): Promise<any> {
        return new Promise(async (resolve, reject) => {
            if (this.classe.periodes.length() === 0) {
                await this.classe.periodes.sync();
            }
            try {
                await Promise.all([this.syncEvaluations(), this.syncDevoirs()]);
                this.periode = _.findWhere(this.classe.periodes.all, {id_type: this.idPeriode});
                let _notes, _devoirs, _eleves;
                if (this._tmp) {
                    _notes = this._tmp.notes;
                    _devoirs = this._tmp.devoirs;
                    _eleves = this._tmp.eleves;
                    this.classe.eleves.load(_eleves);
                    if (this.idPeriode !== null) {
                        this.elementProgramme = this._tmp.elementProgramme;
                        this.syncAppreciationClasse(this._tmp.appreciation_classe.appreciation);
                    }
                    await this.getConversionTable(this._tmp.tableConversions);
                }
                this.hasEvaluatedDevoirs = _.findWhere(this.devoirs.all, {is_evaluated: true});
                this.hasEvaluatedDevoirs = (this.hasEvaluatedDevoirs !== undefined);
                _.each(this.classe.eleves.all, (eleve) => {
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

                        if(d.eleves.length() === 0)
                            d.eleves.all = this.classe.eleves.all;
                    }
                });

                let sumCoeff = this.devoirs.all.reduce((s, c) => {
                    if(c.is_evaluated)
                        s += parseFloat(String(c.coefficient));
                    return s;
                }, 0);

                if (this.hasEvaluatedDevoirs && sumCoeff > 0) {
                    _.each(this.classe.eleves.all, (eleve) => {
                        let e = _.findWhere(_eleves, {id: eleve.id});
                        if (e !== undefined && e.moyenne != null) {
                            eleve.moyenne = e.moyenne;
                        }
                        else {
                            eleve.moyenne = getNN();
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
            }
            catch (e) {
                console.error(e);
                notify.error('evaluations.releve.sync.error');
                reject();
            }
        });
    }

    saveMoyenneFinaleEleve(eleve): any {
        return new Promise((resolve, reject) => {
            let _data = _.extend(this.toJson(), {
                idEleve: eleve.id,
                colonne: 'moyenne',
                moyenne: parseFloat(eleve.moyenneFinale),
                delete: eleve.moyenneFinale === "" || eleve.moyenneFinale.toUpperCase() === "NN"
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

    // todo if no bugs clean it
    // saveAppreciationMatierePeriodeEleve(eleve): any {
    //     return new Promise((resolve, reject) => {
    //         const isDeleted:Boolean = eleve.delete
    //             ? eleve.delete && eleve.appreciation_matiere_periode.length === 0
    //             : false;
    //         let _data = _.extend(this.toJson(), {
    //             idEleve: eleve.id,
    //             appreciation_matiere_periode: eleve.appreciation_matiere_periode,
    //             colonne: 'appreciation_matiere_periode',
    //             delete: isDeleted,
    //         });
    //
    //         http().postJson("/competences/appreciation-subject-period", _data)
    //             .done((res) => {
    //                 resolve(res);
    //             })
    //             .error((err) => {
    //                 reject(err);
    //             });
    //     });
    // }

    getConversionTable(data?): Promise<any> {
        this.collection(TableConversion, {
            sync: async (): Promise<any> => {
                return new Promise((resolve) => {
                    let exec = (d) => {
                        this.tableConversions.load(d);

                        if (resolve && (typeof (resolve) === 'function')) {
                            resolve(d);
                        }
                    };
                    if(data === undefined) {
                        http().getJson(this.api.GET_CONVERSION_TABLE).done((data) => {
                            exec(data);
                        });
                    }
                    else {
                        exec(data);
                    }
                });
            }
        });
        return this.tableConversions.sync();
    }

    getArbreDomaine(eleve) : any {
        return new Promise((resolve, reject) => {
            let uri = this.api.GET_ARBRE_DOMAINE + '&idEleve=' + eleve.id;
            eleve.domaines = {
                all: []
            };
            http().getJson(uri)
                .done((res) => {
                    if (res) {
                        let listTeacher = getTitulairesForRemplacantsCoEnseignant(model.me.userId, eleve.classe)
                        for (let i = 0; i < res.length; i++) {
                            let domaine = new Domaine(res[i], eleve.id);
                            eleve.domaines.all.push(domaine);
                            eleve.tabDomaine = [];
                            Utils.setCompetenceNotes(domaine, eleve.competencesNotes, this.tableConversions,
                                undefined, undefined, eleve.tabDomaine,listTeacher);
                        }
                    }
                    resolve();
                })
                .error((err) => {
                    reject(err);
                });
        });
    }

    getDataForGraph(eleve, forDomaine?,niveauCompetences?): any {
        return Graph.getDataForGraph(this, eleve, forDomaine, niveauCompetences);
    }

    addColumnForExportCsv (line, key): any {
        if(key === 'moyenneFinale' && line[key] === null)
            line[key] = 'NN';
        if(line[key] === undefined) {
            line[key] = ' ';
        }
        else {
            if (key === 'appreciation_matiere_periode') {
                while (!_.isEmpty(line.appreciation_matiere_periode.match('\n'))) {
                    line.appreciation_matiere_periode =
                        line.appreciation_matiere_periode.replace('\n',' ');
                }
            }
        }
        return line[key]
    }

    async export  () {
        return new Promise(async (resolve, reject) => {
            let parameter = this.toJson();
            let moyspan = _.filter(_.values(this.exportOptions.sousMatieres.positionnements_auto),
                (val) => { return val === true;}).length;
            if(this.exportOptions.positionnementFinal){
                ++moyspan;
            }
            if(this.exportOptions.appreciation){
                ++ moyspan;
            }
            _.filter(_.values(this.exportOptions),(val) => { return val === true;}).length;
            let colspan = _.filter(_.values(this.exportOptions),(val) => { return val === true;}).length;
            colspan += _.filter(_.values(this.exportOptions.sousMatieres.moyennes),
                (val) => { return val === true;}).length;
            colspan += _.filter(_.values(this.exportOptions.sousMatieres.positionnements_auto),
                (val) => { return val === true;}).length;
            _.extend(parameter, this.exportOptions);
            _.extend(parameter, {typeClasse: this.classe.type_groupe, classeName: this.classe.name,
                matiere: this.matiere.name,
                colspan: colspan -1 , moyspan: moyspan});
            try {
                let data = await httpAxios.post(this.api.EXPORT, parameter,
                    {responseType: this.exportOptions.fileType === 'pdf'? 'arraybuffer' : 'json'});
                console.dir(data);
                let blob;
                let link = document.createElement('a');
                let response = data.data;
                if(this.exportOptions.fileType === 'csv') {
                    let columnCsv = [];
                    let format = this.formateHeaderAndColumn();
                    _.forEach(response.eleves , (line) => {
                        this.addColumnForExportCsv(line,'displayName');
                        if(this.exportOptions.averageAuto) {
                            this.addColumnForExportCsv(line, 'moyenne');
                        }
                        if(this.exportOptions.averageFinal) {
                            this.addColumnForExportCsv(line, 'moyenneFinale');
                        }
                        _.forEach(line.sousMatieres.moyennes, (sousMatiere) => {
                            if(sousMatiere.print){
                                let idSousMatiere = sousMatiere.id_sousmatiere;
                                let key = 'moyenne';
                                line[key + idSousMatiere] = this.addColumnForExportCsv(sousMatiere, key);
                            }
                        });
                        if(this.exportOptions.positionnementFinal) {
                            this.addColumnForExportCsv(line, 'positionnement');
                        }
                        _.forEach(line.sousMatieres.positionnements_auto, (sousMatiere) => {
                            if(sousMatiere.printPosi){
                                let idSousMatiere = sousMatiere.id_sousmatiere;
                                let key = 'positionnement';
                                line[key + idSousMatiere] = this.addColumnForExportCsv(sousMatiere, key);
                            }
                        });
                        if(this.exportOptions.appreciation) {
                            this.addColumnForExportCsv(line, 'appreciation_matiere_periode');
                        }

                        columnCsv.push(_.pick(line, format.column));
                    });

                    let csvData = Utils.ConvertToCSV(columnCsv, format.header);
                    if(this.exportOptions.appreciationClasse ){
                        csvData += (`${lang.translate('evaluations.releve.appreciation.classe')};${
                            response.appreciation_classe.appreciation}\r\n`);
                    }
                    let classe = response._moyenne_classe;
                    if(this.exportOptions.moyenneClasse){
                        if(classe) {
                            let moyAuto = classe['null'];
                            let moyFinal = classe['nullFinal'];
                            if (Utils.isNotNull(moyAuto)) {
                                moyAuto = moyAuto.moyenne;
                            }
                            if (Utils.isNotNull(moyFinal)) {
                                moyFinal = moyFinal.moyenne;
                            }
                            moyAuto = Utils.isNull(moyAuto) ? '' : moyAuto;
                            moyFinal = Utils.isNull(moyFinal) ? '' : moyFinal;

                            csvData += (`${lang.translate('average.class')}`);
                            if (this.exportOptions.averageAuto) {
                                csvData += (`;${moyAuto}`);
                            }
                            if (this.exportOptions.averageFinal) {
                                csvData += (`;${moyFinal}`);
                            }
                        }else{
                            csvData += (`${lang.translate('average.class')}`);
                            if (this.exportOptions.averageAuto) {
                                csvData += (`; `);
                            }
                            if (this.exportOptions.averageFinal) {
                                csvData += (`; `);
                            }
                        }
                        let classeSousMat = response.moyenneClasseSousMat;
                        if (Utils.isNotNull(classeSousMat)) {
                            _.forEach(classeSousMat, sousMat => {
                                if (sousMat.print) {
                                    csvData += (`;${sousMat._moyenne}`);
                                }
                            })
                        }
                        csvData += '\r\n';
                    }

                    csvData = "\ufeff"+csvData;
                    blob = new Blob([csvData], { type: ' type: "text/csv;charset=UTF-8"' });
                    link = document.createElement('a');
                    link.href = window.URL.createObjectURL(blob);
                    link.download =  `releve_periodique_${this.classe.name}_${this.matiere.name}_${this.idPeriode}.csv`;
                }
                else {
                    blob = new Blob([response]);
                    link = document.createElement('a');
                    link.href = window.URL.createObjectURL(blob);
                    link.download =  `releve_periodique_${this.classe.name}_${this.matiere.name}_${this.idPeriode}.pdf`;
                }
                document.body.appendChild(link);
                link.click();
                resolve();
            }
            catch(e) {
                reject(e);
            };
        });
    }

    formateHeaderAndColumn () : any {
        let header = `${lang.translate('students')}`;
        let column = ['displayName'];

        if(this.exportOptions.averageAuto) {
            header += `; ${lang.translate('average.auto')}`;
            column.push('moyenne');
        }
        if(this.exportOptions.averageFinal) {
            header +=`; ${lang.translate('average.final')}`;
            column.push('moyenneFinale');
        }
        _.mapObject(this.exportOptions.sousMatieres.moyennes, (printSousMatiere, id) => {
            if(printSousMatiere){
                let libelle = _.findWhere(this.matiere.sousMatieres.all , {id_type_sousmatiere:parseInt(id)});
                libelle = (libelle !== undefined)? `${lang.translate('average')} ${libelle.libelle}`: '';
                header += `; ${libelle}`;
                column.push('moyenne' + id);
            }
        });
        if(this.exportOptions.positionnementFinal) {
            header += `; ${lang.translate('evaluations.releve.positionnement')}`;
            column.push('positionnement');
        }
        _.mapObject(this.exportOptions.sousMatieres.positionnements_auto, (printSousMatiere, id) => {
            if(printSousMatiere){
                let libelle = _.findWhere(this.matiere.sousMatieres.all , {id_type_sousmatiere:  parseInt(id)});
                libelle = (libelle !== undefined)? `${lang.translate('evaluations.releve.positionnement')} ${
                    libelle.libelle}`: '';
                header += `; ${libelle}`;
                column.push('positionnement'+ id);
            }
        });

        if(this.exportOptions.appreciation) {
            header += `; ${lang.translate('viescolaire.utils.appreciations')}`;
            column.push('appreciation_matiere_periode');
        }

        return  {header: header, column: column};
    }
}
