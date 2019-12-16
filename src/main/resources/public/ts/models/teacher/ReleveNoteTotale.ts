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

import {Model, IModel, _, Collection, idiom as lang, http} from 'entcore';
import httpAxios from 'axios';
import {
    Periode,
    Matiere,
    Classe,
    Devoir,
    Structure,
    evaluations,
    TableConversion, Utils
} from './index';
import * as utils from "../../utils/teacher";

export class ReleveNoteTotale extends  Model implements IModel {
    periode: Periode;
    matiere: Matiere;
    classe: Classe;
    devoirs: Collection<Devoir>;
    tableConversions: Collection<TableConversion>;
    structure: Structure;
    ennseignantsNames;
    idClasse: string;
    idPeriode: number;
    periodeName: string;
    periodes:any;
    idEtablissement: string;
    exportOptions : any;
    dataByMatiere : any;
    matiereWithDevoirs : any;
    matieresId : any;
    format:any;
    allMatieres:any;
    idGroups:any;

    get api() {
        return {
            get: ``,
            EXPORT: `/competences/releve/exportTotale`,
            GET_DEVOIRS:`/competences/releve/export/checkDevoirs?idEtablissement=`
        }
    }

    constructor(o?: any) {
        super();
        if (o) this.updateData(o, false);
        this.structure = evaluations.structure;
        let c = _.findWhere(evaluations.structure.classes.all, {id: this.idClasse});
        this.classe = new Classe({id: c.id, name: c.name, type_groupe: c.type_groupe, externalId: c.externalId});
        this.periode = _.findWhere(this.classe.periodes.all, {id_type: this.idPeriode});
        this.dataByMatiere = [];
        this.matiereWithDevoirs = [];
        this.matieresId = [];
        this.idGroups = [];
        this.collection(Devoir, {
            sync: () => {
            }
        });
    }

    toJson() {
        return {
            idMatieres: this.matieresId,
            idClasse: this.idClasse,
            idEtablissement: this.idEtablissement,
            idPeriode: this.idPeriode,
            idPeriodes: _.pluck(this.periodes,'idPeriode'),
            typeClasse: this.classe.type_groupe
        };
    }

