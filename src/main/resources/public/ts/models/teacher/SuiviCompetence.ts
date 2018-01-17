import { Model, Collection, http, _, model } from 'entcore';
import { EleveEnseignementCpl, EnsCpls, EnsCpl } from '../eval_ens_complement_mdl';
import { BfcSynthese }from '../eval_bfc_synthese_mdl';
import {
    CompetenceNote,
    Domaine,
    Periode,
    Classe,
    BilanFinDeCycle,
    TableConversion,
    Eleve,
    Structure,
    Utils
} from './index';

export class SuiviCompetence extends Model {
    competenceNotes : Collection<CompetenceNote>;
    domaines : Collection<Domaine>;
    periode : Periode;
    classe : Classe;
    bilanFinDeCycles : Collection<BilanFinDeCycle>;
    tableConversions : Collection<TableConversion>;
    bfcSynthese : BfcSynthese;
    ensCpls : EnsCpls;
    ensCplSelected : EnsCpl;
    eleveEnsCpl : EleveEnseignementCpl;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    get api() {
        return {
            getCompetencesNotes : '/competences/competence/notes/eleve/',
            getArbreDomaines : '/competences/domaines/classe/',
            getDomainesBFC : '/competences/bfc/eleve/',
            getCompetenceNoteConverssion : '/competences/competence/notes/bilan/conversion'
        }
    }
    that = this;
    constructor (eleve : Eleve, periode : any, classe : Classe, structure :Structure) {
        super();
        this.periode = periode;
        this.classe = classe;
        this.bfcSynthese= new BfcSynthese(eleve.id);
        this.bfcSynthese.syncBfcSynthese();
        this.ensCpls = new EnsCpls();
        this.eleveEnsCpl = new EleveEnseignementCpl(eleve.id);

        var that = this;
        this.collection(TableConversion);
        this.collection(Domaine, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    var url = that.api.getArbreDomaines + that.classe.id;
                    http().getJson(url).done((resDomaines) => {
                        var url = that.api.getCompetencesNotes + eleve.id;
                        if (periode !== null && periode !== undefined && periode !== '*') {
                            if(periode.id_type) url += "?idPeriode=" + periode.id_type;
                        }
                        http().getJson(url).done((resCompetencesNotes) => {
                            if (resDomaines) {
                                for (var i = 0; i < resDomaines.length; i++) {
                                    var domaine = new Domaine(resDomaines[i]);
                                    if(that.bilanFinDeCycles !== undefined && that.bilanFinDeCycles.all.length>0){
                                        let tempBFC = _.findWhere(that.bilanFinDeCycles.all, {id_domaine : domaine.id});
                                        if(tempBFC !== undefined){
                                            domaine.bfc = tempBFC;
                                        }
                                    }
                                    domaine.id_eleve = eleve.id;
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
                return new Promise((resolve, reject) => {
                    var url = that.api.getDomainesBFC + eleve.id +'?idEtablissement=' + structure.id;
                    http().getJson(url).done((resBFC) => {
                        if (resBFC) {
                            for (var i = 0; i < resBFC.length; i++) {
                                var BFC = new BilanFinDeCycle(resBFC[i]);
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
        for(var i=0; i< this.domaines.all.length; i++) {
            var oEvaluationsArray = [];
            var oDomaine = this.domaines.all[i] as Domaine;

            // recherche de toutes les évaluations du domaine et ses sous domaines
            // (uniquement les max de chaque compétence)
            Utils.getMaxEvaluationsDomaines(oDomaine, oEvaluationsArray,this.tableConversions.all, false,this.bilanFinDeCycles);
        }
    }

    findCompetence (idCompetence) {
        for(var i=0; i<this.domaines.all.length; i++) {
            var comp = Utils.findCompetenceRec(idCompetence, this.domaines.all[i]);
            if(comp !== undefined) {
                return comp;
            }
        }
        return false;
    }

    getConversionTable(idetab, idClasse, mapCouleur) : Promise<any> {
        return new Promise((resolve, reject) => {
            var that = this;
            http().getJson(this.api.getCompetenceNoteConverssion + '?idEtab='+ idetab+'&idClasse='+idClasse  ).done(function(data){
                _.map(data, (_d) => {
                    _d.couleur = mapCouleur[_d.ordre -1];
                });
                that.tableConversions.load(data);

                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    sync () : Promise<any> {
        return new Promise((resolve, reject) => {
            resolve();
        });
    }
}