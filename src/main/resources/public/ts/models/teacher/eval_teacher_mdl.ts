import {model, http, IModel, Model, Collection, idiom as lang, moment, _, $} from 'entcore';
import {TypePeriode} from "../common/TypePeriode";
import {DefaultPeriode} from "../common/DefaultPeriode";
import * as utils from '../../utils/teacher';
import {BfcSynthese}from '../eval_bfc_synthese_mdl';
import {EleveEnseignementCpl, EnsCpls, EnsCpl} from '../eval_ens_complement_mdl';
export * from '../common/LSU';
import {Defaultcolors, NiveauCompetence} from "../eval_niveau_comp";
import {Cycle} from "../eval_cycle";

//TODO Delete when infra-front will be fixed
declare let require: any;

export class Periode extends DefaultPeriode {}

export class Structure extends Model {
    id: string;
    libelle: string;
    eleves: Collection<Eleve>;
    enseignants: Collection<Enseignant>;
    devoirs: Devoirs;
    synchronized: any;
    classes: Collection<Classe>;
    matieres: Collection<Matiere>;
    types: Collection<Type>;
    enseignements: Collection<Enseignement>;
    annotations: Collection<Annotation>;
    annotationsfull: [Annotation];
    releveNotes: Collection<ReleveNote>;
    isSynchronized: boolean;
    cycles: Array<Cycle>;
    cycle: Cycle;
    typePeriodes: Collection<TypePeriode>;
    niveauCompetences: Collection<NiveauCompetence>;
    usePerso: any;
    private syncRemplacement: () => any;
    responsables: Collection<Responsable>;

    //TODO Delete when infra-front will be fixed
    collection:  (type, mixin?, name?) => void;
    updateData: (o) => void;


    get api() {
        return {
            getEleves: '/competences/eleves?idEtablissement=' + this.id,
            getEnseignants: '/competences/user/list?profile=Teacher&structureId=',
            getDevoirs: '/competences/etab/devoirs/',
            getClasses: '/viescolaire/classes?idEtablissement=' + this.id,
            TYPE: {
                synchronization: '/competences/types?idEtablissement=' + this.id
            },
            ENSEIGNEMENT: {
                synchronization: '/competences/enseignements'
            },
            MATIERE: {
                synchronizationCE: '/viescolaire/matieres?idEtablissement=' + this.id,
                synchronization: '/viescolaire/matieres?idEnseignant=' + model.me.userId + '&idEtablissement=' + this.id
            },
            CLASSE: {
                synchronization: '/viescolaire/classes?idEtablissement=' + this.id,
                synchronizationRemplacement: '/competences/remplacements/classes?idEtablissement=' + this.id
            },
            ELEVE: {
                synchronization: '/viescolaire/eleves?idEtablissement=' + this.id
            },
            ANNOTATION: {
                synchronization: '/competences/annotations?idEtablissement=' + this.id
            },
            RESPONSABLE : {
                synchronisation : '/competences/responsablesDirection?idStructure=' + this.id
            },
            NIVEAU_COMPETENCES: {
                synchronisation: '/competences/maitrise/level/' + this.id,
                use: '/competences/maitrise/perso/use/' + model.me.userId
            },
            TYPEPERIODES : {
                synchronisation: '/viescolaire/periodes/types'
            }
        }
    }

