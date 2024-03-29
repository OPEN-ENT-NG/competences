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

import {Model, Collection, _, notify, http as httpCore, moment, skin} from 'entcore';
import http  from 'axios';
import {Devoirs, Evaluation, SuiviCompetence} from './index';
import {ElementBilanPeriodique} from "./ElementBilanPeriodique";
import {ExportBulletins} from "../common/ExportBulletins";
import {DefaultEleve} from "../common/DefaultEleve";

export class Eleve extends DefaultEleve  {
    moyenne: number;
    evaluations : Collection<Evaluation>;
    evaluation : Evaluation;
    suiviCompetences : Collection<SuiviCompetence>;
    idEtablissement :string;
    details : any;
    cycles : any;
    deleteDate : any;
    elementBilanPeriodique : ElementBilanPeriodique[];
    evenements : any;
    evenement : any;
    selected : boolean;
    classeName : string;
    img : string;

    get api() {
        return {
            GET_MOYENNE : `/competences/eleve/${this.id}/moyenne?`,
            GET_DATA_FOR_DETAILS_RELEVE: `/competences/releve/informations/eleve/${this.id}`,
            GET_CYCLES : `/competences/cycles/eleve/`,
            GET_RETARDS_AND_ABSENCES: `/competences/eleve/evenements/${this.id}`,
            POST_RETARDS_OR_ABSENCES: `/competences/eleve/evenements`,
        }
    }

    constructor (o?: any) {
        super();
        if (o) {
            this.updateData(o, false);
        }
        this.collection(Evaluation);
        this.collection(SuiviCompetence);
    }
    toString () {
        return this.hasOwnProperty("displayName") ? this.displayName : this.firstName+" "+this.lastName;
    }

    getMoyenne (devoirs:any, idEtablissement:string, idClasse:string, idPeriode:string, idMatiere?:string) : Promise<any> {
        return new Promise((resolve, reject) => {
            if (devoirs) {
                let idDevoirsURL:string = "";
                let filteredEvaluations : Devoirs = devoirs.filter ( (devoir) => { return !devoir.formative});
                _.each(_.pluck(filteredEvaluations,'id'), (id) => {
                    idDevoirsURL += "devoirs=" + id + "&";
                });
                idDevoirsURL = idDevoirsURL.slice(0, idDevoirsURL.length - 1);
                let idMatiereURL:string = (idMatiere != null ? "&idMatiere=" + idMatiere : "");
                let idEtablissementURL:string = "&idEtablissement=" + idEtablissement;
                let idClasseURL:string = "&idClasse=" + idClasse;
                let idPeriodeURL:string = "&idPeriode=" + idPeriode;
                httpCore().getJson(this.api.GET_MOYENNE + idDevoirsURL + idMatiereURL + idEtablissementURL + idClasseURL + idPeriodeURL)
                    .done(function (res) {
                    if (!res.error) {
                        if (!res.hasNote) {
                            this.moyenne = "NN";
                            this.hasNote = res.hasNote;
                        }
                        else {
                            this.moyenne = res.moyenne;
                        }
                    } else {
                        this.moyenne = "";
                    }
                    if(resolve && typeof(resolve) === 'function'){
                        resolve();
                    }
                }.bind(this));
            }
        });
    }

    getDetails (idEtablissement, idClasse, idMatiere) : Promise<any> {
        return new Promise( ((resolve, reject) => {
            let uri = this.api.GET_DATA_FOR_DETAILS_RELEVE
                + `?idEtablissement=${idEtablissement}&idClasse=${idClasse}&idMatiere=${idMatiere}`;
            httpCore().getJson(uri).done( (res) => {
                if (!res.error) {
                    this.details = res;
                    resolve ();
                }
            });
        }))
    }

    async getCycles () {
        try {
            let {data} = await http.get(this.api.GET_CYCLES + this.id);
            if (!data.error) {
                this.cycles = data;
            }
        } catch (e) {
            notify.error('evaluations.eleve.cycle.get.error');
        }

    }

    isEvaluable(periode) {
        if (this.deleteDate === null || this.deleteDate === undefined) {
            return true;
        }
        else if(periode === undefined) {
            return true;
        }
        else if (periode.id === null) {
            return true;
        }
        else {
            let deleteDate = moment(this.deleteDate);
            let start = moment(periode.timestamp_dt);
            let end = moment(periode.timestamp_fn);

            return deleteDate.isBetween(start,end) || deleteDate.isAfter(end);
        }
    }

    getEvenements (idStructure:string) {
        return new Promise( ((resolve) => {
            httpCore().getJson(this.api.GET_RETARDS_AND_ABSENCES+`?idEtablissement=${idStructure}&idClasse=${this.idClasse}`).done((res) => {
                if (!res.error) {
                    this.evenements = res;
                    resolve();
                }
            });
        }))
    }

    async setColonne(colonne, idPeriode) {
        let data = {
            idEleve: this.id,
            colonne : colonne,
            idPeriode: idPeriode,
            value: this.evenement[colonne]
        };

        try {
            await http.post(this.api.POST_RETARDS_OR_ABSENCES, data);
        } catch (e) {
            notify.error(e);
        }
    }

    getAvatar(idStructure) {
        this.img = `/viescolaire/structures/${idStructure}/students/${this.id}/picture`;
    }
}