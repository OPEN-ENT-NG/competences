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

import {_, http, idiom as lang, Model} from 'entcore';
import {Classe, Eleve, evaluations, Structure, SuivisDesAcquis, TypePeriode} from "./index";
import {Defaultcolors} from "../eval_niveau_comp";
import {SyntheseBilanPeriodique} from "./SyntheseBilanPeriodique";
import {AppreciationCPE} from "./AppreciationCPE";
import {AvisConseil} from "./AvisConseil";
import {Graph} from "../common/Graph";


declare  let Chart: any;

export class ElementBilanPeriodique extends Model {
    suivisAcquis : SuivisDesAcquis;
    projet : object;
    vieScolaire : object;
    graphique : object;
    synchronized: any;
    elementProgramme: any;
    idTypePeriode : number;
    typePeriode : TypePeriode[];
    idPeriode : number;
    classe: Classe;
    eleve: Eleve;
    structure: Structure;
    syntheseBilanPeriodique : SyntheseBilanPeriodique;
    appreciationCPE : AppreciationCPE;
    avisConseil : AvisConseil;
    graph: Graph;

    get api() {
        return {
            GET_DATA_FOR_GRAPH: `/competences/bilan/periodique/datas/graph?idEtablissement=${this.structure.id}&idClasse=${
                this.classe.id}&typeClasse=${this.classe.type_groupe}`,
            GET_DATA_FOR_GRAPH_DOMAINE: `/competences/bilan/periodique/datas/graph/domaine?idEtablissement=${
                this.structure.id}&idClasse=${this.classe.id}&typeClasse=${this.classe.type_groupe}`
        }
    }


     constructor(pClasse, pEleve, pIdTypePeriode,pStructure, pTypePeriode) {
        super();
        this.structure = pStructure;
        this.classe = pClasse;
        this.eleve = pEleve;
        this.idTypePeriode = pIdTypePeriode;
        this.idPeriode = this.idTypePeriode;
        this.typePeriode = pTypePeriode;
            this.suivisAcquis = new SuivisDesAcquis(this.eleve.id, this.classe.id, this.structure.id,
                this.idTypePeriode, this.typePeriode);
    }


    async getDataForGraph(eleve, forDomaine?) {
        await Graph.getDataForGraph(this, eleve, forDomaine);
    }

}