    constructor(o?: any) {
        super();
        if (o) this.updateData(o);
        this.isSynchronized = false;
        this.synchronized = {
            devoirs: false,
            classes: false,
            matieres: false,
            typePeriodes: false,
            types: false,
            annotations: false,
            enseignements: false,
            niveauCompetences: false
        };
        if (isChefEtab()) {
            this.synchronized.enseignants = false;
        }
        let that: Structure = this;
        this.collection(NiveauCompetence, {
            sync: async function (defaut) {
                if (typeof(defaut) == 'undefined') {
                    defaut = true;
                }
                // Récupération (sous forme d'arbre) des niveaux de compétences de l'établissement en cours
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.NIVEAU_COMPETENCES.synchronisation).done(function (niveauCompetences) {
                        if (_.filter(niveauCompetences, {couleur: null}).length == niveauCompetences.length) {
                            that.usePerso = 'noPerso';
                        }
                        else {
                            if (that.usePerso) {
                                that.usePerso = 'true';
                            }
                            else  {
                                that.usePerso = 'false';
                            }
                        }
                        if (_.filter(niveauCompetences, {couleur: null}).length > 0 || defaut) {
                            niveauCompetences.forEach((niveauCompetence) => {
                                if (niveauCompetence.couleur === null || defaut) {
                                    niveauCompetence.couleur = Defaultcolors[niveauCompetence.default];
                                }
                                if (niveauCompetence.lettre === null || defaut) {
                                    niveauCompetence.lettre = " ";
                                }
                                niveauCompetence.id_etablissement = this.composer.id;
                            });
                        }

                        that.niveauCompetences.load(niveauCompetences);
                        let cycles = [];
                        let tree = _.groupBy(niveauCompetences, "id_cycle");

                        _.map(tree, function (node) {
                            let cycleNode = {
                                id_cycle: node[0].id_cycle,
                                libelle: node[0].cycle,
                                selected: false,
                                niveauCompetencesArray: _.sortBy(node, function (niv) {
                                    return niv.ordre;
                                })
                            }
                            cycleNode.niveauCompetencesArray = cycleNode.niveauCompetencesArray.reverse();
                            cycles.push(cycleNode);
                        });
                        that.cycles = cycles;
                        if (that.cycles.length > 0) {
                            that.cycle = cycles[0];
                        }
                        that.synchronized.niveauCompetences = true;

                        if (resolve && typeof resolve === 'function') {
                            resolve();
                        }
                    }.bind(this))
                        .error(function () {
                            if (reject && typeof reject === 'function') {
                                reject();
                            }
                        })
                })
            }
        });
        this.collection(Enseignant);
        this.collection(Responsable, {//responsable de Direction
            sync :  function(){
                return new Promise ((resolve, reject)=>{
                    http().getJson(that.api.RESPONSABLE.synchronisation).done(function (res) {
                        that.responsables.load(res);
                        resolve();
                    });
                })
            }
        });
        this.collection(Eleve, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    //chargement des élèves Pour les enseignants ou personnel de l'établissement
                    let url = that.api.ELEVE.synchronization;
                    //filtre par classe pour les enseignants
                    if ((model.me.type === 'ENSEIGNANT')) {
                        that.classes.forEach((classe) => {
                            url += '&idClasse=' + classe.id;
                        });
                    }
                    if (model.me.type === 'PERSEDUCNAT'
                        || model.me.type === 'ENSEIGNANT') {
                        http().getJson(url).done((res) => {
                            // On tri les élèves par leur lastName en ignorant les accents
                            utils.sortByLastnameWithAccentIgnored(res);
                            that.eleves.load(res);
                            that.synchronized.eleves = true;
                            resolve();
                        });
                    }
                });
            }
        });
        this.collection(Type, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.TYPE.synchronization).done(function (res) {
                        this.load(res);
                        that.synchronized.types = true;
                        resolve();
                    }.bind(this));
                });
            }
        });
        this.collection(Devoir, new DevoirsCollection(that.id));
        this.collection(Enseignement, {
            sync: function (idClasse: string) {
                return new Promise((resolve, reject) => {
                    var uri = that.api.ENSEIGNEMENT.synchronization;
                    if (idClasse !== undefined) {
                        uri += '?idClasse=' + idClasse;
                        http().getJson(uri).done(function (res) {
                            this.load(res);
                            this.each(function (enseignement) {
                                enseignement.competences.load(enseignement['competences_1']);
                                _.map(enseignement.competences.all, function (competence) {
                                    return competence.composer = enseignement;
                                });
                                enseignement.competences.each(function (competence) {
                                    if (competence['competences_2'].length > 0) {
                                        competence.competences.load(competence['competences_2']);
                                        _.map(competence.competences.all, function (sousCompetence) {
                                            return sousCompetence.composer = competence;
                                        });
                                    }
                                    delete competence['competences_2'];
                                });
                                delete enseignement['competences_1'];
                            });
                            that.synchronized.enseignements = true;
                            resolve();
                        }.bind(this));
                    } else {
                        console.error('idClasse must be defined');
                    }
                });
            }
        });
        this.collection(Matiere, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    if (isChefEtab()) {
                        http().getJson(that.api.MATIERE.synchronizationCE).done(function (res) {
                            this.load(res);
                            that.synchronized.matieres = true;
                            resolve();
                        }.bind(this));
                    } else {
                        http().getJson(that.api.MATIERE.synchronization)
                            .done(function (res) {
                                this.load(res);
                                this.each(function (matiere) {
                                    if (matiere.hasOwnProperty('sous_matieres')) {
                                        matiere.sousMatieres.load(matiere.sous_matieres);
                                        delete matiere.sous_matieres;
                                    }
                                });
                                that.synchronized.matieres = true;
                                resolve();
                            }.bind(this));
                    }
                });
            }
        });
        this.collection(Annotation, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.ANNOTATION.synchronization).done(function (res) {
                        this.load(res);
                        evaluations.annotations = this;
                        evaluations.annotationsfull = _.clone(evaluations.annotations.all);
                        evaluations.annotationsfull.push(new Annotation({
                            id: 0, libelle: lang.translate('no.annotation'), id_etablissement: this.id,
                            libelle_court: ""
                        }));
                        that.synchronized.annotations = true;
                        this.trigger('sync');
                        resolve();
                    }.bind(this));
                });
            }
        });
        this.collection(ReleveNote);
        const libelle = {
            CLASSE: 'Classe',
            GROUPE: "Groupe d'enseignement"
        };
        const castClasses = (classes) => {
            return _.map(classes, (classe) => {
                let libelleClasse;
                if (classe.type_groupe_libelle = classe.type_groupe === 0) {
                    libelleClasse = libelle.CLASSE;
                } else {
                    libelleClasse = libelle.GROUPE;
                }
                classe.type_groupe_libelle = libelleClasse;
                if (!classe.hasOwnProperty("remplacement")) classe.remplacement = false;
                classe = new Classe(classe);
                return classe;
            });
        };
        this.syncRemplacement = function () {
            return new Promise((resolve, reject) => {
                http().getJson(that.api.CLASSE.synchronizationRemplacement)
                    .done((res) => {
                        this.classes.addRange(castClasses(res));
                        model.trigger('apply');
                        resolve();
                    });
            })
        };
        this.collection(Classe, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.CLASSE.synchronization).done((res) => {
                        that.classes.addRange(castClasses(res));
                        that.synchronized.classes = true;
                        if (!isChefEtab()) {
                            that.eleves.sync().then(() => {
                                model.trigger('apply');
                            });
                            that.syncRemplacement().then(() => {
                                model.trigger('apply');
                            });
                            resolve();
                        } else {
                            that.eleves.sync().then(() => {
                                resolve();
                            });
                        }
                    });
                });
            },
        });
        this.collection(TypePeriode, {
            sync: async () : Promise<any> => {
                return await http().getJson(this.api.TYPEPERIODES.synchronisation).done((res) => {
                    this.typePeriodes.load(res);
                    this.synchronized.typePeriodes = true;
                });
            }
        });

        this.devoirs.on('sync', function () {
            that.synchronized.devoirs = true;
        });
    }

    sync() {
        return new Promise((resolve, reject) => {
            let isSynced = () => {
                let b =
                    this.synchronized.matieres &&
                    this.synchronized.types &&
                    this.synchronized.classes &&
                    this.synchronized.annotations &&
                    this.synchronized.niveauCompetences &&
                    this.synchronized.devoirs &&
                    this.synchronized.typePeriodes;
                if (isChefEtab()) {
                    b = b && this.synchronized.enseignants;
                }
                if (b) {
                    this.isSynchronized = true;
                    resolve();
                }
            };
            this.matieres.sync().then(isSynced);
            this.annotations.sync().then(isSynced);
            this.types.sync().then(isSynced);
            this.classes.sync().then(isSynced);
            this.usePersoFun(model.me.userId).then((res) => {
                let useDefautTheme = !res;
                this.usePerso = res;
                this.niveauCompetences.sync(useDefautTheme).then(isSynced);
            });
            this.syncDevoirs().then(isSynced);
            if (isChefEtab()) {
                this.syncEnseignants().then(isSynced);
            }
            this.typePeriodes.sync().then(isSynced);
        });
    }

    syncDevoirs(): Promise<any> {
        return new Promise((resolve, reject) => {
            this.devoirs.sync().then((data) => {
                this.synchronized.devoirs = true;
                this.devoirs.trigger('devoirs-sync');
                resolve();
            });
        });
    }

    syncEnseignants(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().getJson(this.api.getEnseignants + this.id).done(function (res) {
                this.enseignants.load(res);
                this.synchronized.enseignants = true;
                if (resolve && (typeof(resolve) === 'function')) {
                    resolve();
                }
            }.bind(this));
        });
    }

    syncClasses(idEtab): Promise<any> {
        return new Promise((resolve, reject) => {
            var that = this;
            const libelle = {
                CLASSE: "Classe",
                GROUPE: "Groupe d'enseignement"
            };
            http().getJson(this.api.getClasses).done((res) => {
                _.map(res, (classe) => {
                    let libelleClasse;
                    if (classe.type_groupe === 0) {
                        libelleClasse = libelle.CLASSE;
                    } else {
                        libelleClasse = libelle.GROUPE;
                    }
                    classe.type_groupe_libelle = libelleClasse;
                    return classe;
                });

                that.classes.load(res);
            });
            this.classes.sync();
        });
    }

    usePersoFun(idUser): Promise<boolean> {
        return new Promise((resolve, reject) => {

            http().getJson(this.api.NIVEAU_COMPETENCES.use).done((res) => {
                if (!res) {
                    reject();
                }
                if (res.length > 0) {
                    resolve(true)
                }
                else {
                    resolve(false)
                }
            });
        });
    }
}
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

