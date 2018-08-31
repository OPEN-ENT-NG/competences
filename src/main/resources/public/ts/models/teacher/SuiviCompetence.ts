import { Model, Collection, http, _, model } from 'entcore';
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

export class SuiviCompetence extends Model {
    competenceNotes: Collection<CompetenceNote>;
    domaines: Collection<Domaine>;
    periode: Periode;
    classe: Classe;
    matieres: Collection<Matiere>;
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
    static get api() {
        return {
            getCompetencesNotes : '/competences/competence/notes/eleve/',
            getArbreDomaines : '/competences/domaines?idClasse=',
            getDomainesBFC : '/competences/bfc/eleve/',
            getCompetenceNoteConverssion : '/competences/competence/notes/bilan/conversion'
        };
    }
    that = this;
     constructor (eleve: Eleve, periode: any, classe: Classe, cycle: Cycle, isCycle: boolean, structure: Structure,matieres: any) {
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
        this.collection(Domaine, {
            sync: function () {
                return new Promise((resolve) => {
                    let url = SuiviCompetence.api.getArbreDomaines + that.classe.id + '&idEleve=' + eleve.id + '&idCycle=' + cycle.id_cycle;
                    http().getJson(url).done((resDomaines) => {
                        let url = SuiviCompetence.api.getCompetencesNotes + eleve.id + '?idCycle=' + cycle.id_cycle;

                        if (periode !== null && periode !== undefined && periode !== '*') {
                            if (periode.id_type) url += '&idPeriode=' + periode.id_type;
                        }
                        if (isCycle !== null && isCycle !== undefined) {
                            url += '&isCycle=' + isCycle;
                        }
                        if(matieres !== undefined
                            && matieres.all !== undefined
                            && matieres.all.length > 0 ){
                            for (let i = 0; i < matieres.all.length; i++) {
                                url += '&idMatiere='+ matieres.all[i].id;
                            }
                        }
                        url += "&idClasse="+ that.classe.id ;


                        http().getJson(url).done((resCompetencesNotes) => {
                            if (resDomaines) {
                                for (let i = 0; i < resDomaines.length; i++) {
                                    let domaine = new Domaine(resDomaines[i], eleve.id);
                                    if ( that.bilanFinDeCycles !== undefined && that.bilanFinDeCycles.all.length > 0 ) {
                                        let tempBFC = _.findWhere(that.bilanFinDeCycles.all, {id_domaine : domaine.id});
                                        if (tempBFC !== undefined) {
                                            domaine.bfc = tempBFC;
                                        }
                                    }

                                    domaine.id_chef_etablissement = model.me.userId;
                                    domaine.id_etablissement = structure.id;
                                    that.domaines.all.push(domaine);
                                    Utils.setCompetenceNotes(domaine, resCompetencesNotes, this, null);
                                }
                            }
                            if (resolve && typeof (resolve) === 'function') {
                                resolve();
                            }
                        });
                    });
                });
            }
        });

        this.collection(BilanFinDeCycle, {
            sync: function () {
                return new Promise((resolve) => {
                    let url = SuiviCompetence.api.getDomainesBFC + eleve.id + '?idEtablissement=' + structure.id  + "&idCycle=" +cycle.id_cycle;
                    http().getJson(url).done((resBFC) => {
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
            // (uniquement les max de chaque compétence)
            Utils.getMaxEvaluationsDomaines(oDomaine, oEvaluationsArray,
                this.tableConversions.all, false, this.bilanFinDeCycles, this.classe);
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
        return new Promise((resolve) => {
            let that = this;
            let uri = SuiviCompetence.api.getCompetenceNoteConverssion + '?idEtab=' + idetab + '&idClasse=' + idClasse;
            http().getJson(uri).done(function(data) {
                _.map(data, (_d) => {
                    _d.couleur = mapCouleur[_d.ordre - 1];
                });
                that.tableConversions.load(data);

                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    sync (): Promise<any> {
        return new Promise((resolve) => {
            resolve();
        });
    }
}