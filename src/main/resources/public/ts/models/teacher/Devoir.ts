import { Model, IModel, Collection, http, _, moment, $, model } from 'entcore';
import {
    Competence,
    CompetenceNote,
    Eleve,
    Enseignement,
    Evaluation,
    EvaluationDevoir,
    evaluations,
    Matiere,
    Type,
} from './index';

export class Devoir extends Model implements IModel{
    statistiques : any;
    eleves : Collection<Eleve>;
    matiere : Matiere;
    type : Type;
    competences : Collection<Competence> | any;
    competenceEvaluee : CompetenceNote;

    // DATABASE FIELDS
    id : number;
    old_id_groupe : string;
    id_groupe : string;
    type_groupe : number;
    ramener_sur : boolean;
    coefficient : number;
    name : string ;
    owner : string;
    libelle : string;
    id_sousmatiere : number | string;
    id_periode : number | string;
    id_type : number | string;
    id_matiere : string;
    id_etat : number | string;
    date_publication : any;
    id_etablissement : string;
    diviseur : number;
    date : any;
    is_evaluated  : boolean;
    apprec_visible: boolean;
    that: any;
    competencesAdd: any;
    competencesRem: any;
    competencesUpdate: any;
    percent: any;
    teacher: string;
    evaluationDevoirs : Collection<EvaluationDevoir> ;
    nameClass: number | string;
    get api () {
        return {
            create : '/competences/devoir',
            update : '/competences/devoir?idDevoir=',
            delete : '/competences/devoir?idDevoir=',
            duplicate : '/competences/devoir/' + this.id + '/duplicate',
            getCompetencesDevoir : '/competences/competences/devoir/',
            getCompetencesLastDevoir : '/competences/competences/last/devoir/',
            getNotesDevoir : '/competences/devoir/' + this.id + '/notes',
            getAppreciationDevoir: '/competences/appreciation/' + this.id + '/appreciations',
            getStatsDevoir : '/competences/devoir/' + this.id + '/moyenne?stats=true',
            getCompetencesNotes : '/competences/competence/notes/devoir/',
            saveCompetencesNotes : '/competences/competence/notes',
            updateCompetencesNotes : '/competences/competence/notes',
            deleteCompetencesNotes : '/competences/competence/notes',
            isEvaluatedDevoir : '/competences/devoirs/evaluations/information?idDevoir=',
            switchVisiApprec : '/competences/devoirs/' + this.id + '/visibility'
        }
    }

