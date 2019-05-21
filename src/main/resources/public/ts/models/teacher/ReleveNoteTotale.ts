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
    evaluations, TableConversion, Utils
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
    idEtablissement: string;
    exportOptions : any;
    dataByMatiere : any;
    matiereWithDevoirs : any;
    matieresId : any;

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
        this.exportOptions = {
            show : false,
            fileType: 'csv',

            appreciation: true,
            averageFinal: true,
            averageAuto: true,
            positionnementFinal: true,
            appreciationClasse: true,
            moyenneClasse: true
        };
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
            idPeriode: this.idPeriode
        };
    }

    async export  () {
        return new Promise(async (resolve, reject) => {
            try {
                await this.formateHeaderAndColumn().then(async result => {
                    let columnCsv = [];
                    let format = result;
                    let blob;
                    let addingAllStudents = false;
                    let nbMatieres = 0;
                    let nbEleves = 0;
                    let elevesNbMatieresEvaluate = [];
                    let allPromise = [];
                    let matieresId = [];
                    let parameter = this.toJson();
                    _.extend(parameter, this.exportOptions);
                    _.extend(parameter, {
                        typeClasse: this.classe.type_groupe
                    });
                    let data = await httpAxios.post(this.api.EXPORT, parameter);
                    console.dir(data);
                    let response = data.data;
                    nbEleves = 0;
                    let moyennePos = 0;
                    let min;
                    let max;
                    if (response.eleves[0].moyenneFinale != undefined) {
                        min = response.eleves[0].moyenneFinale;
                        max = response.eleves[0].moyenneFinale;
                    } else {
                        min = "";
                        max = "";
                    }
                    let minPos;
                    let maxPos;
                    if (response.eleves[0].positionnement != undefined) {
                        minPos = Number(response.eleves[0].positionnement);
                        maxPos = Number(response.eleves[0].positionnement);
                    } else {
                        minPos = "";
                        maxPos = "";
                    }
                    /*if (!addingAllStudents) {
                        _.forEach(response.eleves, (line) => {
                            nbEleves++;
                            line['eleveNonNote']= false;
                            if (this.exportOptions.averageFinal) {
                                if(line.moyenneFinale != undefined) {
                                    line[matiereToAdd.name + 'Moyenne'] = line.moyenneFinale;
                                    if (line.moyenneFinale != "NN") {
                                        line['moyenne_generale'] =  Number(line.moyenneFinale);
                                        line['nbMatieres'] = 1;
                                        if (max != "NN" && min != "NN" && max != "" && min != "") {
                                            if (max < line.moyenneFinale)
                                                max =  Number(line.moyenneFinale);
                                            if (min > line.moyenneFinale)
                                                min =  Number(line.moyenneFinale);
                                        } else {
                                            max = line.moyenneFinale;
                                            min = line.moyenneFinale;
                                        }
                                    } else {
                                        line['moyenne_generale'] = 0;
                                        line['nbMatieres'] = 0;
                                        line['eleveNonNote'] = true;
                                        if (max == "" && min == "") {
                                            max = line.moyenneFinale;
                                            min = line.moyenneFinale;
                                        }
                                    }
                                }else{
                                    line[matiereToAdd.name + 'Moyenne'] = "";
                                    line['moyenne_generale'] = 0;
                                    line['nbMatieres'] = 0;
                                    line['eleveNonNote'] = true;
                                }
                                if(line.positionnement != undefined) {
                                    line[matiereToAdd.name + 'Positionnement'] = Number(line.positionnement);
                                    moyennePos += Number(line.positionnement);
                                    if(maxPos != "" && maxPos != "") {
                                        if (maxPos < line.positionnement)
                                            maxPos = Number(line.positionnement);
                                        if (minPos > line.positionnement)
                                            minPos = Number(line.positionnement);
                                    }else{
                                        maxPos = Number(line.positionnement);
                                        minPos = Number(line.positionnement);
                                    }
                                }
                                else {
                                    line[matiereToAdd.name + 'Positionnement'] = "";
                                    nbEleves--;
                                }
                                if (response.appreciations_eleve.filter(eleve => eleve.id_eleve == line.id)[0] != undefined) {
                                    line['appreciation_conseil_de_classe'] = response.appreciations_eleve.filter(eleve => eleve.id_eleve == line.id)[0].synthese;
                                    while (!_.isEmpty(line['appreciation_conseil_de_classe'].match('\n'))) {
                                        line['appreciation_conseil_de_classe'] = line['appreciation_conseil_de_classe'].replace('\n',' ');
                                    }
                                }
                                else
                                    line['appreciation_conseil_de_classe'] = "";
                                if (response.avis_conseil_de_classe.filter(eleve => eleve.id_eleve == line.id)[0] != undefined)
                                    line['avis_conseil_de_classe'] = response.avis_conseil_de_classe.filter(eleve => eleve.id_eleve == line.id)[0].libelle;
                                else
                                    line['avis_conseil_de_classe'] = "";
                                if (response.avis_conseil_orientation.filter(eleve => eleve.id_eleve == line.id)[0] != undefined)
                                    line['avis_orientation'] = response.avis_conseil_orientation.filter(eleve => eleve.id_eleve == line.id)[0].libelle;
                                else
                                    line['avis_orientation'] = "";
                            }
                            elevesNbMatieresEvaluate.push(line);
                            columnCsv.push(_.pick(line, format['column']));
                        });
                        if (this.exportOptions.appreciationClasse || this.exportOptions.moyenneClasse) {
                            if (this.exportOptions.appreciationClasse) {
                                let jsonMoyenneToAdd = {};
                                jsonMoyenneToAdd["displayName"] = lang.translate('viescolaire.classe.moyenne');
                                if(response.moyenne_classe != undefined)
                                    jsonMoyenneToAdd[matiereToAdd.name + 'Moyenne'] = Number(response.moyenne_classe);
                                else
                                    jsonMoyenneToAdd[matiereToAdd.name + 'Moyenne'] = "NN";
                                if(moyennePos != 0)
                                    jsonMoyenneToAdd[matiereToAdd.name + 'Positionnement'] = Number((moyennePos / nbEleves).toFixed(2));
                                else
                                    jsonMoyenneToAdd[matiereToAdd.name + 'Positionnement'] = "";

                                columnCsv.push(jsonMoyenneToAdd);
                            }
                            if (this.exportOptions.moyenneClasse) {
                                let jsonMinToAdd = {};
                                jsonMinToAdd["displayName"] = "Minimum";
                                jsonMinToAdd[matiereToAdd.name + 'Moyenne'] = min;
                                jsonMinToAdd[matiereToAdd.name + 'Positionnement'] = minPos;
                                columnCsv.push(jsonMinToAdd);
                                let jsonMaxToAdd = {};
                                jsonMaxToAdd["displayName"] = "Maximum";
                                jsonMaxToAdd[matiereToAdd.name + 'Moyenne'] = max;
                                jsonMaxToAdd[matiereToAdd.name + 'Positionnement'] = maxPos;
                                columnCsv.push(jsonMaxToAdd);
                            }
                        }
                        addingAllStudents = true;
                    } else {
                        _.forEach(response.eleves, (line) => {
                            nbEleves++;
                            if (this.exportOptions.averageFinal) {
                                if (line.moyenneFinale != undefined) {
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiereToAdd.name + 'Moyenne'] = line.moyenneFinale;
                                    if (line.moyenneFinale != "NN") {
                                        columnCsv.filter(eleve => eleve.displayName == line.displayName)[0]['moyenne_generale'] += Number(line.moyenneFinale);
                                        elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['nbMatieres']++;
                                        if (max != "NN" && min != "NN" && max != "" && min != "") {
                                            if (max < line.moyenneFinale)
                                                max = Number(line.moyenneFinale);
                                            if (min > line.moyenneFinale)
                                                min = Number(line.moyenneFinale);
                                        } else {
                                            max = Number(line.moyenneFinale);
                                            min = Number(line.moyenneFinale);
                                        }
                                    } else {
                                        elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['eleveNonNote'] = true;
                                        if (max == "" && min == "") {
                                            max = line.moyenneFinale;
                                            min = line.moyenneFinale;
                                        }
                                    }
                                } else {
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiereToAdd.name + 'Moyenne'] = "";
                                    elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['eleveNonNote'] = true;
                                }
                                if (line.positionnement != undefined) {
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiereToAdd.name + 'Positionnement'] = Number(line.positionnement);
                                    moyennePos += Number(line.positionnement);
                                    if (maxPos != "" && maxPos != "") {
                                        if (maxPos < line.positionnement)
                                            maxPos = Number(line.positionnement);
                                        if (minPos > line.positionnement)
                                            minPos = Number(line.positionnement);
                                    } else {
                                        maxPos = Number(line.positionnement);
                                        minPos = Number(line.positionnement);
                                    }
                                } else {
                                    nbEleves--;
                                    columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiereToAdd.name + 'Positionnement'] = "";
                                }
                            }
                        });
                        if (this.exportOptions.appreciationClasse || this.exportOptions.moyenneClasse) {
                            if (this.exportOptions.appreciationClasse) {
                                if (response.moyenne_classe != undefined)
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiereToAdd.name + 'Moyenne'] = Number(response.moyenne_classe);
                                else
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiereToAdd.name + 'Moyenne'] = "NN";
                                if (moyennePos != 0)
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiereToAdd.name + 'Positionnement'] = Number((moyennePos / nbEleves).toFixed(2));
                                else
                                    columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiereToAdd.name + 'Positionnement'] = "";
                            }
                            if (this.exportOptions.moyenneClasse) {
                                columnCsv.filter(line => line.displayName == "Minimum")[0][matiereToAdd.name + 'Moyenne'] = min;
                                columnCsv.filter(line => line.displayName == "Maximum")[0][matiereToAdd.name + 'Moyenne'] = max;
                                columnCsv.filter(line => line.displayName == "Minimum")[0][matiereToAdd.name + 'Positionnement'] = minPos;
                                columnCsv.filter(line => line.displayName == "Maximum")[0][matiereToAdd.name + 'Positionnement'] = maxPos;
                            }
                        }
                    }*/

                    if (columnCsv.length > 0) {

                        let firstTime = false;
                        let min;
                        let max;
                        let moy = 0;
                        nbEleves = 0;

                        _.forEach(columnCsv, line => {
                            if (line['moyenne_generale'] == 0 && elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['eleveNonNote']) {
                                line['moyenne_generale'] = "NN";
                                nbEleves--;
                            } else if (elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0] != undefined) {
                                nbEleves++;
                                line['moyenne_generale'] = Number((line['moyenne_generale'] / elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['nbMatieres']).toFixed(2));
                                if (!firstTime) {
                                    min = line['moyenne_generale'];
                                    max = line['moyenne_generale'];
                                    firstTime = true;
                                }
                                moy += line['moyenne_generale'];
                                if (min > line['moyenne_generale'])
                                    min = line['moyenne_generale'];
                                if (max < line['moyenne_generale'])
                                    max = line['moyenne_generale'];
                            }
                        });
                        if (nbEleves > 0) {
                            columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0]['moyenne_generale'] = Number((moy / nbEleves).toFixed(2));
                            columnCsv.filter(line => line.displayName == 'Minimum')[0]['moyenne_generale'] = min;
                            columnCsv.filter(line => line.displayName == 'Maximum')[0]['moyenne_generale'] = max;
                        } else {
                            columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0]['moyenne_generale'] = "";
                            columnCsv.filter(line => line.displayName == 'Minimum')[0]['moyenne_generale'] = "";
                            columnCsv.filter(line => line.displayName == 'Maximum')[0]['moyenne_generale'] = "";
                        }

                        let csvData = Utils.ConvertToCSV(columnCsv, format['header'], format['column']);

                        blob = new Blob([csvData]);
                        let link = document.createElement('a');
                        link.href = window.URL.createObjectURL(blob);
                        link.download = `tableau_moyenne_positionnement_toutes_matieres_${this.classe.name}_${this.periodeName}.csv`;

                        document.body.appendChild(link);
                        link.click();
                        resolve();
                    }
                })
                    .catch(err => {
                        reject(err);
                    });
            } catch (e) {
                reject(e);
            }
        });
    }

    async formateHeaderAndColumn () {
        let nbMatieres = 0;
        let header = `${lang.translate('evaluations.classe.groupe')} : ${this.classe.name}\r\n${lang.translate('viescolaire.utils.periode')} : ${this.periodeName}\r\n`;
        header += `${lang.translate('matieres')}`;
        let column = ['displayName'];
        let enseignants=[];
        let uri = this.api.GET_DEVOIRS
            + this.idEtablissement + '&idClasse=' + this.idClasse + '&idPeriode=' + this.idPeriode;
        await httpAxios.get(uri).then((data) => {
            _.forEach(evaluations.structure.matieres.all, matiere => {
                let _devoirs = data.data.filter(devoir => devoir.id_matiere == matiere.id);
                if (_devoirs.length > 0) {
                    this.matiereWithDevoirs.push(matiere);
                    this.matieresId.push(matiere.id);
                    this.dataByMatiere[matiere.id] = _devoirs;
                }
            });
        });
        for (let matiere of this.matiereWithDevoirs) {
            let _devoirs = this.dataByMatiere[matiere.id];
            nbMatieres++;
            header += `;${matiere.name}`;
            column.push(matiere.name + 'Moyenne');
            header += `;${matiere.name}`;
            column.push(matiere.name + 'Positionnement');
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

        if(this.exportOptions.positionnementFinal) {
            header += `; ${lang.translate('average.min')}`;
            column.push('moyenne_generale');
        }
        if(this.exportOptions.appreciation) {
            header += `; ${lang.translate('viescolaire.utils.appreciations')}`;
            column.push('appreciation_conseil_de_classe');
        }
        if(this.exportOptions.appreciation) {
            header += `; ${lang.translate('evaluations.evaluation.avis.conseil')}`;
            column.push('avis_conseil_de_classe');
        }
        if(this.exportOptions.appreciation) {
            header += `; ${lang.translate('evaluations.evaluation.avis.orientation')}`;
            column.push('avis_orientation');
        }
        header +=`\r\n${lang.translate('teachers')}`;
        for(let j =0; j<nbMatieres;j++){
            header += `${enseignants[j]}`;
            header += `${enseignants[j]}`;
        }
        header +=`\r\n${lang.translate('students')};`;
        for(let i =0; i<nbMatieres;i++){
            header +=`${lang.translate('average.min')};`;
            header +=`${lang.translate('evaluations.releve.positionnement.min')};`;
        }
        return {header: header, column: column};
    }
}