import {_, http} from 'entcore';
import {Domaine} from '../models/teacher';
import * as utils from '../utils/teacher';


export const itemsCompetences = {
    title: 'Ajout/Modification items de compétences',
    description: "Permet d'ajouter ou de modifier les items de compétences pour un établissement",
    controller: {
        init: function () {
            this.idStructure = this.source.idStructure;
            this.cycles = this.source.cycles;
            this.search = {
                keyword: undefined
            };
            this.synchronized = {
                domaines: false,
                enseingments: false
            };
            this.lastSelectedCycle = this.$parent.lastSelectedCycle;
            this.showCompetencesDomaine = {};
            this.displayFilterDomaine = false;
            this.$watch(() => this.$parent.lastSelectedCycle, () => {
                this.lastSelectedCycle = this.$parent.lastSelectedCycle;
                utils.safeApply(this);
            });
            this.$watch( () => this.search.keyword, (newValue, oldValue) => {
                this.search.haschange = (newValue !== oldValue);
                utils.safeApply(this);
            });
            http().get(`/competences/domaines?idStucture=${this.idStructure}`)
                .done((resDomaines) => {
                    if (resDomaines) {
                        let _res = [];
                        for (let i = 0; i < resDomaines.length; i++) {

                            let domaine = new Domaine(resDomaines[i]);
                            _res.push(domaine);
                        }
                        this.domaines = {
                            all: _res
                        };
                        this.synchronized.domaines = true;
                    }
                })
                .error(function () {
                    console.log('domaine not founded');
                    this.domaines = {
                        all: []
                    };
                }).bind(this);
            http().get('/competences/enseignements').done(function (res) {
                _.forEach(res, function (enseignement) {
                    enseignement.competences = {
                        all: enseignement['competences_1']
                    };
                    _.map(enseignement.competences.all, function (competence) {
                        return competence.composer = enseignement;
                    });
                    _.forEach(enseignement.competences.all, function (competence) {
                        if (competence['competences_2'].length > 0) {
                            competence.competences = {
                                all: competence['competences_2']
                            };
                            _.map(competence.competences.all, function (sousCompetence) {
                                return sousCompetence.composer = competence;
                            });
                        }
                        delete competence['competences_2'];
                    });
                    delete enseignement['competences_1'];
                });
                this.enseignements = {
                    all: res
                };
                this.synchronized.enseignements = true;
                this.initCycles();
            }.bind(this));
        },
        initSource: function () {
        },
        initCycles: function () {
            if (this.synchronized.enseignements && this.synchronized.domaines) {
                let domaines = _.groupBy(this.domaines.all, 'id_cycle');
                for (let index in domaines) {
                    let cycle = _.findWhere(this.source.cycles, {id_cycle: parseInt(index)});
                    if (cycle !== undefined) {
                        cycle.domaines = {
                            all: domaines[index]
                        };
                        cycle.enseignements = {
                            all: []
                        };
                        _.forEach(this.enseignements.all, function (enseignement) {
                            let _enseignement = {
                                id: enseignement.id,
                                ids_domaine_int: enseignement.ids_domaine_int,
                                nom: enseignement.nom,
                                competences: {all: []}
                            };
                            _.forEach(enseignement.competences.all, function (connaissance) {
                                if (connaissance.id_cycle === cycle.id_cycle) {
                                    _enseignement.competences.all.push(connaissance);
                                }
                            });
                            if (_enseignement.competences.all.length > 0) {
                                cycle.enseignements.all.push(_enseignement);
                            }
                        });
                    }
                }
                this.searchBilan = {
                    parDomaine: 'false'
                };
                this.initFilter(true);
                this.$apply();
                console.log(' Sniplets ItemsCompetences Loaded');
            }
        },
        initFilter: function (pbInitSelected) {
            this.enseignementsFilter = {};
            this.competencesFilter = {};
            this._domaines = [];
            this.showCompetencesDomaine = {};
            this.displayFilterDomaine = false;
            for (let i = 0; i < this.enseignements.all.length; i++) {
                let currEnseignement = this.enseignements.all[i];
                this.enseignementsFilter[currEnseignement.id] = {
                    isSelected: pbInitSelected,
                    nomHtml: currEnseignement.nom
                };
                // on initialise aussi les compétences
                this.initFilterRec(currEnseignement.competences, pbInitSelected);
            }
            this._domaines = _.sortBy(this._domaines, 'code_domaine');
        },

        /**
         * Initialise le nom html des compétences (pour gérer le surlignement lors des recherches)
         *
         * @param poCompetences la liste des compétences
         * @param pbInitSelected boolean d'initialisation
         */

        initFilterRec: function (poCompetences, pbInitSelected) {
            if (poCompetences !== undefined) {
                let _b = false;
                let comp: any = null;
                for (let i = 0; i < poCompetences.all.length; i++) {
                    let currCompetence = poCompetences.all[i];
                    if ((currCompetence.ids_domaine_int !== undefined && currCompetence.ids_domaine_int[0].lengh === 1
                            && this.showCompetencesDomaine[currCompetence.ids_domaine_int[0]] === true)
                        || this.showCompetencesDomaine.length === undefined) {
                        comp = _.findWhere(poCompetences.all, {id: poCompetences.all[i].id}) !== undefined;
                        if (comp !== undefined) _b = false;
                        this.competencesFilter[currCompetence.id + '_' + currCompetence.id_enseignement] = {
                            isSelected: _b,
                            nomHtml: currCompetence.nom,
                            data: currCompetence
                        };

                        this.initFilterRec(currCompetence.competences, pbInitSelected);
                    }
                }
            }
        },

        deletePersoItem: function () {
            http().delete(`/competences/items/${this.idStructure}`)
                .done((res) => {
                    // this.getPersoItem().then(() => {
                    console.log('delete work');
                    this.opened.lightboxDeletePersoItem = false;
                    utils.safeApply(this);
                    // });
                })
                .error(function () {
                    console.log('delete not work');
                }).bind(this);
        }
    }
};