function isChefEtab () {
    return  model.me.type === 'PERSEDUCNAT' &&
        model.me.functions !== undefined &&
        model.me.functions.DIR !== undefined &&
        model.me.functions.DIR.code === 'DIR';
}

export class Classe extends Model {
    eleves : Collection<Eleve>;
    id : number;
    name : string;
    type_groupe : number;
    periodes : Collection<Periode>;
    type_groupe_libelle : string;
    suiviCompetenceClasse : Collection<SuiviCompetenceClasse>;
    mapEleves : any;
    remplacement: boolean;
    id_cycle: any;
    selected : boolean;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

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
        if (o !== undefined) this.updateData(o);
        this.collection(Eleve, {
            sync : () : Promise<any> => {
                return new Promise((resolve, reject) => {
                    this.mapEleves = {};
                    let url;
                    if(isChefEtab()){
                        url = this.type_groupe === 1 ? this.api.syncGroupe : this.api.syncClasseChefEtab;
                    }else {
                        url = this.type_groupe === 1 ? this.api.syncGroupe : this.api.syncClasse;
                    }
                    http().getJson(url).done((data) => {
                        // On tri les élèves par leur lastName en ignorant les accents
                        utils.sortByLastnameWithAccentIgnored(data);
                        this.eleves.load(data);
                        for (var i = 0; i < this.eleves.all.length; i++) {
                            this.mapEleves[this.eleves.all[i].id] = this.eleves.all[i];
                        }
                        this.trigger('sync');
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
                        resolve();
                    });
                });
            }
        });
    }
}

export class Eleve extends Model {
    moyenne: number;
    evaluations : Collection<Evaluation>;
    evaluation : Evaluation;
    id : string;
    firstName: string;
    lastName: string;
    suiviCompetences : Collection<SuiviCompetence>;
    displayName: string;
    idClasse: string;

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
            getMoyenne : '/competences/eleve/' + this.id + '/moyenne?'
        }
    }

    constructor (o?: any) {
        super();
        if (o) {
            this.updateData(o);
        }
        this.collection(Evaluation);
        this.collection(SuiviCompetence);
    }
    toString () {
        return this.hasOwnProperty("displayName") ? this.displayName : this.firstName+" "+this.lastName;
    }

    getMoyenne (devoirs?) : Promise<any> {
        return new Promise((resolve, reject) => {
            if (devoirs) {
                let idDevoirsURL = "";
                _.each(_.pluck(devoirs,'id'), (id) => {
                    idDevoirsURL += "devoirs=" + id + "&";
                });
                idDevoirsURL = idDevoirsURL.slice(0, idDevoirsURL.length - 1);
                http().getJson(this.api.getMoyenne + idDevoirsURL).done(function (res) {
                    if (!res.error) {
                        this.moyenne = res.moyenne;
                    } else {
                        this.moyenne = "";
                    }
                }.bind(this));
                if(resolve && typeof(resolve) === 'function'){
                    resolve();
                }
            }
        });
    }
}

export class Evaluation extends Model implements IModel{
    id : number;
    id_eleve : string;
    id_devoir : number;
    id_appreciation : number;
    valeur : any;
    appreciation : any;
    coefficient : number;
    ramener_sur : boolean;
    competenceNotes : Collection<CompetenceNote>;
    oldValeur : any;
    is_evaluated  : boolean;
    oldAppreciation : any;
    oldId_annotation : number;
    id_annotation : number;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    get api () {
        return {
            create : '/competences/note',
            update : '/competences/note?idNote=' + this.id,
            delete : '/competences/note?idNote=' + this.id,
            createAppreciation : '/competences/appreciation',
            updateAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation,
            deleteAppreciation : '/competences/appreciation?idAppreciation=' + this.id_appreciation,
            updateAnnotation : '/competences/annotation?idDevoir=' + this.id_devoir,
            deleteAnnotation : '/competences/annotation?idDevoir=' + this.id_devoir+'&idEleve='+this.id_eleve,
            createAnnotation : '/competences/annotation'
        };
    }

    constructor (o? : any) {
        super();
        if (o) this.updateData(o);
        this.collection(CompetenceNote);
    }

    toJSON () {
        var o = new Evaluation();
        if(this.id !== null) o.id = this.id;
        o.id_eleve  = this.id_eleve;
        o.id_devoir = parseInt(this.id_devoir.toString());
        o.valeur   = parseFloat(this.valeur);
        if (this.appreciation) o.appreciation = this.appreciation;
        //delete o.appreciation;
        delete o.competenceNotes;
        return o;
    }


