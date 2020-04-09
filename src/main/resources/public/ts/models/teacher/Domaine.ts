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

import {Model, Collection, _, notify} from 'entcore';
import {Mix} from 'entcore-toolkit';
import { Competence, BilanFinDeCycle, Utils,DispenseDomaine } from './index';
import http from "axios";


export class Domaine extends Model {
    domaines : Collection<Domaine>;
    competences : Collection<Competence>;
    id : number;
    niveau : number;
    id_parent : number;
    moyenne : number;
    bfc : BilanFinDeCycle;
    libelle : string;
    codification : string;
    composer : any;
    evaluated : boolean;
    visible : boolean;
    id_eleve : string;
    id_chef_etablissement : string;
    id_etablissement : string;
    dispensable: boolean;
    //dispense_eleve : DispenseDomaine;
    dispense_eleve : boolean;


    /**
     * Méthode activant l'affichage des sous domaines d'un domaine
     *
     * @pbMesEvaluations booleen indiquant d'afficher ou non un domaine
     *
     */
    setVisibleSousDomaines (pbVisible) {
        Utils.setVisibleSousDomainesRec(this.domaines, pbVisible);
    }


    async saveDispenseEleve() {
        let dispenseDomaineEleve = new DispenseDomaine(this.id,this.id_eleve,this.dispense_eleve,
            this.id_etablissement);
        try{
        await dispenseDomaineEleve.save();
        }catch(e){
            this.dispense_eleve = dispenseDomaineEleve.dispense;
        }
    }

    constructor (poDomaine?,id_eleve?: string) {
        super();
        if(id_eleve) poDomaine.id_eleve = id_eleve;
        if(poDomaine.dispense_eleve === null){
            poDomaine.dispense_eleve = false;
        }

        this.collection(Competence);
        this.collection(Domaine);

        let sousDomaines = poDomaine.domaines;
        let sousCompetences = poDomaine.competences;
        if(sousDomaines !== undefined){
            for (let i=0; i< sousDomaines.length; i++){
                sousDomaines[i].id_eleve = poDomaine.id_eleve;
                if(sousDomaines[i].dispense_eleve === null){
                    sousDomaines[i].dispense_eleve = false;
                }
            }
        }
        this.updateData(poDomaine, false);

        if(sousDomaines !== undefined) {
            this.domaines.load(sousDomaines);
        }

        if(sousCompetences !== undefined) {
            this.competences.load(sousCompetences);
        }
    }


}