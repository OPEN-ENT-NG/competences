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

import {_, Collection, idiom as lang, Model, model, moment, notify} from 'entcore';
import http, {AxiosResponse} from 'axios';
import {Classe, ElementBilanPeriodique, evaluations, Matiere, Periode, ReleveNoteTotale, Structure} from './index';
import {AppreciationElement} from "./AppreciationElement";
import {Utils} from "./Utils";
import {getNN} from "../../utils/functions/utilsNN";


export class BilanPeriodique extends  Model {
    synchronized: any;
    periode: Periode;
    classe: Classe;
    structure: Structure;
    elements: Collection<ElementBilanPeriodique>;
    appreciations : Collection<AppreciationElement>;
    endSaisie : Boolean;


    static get api() {
        return {
            GET_ELEMENTS: '/competences/elementsBilanPeriodique?idEtablissement=' + evaluations.structure.id,
            GET_ENSEIGNANTS: '/competences/elementsBilanPeriodique/enseignants',
            GET_APPRECIATIONS: '/competences/elementsAppreciations',
            CREATE_APPRECIATIONS_SAISIE_PROJETS: '/competences/elementsAppreciationsSaisieProjet',
            CREATE_APPRECIATIONS_BILAN_PERIODIQUE: '/competences/elementsAppreciationBilanPeriodique',
            GET_SYNTHESIS: '/competences/releve/exportTotale',
            GET_HOMEWORK:"/competences/releve/export/checkDevoirs?idEtablissement=",
            GET_APPRAISALS:"/competences/recapAppreciations/print/",
            GET_SUBJECTS: "/competences/subjects/short-label/subjects?ids="
        }
    }

    constructor(periode: any, classe: Classe) {
        super();
        this.synchronized = {
            classe: false
        };

        this.periode = periode;
        this.classe = classe;

        this.structure = evaluations.structure;
    }

    syncClasse(): Promise<any> {
        return new Promise(async (resolve) => {
            if (this.classe.eleves.length() === 0) {
                await this.classe.eleves.sync();
            }
            if (this.classe.periodes.length() === 0) {
                await this.classe.periodes.sync();
            }
            this.synchronized.classe = true;
            resolve();
        });
    }

    async syncElements (param?) {
        try {
            let url = BilanPeriodique.api.GET_ELEMENTS + "&idClasse=" + this.classe.id

            if(param !== "isBilanPeriodique") {
                url = url + "&idEnseignant=" + model.me.userId;
            }

            let data = await http.get(url);
            this.elements = data.data;

            if(data.data.length > 0) {

                let url = BilanPeriodique.api.GET_ENSEIGNANTS + "?idClasse=" + this.classe.id;
                for (let i = 0; i < data.data.length; i++) {
                    url += "&idElement=" + this.elements[i].id;
                }

                let result = await http.get(url);
                _.forEach(this.elements, (element) => {
                    let enseignants = _.findWhere(result.data, {idElement: element.id});
                    if(enseignants) {
                        element.enseignants = enseignants.idsEnseignants;
                    }
                });
            }

        } catch (e) {
            notify.error('evaluations.elements.get.error');
        }
    }

    async syncAppreciations (elements, periode, classe) {
        try {
            let url = BilanPeriodique.api.GET_APPRECIATIONS + '?idPeriode=' + periode.id + '&idClasse=' + classe.id;;

            for (let i = 0; i < elements.length; i++) {
                url += "&idElement=" + elements[i].id;
            }
            let data = await http.get(url);
            this.appreciations = data.data;

            _.forEach(this.classe.eleves.all, (eleve) => {
                eleve.appreciations = [];
            });


            _.forEach(elements, (element) => {
                var elemsApprec = _.where(this.appreciations, {id_elt_bilan_periodique: element.id});
                _.forEach(elemsApprec, (elemApprec) => {
                    if(elemApprec.id_eleve === undefined){
                        if(element.appreciationClasse === undefined){
                            element.appreciationClasse = [];
                        }
                        if(element.appreciationClasse[periode.id] === undefined){
                            element.appreciationClasse[periode.id] = [];
                        }
                        element.appreciationClasse[periode.id][classe.id] = elemApprec.commentaire;
                    }
                    else {
                        _.find(this.classe.eleves.all, function(eleve){
                            if(eleve.id === elemApprec.id_eleve){

                                if(eleve.appreciations === undefined){
                                    eleve.appreciations = [];
                                }
                                if(eleve.appreciations[periode.id] === undefined){
                                    eleve.appreciations[periode.id] = [];
                                }
                                eleve.appreciations[periode.id][element.id] = elemApprec.commentaire;
                            }
                        })
                    }
                });
            });
        } catch (e) {
            notify.error('evaluations.appreciations.get.error');
        }


        if(Utils.isChefEtab()){
            this.endSaisie = false;
        }
        else {
            let period = _.findWhere(this.classe.periodes.all, {id_type: periode.id_type});
            if (period) {
                this.endSaisie = moment(period.date_fin_saisie).isBefore(moment(), "days");
            }
        }
    }