    save () : Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            if ( this.id_annotation !== undefined && this.id_annotation !== -1){
                this.deleteAnnotationDevoir().then(()=>{
                    delete this.oldId_annotation;
                    delete this.id_annotation;
                    this.oldValeur = "";
                    this.create().then((data) => {
                        resolve(data);
                    });
                });
            }else {
                if (!this.id) {
                    this.create().then((data) => {
                        resolve(data);
                    });
                } else {
                    this.update().then((data) =>  {
                        resolve(data);
                    });
                }
            }


        });
    }

    saveAppreciation () : Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            if (!this.id_appreciation) {
                this.createAppreciation().then((data) => {
                    resolve(data);
                });
            } else {
                this.updateAppreciation().then((data) =>  {
                    resolve(data);
                });
            }
        });
    }
    create () : Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            let _noteData = this.toJSON();
            delete _noteData.appreciation;
            delete _noteData.id_appreciation;
            http().postJson(this.api.create, _noteData).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    update () : Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            let _noteData = this.toJSON();
            delete _noteData.appreciation;
            delete _noteData.id_appreciation;
            http().putJson(this.api.update, _noteData).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    delete () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.delete).done(function (data) {
                if(resolve && typeof(resolve) === 'function'){
                    resolve(data);
                }
            });
        });
    }
    createAppreciation () : Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            var _appreciation = {
                id_devoir : this.id_devoir,
                id_eleve  : this.id_eleve,
                valeur    : this.appreciation
            };
            http().postJson(this.api.createAppreciation, _appreciation).done ( function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            }) ;

        });

    }

    updateAppreciation () : Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            var _appreciation = {
                id : this.id_appreciation,
                id_devoir : this.id_devoir,
                id_eleve  : this.id_eleve,
                valeur    : this.appreciation
            };
            http().putJson(this.api.updateAppreciation, _appreciation).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });

        });
    }

    saveAnnotation (): Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            if (this.oldId_annotation !== undefined && this.oldId_annotation > 0) {
                this.updateAnnotationDevoir().then((data) => {
                    resolve(data);
                });
            } else {
                this.createAnnotationDevoir().then((data) =>  {
                    resolve(data);
                });
            }
        });
    }

    createAnnotationDevoir (): Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            let _annotation = {
                id_devoir : this.id_devoir,
                id_annotation  : this.id_annotation,
                id_eleve    : this.id_eleve
            };
            http().postJson(this.api.createAnnotation, _annotation).done ( function (data) {
                if (resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });

    }

    updateAnnotationDevoir (): Promise<Evaluation> {
        return new Promise((resolve, reject) => {
            let _annotation = {
                id_devoir : this.id_devoir,
                id_annotation  : parseInt(this.id_annotation.toString()),
                id_eleve    : this.id_eleve
            };
            http().putJson(this.api.updateAnnotation, _annotation).done ( function (data) {
                if (resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }
    deleteAnnotationDevoir () : Promise<any> {
        return new Promise((resolve, reject) => {
            let _annotation = {
                id_devoir : this.id_devoir,
                id_eleve    : this.id_eleve
            };
            http().delete(this.api.deleteAnnotation,_annotation).done(function (data) {
                if(resolve && typeof(resolve) === 'function') {
                    resolve(data);
                }
            });
        });
    }

    deleteAppreciation () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.deleteAppreciation).done(function (data) {
                if(resolve && typeof(resolve) === 'function') {
                    resolve(data);
                }
            });
        });
    }

    formatMoyenne() {
        return {
            valeur : parseFloat(this.valeur),
            coefficient : this.coefficient,
            ramenersur : this.ramener_sur
        }
    }


}
export class EvaluationDevoir extends  Model {
    nbreval : number;
    id : string;
    evaluation : number;
    typeeval : string;