    async export  () {
        return new Promise(async (resolve, reject) => {
            try {
                await this.formateHeaderAndColumn();
                let columnCsv = [];
                let blob;
                let addingAllStudents = false;
                let parameter = this.toJson();
                if(this.idGroups.length != 0)
                    _.extend(parameter, {
                        idGroups: this.idGroups
                    });
                let data = await httpAxios.post(this.api.EXPORT, parameter);
                console.dir(data);
                let response;
                let responseOtherPeriodes;
                if(this.periodes.length > 0){
                    response = data.data.annual;
                    responseOtherPeriodes = data.data;
                }else{
                    response = data.data;
                }
                _.forEach(this.matiereWithDevoirs, (matiere)=> {
                    if (!addingAllStudents) {
                        _.forEach(response.eleves, (line) => {
                            if (this.exportOptions.moyenneMat) {
                                if (line.moyenneFinale != undefined && line.moyenneFinale[matiere.id] != undefined) {
                                    line[matiere.name + 'Moyenne'] = line.moyenneFinale[matiere.id];
                                } else {
                                    line[matiere.name + 'Moyenne'] = "";
                                }
                            }
                            if(this.exportOptions.positionnementFinal) {
                                if (line.positionnement && line.positionnement[matiere.id] != undefined) {
                                    line[matiere.name + 'Positionnement'] = Number(line.positionnement[matiere.id]);
                                } else {
                                    line[matiere.name + 'Positionnement'] = "";
                                }
                            }
                            if(this.exportOptions.averageFinal && this.periodes.length >0) {
                                if(line.moyenne_generale != undefined)
                                    line['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = line.moyenne_generale;
                                else
                                    line['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = "NN"
                                for (let periode of this.periodes){
                                    if((responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName).length != 0 &&
                                        (responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName)[0].moyenne_generale != undefined)
                                        line['moyenne_generale'+periode.periodeName] = (responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName)[0].moyenne_generale;
                                    else
                                        line['moyenne_generale'+periode.periodeName] = "NN"
                                }
                            }else if (this.exportOptions.averageFinal) {
                                if(line.moyenne_generale != undefined)
                                    line['moyenne_generale'] = line.moyenne_generale;
                                else
                                    line['moyenne_generale'] = "NN"
                            }
                            if(this.exportOptions.appreciation) {
                                if (line.synthese_bilan_periodique != undefined) {
                                    line['appreciation_conseil_de_classe'] = line.synthese_bilan_periodique;
                                    while (!_.isEmpty(line['appreciation_conseil_de_classe'].match('\n'))) {
                                        line['appreciation_conseil_de_classe'] = line['appreciation_conseil_de_classe'].replace('\n', ' ');
                                    }
                                } else
                                    line['appreciation_conseil_de_classe'] = "";
                            }
                            if(this.exportOptions.avisConseil && this.periodes.length >0) {
                                for (let periode of this.periodes){
                                    if((responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName).length != 0 &&
                                        (responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName)[0].avis_conseil_de_classe != undefined)
                                        line['avis_conseil_de_classe'+periode.periodeName] = (responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName)[0].avis_conseil_de_classe;
                                    else
                                        line['avis_conseil_de_classe'+periode.periodeName] = ""
                                }
                            }else if(this.exportOptions.avisConseil){
                                if (line.avis_conseil_de_classe != undefined)
                                    line['avis_conseil_de_classe'] = line.avis_conseil_de_classe;
                                else
                                    line['avis_conseil_de_classe'] = "";
                            }
                            if(this.exportOptions.avisOrientation && this.periodes.length >0) {
                                for (let periode of this.periodes){
                                    if((responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName).length != 0 &&
                                        ((responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName)[0] as any).avis_conseil_orientation != undefined)
                                        line['avis_orientation'+periode.periodeName] = ((responseOtherPeriodes[periode.idPeriode].eleves).filter(eleve => eleve.displayName == line.displayName)[0] as any).avis_conseil_orientation;
                                    else
                                        line['avis_orientation'+periode.periodeName] = ""
                                }
                            }else if(this.exportOptions.avisOrientation) {
                                if (line.avis_conseil_orientation != undefined)
                                    line['avis_orientation'] = line.avis_conseil_orientation;
                                else
                                    line['avis_orientation'] = "";
                            }
                            columnCsv.push(_.pick(line, this.format['column']));
                        });
                        if (this.exportOptions.statistiques) {
                            let jsonMoyenneToAdd = {};
                            jsonMoyenneToAdd["displayName"] = lang.translate('viescolaire.classe.moyenne');
                            let jsonMinToAdd = {};
                            jsonMinToAdd["displayName"] = "Minimum";
                            let jsonMaxToAdd = {};
                            jsonMaxToAdd["displayName"] = "Maximum";
                            if(this.exportOptions.moyenneMat) {
                                if(response.statistiques[matiere.id] != undefined && response.statistiques[matiere.id].moyenne != undefined) {
                                    jsonMoyenneToAdd[matiere.name + 'Moyenne'] = response.statistiques[matiere.id].moyenne.moyenne;
                                    jsonMinToAdd[matiere.name + 'Moyenne'] = response.statistiques[matiere.id].moyenne.minimum;
                                    jsonMaxToAdd[matiere.name + 'Moyenne'] = response.statistiques[matiere.id].moyenne.maximum;
                                }else{
                                    jsonMoyenneToAdd[matiere.name + 'Moyenne'] = "NN";
                                    jsonMinToAdd[matiere.name + 'Moyenne'] = "NN";
                                    jsonMaxToAdd[matiere.name + 'Moyenne'] = "NN";
                                }
                            }
                            if(this.exportOptions.positionnementFinal) {
                                if(response.statistiques[matiere.id] != undefined && response.statistiques[matiere.id].positionnement != undefined) {
                                    jsonMoyenneToAdd[matiere.name + 'Positionnement'] = response.statistiques[matiere.id].positionnement.moyenne;
                                    jsonMinToAdd[matiere.name + 'Positionnement'] = response.statistiques[matiere.id].positionnement.minimum;
                                    jsonMaxToAdd[matiere.name + 'Positionnement'] = response.statistiques[matiere.id].positionnement.maximum;
                                }else{
                                    jsonMoyenneToAdd[matiere.name + 'Positionnement'] = "NN";
                                    jsonMinToAdd[matiere.name + 'Positionnement'] = "NN";
                                    jsonMaxToAdd[matiere.name + 'Positionnement'] = "NN";
                                }

                            }
                            if(this.exportOptions.averageFinal && this.periodes.length >0) {
                                if(response.statistiques.moyenne_generale != undefined && response.statistiques.moyenne_generale.moyenne != undefined) {
                                    jsonMoyenneToAdd['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = response.statistiques.moyenne_generale.moyenne;
                                    jsonMinToAdd['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = response.statistiques.moyenne_generale.minimum;
                                    jsonMaxToAdd['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = response.statistiques.moyenne_generale.maximum;
                                }else{
                                    jsonMoyenneToAdd['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = "NN";
                                    jsonMinToAdd['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = "NN";
                                    jsonMaxToAdd['moyenne_generale'+lang.translate('viescolaire.utils.annee')] = "NN";
                                }
                                for (let periode of this.periodes) {
                                    if(responseOtherPeriodes[periode.idPeriode].statistiques.moyenne_generale != undefined && responseOtherPeriodes[periode.idPeriode].statistiques.moyenne_generale.moyenne != undefined) {
                                        jsonMoyenneToAdd['moyenne_generale'+periode.periodeName] = responseOtherPeriodes[periode.idPeriode].statistiques.moyenne_generale.moyenne;
                                        jsonMinToAdd['moyenne_generale'+periode.periodeName] = responseOtherPeriodes[periode.idPeriode].statistiques.moyenne_generale.minimum;
                                        jsonMaxToAdd['moyenne_generale'+periode.periodeName] = responseOtherPeriodes[periode.idPeriode].statistiques.moyenne_generale.maximum;
                                    }else{
                                        jsonMoyenneToAdd['moyenne_generale'+periode.periodeName] = "NN";
                                        jsonMinToAdd['moyenne_generale'+periode.periodeName] = "NN";
                                        jsonMaxToAdd['moyenne_generale'+periode.periodeName] = "NN";
                                    }
                                }
                            }else if(this.exportOptions.averageFinal) {
                                if(response.statistiques.moyenne_generale != undefined && response.statistiques.moyenne_generale.moyenne != undefined) {
                                    jsonMoyenneToAdd['moyenne_generale'] = response.statistiques.moyenne_generale.moyenne;
                                    jsonMinToAdd['moyenne_generale'] = response.statistiques.moyenne_generale.minimum;
                                    jsonMaxToAdd['moyenne_generale'] = response.statistiques.moyenne_generale.maximum;
                                }else{
                                    jsonMoyenneToAdd['moyenne_generale'] = "NN";
                                    jsonMinToAdd['moyenne_generale'] = "NN";
                                    jsonMaxToAdd['moyenne_generale'] = "NN";
                                }
                            }
                            columnCsv.push(jsonMoyenneToAdd);
                            columnCsv.push(jsonMinToAdd);
                            columnCsv.push(jsonMaxToAdd);
                        }
                        addingAllStudents = true;
                    } else {
                        _.forEach(response.eleves, (line) => {
                            if (this.exportOptions.moyenneMat) {
                                if (line.moyenneFinale != undefined && line.moyenneFinale[matiere.id] != undefined) {
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Moyenne'] = line.moyenneFinale[matiere.id];
                                } else {
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Moyenne'] = "";
                                }
                            }
                            if(this.exportOptions.positionnementFinal) {
                                if (line.positionnement != undefined && line.positionnement[matiere.id] != undefined) {
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Positionnement'] = Number(line.positionnement[matiere.id]);
                                } else {
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Positionnement'] = "";
                                }
                            }
                        });
                        if (this.exportOptions.statistiques && (this.exportOptions.moyenneMat || this.exportOptions.positionnementFinal)) {
                            if(this.exportOptions.moyenneMat) {
                                if(response.statistiques[matiere.id] != undefined && response.statistiques[matiere.id].moyenne != undefined) {
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Moyenne'] = response.statistiques[matiere.id].moyenne.moyenne;
                                    columnCsv.filter(line => line.displayName == "Minimum")[0][matiere.name + 'Moyenne'] = response.statistiques[matiere.id].moyenne.minimum;
                                    columnCsv.filter(line => line.displayName == "Maximum")[0][matiere.name + 'Moyenne'] = response.statistiques[matiere.id].moyenne.maximum;
                                }else{
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Moyenne'] = "NN";
                                    columnCsv.filter(line => line.displayName == "Minimum")[0][matiere.name + 'Moyenne'] = "NN";
                                    columnCsv.filter(line => line.displayName == "Maximum")[0][matiere.name + 'Moyenne'] = "NN";
                                }
                            }
                            if(this.exportOptions.positionnementFinal) {
                                if(response.statistiques[matiere.id] != undefined && response.statistiques[matiere.id].positionnement != undefined) {
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Positionnement'] = response.statistiques[matiere.id].positionnement.moyenne;
                                    columnCsv.filter(line => line.displayName == "Minimum")[0][matiere.name + 'Positionnement'] = response.statistiques[matiere.id].positionnement.minimum;
                                    columnCsv.filter(line => line.displayName == "Maximum")[0][matiere.name + 'Positionnement'] = response.statistiques[matiere.id].positionnement.maximum;
                                }else{
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Positionnement'] = "NN";
                                    columnCsv.filter(line => line.displayName == "Minimum")[0][matiere.name + 'Positionnement'] = "NN";
                                    columnCsv.filter(line => line.displayName == "Maximum")[0][matiere.name + 'Positionnement'] = "NN";
                                }
                            }
                        }
                    }
                });

                if (columnCsv.length > 0) {
                    let csvData = Utils.ConvertToCSV(columnCsv, this.format['header'], this.format['column']);
                    csvData = "\ufeff"+csvData;
                    blob = new Blob([csvData], { type: ' type: "text/csv;charset=UTF-8"' });
                    let link = document.createElement('a');
                    link.href = (window as any).URL.createObjectURL(blob);
                    if(this.exportOptions.positionnementFinal && this.exportOptions.moyenneMat)
                        link.download = `tableau_moyenne_positionnement_toutes_matieres_${this.classe.name}_${this.periodeName}.csv`;
                    else
                    if(this.exportOptions.positionnementFinal)
                        link.download = `tableau_positionnement_toutes_matieres_${this.classe.name}_${this.periodeName}.csv`;
                    else
                        link.download = `tableau_moyenne_toutes_matieres_${this.classe.name}_${this.periodeName}.csv`;
                    document.body.appendChild(link);
                    link.click();
                    resolve();
                }
            } catch (e) {
                reject(e);
            }
        });
    }

    async formateHeaderAndColumn () {
        return new Promise(async (resolve, reject) => {
            try {
                let header = `${lang.translate('evaluations.classe.groupe')} : ${this.classe.name}\r\n${lang.translate('viescolaire.utils.periode')} : ${this.periodeName}\r\n`;
                header += `${lang.translate('matieres')}`;
                let column = ['displayName'];
                let enseignants = [];
                let uri = this.api.GET_DEVOIRS
                    + this.idEtablissement + '&idClasse=' + this.idClasse;
                if(this.idPeriode)
                    uri += '&idPeriode=' + this.idPeriode;
                await httpAxios.get(uri).then((data) => {
                    _.forEach(this.allMatieres, matiere => {
                        let _devoirs = data.data.filter(devoir => devoir.id_matiere == matiere.id);
                        if (_devoirs.length > 0) {
                            this.matiereWithDevoirs.push(matiere);
                            this.matieresId.push(matiere.id);
                            this.dataByMatiere[matiere.id] = _devoirs;
                            _devoirs.forEach(devoir => {
                                if(devoir.id_groupe != this.idClasse)
                                    this.idGroups.push(devoir.id_groupe);
                            });
                        }
                    });
                });
                if(this.idGroups.length != 0)
                    this.idGroups.push(this.idClasse);
                if (this.matieresId.length == 0)
                    throw "Pas d'évaluations réalisées pour cette classe sur cette période";
                for (let matiere of this.matiereWithDevoirs) {
                    let _devoirs = this.dataByMatiere[matiere.id];
                    if (this.exportOptions.moyenneMat) {
                        header += `;${matiere.name}`;
                        column.push(matiere.name + 'Moyenne');
                    }
                    if (this.exportOptions.positionnementFinal) {
                        header += `;${matiere.name}`;
                        column.push(matiere.name + 'Positionnement');
                    }
                    this.devoirs.load(_devoirs, null, false);
                    this.ennseignantsNames = "; ";

                    for (let i = 0; i < this.devoirs.all.length; i++) {
                        let teacher = this.devoirs.all[i].teacher;
                        if (!utils.containsIgnoreCase(this.ennseignantsNames, teacher)) {
                            this.ennseignantsNames += teacher + " "
                        }
                    }
                    enseignants.push(this.ennseignantsNames);
                }

                if (this.exportOptions.averageFinal) {
                    if(this.periodes.length == 0) {
                        header += `;${lang.translate('average.min')}`;
                        column.push('moyenne_generale');
                    }else{
                        for (let periode of this.periodes){
                            header += `;${lang.translate('average')} `;
                            column.push('moyenne_generale'+periode.periodeName);
                        }
                        header += `;${lang.translate('average')} `;
                        column.push('moyenne_generale'+lang.translate('viescolaire.utils.annee'));
                    }
                }
                if (this.exportOptions.appreciation) {
                    header += `;${lang.translate('viescolaire.utils.appreciations')}`;
                    column.push('appreciation_conseil_de_classe');
                }
                if (this.exportOptions.avisConseil) {
                    if(this.periodes.length == 0) {
                        header += `;${lang.translate('evaluations.evaluation.avis.conseil')}`;
                        column.push('avis_conseil_de_classe');
                    }else{
                        for (let periode of this.periodes){
                            header += `;${lang.translate('evaluations.evaluation.avis.conseil')}`;
                            column.push('avis_conseil_de_classe'+periode.periodeName);
                        }
                    }
                }
                if (this.exportOptions.avisOrientation) {
                    if(this.periodes.length == 0) {
                        header += `;${lang.translate('evaluations.evaluation.avis.orientation')}`;
                        column.push('avis_orientation');
                    }else{
                        for (let periode of this.periodes){
                            header += `;${lang.translate('evaluations.evaluation.avis.orientation')}`;
                            column.push('avis_orientation'+periode.periodeName);
                        }
                    }
                }
                header += `\r\n${lang.translate('teachers')}`;
                for (let j = 0; j < this.matieresId.length; j++) {
                    if (this.exportOptions.moyenneMat)
                        header += `${enseignants[j]}`;
                    if (this.exportOptions.positionnementFinal)
                        header += `${enseignants[j]}`;
                }
                if(this.periodes.length > 0){
                    if (this.exportOptions.averageFinal) {
                        for (let periode of this.periodes) {
                            header += `;${periode.periodeName.substring(0, periode.periodeName.length - 1)}`;
                        }
                        header += `;${lang.translate('viescolaire.utils.annee')}`;
                    }
                    if (this.exportOptions.avisConseil) {
                        for (let periode of this.periodes) {
                            header += `;${periode.periodeName.substring(0, periode.periodeName.length - 1)}`;
                        }
                    }
                    if (this.exportOptions.avisOrientation) {
                        for (let periode of this.periodes) {
                            header += `;${periode.periodeName.substring(0, periode.periodeName.length - 1)}`;
                        }
                    }
                }
                header += `\r\n${lang.translate('students')};`;
                for (let i = 0; i < this.matieresId.length; i++) {
                    if (this.exportOptions.moyenneMat) {
                        header += `${lang.translate('average.min')};`;
                    }
                    if (this.exportOptions.positionnementFinal) {
                        header += `${lang.translate('evaluations.releve.positionnement.min')};`;
                    }
                }
                if(this.periodes.length > 0){
                    if (this.exportOptions.averageFinal) {
                        for (let periode of this.periodes) {
                            header += `${periode.periodeName[periode.periodeName.length - 1]};`;
                        }
                        header += ";";
                    }
                    if (this.exportOptions.avisConseil) {
                        for (let periode of this.periodes) {
                            header += `${periode.periodeName[periode.periodeName.length - 1]};`;
                        }
                    }
                    if (this.exportOptions.avisOrientation) {
                        for (let periode of this.periodes) {
                            header += `${periode.periodeName[periode.periodeName.length - 1]};`;
                        }
                    }
                }
                this.format = {header: header, column: column};
                resolve();
            } catch (e) {
                reject(e);
            }
        });
    }
}