    toJSON(periode, element, eleve, classe ,structure){
        let data = {
            id_periode : periode.id,
            id_element : element.id,
            id_etablissement : structure.id
        };
        eleve ? _.extend(data, {id_eleve : eleve.id, appreciation : eleve.appreciations[periode.id][element.id], id_classe : classe.id})
            :  _.extend(data, {appreciation : element.appreciationClasse[periode.id][classe.id], id_classe : classe.id, externalid_classe : classe.externalId});

        return data;
    }

    async saveAppreciation (periode, element, eleve, classe, isBilanPeriodique) {
        try {
            if(isBilanPeriodique !== true) {
                eleve ? await http.post(BilanPeriodique.api.CREATE_APPRECIATIONS_SAISIE_PROJETS + "?type=eleve",
                    this.toJSON(periode, element, eleve, classe, this.structure))
                    : await http.post(BilanPeriodique.api.CREATE_APPRECIATIONS_SAISIE_PROJETS + "?type=classe",
                    this.toJSON(periode, element, null, classe, this.structure));
            } else {
                await http.post(BilanPeriodique.api.CREATE_APPRECIATIONS_BILAN_PERIODIQUE
                    + "?type=eleve-bilanPeriodique", this.toJSON(periode, element, eleve, classe, this.structure));
            }

        } catch (e) {
            notify.error('evaluations.appreciation.post.error');
        }
    }

    private async getHomework (parameter:any):Promise<any | Error>{
        let url:string = `${BilanPeriodique.api.GET_HOMEWORK}${parameter.idEtablissement}&idClasse=${parameter.idClasse}`;
        if(parameter.idPeriode) url += `&idPeriode=${parameter.idPeriode}`;
        const { data, status }:AxiosResponse = await http.get(url);
        if(status === 200) return data;
        throw new Error("getHomework");
    }

    private async getSynthesis(parameter: any, isAnnual: Boolean): Promise<any | Error> {
        const {data, status}: AxiosResponse = await http.post(`${BilanPeriodique.api.GET_SYNTHESIS}`, parameter);
        if (status === 200) {
            if (isAnnual) return data.annual;
            return data;
        }
        throw new Error("getSynthesis");
    }

    private async getAppraisals(idClasse:string, idPeriode:string, idStructure:string):Promise<any | Error> {
        let url:string = `${BilanPeriodique.api.GET_APPRAISALS}${idClasse}/export?text=false&idStructure=${idStructure}&json=true`;
        if(idPeriode) url += `&idPeriode=${idPeriode}`;
        const {data, status}:AxiosResponse = await http.get(url);
        if(status === 200) return data.data;
        throw new Error("getAppraisals");
    }

    private async getSubjects(subjectsSent:Array<Matiere>):Promise<any | Error>{
        const {data, status}:AxiosResponse = await http.get(`${BilanPeriodique.api.GET_SUBJECTS}${subjectsSent.join(",")}`);
        if(status === 200) return data;
        throw new Error("getAppraisals");
    }

    public async summaryEvaluations (idClass:string, idPeriod:number):Promise<void>{
        let url:string =`/competences/recapEval/print/${idClass}/export?text=false&usePerso=false`;
        if(idPeriod) url += `&idPeriode=${idPeriod}`;
        url += "&json=true";
        const { data, status }:AxiosResponse = await http.get(url);
        if(status === 200) return data;
        notify.error(lang.translate("competance.error.results.class"));
    }

    private async callsDataSynthesisAndAppraisals(parameter: any, scope: any): Promise<any> {
        try {
            return await Promise.all([
                this.getHomework(parameter),
                this.getSubjects(parameter.idMatieres),
            ])
                .then((dataResponse: any): any => dataResponse)
                .catch((error: String): void => {
                    Utils.stopMessageLoader(scope);
                    console.error(error);
                    notify.error(lang.translate("competance.error.results.class"))
                });
        } catch (errorReturn) {
            Utils.stopMessageLoader(scope);
            notify.error(lang.translate("competance.error.results.class" + errorReturn));
            return undefined;
        }
    };

