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

import {Model, Collection, _, model, notify, idiom as lang} from 'entcore';
import {
    CompetenceNote,
    Domaine,
    Periode,
    Classe,
    Cycle,
    BilanFinDeCycle,
    TableConversion,
    Eleve,
    Structure,
    Utils,
    BaremeBrevetEleves,
    LanguesCultRegs,
    LangueCultReg,
    BfcSynthese,
    Matiere,
    EleveEnseignementCpl, EnsCpls, EnsCpl, NiveauEnseignementCpls,
} from './index';
import {Enseignement} from "../parent_eleve/Enseignement";
import http from 'axios';
import {getTitulairesForRemplacantsCoEnseignant} from "../../utils/functions/getTitulairesForRemplacantsCoEnseignant";

export class SuiviCompetence extends Model {
    competenceNotes: Collection<CompetenceNote>;
    domaines: Collection<Domaine>;
    enseignements: Collection<Enseignement>;
    periode: Periode;
    classe: Classe;
    cycle: Cycle;
    isCycle: boolean;
    bilanFinDeCycles: Collection<BilanFinDeCycle>;
    tableConversions: Collection<TableConversion>;
    bfcSynthese: BfcSynthese;
    ensCpls: EnsCpls;
    ensCplSelected: EnsCpl;
    langues : LanguesCultRegs;
    langueSelected : LangueCultReg;
    eleveEnsCpl: EleveEnseignementCpl;
    niveauEnsCpls : NiveauEnseignementCpls;
    baremeBrevetEleves : BaremeBrevetEleves;
    matieres: Collection<Matiere>;

    static get api() {
        return {
            getCompetencesNotes : '/competences/competence/notes/eleve/',
            getArbreDomaines : '/competences/domaines?idClasse=',
            getDomainesBFC : '/competences/bfc/eleve/',
            getCompetenceNoteConverssion : '/competences/competence/notes/bilan/conversion'
        };
    }
    that = this;
    constructor (eleve: Eleve, periode: any, classe: Classe, cycle: Cycle, isCycle: boolean, structure: Structure) {
        super();
        this.periode = periode;
        this.classe = classe;
        this.cycle = cycle;
        this.isCycle = isCycle;
        this.bfcSynthese = new BfcSynthese(eleve.id, cycle.id_cycle);
        this.bfcSynthese.syncBfcSynthese();
        this.ensCpls = new EnsCpls();
        this.langues = new LanguesCultRegs();
        this.eleveEnsCpl = new EleveEnseignementCpl(eleve.id, cycle.id_cycle);
        this.niveauEnsCpls = new NiveauEnseignementCpls();
        this.baremeBrevetEleves = new BaremeBrevetEleves();

        let that = this;
        this.collection(TableConversion);
        this.collection(Matiere);
        this.collection(Domaine, {
            sync: () => {
                return new Promise(async (resolve) => {
                    if(cycle.id_cycle != null) {
                        let urlGetArbreDomaines = `${SuiviCompetence.api.getArbreDomaines + classe.id}&idEleve=${
                            eleve.id}&idCycle=${cycle.id_cycle}`;

                        let response = await Promise.all([http.get(urlGetArbreDomaines),
                            this.getCompetencesNotes(eleve, periode)]);

                        let resDomaines = response[0].data;
                        let resCompetencesNotes = response[1].data;
                        let listTeacher = getTitulairesForRemplacantsCoEnseignant(model.me.userId, this.classe);
                        if (resDomaines) {
                            this.domaines.all.length = 0;
                            for (let i = 0; i < resDomaines.length; i++) {
                                let domaine = new Domaine(resDomaines[i], eleve.id);
                                if (this.bilanFinDeCycles !== undefined && this.bilanFinDeCycles.all.length > 0) {
                                    let tempBFC = _.findWhere(this.bilanFinDeCycles.all, {id_domaine: domaine.id});
                                    if (tempBFC !== undefined) {
                                        domaine.bfc = tempBFC;
                                    }
                                }
                                domaine.id_chef_etablissement = model.me.userId;
                                domaine.id_etablissement = structure.id;
                                this.domaines.all.push(domaine);
                                Utils.setCompetenceNotes(domaine, resCompetencesNotes, this.tableConversions,
                                    this.domaines, null,undefined , this.isCycle, periode, listTeacher);
                            }
                        }
                        if (resolve && typeof (resolve) === 'function') {
                            resolve();
                        }
                    }
                });
            }
        });
        this.collection(BilanFinDeCycle, {
            sync: function () {
                return new Promise(async (resolve) => {
                    let url = `${SuiviCompetence.api.getDomainesBFC + eleve.id}?idEtablissement=${
                        structure.id}&idCycle=${cycle.id_cycle}`;
                    let res = await http.get(url);
                    let resBFC = res.data;
                    if (resBFC) {
                        for (let i = 0; i < resBFC.length; i++) {
                            let BFC = new BilanFinDeCycle(resBFC[i]);
                            that.bilanFinDeCycles.all.push(BFC);
                        }
                    }
                    if (resolve && typeof (resolve) === 'function') {
                        resolve();
                    }
                });
            }
        });

        this.collection(Enseignement, {
            sync: async () => {
                return new Promise( async (resolve) => {
                    let response: any = await Promise.all([

                        Enseignement.getAll(classe.id, structure.id, (cycle.id_cycle !== undefined && cycle.id_cycle !== null ) ?
                            cycle.id_cycle: classe.id_cycle , this.enseignements),
                        this.getCompetencesNotes(eleve, periode)
                    ]);
                    this.enseignements.load(response[0].data);
                    let competences = response[1].data;
                    if(structure.matieres.all !== undefined)this.matieres.load(structure.matieres.all);
                    await Enseignement.loadCompetences(classe.id, structure.id, competences, classe.id_cycle, this.enseignements);
                    resolve();
                });
            }
        });
    }
    /**
     * Calcul la moyenne d'un domaine (moyenne des meilleurs évaluations de chaque compétence)
     *
     */
    setMoyenneCompetences () {
        for (let i = 0; i < this.domaines.all.length; i++) {
            let oEvaluationsArray = [];
            let oDomaine = this.domaines.all[i] as Domaine;

            // recherche de toutes les évaluations du domaine et ses sous domaines
            // (uniquement les max de chaque compétence si la compétence a été évalué au cours de l'année,
            // sinon la note de la compétence obtenue sur la dernière année)
            Utils.getMaxEvaluationsDomaines(oDomaine, oEvaluationsArray,
                this.tableConversions.all, false, this.bilanFinDeCycles, this.classe, this);
        }
    }

