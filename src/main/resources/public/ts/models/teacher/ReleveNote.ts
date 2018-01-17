import { Model, IModel, _, moment, Collection, http } from 'entcore';
import {
    Periode,
    Matiere,
    Evaluation,
    Classe,
    Devoir,
    Structure,
    evaluations
} from './index';

export class ReleveNote extends  Model implements IModel{
    synchronized : any;
    periode : Periode;
    matiere : Matiere;
    classe : Classe;
    devoirs : Collection<Devoir>;
    structure : Structure;
    idClasse: string;
    idMatiere: string;
    idPeriode: number;
    idEtablissement: string;
    _tmp : any;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin, name?) => void;
    where: (params) => any;

    get api () {
        return {
            get : '/competences/releve?idEtablissement='+this.structure.id+'&idClasse='+this.idClasse+'&idMatiere='+this.idMatiere
        }
    }

    constructor (o? : any) {
        super();
        if (o && o !== undefined) this.updateData(o);
        this.synchronized = {
            classe : false,
            devoirs : false,
            evaluations : false,
            releve : false
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
                    }
                }
            }
        });
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

    sync () : Promise<any> {
        return new Promise(async (resolve, reject) => {
            await Promise.all([this.syncEvaluations(), this.syncDevoirs(), this.syncClasse()]);
            let _notes ,_devoirs, _eleves;
            if(this._tmp) {
                _notes = this._tmp.notes;
                _devoirs = this._tmp.devoirs;
                _eleves = this._tmp.eleves;
            }
            _.each(this.classe.eleves.all, (eleve) => {
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
            _.each(_eleves, (eleve) => {
                let e = _.findWhere(this.classe.eleves.all, {id: eleve.id});
                if (e) {
                    e.moyenne = eleve.moyenne;
                }
            });
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
}