    private makerHeaderWithTeachersAndSubjects (data:any, teacherBySubject):Array<any>{
        let matchingDataApi:Array<any> = [];
        const homeworks:Array<any> = data.homeworks;
        const statistics:any = data.synthesis.statistiques;
        const subjects = data.subjects;
        for (let subjectId in statistics) {
            const statistic = statistics[subjectId];
            for (let k = 0; k < homeworks.length ; k++) {
                if(homeworks[k].id_matiere === subjectId
                    && !_.contains(matchingDataApi.map(subject => subject.idSubject), subjectId)
                    && !(_.values(statistic.moyenne).every(note => note === "NN"))){  //clean column with that "NN"
                    const subject = _.values(subjects).find(subject => subject.id === homeworks[k].id_matiere);
                    matchingDataApi.push({
                        idSubject: subjectId,
                        average: statistic.moyenne,
                        teacherId : teacherBySubject && teacherBySubject[subjectId] ? teacherBySubject[subjectId].id : undefined,
                        teacherName: teacherBySubject && teacherBySubject[subjectId] && teacherBySubject[subjectId].displayName ?
                            teacherBySubject[subjectId].displayName : undefined,
                        coTeachers:  teacherBySubject && teacherBySubject[subjectId] ? teacherBySubject[subjectId].coTeachers : undefined,
                        substituteTeachers:  teacherBySubject && teacherBySubject[subjectId] ? teacherBySubject[subjectId].substituteTeachers : undefined,
                        subjectName: subject? subject.name : undefined,
                        subjectShortName:  subject? subject.libelle_court : undefined,
                        subjectRank:  subject? subject.rank : 0,
                    });
                    break;
                }
            }
        }
        return matchingDataApi.sort((subjectOne, subjectTwo) => subjectOne.subjectRank - subjectTwo.subjectRank);
    }

    private async createHeaderTable (data, teacherBysubject) {
        if(!Object.keys(data).map(element => data[element]).some(array => array.length > 0)) return [];
        let resultHeader : Array<any> = [];
        const dataSynthesis = await this.makerHeaderWithTeachersAndSubjects(data, teacherBysubject);
        const dataSynthesisClean = dataSynthesis
            .filter(subjectToSynthesis => subjectToSynthesis.subjectShortName)
            .map(subjectToSynthesis => {
                let lastName, firstName;
                let teacherName;
                if(subjectToSynthesis.teacherName){
                    [lastName, firstName] = subjectToSynthesis.teacherName.split(" ");
                    teacherName = Utils.makeShortName(lastName, firstName);
                }
                return {
                    idSubject: subjectToSynthesis.idSubject,
                    subjectName: subjectToSynthesis.subjectShortName,
                    teacherName: teacherName,
                    coTeachers: subjectToSynthesis.coTeachers,
                    substituteTeachers: subjectToSynthesis.substituteTeachers
                }
            });

        resultHeader =  [
            {
                idSubject: undefined,
                subjectName: lang.translate("student"),
                teacherName: undefined,
            },
            ...dataSynthesisClean,
            {
                idSubject: undefined,
                subjectName: lang.translate("average"),
                teacherName: undefined,
            },
        ];
        return resultHeader;
    }

    private async createBodyTable (headerArrayTeachers:Array<any>, data:Array<any>):Promise<Array<any>>{
        let resultBody:Array<any> = [];
        const students = data['students'];
        for (let i = 0; i < students.length; i++) {
            let student = students[i];
            const subjectsAverages:Array<any> = await this.aggregationNotesBySubjects(headerArrayTeachers, student.moyenneFinale);
            resultBody.push([
                `${student.lastName} ${student.firstName}`,
                ...subjectsAverages,
                student.moyenne_generale,
            ]);
        }
        return resultBody.sort((firstStudent:Array<string>, secondStudent:Array<string>):number => {
            return firstStudent[0].localeCompare(secondStudent[0])
        });
    }

    private async createFooterTable (headerArrayTeachers:Array<any>, data:Array<any>):Promise<Array<any>>{
        if(data.length === 0) return [];
        let resultFooter:Array<any> = [];
        const statistics = data['statistiques'];
        resultFooter = await this.aggregationNotesBySubjects(headerArrayTeachers, statistics);
        resultFooter = [
            ...resultFooter.map( mapRow => mapRow.moyenne),
            data['statistiques'].moyenne_generale
        ];
        return [
            [lang.translate("average"), ...resultFooter.map(averageNote => averageNote.moyenne)],
            [lang.translate("average.minimum"), ...resultFooter.map(minimumNote => minimumNote.minimum)],
            [lang.translate("average.maximum"), ...resultFooter.map(maximumNote => maximumNote.maximum)],
        ]
    }

