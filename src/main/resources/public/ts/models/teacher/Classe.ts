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

import {Model, Collection, http, idiom as lang, _} from 'entcore';
import { Eleve, Periode, SuiviCompetenceClasse, Utils, BaremeBrevetEleves } from './index';
import * as utils from '../../utils/teacher';
declare let bundle:any;

export class Classe extends Model {
    eleves : Collection<Eleve>;
    id : string;
    name : string;
    type_groupe : number;
    periodes : Collection<Periode>;
    type_groupe_libelle : string;
    suiviCompetenceClasse : Collection<SuiviCompetenceClasse>;
    mapEleves : any;
    remplacement: boolean;
    id_cycle: any;
    selected : boolean;
    baremeBrevetEleves : BaremeBrevetEleves;
    synchronized : {
        eleves :  boolean,
        periodes:  boolean
    };

    public static  libelle = {
        CLASSE:'Classe',
        GROUPE: "Groupe d'enseignement",
        GROUPE_MANUEL: "Groupe manuel"
    };

    public static type = {
        CLASSE: 0,
        GROUPE: 1,
        GROUPE_MANUEL: 2
    };

    get api () {
        return {
            syncClasse: '/viescolaire/classes/' + this.id + '/users?type=Student',
            syncGroupe : '/viescolaire/groupe/enseignement/users/' + this.id + '?type=Student',
            syncClasseChefEtab : '/viescolaire/classes/'+this.id+'/users',
            syncPeriode : '/viescolaire/periodes?idGroupe=' + this.id
        }
    }

    constructor (o? : any) {
        super();
        let synchronizeObject = {eleves: false, periodes: false };
        if (o !== undefined) {
            o = _.extend(o, {synchronized: synchronizeObject});
            this.updateData(o, false);
        }
        else {
            this.synchronized = synchronizeObject;
        }
        this.collection(Eleve, {
            sync : () : Promise<any> => {
                return new Promise((resolve, reject) => {
                    this.mapEleves = {};
                    let url;
                    if(Utils.isChefEtab(this)){
                        url = this.type_groupe !== Classe.type.CLASSE ?
                            this.api.syncGroupe : this.api.syncClasseChefEtab;
                    }else {
                        url = this.type_groupe !== Classe.type.CLASSE ? this.api.syncGroupe : this.api.syncClasse;
                    }
                    http().getJson(url).done((data) => {
                        // On tri les élèves par leur lastName en ignorant les accents
                        utils.sortByLastnameWithAccentIgnored(data);
                        _.forEach(data, (_d) => {
                           _d.idClasse = this.id;
                           _d.selected = false;
                        });
                        this.eleves.load(data);
                        for (var i = 0; i < this.eleves.all.length; i++) {
                            this.mapEleves[this.eleves.all[i].id] = this.eleves.all[i];
                        }
                        this.trigger('sync');
                        this.synchronized.eleves = true;
                        resolve();
                    });
                });
            }
        });
        this.collection(SuiviCompetenceClasse);
        this.collection(Periode, {
            sync : async (): Promise<any> => {
                return new Promise((resolve, reject) => {
                    http().getJson(this.api.syncPeriode).done((res) => {
                        res.push({id: null});
                        this.periodes.load(res);
                        this.synchronized.periodes = true;
                        resolve();
                    }).error( (res) =>{
                        this.periodes.load([]);
                        this.synchronized.periodes = true;
                        resolve();
                    });
                });
            }
        });
    }

    public static get_type_groupe_libelle = (classe) => {
        let libelleClasse;

        if ( classe.type_groupe === Classe.type.CLASSE) {
            libelleClasse = Classe.libelle.CLASSE;
        } else if ( classe.type_groupe === Classe.type.GROUPE) {
            libelleClasse = Classe.libelle.GROUPE;
        }else if ( classe.type_groupe === Classe.type.GROUPE_MANUEL) {
            libelleClasse = Classe.libelle.GROUPE_MANUEL;
        }
        return libelleClasse;

    }

    filterEvaluableEleve (periode) {
        let res = _.omit(this, 'eleves');

        if (periode !== undefined) {
            res.eleves = {
                all: _.reject(this.eleves.all, function (eleve) {
                    return !eleve.isEvaluable(periode);
                })
            };
        }
        else {
            res.eleves = this.eleves;
        }
        return res;
    };

}