    constructor(p? : any) {
        super();
        var that = this;
        this.collection(Enseignement);
        this.collection(EvaluationDevoir);
        this.collection(Competence, {
            sync : function () : Promise<any> {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.getCompetencesDevoir + that.id).done(function(res) {
                        this.load(res);
                        if(resolve && (typeof(resolve) === 'function')) {
                            resolve();
                        }
                    }.bind(this));
                });
            }
        });
        this.collection(Eleve, {
            sync : function () : Promise<any> {
                return new Promise((resolve, reject) => {
                    var _classe = evaluations.structure.classes.findWhere({id : that.id_groupe});
                    // that.eleves.load(JSON.parse(JSON.stringify(_classe.eleves.all)));
                    // that.eleves.load($.extend(true, {}, JSON.stringify(_classe.eleves.all)));
                    var e = $.map($.extend(true, {}, _classe.eleves.all), function (el) {
                        return el;
                    });
                    that.eleves.load(e);
                    http().getJson(that.api.getNotesDevoir).done(function (res) {
                        for (var i = 0; i < res.length; i++) {
                            var _e = that.eleves.findWhere({id : res[i].id_eleve});
                            if (_e !== undefined) {
                                _e.evaluation = new Evaluation(res[i]);
                                _e.evaluation.oldAppreciation = _e.evaluation.appreciation !== undefined ? _e.evaluation.appreciation : '';
                                if (_e.evaluation.id_annotation === undefined
                                    || _e.evaluation.id_annotation === null) {
                                    _e.evaluation.id_annotation = -1;
                                } else {
                                    let annotation = _.findWhere(evaluations.structure.annotations.all, {id : _e.evaluation.id_annotation});
                                    if (annotation !== undefined && annotation !== null) {
                                        _e.evaluation.valeur = annotation.libelle_court;
                                    }
                                }
                                _e.evaluation.oldValeur = _e.evaluation.valeur;
                                _e.evaluation.oldId_annotation = _e.evaluation.id_annotation;
                                delete _e.evaluations;
                            }
                        }
                        var _t = that.eleves.filter(function (eleve) {
                            delete eleve.evaluations;
                            return (!_.has(eleve, "evaluation"));
                        });
                        for (var j = 0; j < _t.length; j++) {
                            _t[j].evaluation = new Evaluation({valeur:"", oldValeur : "", appreciation : "", oldAppreciation : "", id_devoir : that.id, id_eleve : _t[j].id, ramener_sur : that.ramener_sur, coefficient : that.coefficient});
                        }
                        that.syncCompetencesNotes().then(() => {
                            if(resolve && (typeof(resolve) === 'function')) {
                                resolve();
                            }
                        });
                    });
                });
            }
        });
        if (p) this.updateData(p, false);
    }

    switchVisibilityApprec(): Promise<any> {
        return new Promise<any>((resolve, reject) => {
            http().putJson(this.api.switchVisiApprec)
                .done(() => {
                    this.apprec_visible = !this.apprec_visible;
                    resolve();
                })
                .error(reject);
        });
    }

    getLastSelectedCompetence (): Promise<[any]> {
        return new Promise((resolve, reject) => {
            http().getJson(this.api.getCompetencesLastDevoir).done(function(competencesLastDevoirList){
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(competencesLastDevoirList);
                }
            });
        });
    }

    toJSON () {
        let classe = evaluations.structure.classes.findWhere({id : this.id_groupe});
        let  type_groupe = -1;
        let  id_groupe = null;
        if(classe !== undefined){
            if(classe.type_groupe !== undefined){
                type_groupe = classe.type_groupe;
            }
            id_groupe = this.id_groupe;
        }
        return {
            name            : this.name,
            owner           : this.owner,
            libelle         : this.libelle,
            old_id_groupe   : this.old_id_groupe,
            id_groupe       : id_groupe,
            type_groupe     : type_groupe,
            id_sousmatiere   : parseInt(this.id_sousmatiere as string),
            id_periode       : parseInt(this.id_periode as string),
            id_type          : parseInt(this.id_type as string),
            id_matiere       : this.id_matiere,
            id_etat          : parseInt(this.id_etat as string),
            date_publication : this.date_publication,
            id_etablissement : this.id_etablissement,
            diviseur        : this.diviseur,
            coefficient     : this.coefficient,
            date            : this.date,
            ramener_sur      : this.ramener_sur,
            is_evaluated     : this.is_evaluated,
            competences     : this.competences,
            competenceEvaluee : this.competenceEvaluee,
            apprec_visible : this.apprec_visible,
            competencesAdd : null,
            competencesRem : null,
            competencesUpdate :  null
        };
    }

    create () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.create, this.toJSON()).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    duplicate (classes?: string[]): Promise<any> {
        return new Promise((resolve, reject) => {
            if (classes.length > 0) {
                http().postJson(this.api.duplicate, {classes: classes}).done((res) => {
                    resolve();
                });
            } else {
                reject();
            }
        });
    }

    isEvaluatedDevoir (idDevoir) : Promise<any> {

        return new Promise((resolve, reject) => {
            var that = this;
            http().getJson(this.api.isEvaluatedDevoir+idDevoir).done(function(data){


                that.evaluationDevoirs.load(data);
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }
    update (addArray, remArray, updateArray) : Promise<any> {
        return new Promise((resolve, reject) => {
            let devoirJSON = this.toJSON();
            devoirJSON.competencesAdd = addArray;
            devoirJSON.competencesRem = remArray;
            devoirJSON.competencesUpdate = updateArray;
            devoirJSON.competences = [];
            if(devoirJSON.competenceEvaluee == undefined) {
                delete devoirJSON.competenceEvaluee;
            }
            http().putJson(this.api.update + this.id, devoirJSON).done(function(data){
                evaluations.devoirs.sync();
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    remove () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.delete + this.id).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            })
                .error(function () {
                    reject();
                });
        });
    }

    save (add? : any,rem? : any, update? : any) : Promise<any> {
        return new Promise((resolve, reject) => {
            if(!this.id){
                this.create().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve(data);
                    }
                });
            }else{
                this.update(add, rem, update).then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve(data);
                    }
                });
            }
        });
    }

    calculStats () : Promise<any> {
        return new Promise((resolve, reject) => {
            let that = this;
            http().getJson(this.api.getStatsDevoir).done(function (res) {
                if(!res.error) {
                    that.statistiques = res;
                    let id = [];
                    id.push(that.id);
                    evaluations.devoirs.getPercentDone(that).then(() => {
                        that.statistiques.percentDone = _.findWhere(evaluations.structure.devoirs.all,{id : that.id}).percent;
                    });
                } else {
                    _.mapObject(that.statistiques, (val) => {
                        return "";
                    });
                }
                //model.trigger('apply');
                if(resolve && typeof(resolve) === 'function'){
                    resolve();
                }
            });
        });
    }

    syncCompetencesNotes() : Promise<any> {
        return new Promise((resolve, reject) => {
            var that = this;
            http().getJson(that.api.getCompetencesNotes + that.id).done(function (res) {
                for (var i = 0; i < that.eleves.all.length; i++) {
                    var _comps = _.where(res, {id_eleve : that.eleves.all[i].id});
                    if (_comps.length > 0) {
                        var _results = [];
                        for (var j = 0; j < that.competences.all.length; j++) {
                            var _c = that.competences.all[j];
                            var _t = _.findWhere(_comps, {id_competence : _c.id_competence});
                            if (_t === undefined) {
                                _results.push(new CompetenceNote({id_competence : _c.id_competence, nom : _c.nom, id_devoir : that.id, id_eleve : that.eleves.all[i].id, evaluation : -1}));
                            } else {
                                _results.push(_t);
                            }
                        }
                        that.eleves.all[i].evaluation.competenceNotes.load(_results);
                    } else {
                        var _results = [];
                        for (var j = 0; j < that.competences.all.length; j++) {
                            _results.push(new CompetenceNote({id_competence : that.competences.all[j].id_competence, nom : that.competences.all[j].nom, id_devoir : that.id, id_eleve : that.eleves.all[i].id, evaluation : -1}));
                        }
                        that.eleves.all[i].evaluation.competenceNotes.load(_results);
                    }
                }
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve();
                }
            });
        });
    }

    saveCompetencesNotes (_data) {
        var that = this;
        if (_data[0].evaluation !== -1){
            var _post = _.filter(_data, function (competence) {
                return competence.id === undefined;
            });
            if (_post.length > 0) {
                http().postJson(this.api.saveCompetencesNotes, {data : _post}).done(function (res) {
                    if (_post.length === _data.length) {
                        that.syncCompetencesNotes().then(() => {
                            evaluations.devoirs.getPercentDone(that).then(()=> {
                                that.statistiques.percentDone = that.percent;
                                evaluations.trigger('apply');
                            });
                        });
                    } else {
                        var _put = _.filter(_data, function (competence) {
                            return competence.id !== undefined;
                        });
                        if (_put.length > 0) {
                            var url = that.api.updateCompetencesNotes + "?";
                            for (var i = 0 ; i < _put.length; i++) {
                                url += "id="+_put[i].id+"&";
                            }
                            url = url.slice(0, -1);
                            http().putJson(url, {data : _put}).done(function (res) {
                                that.syncCompetencesNotes().then(() => {
                                    evaluations.devoirs.getPercentDone(that).then(()=> {
                                        that.statistiques.percentDone = that.percent;
                                        evaluations.trigger('apply');
                                    });
                                });
                            });
                        }
                    }
                });
            } else {
                var _put = _.filter(_data, function (competence) {
                    return competence.id !== undefined;
                });
                if (_put.length > 0) {
                    var url = that.api.updateCompetencesNotes + "?";
                    for (var i = 0 ; i < _put.length; i++) {
                        url += "id="+_put[i].id+"&";
                    }
                    url = url.slice(0, -1);
                    http().putJson(url, {data : _put}).done(function (res) {
                        that.syncCompetencesNotes().then(() => {
                            evaluations.devoirs.getPercentDone(that).then(()=> {
                                that.statistiques.percentDone = that.percent;
                                evaluations.trigger('apply');
                            });
                        });
                    });
                }
            }
        } else {
            var _delete = [];
            for (var i = 0; i < _data.length; i++) {
                if (_data[i].id !== undefined)_delete.push(_data[i].id);
            }
            if (_delete.length > 0) {
                http().delete(this.api.deleteCompetencesNotes, {id : _delete}).done(function (res) {
                    that.syncCompetencesNotes().then(() => {
                        evaluations.devoirs.getPercentDone(that).then(()=> {
                            that.statistiques.percentDone = that.percent;
                            evaluations.trigger('apply');
                        });
                    });
                });
            }
        }
    }
}