    private async aggregationNotesBySubjects (subjectsFromHeaderTable:Array<any>, notes:object):Promise<Array<any>>{
        if(!notes) return [];
        let subjectsNotes:Array<any> = [];
        for (let i = 0; i < subjectsFromHeaderTable.length; i++) {
            let hasNoteSubject:Boolean = false;
            for(let idSubject in notes){
                const subjectFromHeader = subjectsFromHeaderTable[i];
                if(!subjectFromHeader) break;
                if(subjectFromHeader.idSubject === idSubject){
                    subjectsNotes.push(notes[idSubject]);
                    hasNoteSubject = true;
                    break;
                }
                if(!subjectFromHeader.idSubject) {
                    hasNoteSubject = true;
                    break;
                }
            }
            if(!hasNoteSubject) subjectsNotes.push(getNN());
        }
        return subjectsNotes;
    }

    public async getAppraisalsAndNotesByClassAndPeriod(idClasse:string, idPeriode:string, idStructure:string):Promise<Array<any>>{
        let result = [];
        const appraisals = await this.getAppraisals(idClasse, idPeriode, idStructure);

        result = appraisals.map(appraisal => {
            let appraisalReturned:any = {
                teacherName: "",
                subjectName: "",
                appraisal:{content_text: ""},
                average:{
                    maximum: "",
                    minimum: "",
                    moyenne: "",
                },
                coTeachers: [],
            };
            appraisalReturned.teacherName = appraisal.prof || "";
            appraisalReturned.subjectName = appraisal.mat || "";
            appraisalReturned.average.maximum = appraisal.max || "";
            appraisalReturned.average.minimum = appraisal.min || "";
            appraisalReturned.average.moyenne = appraisal.moy || "";
            appraisalReturned.appraisal.content_text = appraisal.appr || "";
            appraisalReturned.coTeachers = appraisal.coT || [];
            return appraisalReturned;
        });
        return result;
    }

    public async getAverage(data:any, teacherBysubject):Promise<any>{
        const headerTable = await this.createHeaderTable(data, teacherBysubject);
        if(headerTable)
            return {
                headerTable: headerTable,
                bodyTable: await this.createBodyTable(headerTable, data),
                footerTable: await this.createFooterTable(headerTable, data.synthesis),
            }
    }

    public makeCsv (fileName:string, dataHeader:Array<Array<any>>, dataBody:Array<Array<any>>):void{
        let csvPrepared:Array<Array<any>> = [...dataHeader, ...dataBody];
        if (csvPrepared.length > 0) {
            const blob = new Blob([`\ufeff${Utils.prepareCsvString(csvPrepared)}`], {type: ' type: "text/csv;charset=UTF-8"'});
            const link = document.createElement('a');
            link.href = (window as any).URL.createObjectURL(blob);
            link.setAttribute("target", "_blank");
            link.download = `${fileName}.csv`;
            document.body.appendChild(link);
            link.click();
        }
    }

    public async synthesisAndAppraisals(initResultPeriodic: any, scope: any): Promise<any> {
        const noteTotal: ReleveNoteTotale = new ReleveNoteTotale(initResultPeriodic);
        const isAnnual: Boolean = initResultPeriodic.idPeriode === undefined;
        const parameter: any = {
            isAnnual: isAnnual,
            idMatieres: initResultPeriodic.idMatieres,
            idClasse: noteTotal.idClasse,
            idEtablissement: noteTotal.idEtablissement,
            idPeriode: initResultPeriodic.idPeriode,
            idPeriodes: isAnnual ? initResultPeriodic.periodes : [],
            typeClasse: noteTotal.classe.type_groupe,
            idGroups: [],
        };
        const result = await this.callsDataSynthesisAndAppraisals(parameter, scope);
        if (result) {
            parameter.idGroups = this.getGroupsId(initResultPeriodic.allMatieres, result[0]);
            const synthesis = await this.getSynthesis(parameter, isAnnual)

            if (result.length === 2) {
                return {
                    homeworks: result[0],
                    synthesis: synthesis,
                    students: synthesis.eleves,
                    subjects: result[1].subjects,
                }
            }
        }
        return {
            homeworks: [],
            synthesis: [],
            students: [],
            subjects: [],
        }
    }

    private getGroupsId(allSubject, dirtyHomework): Array<String> {
        let result = new Set();
        if (dirtyHomework.length === 0 || dirtyHomework.length === 0) return [];
        allSubject.forEach(subject => {
            let cleanHomework = dirtyHomework.filter(homeworkFiltred => homeworkFiltred.id_matiere == subject.id);
            if (cleanHomework.length > 0) {
                cleanHomework.forEach(homeworkEach => {
                    result.add(homeworkEach.id_groupe);
                });
            }
        });
        return Array.from(result);
    }
}