    constructor(p? : any) {
        super();
    }

}
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
    percent: any;
    teacher: string;
    evaluationDevoirs : Collection<EvaluationDevoir> ;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

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
        if (p) this.updateData(p);
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

    getLastSelectedCompetence () : Promise<[any]> {
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
            competencesRem : null
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
    update (addArray, remArray) : Promise<any> {
        return new Promise((resolve, reject) => {
            var devoirJSON = this.toJSON();
            devoirJSON.competencesAdd = addArray;
            devoirJSON.competencesRem = remArray;
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

    save (add? : any,rem? : any) : Promise<any> {
        return new Promise((resolve, reject) => {
            if(!this.id){
                this.create().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve(data);
                    }
                });
            }else{
                this.update(add, rem).then((data) => {
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
                model.trigger('apply');
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

export class DevoirsCollection {
    all : Devoir[];
    sync : any;
    percentDone : boolean;
    idEtablissement: string;

    get api () {
        return {
            get : '/competences/devoirs?idEtablissement=' + this.idEtablissement,
            areEvaluatedDevoirs : '/competences/devoirs/evaluations/informations?',
            done : '/competences/devoirs/done'
        }
    }

    constructor (idEtablissement : string) {
        this.idEtablissement = idEtablissement;
        this.sync =  function () {
            return new Promise((resolve, reject) => {
                http().getJson(this.api.get).done(function (res) {
                    this.load(res);
                    if (evaluations.synchronized.matieres) {
                        evaluations.devoirs.synchronizeDevoirMatiere();
                    } else {
                        evaluations.matieres.on('sync', function () {
                            evaluations.devoirs.synchronizeDevoirMatiere();
                        });
                    }
                    if (evaluations.synchronized.types) {
                        evaluations.devoirs.synchronizedDevoirType();
                    } else {
                        evaluations.types.on('sync', function () {
                            evaluations.devoirs.synchronizedDevoirType();
                        });
                    }
                    evaluations.devoirs.trigger('sync');
                    if (resolve && (typeof(resolve) === 'function')) {
                        resolve(res);
                    }
                }.bind(this));
            });
        };
    }

    synchronizeDevoirMatiere () {
        for (var i = 0; i < evaluations.devoirs.all.length; i++) {
            var matiere = evaluations.matieres.findWhere({id : evaluations.devoirs.all[i].id_matiere});
            if (matiere) evaluations.devoirs.all[i].matiere = matiere;
        }
    }

    synchronizedDevoirType () {
        for (var i = 0 ; i < evaluations.devoirs.all.length; i++) {
            var type = evaluations.types.findWhere({id : evaluations.devoirs.all[i].id_type});
            if (type) evaluations.devoirs.all[i].type = type;
        }
    }

    areEvaluatedDevoirs (idDevoirs) : Promise<any> {
        return new Promise((resolve, reject) => {
            var URLBuilder = "";
            for (var i=0; i<idDevoirs.length; i++){
                if(i==0)
                    URLBuilder = "idDevoir="+idDevoirs[i];
                else
                    URLBuilder += "&idDevoir="+idDevoirs[i];
            }
            http().getJson(this.api.areEvaluatedDevoirs + URLBuilder  ).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }


    getPercentDone (devoir?) : Promise<any> {
        return new Promise((resolve, reject) => {
            if(devoir && evaluations.structure.synchronized.devoirs) {
                let url = this.api.done + "?idDevoir="+devoir.id + "&is_evaluated=" +devoir.is_evaluated;
                url += "&idGroupe=" + devoir.id_groupe;
                url += "&has_competence=" + (devoir.competences.all.length > 0 || devoir.nbcompetences > 0);
                /*
                if(front){
                    let calculatedFrontPercent = 0;
                    let _devoir = _.findWhere(this.all, {id : devoir.id});

                    let is_evaluated = devoir.is_evaluated;
                    let has_competence = devoir.nbcompetences > 0 ? true : false;
                    let nbEleves = devoir.eleves.all.length;
                    let nbNotesEtAnnotations = devoir.eleves.filter((eleve) => { return eleve.evaluation.valeur != "" }).length;
                    let nbNotes = devoir.eleves.filter((eleve) => { return (parseInt(eleve.evaluation.valeur) > 0)}).length;
                    let nbAnnotations = nbNotesEtAnnotations - nbNotes;
                    let nbCompetences = devoir.eleves.filter((eleve) => {
                        return eleve.evaluation.competenceNotes.filter(
                            (cnote) => {return cnote.evaluation!=-1}).length > 0
                    }).length;

                    if (is_evaluated && !has_competence) {
                        calculatedFrontPercent = Math.round((nbNotesEtAnnotations* 100 / nbEleves));
                    }
                    else if (is_evaluated && has_competence){
                        calculatedFrontPercent = Math.round((((nbNotes+nbCompetences)/2 + nbAnnotations)*100/(nbEleves)));
                    }
                    else if (has_competence && !is_evaluated) {
                        calculatedFrontPercent = Math.round((nbCompetences * 100 / nbEleves ));
                    }

                    if (_devoir !== undefined) {
                        _devoir.percent = calculatedFrontPercent;
                        model.trigger('apply');
                        resolve();
                    }

                }
                */
                http().getJson(url)
                    .done((res) => {
                        let calculatedPercent = _.findWhere(res, {id : devoir.id});
                        let _devoir = _.findWhere(this.all, {id : devoir.id});
                        if (_devoir !== undefined) {
                            _devoir.percent = calculatedPercent === undefined ? 0 : calculatedPercent.percent;
                        }
                        model.trigger('apply');
                        resolve();
                    })
                    .error(() => {
                        reject();
                    });
            }
        });
    }
}

export interface Devoirs extends Collection<Devoir>, DevoirsCollection {
    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin, name?) => void;
    where: (params) => any;
}


export class Enseignement extends Model {
    competences : Collection<Competence>;
    id;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    constructor () {
        super();
        this.collection(Competence);
    }
}

/**
 * Méthode récursive de l'affichage des sous domaines d'un domaine
 *
 * @param poDomaines la liste des domaines
 * @pbMesEvaluations booleen indiquant d'afficher ou non un domaine
 *
 */
function setVisibleSousDomainesRec (poDomaines, pbVisible) {
    if(poDomaines !== null && poDomaines !== undefined) {
        for (var i = 0; i < poDomaines.all.length; i++) {
            var oSousDomaine = poDomaines.all[i];
            oSousDomaine.visible = pbVisible;
            setVisibleSousDomainesRec(oSousDomaine.domaines, pbVisible);
        }
    }
}

export class BilanFinDeCycle extends Model {
    id : number;
    id_eleve : string;
    id_domaine : number;
    id_etablissement : string;
    owner : string;
    valeur : number;

    constructor(p? : any) {
        super();
        if(p !== undefined){
            this.id = p.id;
            this.id_eleve = p.id_eleve;
            this.id_domaine = p.id_domaine;
            this.id_etablissement = p.id_etablissement;
            this.owner = p.owner;
            this.valeur = p.valeur;
        }
    }

    get api () {
        return {
            createBFC : '/competences/bfc',
            updateBFC : '/competences/bfc?id=' + this.id,
            deleteBFC : '/competences/bfc?id=' + this.id
        }
    }

    saveBilanFinDeCycle () : Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            if (!this.id) {
                this.createBilanFinDeCycle().then((data) => {
                    resolve(data);
                });
            } else {
                this.updateBilanFinDeCycle().then((data) =>  {
                    resolve(data);
                });
            }
        });
    }

    createBilanFinDeCycle () : Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            var _bilanFinDeCycle = {
                id_eleve : this.id_eleve,
                id_domaine : this.id_domaine,
                id_etablissement : this.id_etablissement,
                owner : this.owner,
                valeur : this.valeur
            };
            http().postJson(this.api.createBFC, _bilanFinDeCycle).done ( function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            }) ;
        });
    }

    updateBilanFinDeCycle () : Promise<BilanFinDeCycle> {
        return new Promise((resolve, reject) => {
            var _bilanFinDeCycle = {
                id : this.id,
                id_eleve : this.id_eleve,
                id_domaine : this.id_domaine,
                id_etablissement : this.id_etablissement,
                owner : this.owner,
                valeur : this.valeur
            };
            http().putJson(this.api.updateBFC, _bilanFinDeCycle).done(function (data) {
                if(resolve && (typeof(resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }

    deleteBilanFinDeCycle () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().delete(this.api.deleteBFC).done(function (data) {
                if(resolve && typeof(resolve) === 'function'){
                    resolve(data);
                }
            });
        });
    }
}

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

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;


    /**
     * Méthode activant l'affichage des sous domaines d'un domaine
     *
     * @pbMesEvaluations booleen indiquant d'afficher ou non un domaine
     *
     */
    setVisibleSousDomaines (pbVisible) {
        setVisibleSousDomainesRec(this.domaines, pbVisible);
    }

    constructor (poDomaine?) {
        super();
        var that = this;
        this.collection(Competence);
        this.collection(Domaine);

        if(poDomaine !== undefined) {

            var sousDomaines = poDomaine.domaines;
            var sousCompetences = poDomaine.competences;

            this.updateData(poDomaine);

            if(sousDomaines !== undefined) {
                this.domaines.load(sousDomaines);
            }

            if(sousCompetences !== undefined) {
                this.competences.load(sousCompetences);
            }
        }
    }

}

export class Competence extends Model {
    competences : Collection<Competence>;
    selected : boolean;
    id : number;
    id_competence : number;
    nom : string;
    code_domaine : string;
    ids_domaine : string;
    composer : any;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    constructor () {
        super();
        this.collection(Competence);
    }

    selectChildren (bool) : Promise<any> {
        return new Promise((resolve, reject) => {
            if(this.competences.all.length !== 0){
                _.each(this.competences.all, function(child){
                    child.selected = bool;
                    child.selectChildren(bool).then(resolve);
                });
            }else{
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            }
        });
    }

    findSelectedChildren () {
        if(this.selected === true){
            evaluations.competencesDevoir.push(this.id);
        }
        if(this.competences.all.length !== 0){
            _.each(this.competences.all, function(child){
                child.findSelectedChildren();
            });
        }
    }
}

export class Type extends Model {
    id : number;
}
export class Matiere extends Model {
    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    constructor () {
        super();
        this.collection(SousMatiere);
    }
}
export class Annotation extends Model {
    libelle: string;
    libelle_court: string;

    //TODO Delete when infra-front will be fixed
    collection:  (type, mixin?, name?) => void;
    updateData: (o) => void;

    constructor (o?) {
        super();
        if (o) this.updateData(o);
    }

    toString () {
        return this.libelle;
    }
}
export class SousMatiere extends Model {}
export class CompetenceNote extends Model implements IModel {
    id: number;
    id_devoir: number;
    id_competence: number;
    evaluation: number;
    id_eleve: string;

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
            create: '/competences/competence/note',
            update: '/competences/competence/note?id=' + this.id,
            delete: '/competences/competence/note?id=' + this.id
        }
    }

    constructor(o? : any) {
        super();
        if (o !== undefined) this.updateData(o);
    }

    toJSON() {
        return {
            id: this.id,
            id_devoir: this.id_devoir,
            id_competence: this.id_competence,
            evaluation: this.evaluation,
            id_eleve: this.id_eleve
        }
    }

    create(): Promise<number> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.create, this.toJSON()).done((data) => {
                this.id = data.id;
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data.id);
                }
            });
        });
    }

    update(): Promise<any> {
        return new Promise((resolve, reject) => {
            http().putJson(this.api.update, this.toJSON()).done(function (data) {
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            });
        });
    }

    delete(): Promise<any> {
        return new Promise((resolve, reject) => {
            let that = this;
            http().delete(this.api.delete).done(function (data) {
                delete that.id;
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            });
        });
    }

    save(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (!this.id) {
                this.create().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve(data);
                    }
                });
            } else if (this.evaluation === -1) {
                this.delete().then((data) => {
                    if (resolve && (typeof (resolve) === 'function')) {
                        resolve();
                    }
                });
            } else {
                this.update();
            }
        });
    }
}


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

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

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

