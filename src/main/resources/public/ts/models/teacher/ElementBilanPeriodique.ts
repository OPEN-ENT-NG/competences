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

import {Model, notify} from 'entcore';
import {Classe, Eleve,  Structure, SuivisDesAcquis, TypePeriode} from "./index";
import {SyntheseBilanPeriodique} from "./SyntheseBilanPeriodique";
import {AppreciationCPE} from "./AppreciationCPE";
import {AvisConseil} from "./AvisConseil";
import {AvisOrientation} from "./AvisOrientation";
import {Graph} from "../common/Graph";
import http from "axios";

export class ElementBilanPeriodique extends Model {
    suivisAcquis : SuivisDesAcquis;
    projet : object;
    vieScolaire : object;
    graphique : object;
    synchronized: any;
    elementProgramme: any;
    typePeriode : TypePeriode[];
    idPeriode : number;
    classe: Classe;
    eleve: Eleve;
    structure: Structure;
    syntheseBilanPeriodique : SyntheseBilanPeriodique;
    appreciationCPE : AppreciationCPE;
    avisConseil : AvisConseil;
    avisOrientation : AvisOrientation;
    graph: Graph;

    get api() {
        return {
            GET_DATA_FOR_GRAPH: `/competences/bilan/periodique/datas/graph?idEtablissement=${this.structure.id}&idClasse=${
                this.classe.id}&typeClasse=${this.classe.type_groupe}`,
            GET_DATA_FOR_GRAPH_DOMAINE: `/competences/bilan/periodique/datas/graph/domaine?idEtablissement=${
                this.structure.id}&idClasse=${this.classe.id}&typeClasse=${this.classe.type_groupe}`,
            GET_DATA_FOR_AVIS_SYNTHESES: `/competences/bilan/periodique/datas/avis/synthses?idEtablissement=${
                this.structure.id}&idEleve=${this.eleve.id}`
        }
    }


    constructor(pClasse, pEleve, pIdPeriode, pStructure, pTypePeriode) {
        super();
        this.structure = pStructure;
        this.classe = pClasse;
        this.eleve = pEleve;
        this.idPeriode = pIdPeriode;
        this.typePeriode = pTypePeriode;
        this.suivisAcquis = new SuivisDesAcquis(this.eleve ? this.eleve.id : undefined,
            this.classe ? this.classe.id : undefined, this.structure ? this.structure.id : undefined,
            this.idPeriode, this.typePeriode);
    }


    async getDataForGraph(eleve, forDomaine?, niveauCompetences?, idPeriode?) {
        if(idPeriode !== undefined) {
            this.idPeriode = idPeriode;
        }
        await Graph.getDataForGraph(this, eleve, forDomaine, niveauCompetences);
    }

    async getAllAvisSyntheses() {
        try {
            let data = await http.get(this.api.GET_DATA_FOR_AVIS_SYNTHESES);
            return data.data;
        } catch (e) {
            notify.error('evaluations.avis.synthses.bilan.periodique.get.error');
        }
    }
}