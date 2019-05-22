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
            appreciation: true,
            averageFinal: true,
            statistiques: true,
            positionnementFinal: true,
            avisConseil: true,
            avisOrientation: true
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
                    _.forEach(this.matiereWithDevoirs, (matiere)=> {
                        nbEleves = 0;
                        let moyennePos = 0;
                        let min;
                        let max;
                        if (response.eleves[0].moyenneFinale[matiere.id] != undefined) {
                            min = response.eleves[0].moyenneFinale[matiere.id];
                            max = response.eleves[0].moyenneFinale[matiere.id];
                        } else {
                            min = "";
                            max = "";
                        }
                        let minPos;
                        let maxPos;
                        if (response.eleves[0].positionnement[matiere.id] != undefined) {
                            minPos = Number(response.eleves[0].positionnement[matiere.id]);
                            maxPos = Number(response.eleves[0].positionnement[matiere.id]);
                        } else {
                            minPos = "";
                            maxPos = "";
                        }
                        if (!addingAllStudents) {
                            _.forEach(response.eleves, (line) => {
                                nbEleves++;
                                line['eleveNonNote']= false;
                                if (this.exportOptions.averageFinal) {
                                    if(line.moyenneFinale[matiere.id] != undefined) {
                                        line[matiere.name + 'Moyenne'] = line.moyenneFinale[matiere.id];
                                        if (line.moyenneFinale[matiere.id] != "NN") {
                                            line['moyenne_generale'] =  Number(line.moyenneFinale[matiere.id]);
                                            line['nbMatieres'] = 1;
                                            if (max != "NN" && min != "NN" && max != "" && min != "") {
                                                if (max < line.moyenneFinale[matiere.id])
                                                    max =  Number(line.moyenneFinale[matiere.id]);
                                                if (min > line.moyenneFinale[matiere.id])
                                                    min =  Number(line.moyenneFinale[matiere.id]);
                                            } else {
                                                max = line.moyenneFinale[matiere.id];
                                                min = line.moyenneFinale[matiere.id];
                                            }
                                        } else {
                                            line['moyenne_generale'] = 0;
                                            line['nbMatieres'] = 0;
                                            line['eleveNonNote'] = true;
                                            if (max == "" && min == "") {
                                                max = line.moyenneFinale[matiere.id];
                                                min = line.moyenneFinale[matiere.id];
                                            }
                                        }
                                    }else{
                                        line[matiere.name + 'Moyenne'] = "";
                                        line['moyenne_generale'] = 0;
                                        line['nbMatieres'] = 0;
                                        line['eleveNonNote'] = true;
                                    }
                                    if(line.positionnement[matiere.id] != undefined) {
                                        line[matiere.name + 'Positionnement'] = Number(line.positionnement[matiere.id]);
                                        moyennePos += Number(line.positionnement[matiere.id]);
                                        if(maxPos != "" && maxPos != "") {
                                            if (maxPos < line.positionnement[matiere.id])
                                                maxPos = Number(line.positionnement[matiere.id]);
                                            if (minPos > line.positionnement[matiere.id])
                                                minPos = Number(line.positionnement[matiere.id]);
                                        }else{
                                            maxPos = Number(line.positionnement[matiere.id]);
                                            minPos = Number(line.positionnement[matiere.id]);
                                        }
                                    }
                                    else {
                                        line[matiere.name + 'Positionnement'] = "";
                                        nbEleves--;
                                    }
                                    if (line.synthese_bilan_periodique != undefined) {
                                        line['appreciation_conseil_de_classe'] = line.synthese_bilan_periodique;
                                        while (!_.isEmpty(line['appreciation_conseil_de_classe'].match('\n'))) {
                                            line['appreciation_conseil_de_classe'] = line['appreciation_conseil_de_classe'].replace('\n',' ');
                                        }
                                    }
                                    else
                                        line['appreciation_conseil_de_classe'] = "";
                                    if (line.avis_conseil_de_classe != undefined)
                                        line['avis_conseil_de_classe'] = line.avis_conseil_de_classe;
                                    else
                                        line['avis_conseil_de_classe'] = "";
                                    if (line.avis_conseil_orientation != undefined)
                                        line['avis_orientation'] = line.avis_conseil_orientation;
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
                                    if(response.moyenne[matiere.id].moyenne_classe != undefined)
                                        jsonMoyenneToAdd[matiere.name + 'Moyenne'] = Number(response.moyenne[matiere.id].moyenne_classe);
                                    else
                                        jsonMoyenneToAdd[matiere.name + 'Moyenne'] = "NN";
                                    if(moyennePos != 0)
                                        jsonMoyenneToAdd[matiere.name + 'Positionnement'] = Number((moyennePos / nbEleves).toFixed(2));
                                    else
                                        jsonMoyenneToAdd[matiere.name + 'Positionnement'] = "";

                                    columnCsv.push(jsonMoyenneToAdd);
                                }
                                if (this.exportOptions.moyenneClasse) {
                                    let jsonMinToAdd = {};
                                    jsonMinToAdd["displayName"] = "Minimum";
                                    jsonMinToAdd[matiere.name + 'Moyenne'] = min;
                                    jsonMinToAdd[matiere.name + 'Positionnement'] = minPos;
                                    columnCsv.push(jsonMinToAdd);
                                    let jsonMaxToAdd = {};
                                    jsonMaxToAdd["displayName"] = "Maximum";
                                    jsonMaxToAdd[matiere.name + 'Moyenne'] = max;
                                    jsonMaxToAdd[matiere.name + 'Positionnement'] = maxPos;
                                    columnCsv.push(jsonMaxToAdd);
                                }
                            }
                            addingAllStudents = true;
                        } else {
                            _.forEach(response.eleves, (line) => {
                                nbEleves++;
                                if (this.exportOptions.averageFinal) {
                                    if (line.moyenneFinale[matiere.id] != undefined) {
                                        columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Moyenne'] = line.moyenneFinale[matiere.id];
                                        if (line.moyenneFinale[matiere.id] != "NN") {
                                            columnCsv.filter(eleve => eleve.displayName == line.displayName)[0]['moyenne_generale'] += Number(line.moyenneFinale[matiere.id]);
                                            elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['nbMatieres']++;
                                            if (max != "NN" && min != "NN" && max != "" && min != "") {
                                                if (max < line.moyenneFinale[matiere.id])
                                                    max = Number(line.moyenneFinale[matiere.id]);
                                                if (min > line.moyenneFinale[matiere.id])
                                                    min = Number(line.moyenneFinale[matiere.id]);
                                            } else {
                                                max = Number(line.moyenneFinale[matiere.id]);
                                                min = Number(line.moyenneFinale[matiere.id]);
                                            }
                                        } else {
                                            elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['eleveNonNote'] = true;
                                            if (max == "" && min == "") {
                                                max = line.moyenneFinale[matiere.id];
                                                min = line.moyenneFinale[matiere.id];
                                            }
                                        }
                                    } else {
                                        columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Moyenne'] = "";
                                        elevesNbMatieresEvaluate.filter(eleve => eleve.displayName == line.displayName)[0]['eleveNonNote'] = true;
                                    }
                                    if (line.positionnement[matiere.id] != undefined) {
                                        columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Positionnement'] = Number(line.positionnement[matiere.id]);
                                        moyennePos += Number(line.positionnement[matiere.id]);
                                        if (maxPos != "" && maxPos != "") {
                                            if (maxPos < line.positionnement[matiere.id])
                                                maxPos = Number(line.positionnement[matiere.id]);
                                            if (minPos > line.positionnement[matiere.id])
                                                minPos = Number(line.positionnement[matiere.id]);
                                        } else {
                                            maxPos = Number(line.positionnement[matiere.id]);
                                            minPos = Number(line.positionnement[matiere.id]);
                                        }
                                    } else {
                                        nbEleves--;
                                        columnCsv.filter(eleve => eleve.displayName == line.displayName)[0][matiere.name + 'Positionnement'] = "";
                                    }
                                }
                            });
                            if (this.exportOptions.appreciationClasse || this.exportOptions.moyenneClasse) {
                                if (this.exportOptions.appreciationClasse) {
                                    if (response.moyenne[matiere.id] != undefined)
                                        columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Moyenne'] = Number(response.moyenne[matiere.id].moyenne_classe);
                                    else
                                        columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Moyenne'] = "NN";
                                    if (moyennePos != 0)
                                        columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Positionnement'] = Number((moyennePos / nbEleves).toFixed(2));
                                    else
                                        columnCsv.filter(line => line.displayName == lang.translate('viescolaire.classe.moyenne'))[0][matiere.name + 'Positionnement'] = "";
                                }
                                if (this.exportOptions.moyenneClasse) {
                                    columnCsv.filter(line => line.displayName == "Minimum")[0][matiere.name + 'Moyenne'] = min;
                                    columnCsv.filter(line => line.displayName == "Maximum")[0][matiere.name + 'Moyenne'] = max;
                                    columnCsv.filter(line => line.displayName == "Minimum")[0][matiere.name + 'Positionnement'] = minPos;
                                    columnCsv.filter(line => line.displayName == "Maximum")[0][matiere.name + 'Positionnement'] = maxPos;
                                }
                            }
                        }
                    });

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