export class SuiviCompetenceClasse extends Model {
    domaines : Collection<Domaine>;
    competenceNotes : Collection<CompetenceNote>;
    periode : Periode;

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
            getCompetencesNotesClasse : '/competences/competence/notes/classe/',
            getArbreDomaines : '/competences/domaines/classe/'
        }
    }

    constructor (classe : Classe, periode : any) {
        super();
        this.periode = periode;
        var that = this;

        this.collection(Domaine, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    var url = that.api.getArbreDomaines + classe.id;
                    http().getJson(url).done((resDomaines) => {
                        var url = that.api.getCompetencesNotesClasse + classe.id+"/"+ classe.type_groupe;
                        if (periode !== null && periode !== undefined && periode !== '*') {
                            if(periode.id_type !== undefined)url += "?idPeriode="+periode.id_type;
                        }
                        http().getJson(url).done((resCompetencesNotes) => {
                            if(resDomaines) {
                                for(let i=0; i<resDomaines.length; i++) {
                                    var domaine = new Domaine(resDomaines[i]);
                                    that.domaines.all.push(domaine);
                                    setCompetenceNotes(domaine, resCompetencesNotes, this, classe);
                                }
                            }
                        });
                        if (resolve && typeof (resolve) === 'function') {
                            resolve();
                        }
                    });
                });
            }
        });

    }

    addEvalLibre (eleve){


    }
    findCompetence (idCompetence) {
        for(var i=0; i<this.domaines.all.length; i++) {
            var comp = findCompetenceRec(idCompetence, this.domaines.all[i].competences);
            if(comp !== undefined) {
                return comp;
            }
        }
        return false;
    }


    sync () : Promise<any> {
        return new Promise((resolve, reject) => {
            resolve();
        });
    }
}

export class TableConversion extends  Model {
    valmin : number;
    valmax : number;
    libelle : string;
    ordre : number;
    couleur : string;

