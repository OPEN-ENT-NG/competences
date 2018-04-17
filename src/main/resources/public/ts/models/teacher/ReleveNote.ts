import { Model, IModel, _, moment, Collection, http } from 'entcore';
import {
    AppreciationClasse,
    Periode,
    Matiere,
    Evaluation,
    Classe,
    Devoir,
    Structure,
    evaluations
} from './index';
import {getNN} from "../../utils/functions/utilsNN";
import * as utils from "../../utils/teacher";

export class ReleveNote extends  Model implements IModel{
    synchronized : any;
    elementProgramme : any;
    periode : Periode;
    matiere : Matiere;
    classe : Classe;
    devoirs : Collection<Devoir>;
    structure : Structure;
    ennseignantsNames;
    idClasse: string;
    idMatiere: string;
    idPeriode: number;
    idEtablissement: string;
    appreciationClasse : AppreciationClasse;
    hasEvaluatedDevoirs : boolean;
    toogle : boolean = false;
    _tmp : any;
    isNN: boolean = false;

    get api () {
        return {
            get : `/competences/releve?idEtablissement=${this.structure.id}&idClasse=${this.idClasse}&idMatiere=${
                this.idMatiere}`,
            GET_INFO_PERIODIQUE: `/competences/releve/periodique?idEtablissement=${this.structure.id}&idClasse=${
                this.idClasse}&idMatiere=${this.idMatiere}&idPeriode=${this.idPeriode}`,
            POST_DATA_RELEVE_PERIODIQUE: `/competences/releve/periodique`,
            POST_DATA_ELEMENT_PROGRAMME: `/competences/releve/element/programme`,
            GET_ELEMENT_PROGRAMME_DOMAINES: `/competences/element/programme/domaines`,
            GET_ELEMENT_PROGRAMME_SOUS_DOMAINES: `/competences/element/programme/sous/domaines`,
            GET_ELEMENT_PROGRAMME_PROPOSITIONS: `/competences/element/programme/propositions`
        }
    }

    constructor (o? : any) {
        super();
        if (o && o !== undefined) this.updateData(o, false);
        this.synchronized = {
            classe : false,
            devoirs : false,
            evaluations : false,
            releve : false,
            appreciationClasse : false
        };
        this.structure = evaluations.structure;
        this.matiere = _.findWhere(evaluations.structure.matieres.all, {id : this.idMatiere});
        let c = _.findWhere(evaluations.structure.classes.all, {id : this.idClasse});
        this.classe = new Classe({id : c.id, name: c.name });

        this.collection(Devoir, {
            sync : () => {
                if (evaluations.structure.synchronized.devoirs) {
                    let _devoirs = evaluations.devoirs.where({
                        id_groupe: this.idClasse,
                        id_matiere: this.idMatiere,
                        id_etablissement: this.idEtablissement
                    });
                    if(this.idPeriode) {
                        _devoirs = _.where(_devoirs, { id_periode: this.idPeriode });
                    }
                    if (_devoirs.length > 0) {
                        this.devoirs.load(_devoirs);
                        this.ennseignantsNames = "";

                        for (let i = 0; i< this.devoirs.all.length; i++) {
                            let teacher = this.devoirs.all[i].teacher;
                            if (!utils.containsIgnoreCase(this.ennseignantsNames, teacher)){
                                this.ennseignantsNames += teacher + " "
                            }
                        }
                    }
                }
            }
        });
    }

    toJson() {
        return {
            idMatiere: this.idMatiere,
            idClasse: this.idClasse,
            idEtablissement: this.idEtablissement,
            idPeriode: this.idPeriode
        };
    }

    syncClasse () : Promise<any> {
        return new Promise(async (resolve, reject) => {
            if (this.classe.eleves.length() === 0) {
                await this.classe.eleves.sync();
            }
            if(this.classe.periodes.length() === 0) {
                await this.classe.periodes.sync();
            }
            this.synchronized.classe = true;
            resolve();
        });
    }

    syncEvaluations () : Promise<any> {
        return new Promise((resolve, reject) => {
            let url = this.api.get;
            if(this.idPeriode) {
                url += '&idPeriode=' + this.idPeriode;
            }
            http().getJson(url)
                .done((res) => {
                    this._tmp = res;
                    this.synchronized.evaluations = true;
                    resolve();
                });
        });
    }

