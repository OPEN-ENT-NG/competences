import { Model, Collection, http, moment, _, model, idiom as lang } from 'entcore';
import * as utils from '../../utils/teacher';
import {
    Eleve,
    Enseignant,
    evaluations,
    Devoir,
    DevoirsCollection,
    Devoirs,
    Classe,
    Matiere,
    Type,
    Enseignement,
    Annotation,
    ReleveNote,
    Cycle,
    Responsable,
    NiveauCompetence,
    Defaultcolors,
    TypePeriode,
    Utils
} from './index';

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
        if (Utils.isChefEtab()) {
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
                    if (Utils.isChefEtab()) {
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
                        if (!Utils.isChefEtab()) {
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
                if (Utils.isChefEtab()) {
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
            if (Utils.isChefEtab()) {
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