    findCompetence (idCompetence) {
        for (let i = 0; i < this.domaines.all.length; i++) {
            let comp = Utils.findCompetenceRec(idCompetence, this.domaines.all[i]);
            if (comp !== undefined) {
                return comp;
            }
        }
        return false;
    }

    getConversionTable(idetab, idClasse, mapCouleur): Promise<any> {
        return new Promise(async (resolve) => {
            let that = this;
            let uri = SuiviCompetence.api.getCompetenceNoteConverssion + '?idEtab=' + idetab + '&idClasse=' + idClasse;
            let response = await  http.get(uri);
            let data = response.data;
            _.map(data, (_d) => {
                _d.couleur = mapCouleur[_d.ordre - 1];
            });
            that.tableConversions.load(data);

            if (resolve && (typeof (resolve) === 'function')) {
                resolve(data);
            }
        });
    }

    getReleve (idPeriode, idEleve, idTypePeriode, ordrePeriode, idStructure) {
        let uri = `/competences/releve/pdf?idEtablissement=${idStructure}&idEleve=${idEleve}`;

        if (idPeriode !== undefined && idPeriode !== null) {
            uri += '&idPeriode=' + idPeriode;
            if (idTypePeriode !== undefined) {
                uri += '&idTypePeriode=' + idTypePeriode;
                if (ordrePeriode !== undefined) {
                    uri += '&ordrePeriode=' + ordrePeriode;
                }
            }
        }

        location.replace(uri);
    }

    async getClasseReleve(idPeriode, idClasse, idTypePeriode, ordrePeriode, idStructure, classeName) {
        await Utils.getClasseReleve(idPeriode, idClasse, idTypePeriode, ordrePeriode, idStructure, classeName);
    }

    async getCompetencesNotes (eleve: Eleve, periode : any): Promise<any> {
        let url = SuiviCompetence.api.getCompetencesNotes + eleve.id + '?idCycle=' + this.cycle.id_cycle;

        if (periode !== null && periode !== undefined && periode !== '*') {
            if (periode.id_type) url += '&idPeriode=' + periode.id_type;
        }
        if (this.isCycle !== null && this.isCycle !== undefined) {
            url += '&isCycle=' + this.isCycle;
        }
        return http.get(`${url}`);
    };

    sync (): Promise<any> {
        return new Promise((resolve) => {
            resolve();
        });
    }
}