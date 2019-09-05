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

import { Model, Collection } from 'entcore';
import {Domaine, CompetenceNote, Periode, Classe, Utils, Matiere, Structure} from './index';
import {Enseignement} from "../parent_eleve/Enseignement";
import http from "axios";

export class SuiviCompetenceClasse extends Model {
    domaines : Collection<Domaine>;
    enseignements: Collection<Enseignement>;
    competenceNotes : Collection<CompetenceNote>;
    periode : Periode;
    matieres: Collection<Matiere>;

    get api() {
        return {
            getCompetencesNotesClasse : '/competences/competence/notes/classe/',
            getArbreDomaines : '/competences/domaines?idClasse='
        }
    }

    constructor (classe : Classe, periode : any,structure : Structure) {
        super();
        this.periode = periode;
        var that = this;
        this.collection(Matiere);

        this.collection(Domaine, {
            sync: () => {
                return new Promise(async (resolve, reject) => {
                    try {
                        let response = await Promise.all([
                            http.get(`${this.api.getArbreDomaines + classe.id}`),
                            this.getCompetencesNotesClasse(classe, periode)]);

                        let resDomaines = response[0].data;
                        let resCompetencesNotes = response[1].data;

                        if(resDomaines) {
                            for(let i=0; i<resDomaines.length; i++) {
                                let domaine = new Domaine(resDomaines[i]);
                                this.domaines.all.push(domaine);
                                Utils.setCompetenceNotes(domaine, resCompetencesNotes, this.domaines, classe);
                            }
                        }
                        if (resolve && typeof (resolve) === 'function') {
                            resolve();
                        }
                    }
                    catch (e) {
                        reject(e);
                    }
                });
            }
        });
        this.collection(Enseignement, {
            sync: async () => {
                return new Promise(async (resolve ) => {
                    let response = await Promise.all([
                        Enseignement.getAll(classe.id, classe.id_cycle, this.enseignements),
                        this.getCompetencesNotesClasse(classe, periode)
                    ]);
                    this.enseignements.load(response[0].data);
                    let competences = response[1].data;
                    if(structure.matieres.all !== undefined)this.matieres.load(structure.matieres.all);
                    await Enseignement.loadCompetences(classe.id, competences, classe.id_cycle, this.enseignements);
                    resolve();
                });
            }
        });
    }

    findCompetence (idCompetence) {
        for(var i=0; i<this.domaines.all.length; i++) {
            var comp = Utils.findCompetenceRec(idCompetence, this.domaines.all[i].competences);
            if(comp !== undefined) {
                return comp;
            }
        }
        return false;
    }

    async getCompetencesNotesClasse (classe : Classe, periode : any): Promise<any> {
        let urlComp = this.api.getCompetencesNotesClasse + classe.id + "/" + classe.type_groupe;
        if (periode !== null && periode !== undefined && periode !== '*') {
            if(periode.id_type !== undefined && periode.id_type !== null){
                urlComp += "?idPeriode="+periode.id_type;
            }
        }
        return http.get(`${urlComp}`);
    };

    sync () : Promise<any> {
        return new Promise((resolve, reject) => {
            resolve();
        });
    }
}