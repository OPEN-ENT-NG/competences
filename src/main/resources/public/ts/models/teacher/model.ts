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

import { Model, Collection, http, model, _ } from 'entcore';
import {
    Annotation,
    Classe,
    Devoir,
    Devoirs,
    Eleve,
    Enseignement,
    Matiere,
    ReleveNote,
    Structure,
    Type,
    Periode
} from './index';

declare let require: any;

export class Evaluations extends Model {
    periodes : Collection<Periode>;
    types : Collection<Type>;
    devoirs : Devoirs;
    enseignements : Collection<Enseignement>;
    matieres : Collection<Matiere>;
    releveNotes : Collection<ReleveNote>;
    classes : Collection<Classe>;
    annotations: Collection<Annotation>;
    annotationsfull: Collection<Annotation>;
    structure : Structure;
    synchronized : any;
    competencesDevoir : any[];
    structures : Collection<Structure>;
    eleves : Collection<Eleve>;
    domainesEnseignements : any[];
    sousDomainesEnseignements : any[];

    constructor () {
        super();
        this.synchronized = {
            devoirs : false,
            classes : false,
            matieres : false,
            types : false
        };
        // On charge les établissements de l'utilisateur
        let structuresTemp = [];
        this.collection(Devoir);
        this.collection(Enseignement);
        this.collection(Matiere);
        this.collection(ReleveNote);
        this.collection(Classe);
        this.collection(Periode);
        this.collection(Type);
        this.collection(Structure, {
            sync : function () {
                return new Promise((resolve, reject) => {
                    http().getJson('/viescolaire/user/structures/actives?module=notes').done(function (idsEtablissementActifs) {
                        //On récupère tout d'abord la liste des établissements actifs
                        if(idsEtablissementActifs.length > 0) {
                            for (let i = 0; i < model.me.structures.length; i++) {
                                let isEtablissementActif = (_.findWhere(idsEtablissementActifs, {id_etablissement: model.me.structures[i]}) !== undefined);
                                if (isEtablissementActif) {
                                    let structure = {
                                        id : model.me.structures[i],
                                        libelle : model.me.structureNames[i]
                                    };
                                    structuresTemp.push(structure);
                                }
                            }
                            evaluations.structures.load(structuresTemp);
                            evaluations.structure = evaluations.structures.first();

                            if (evaluations.structure !== undefined){
                                resolve();
                            }
                        } else {
                            reject();
                        }

                    }.bind(this));
                })
            }
        });
    }

    async sync () : Promise<any> {
        try {
            await this.structures.sync();
            return;
        } catch (e) {
            throw e;
        }
    }
}

export let evaluations = new Evaluations();

model.build = function () {
    require('angular-chart.js');
    (this as any).evaluations = evaluations;
};