    constructor(p? : any) {
        super();
    }
}

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
                                    setCompetenceNotes(domaine, resCompetencesNotes, this, null);
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
            getMaxEvaluationsDomaines(oDomaine, oEvaluationsArray,this.tableConversions.all, false,this.bilanFinDeCycles);
        }
    }

    findCompetence (idCompetence) {
        for(var i=0; i<this.domaines.all.length; i++) {
            var comp = findCompetenceRec(idCompetence, this.domaines.all[i]);
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


function setSliderOptions(poDomaine,tableConversions) {

    poDomaine.myChangeSliderListener = function(sliderId) {
        // Au changement du Slider on détermine si on est dans le cas d'un ajout d'un bfc ou d'une modification
        // Si c'est un ajout on créee l'objet BFC()
        let bfc = poDomaine.bfc;
        if(bfc === undefined){
            bfc = new BilanFinDeCycle();
            bfc.id_domaine = poDomaine.id;
            bfc.id_etablissement = poDomaine.id_etablissement;
            bfc.id_eleve = poDomaine.id_eleve;
        }
        bfc.owner = poDomaine.id_chef_etablissement;
        // Si la valeur modifiée est égale à la moyenne calculée, on ne fait rien ou on supprime la valeur
        if(poDomaine.slider.value === poDomaine.moyenne){
            if(bfc.id !== undefined){
                poDomaine.bfc.deleteBilanFinDeCycle().then((res) => {
                    if (res.rows === 1) {
                        poDomaine.bfc = undefined;
                        poDomaine.lastSliderUpdated =  poDomaine.moyenne;
                    }
                    model.trigger('apply');
                });
            }
        }else{
            // Sinon on ajoute ou on modifie la valeur du BFC
            bfc.valeur = poDomaine.slider.value;
            bfc.saveBilanFinDeCycle().then((res) => {
                if(res !== undefined && res.id !== undefined){
                    if(bfc.id === undefined){
                        bfc.id = res.id;
                    }
                    poDomaine.bfc = bfc;
                    poDomaine.lastSliderUpdated = bfc.valeur;
                    model.trigger('apply');
                }
            });
        }
    };

    poDomaine.slider = {
        options: {
            ticksTooltip: function(value) {
                return String(poDomaine.moyenne);
            },
            disabled: parseFloat(poDomaine.moyenne) === -1,
            floor: _.min(tableConversions, function(Conversions){ return Conversions.ordre; }).ordre - 1,
            ceil: _.max(tableConversions, function(Conversions){ return Conversions.ordre; }).ordre,
            step: 1,
            showTicksValues: false,
            showTicks: true,
            showSelectionBar: true,
            hideLimitLabels : true,
            id : poDomaine.id,
            onEnd: poDomaine.myChangeSliderListener

        }
    };

    let moyenneTemp = undefined;
    // si Une valeur a été modifiée par le chef d'établissement alors on prend cette valeur
    if(poDomaine.bfc !== undefined && poDomaine.bfc.valeur !== undefined){
        moyenneTemp = poDomaine.bfc.valeur;
    }else{
        moyenneTemp = poDomaine.moyenne;
    }

    // Récupération de la moyenne convertie
    let maConvertion = utils.getMoyenneForBFC(moyenneTemp,tableConversions);

    // si ça ne rentre dans aucune case
    if(maConvertion === -1 ){
        poDomaine.slider.value = -1 ;
        poDomaine.slider.options.getSelectionBarClass = function(){ return '#d8e0f3';};
        poDomaine.slider.options.translate = function(value,sliderId,label){
            let l = '#label#';
            if (label === 'model') {

                l = '<b>#label#</b>';
            }
            return l.replace('#label#', lang.translate('evaluations.competence.unevaluated'));
        };

    }else{
        poDomaine.slider.value = maConvertion ;
        poDomaine.slider.options.getSelectionBarClass = function(value){
            let ConvertionOfValue = _.find(tableConversions,{ordre: value});
            if(ConvertionOfValue !== undefined)
                return ConvertionOfValue.couleur;
        };
        poDomaine.slider.options.translate = function(value,sliderId,label){
            let l = '#label#';
            if (label === 'model') {

                l = '<b>#label#</b>';
            }
            let libelle = _.find(tableConversions,{ordre: value});
            if(libelle !== undefined)
                return l.replace('#label#', lang.translate(libelle.libelle));
        };
    }
};

function getMaxEvaluationsDomaines(poDomaine, poMaxEvaluationsDomaines,tableConversions, pbMesEvaluations,bfcsParDomaine) {
    // si le domaine est évalué, on ajoute les max de chacunes de ses competences
    if(poDomaine.evaluated) {
        for (let i = 0; i < poDomaine.competences.all.length; i++) {
            var competencesEvaluations = poDomaine.competences.all[i].competencesEvaluations;
            var _t = competencesEvaluations;

            // filtre sur les compétences évaluées par l'enseignant
            if (pbMesEvaluations) {
                _t = _.filter(competencesEvaluations, function (competence) {
                    return competence.owner !== undefined && competence.owner === model.me.userId;
                });
            }

            if (_t && _t.length > 0) {
                // TODO récupérer la vrai valeur numérique :
                // par exemple 0 correspond à rouge ce qui mais ça correspond à une note de 1 ou 0.5 ou 0 ?
                poMaxEvaluationsDomaines.push(_.max(_t, function (_t) {
                    return _t.evaluation;
                }).evaluation + 1);
            }
        }
    }

    // calcul de la moyenne pour les sous-domaines
    if(poDomaine.domaines) {
        for(var i=0; i<poDomaine.domaines.all.length; i++) {
            // si le domaine parent n'est pas évalué, il faut vider pour chaque sous-domaine les poMaxEvaluationsDomaines sauvegardés
            if(!poDomaine.evaluated) {
                poMaxEvaluationsDomaines = [];
            }
            // On ajoute les informations utiles au sous-domaine
            poDomaine.domaines.all[i].id_eleve = poDomaine.id_eleve;
            poDomaine.domaines.all[i].id_etablissement = poDomaine.id_etablissement;
            poDomaine.domaines.all[i].id_chef_etablissement= poDomaine.id_chef_etablissement;
            if(bfcsParDomaine !== undefined && bfcsParDomaine.all.length>0){
                let tempBFC = _.findWhere(bfcsParDomaine.all, {id_domaine : poDomaine.domaines.all[i].id});
                if(tempBFC !== undefined){
                    poDomaine.domaines.all[i].bfc = tempBFC;
                }
            }
            getMaxEvaluationsDomaines(poDomaine.domaines.all[i], poMaxEvaluationsDomaines,tableConversions, pbMesEvaluations,bfcsParDomaine);
        }
    }

    // mise à jour de la moyenne
    if (poMaxEvaluationsDomaines.length > 0) {
        poDomaine.moyenne = utils.average(_.without(poMaxEvaluationsDomaines,0) );
    } else {
        poDomaine.moyenne = -1;
    }

    setSliderOptions(poDomaine,tableConversions)

    // Chefs d'établissement

    //Si l'utilisateur n'est pas un chef d'établissement il ne peut pas modifier le slider
    if(!isChefEtab()){
        poDomaine.slider.options.readOnly = true;
    }
}

function findCompetenceRec (piIdCompetence, poDomaine) {
    for (var i = 0; i < poDomaine.competences.all.length; i++) {
        // si compétences trouvée on arrete le traitement
        if(poDomaine.competences.all[i].id === piIdCompetence) {
            return poDomaine.competences.all[i];
        }
    }

    // recherche dans les sous-domaines
    if(poDomaine.domaines) {
        for(var i=0; i<poDomaine.domaines.all.length; i++) {
            let comp = findCompetenceRec(piIdCompetence, poDomaine.domaines.all[i]);
            if(comp !== undefined){
                return comp;
            }
        }
    }
}

function setCompetenceNotes(poDomaine, poCompetencesNotes, object, classe) {
    if(poDomaine.competences) {
        _.map(poDomaine.competences.all, function (competence) {
            competence.competencesEvaluations = _.where(poCompetencesNotes, {
                id_competence: competence.id,
                id_domaine: competence.id_domaine
            });
            if (object.composer.constructor.name === 'SuiviCompetenceClasse') {
                for (var i = 0; i < classe.eleves.all.length; i++) {
                    var mine = _.findWhere(competence.competencesEvaluations, {id_eleve : classe.eleves.all[i].id, owner : model.me.userId});
                    var others = _.filter(competence.competencesEvaluations, function (evaluation) { return evaluation.owner !== model.me.userId; });
                    if (mine === undefined)
                        competence.competencesEvaluations.push(new CompetenceNote({evaluation : -1, id_competence: competence.id, id_eleve : classe.eleves.all[i].id, owner : model.me.userId}));
                    if (others.length === 0)
                        competence.competencesEvaluations.push(new CompetenceNote({evaluation : -1, id_competence: competence.id, id_eleve : classe.eleves.all[i].id}));
                }
            }
        });
    }

    if( poDomaine.domaines) {
        for (var i = 0; i < poDomaine.domaines.all.length; i++) {
            setCompetenceNotes(poDomaine.domaines.all[i], poCompetencesNotes, object, classe);
        }
    }
}


export class Enseignant extends Model{
    id : string;
    displayName : string;

    constructor(p? : any) {
        super();
    }
}

export class Remplacement extends Model implements IModel{

    // DATABASE FIELDS
    titulaire : Enseignant;
    remplacant : Enseignant;
    date_debut : any;
    date_fin : any;
    id_etablissement : string;


    // OTHER FIELDS
    selected : boolean;


    get api () {
        return {
            create : '/competences/remplacement/create',
            update : '/competences/remplacement/update',
            delete : '/competences/remplacement/delete'
        }
    }

    constructor(p? : any) {
        super();
        this.selected = false;

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

    update () : Promise<any> {
        return new Promise((resolve, reject) => {
            http().postJson(this.api.update, this.toJSON()).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }


    remove () : Promise<any> {
        var that = this;
        return new Promise((resolve, reject) => {
            http().delete(this.api.delete, this.toJSON()).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(that);
                }
            })
                .error(function () {
                    reject(that);
                });
        });
    }

    toJSON() {
        return {
            id_titulaire: this.titulaire.id,
            libelle_titulaire : this.titulaire.displayName,
            id_remplacant: this.remplacant.id,
            libelle_remplacant : this.remplacant.displayName,
            date_debut: utils.getFormatedDate(this.date_debut,"YYYY-MM-DD"),
            date_fin: utils.getFormatedDate(this.date_fin,"YYYY-MM-DD"),
            id_etablissement : this.id_etablissement
        }
    }
}

export class GestionRemplacement extends Model {
    remplacements : Collection<Remplacement> | any; // liste des remplacements en cours
    selectedRemplacements : Collection<Remplacement> | any; // liste des remplacements sélectionnés
    remplacement : Remplacement; // remplacementen cours d'ajout
    enseignants : Collection<Enseignant>; // liste des enseignants de l'établissment
    sortType : string; // type de tri de la liste des remplaçants
    sortReverse : boolean; // sens de tri de la liste des remplaçants
    showError : boolean; // condition d'affichage d'un message d'erreur
    confirmation : boolean; // condition d'affichage de la popup de confirmation
    selectAll : boolean; // booleen de sélection de tous/aucun remplacement/s

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    get api () {
        return {
            deleteMultiple : '/competences/remplacements/delete', // TODO A coder
            enseignants : '/competences/user/list?structureId='+model.me.structures[0]+'&profile=Teacher',
            remplacements : '/competences/remplacements/list'
        }
    }

    constructor(p? : any) {
        super();
        var that = this;

        this.showError = false;
        this.selectAll = false;
        this.confirmation = false;
        this.remplacement = new Remplacement();


        this.remplacement.date_debut = new Date();

        var today = new Date();
        today.setFullYear(today.getFullYear() + 1);
        this.remplacement.date_fin = today;

        this.collection(Enseignant, {
            sync : function () : Promise<any> {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.enseignants).done(function(res) {
                        this.load(res);
                        if(resolve && (typeof(resolve) === 'function')) {
                            resolve();
                        }
                    }.bind(this));
                });
            }
        });

        this.collection(Remplacement, {
            sync : function () : Promise<any> {
                return new Promise((resolve, reject) => {
                    http().getJson(that.api.remplacements).done(function(resRemplacements) {

                        this.removeAll();

                        for(var i=0; i<resRemplacements.length; i++) {
                            var remplacementJson = resRemplacements[i];

                            var remplacement = new Remplacement();
                            remplacement.titulaire = new Enseignant();
                            remplacement.titulaire.id = remplacementJson.id_titulaire;
                            remplacement.titulaire.displayName = remplacementJson.libelle_titulaire;

                            remplacement.remplacant = new Enseignant();
                            remplacement.remplacant.id = remplacementJson.id_remplacant;
                            remplacement.remplacant.displayName = remplacementJson.libelle_remplacant;


                            remplacement.date_debut = remplacementJson.date_debut;
                            remplacement.date_fin = remplacementJson.date_fin;
                            remplacement.id_etablissement = remplacementJson.id_etablissement;

                            this.all.push(remplacement);


                        }

                        if(resolve && (typeof(resolve) === 'function')) {
                            resolve();
                        }
                    }.bind(this));
                });
            }
        });

        this.selectedRemplacements = [];

    }
}
export class Responsable extends Model {
    id: string;
    externalId : string;
    displayName : string;
    selected : boolean;

}


export let evaluations = new Evaluations();


model.build = function () {
    require('angular-chart.js');
    (this as any).evaluations = evaluations;
};