    syncDevoirs () : Promise<any> {
        return new Promise((resolve, reject) => {
            this.devoirs.sync();
            this.synchronized.devoirs = true;
            resolve();
        });
    }

    syncAppreciationClasse() {
        return new Promise((resolve, reject) => {

            let periode = _.findWhere(this.classe.periodes.all, {id_type : this.idPeriode});
            let endSaisie = moment(periode.date_fin_saisie).isBefore(moment(), "days");

            this.appreciationClasse = new AppreciationClasse(this.idClasse, this.idMatiere, this.idPeriode, endSaisie, this.structure.id);
            this.appreciationClasse.sync();
            resolve();
        });
    }

    syncMoyenneFinale () : Promise<any> {
        return new Promise( (resolve,reject) => {
            if (this.idPeriode !== null) {
                http().getJson(this.api.GET_INFO_PERIODIQUE + '&colonne=moyenne')
                    .done((res) => {
                        console.log(res);
                        _.forEach(this.classe.eleves.all, (eleve) => {
                            let _eleve  = _.findWhere(res, {id_eleve: eleve.id});
                            if (_eleve  !== undefined && _eleve .moyenne !== null) {
                                eleve.moyenneFinale = _eleve.moyenne;
                            }
                        });
                        resolve();
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            }
            else {
                resolve();
            }
        });
    }

    syncPositionnement () : Promise<any> {
        return new Promise((resolve, reject) => {
            if (this.idPeriode !== null) {
                http().getJson(this.api.GET_INFO_PERIODIQUE + '&colonne=positionnement')
                    .done((res) => {
                        console.log(res);
                        _.forEach(this.classe.eleves.all, (eleve) => {
                            let _eleve = _.findWhere(res, {id_eleve: eleve.id});
                            if (_eleve !== undefined && _eleve.positionnement !== null) {
                                eleve.positionnement = _eleve.positionnement;
                            }
                        });
                        resolve();
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            }
            else {
                resolve();
            }
        });
    }

    syncDomainesEnseignement () : Promise<any> {
        return new Promise((resolve, reject) => {
            if (evaluations.domainesEnseignements === undefined || evaluations.domainesEnseignements.length ==0){
                http().getJson(this.api.GET_ELEMENT_PROGRAMME_DOMAINES)
                    .done((res) => {
                        evaluations.domainesEnseignements = res;
                        resolve();
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            } else {
                resolve();
            }
        });
    }

    syncSousDomainesEnseignement () : Promise<any> {
        return new Promise((resolve, reject) => {
            if (evaluations.sousDomainesEnseignements === undefined || evaluations.sousDomainesEnseignements.length ==0){
                http().getJson(this.api.GET_ELEMENT_PROGRAMME_SOUS_DOMAINES)
                    .done((res) => {
                        evaluations.sousDomainesEnseignements = res;
                        http().getJson(this.api.GET_ELEMENT_PROGRAMME_PROPOSITIONS)
                            .done((propositions) => {
                                _.forEach(evaluations.sousDomainesEnseignements, (sousDomaine) => {
                                    let _propositions = _.where(propositions, {id_sous_domaine: sousDomaine.id});
                                    if (_propositions !== undefined && _propositions.length > 0) {
                                        sousDomaine.propositions = _propositions;
                                    }
                                });
                                resolve();
                            })
                            .error((propositions) => {
                                console.dir(propositions);
                                reject();
                            })
                    })
                    .error((res) => {
                        console.dir(res);
                        reject();
                    })
            } else {
                resolve();
            }
        });
    }

    sync () : Promise<any> {
        return new Promise(async (resolve, reject) => {
            await Promise.all([this.syncEvaluations(), this.syncDevoirs(), this.syncClasse()]);
            this.syncAppreciationClasse();
            let _notes ,_devoirs, _eleves, _moyennesFinales, _appreciations, _competencesNotes;
            if(this._tmp) {
                _notes = this._tmp.notes;
                _devoirs = this._tmp.devoirs;
                _eleves = this._tmp.eleves;
                _moyennesFinales = this._tmp.moyennes;
                _appreciations = this._tmp.appreciations;
                _competencesNotes = this._tmp.competencesNotes;
                this.elementProgramme = this._tmp.elementProgramme;
            }
            this.hasEvaluatedDevoirs = _.findWhere(this.devoirs.all, {is_evaluated : true});
            this.hasEvaluatedDevoirs = (this.hasEvaluatedDevoirs === undefined)? false:true;

            _.each(this.classe.eleves.all, (eleve) => {
                // chargement des  competencesNotes de l'élève
                let competencesNotesEleve =_.where(_competencesNotes, {id_eleve: eleve.id});
                eleve.competencesNotes = (competencesNotesEleve !== undefined)? competencesNotesEleve : [];

                // chargement de la  moyenne finale de l'élève
                let _eleve  = _.findWhere(_moyennesFinales, {id_eleve: eleve.id});
                if (_eleve  !== undefined && _eleve .moyenne !== null) {
                    if(this.hasEvaluatedDevoirs) {
                        eleve.moyenneFinale = _eleve.moyenne;
                    }
                    else {
                        // suppression de la moyenne finale lorsqu'il n'y a pas de devoir avec l'évaluation numérique
                        eleve.moyenneFinale = "";
                        this.saveAppreciationMatierePeriodeEleve(eleve);
                    }

                }
                // load appreciation
                let _eleve_appreciation  = _.findWhere(_appreciations, {id_eleve: eleve.id});
                if (_eleve_appreciation  !== undefined && _eleve_appreciation !== null) {
                    eleve.appreciation_matiere_periode = _eleve_appreciation.appreciation_matiere_periode;
                }
                var _evals = [];
                let _t = _.where(_notes, {id_eleve: eleve.id});
                _.each(this.devoirs.all, async (devoir) => {
                    let periode = _.findWhere(this.classe.periodes.all, {id_type : devoir.id_periode});
                    let endSaisie = moment(periode.date_fin_saisie).isBefore(moment(), "days");
                    if (_t && _t.length !== 0) {
                        var _e = _.findWhere(_t, {id_devoir : devoir.id});

                        if (_e) {
                            _e.oldValeur = _e.valeur;
                            _e.oldAppreciation = _e.appreciation !== undefined ? _e.appreciation : '';
                            if (_e.annotation !== undefined
                                && _e.annotation !== null
                                && _e.annotation > 0 ) {
                                _e.oldAnnotation = _e.annotation;
                                _e.annotation_libelle_court = evaluations.structure.annotations.findWhere({id: _e.annotation}).libelle_court;
                                _e.is_annotation = true;
                            }
                            _e.endSaisie = endSaisie;
                            _evals.push(_e);
                        }
                        else {
                            _evals.push(new Evaluation({valeur:"", oldValeur : "", appreciation : "",
                                oldAppreciation : "", id_devoir : devoir.id, id_eleve : eleve.id,
                                ramener_sur : devoir.ramener_sur, coefficient : devoir.coefficient,
                                is_evaluated : devoir.is_evaluated, endSaisie : endSaisie}));
                        }
                    } else {
                        _evals.push(new Evaluation({valeur:"", oldValeur : "", appreciation : "",
                            oldAppreciation : "", id_devoir : devoir.id, id_eleve : eleve.id,
                            ramener_sur : devoir.ramener_sur, coefficient : devoir.coefficient,
                            is_evaluated : devoir.is_evaluated, endSaisie : endSaisie}));
                    }
                });
                eleve.evaluations.load(_evals);

            });
            _.each(_devoirs, (devoir) => {
                let d = _.findWhere(this.devoirs.all, {id: devoir.id});
                if (d) {
                    d.statistiques = devoir;
                    if(!d.percent) {
                        evaluations.devoirs.getPercentDone(d).then(() => {
                            d.statistiques.percentDone = d.percent;
                        });
                    } else {
                        d.statistiques.percentDone = d.percent;
                    }
                }
            });

            if(this.hasEvaluatedDevoirs) {
                _.each(_eleves, (eleve) => {
                    let e = _.findWhere(this.classe.eleves.all, {id: eleve.id});
                    if (e) {
                        e.moyenne = eleve.moyenne;
                    }
                });
            }
            else {
                this.isNN = true;
                _.each(this.classe.eleves.all, (eleve) => {
                    eleve.moyenne = getNN();
                })
            }
            // await Promise.all([this.syncMoyenneFinale()]);

            this.trigger('noteOK');
            resolve();
        });
    }

    calculStatsDevoirs() : Promise<any> {

        return new Promise((resolve, reject) => {
            this.on('noteOK', function () {
                var that = this;
                var _datas = [];
                _.each(that.devoirs.all, function (devoir) {
                    var _o = {
                        id: String(devoir.id),
                        evaluations: []
                    };
                    _.each(that.classe.eleves.all, function (eleve) {
                        var _e = eleve.evaluations.findWhere({id_devoir: devoir.id});

                        if (_e !== undefined && _e.valeur !== "") _o.evaluations.push(_e.formatMoyenne());
                    });
                    if (_o.evaluations.length > 0) _datas.push(_o);
                });
                if (_datas.length > 0) {
                    http().postJson('/competences/moyennes?stats=true', {data: _datas}).done(function (res) {
                        _.each(res, function (devoir) {
                            var nbEleves = that.classe.eleves.all.length;
                            var nbN = _.findWhere(_datas, {id: devoir.id});
                            var d = that.devoirs.findWhere({id: parseInt(devoir.id)});
                            if (d !== undefined) {
                                d.statistiques = devoir;
                                if (nbN !== undefined) {
                                    that.devoirs.getPercentDone(d).then(() => {
                                        d.statistiques.percentDone = d.percent;
                                    });
                                    if (resolve && typeof(resolve) === 'function') {
                                        resolve();
                                    }
                                }
                            }
                        });
                    });
                }

            });
        });
    }

    calculMoyennesEleves() : Promise<any> {
        return new Promise((resolve, reject) => {
            var that = this;
            var _datas = [];
            _.each(this.classe.eleves.all, function (eleve) {
                var _t = eleve.evaluations.filter(function (evaluation) {
                    return evaluation.valeur !== "" && evaluation.valeur !== null && evaluation.valeur !== undefined && evaluation.is_evaluated === true;
                });
                if (_t.length > 0) {
                    var _evals = [];
                    for (var i = 0; i < _t.length; i++) {
                        _evals.push(_t[i].formatMoyenne());
                    }
                    var _o = {
                        id: eleve.id,
                        evaluations: _evals
                    };
                    _datas.push(_o);
                }
            });
            if (_datas.length > 0) {
                http().postJson('/competences/moyennes', {data: _datas}).done(function (res) {
                    _.each(res, function (eleve) {
                        var e = that.classe.eleves.findWhere({id: eleve.id});
                        if (e !== undefined) {
                            e.moyenne = eleve.moyenne;
                            if (resolve && typeof(resolve) === 'function') {
                                resolve();
                            }
                        }
                    });
                });
            }
        });
    }
    saveMoyenneFinaleEleve(eleve) : any {
        let _data = _.extend(this.toJson(), {
            idEleve: eleve.id,
            colonne: 'moyenne',
            moyenne: parseFloat(eleve.moyenneFinale),
            delete: eleve.moyenneFinale === ""});

        http().postJson(this.api.POST_DATA_RELEVE_PERIODIQUE, _data )
            .done((res) => {
                console.dir('moyenne sauvé' + eleve.name);
            })
            .error((err) => {
                console.dir('error on save' + eleve.name);
            });
    }

    saveElementProgramme(texte) :  Promise<any> {
        return new Promise((resolve, reject) => {
            var that = this;
            let _data = _.extend(this.toJson(), {
                texte: texte
            });

            http().postJson(this.api.POST_DATA_ELEMENT_PROGRAMME, _data )
                .done((res) => {
                    if (resolve && typeof(resolve) === 'function') {
                        resolve();
                    }
                })
                .error((err) => {

                });
        });
    }


    savePositionnementEleve(eleve) : any {
        let _data = _.extend(this.toJson(), {
            idEleve: eleve.id,
            colonne: 'positionnement',
            positionnement: parseInt(eleve.positionnement),
            delete: eleve.positionnement === ""});

        http().postJson(this.api.POST_DATA_RELEVE_PERIODIQUE, _data )
            .done((res) => {
                console.dir('positionnement sauvé' + eleve.lastName);
            })
            .error((err) => {
                console.dir('error on save positionnement' + eleve.lastName);
            });
    }

    saveAppreciationMatierePeriodeEleve(eleve) : any {
        let _data = _.extend(this.toJson(), {
            idEleve: eleve.id,
            appreciation_matiere_periode: eleve.appreciation_matiere_periode,
            colonne: 'appreciation_matiere_periode',
            delete: eleve.appreciation_matiere_periode === ""});

        http().postJson(this.api.POST_DATA_RELEVE_PERIODIQUE , _data)
            .done((res) => {
                console.dir('appreciation_matiere_periode sauvé' + eleve.lastName);
            })
            .error((err) => {
                console.dir('error on save appreciation_matiere_periode' + eleve.lastName);
            });
    }
}