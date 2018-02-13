import {_, http, template} from 'entcore';
import {Domaine} from '../models/teacher';
import * as utils from '../utils/teacher';


export const itemsCompetences = {
    title: 'Ajout/Modification items de compétences',
    description: "Permet d'ajouter ou de modifier les items de compétences pour un établissement",
    that: undefined,
    controller: {
        init: function () {
            itemsCompetences.that = this;
            this.idStructure = this.source.idStructure;
            this.cycles = this.source.cycles;
            this.search = {
                keyword: undefined
            };
            this.synchronized = {
                domaines: false,
                enseingments: false
            };
            this.opened.errorDeletePersoItem = false;
            this.newItem = {};
            this.opened = this.$parent.opened;
            this.lastSelectedCycle = this.$parent.lastSelectedCycle;
            this.showCompetencesDomaine = {};
            this.displayFilterDomaine = false;
            this.enableWatchers();
            this.getCompetences();
        },
        initSource: function () {
        },
        getCompetences: function () {
            http().getJson(`/competences/domaines?idStructure=${this.idStructure}`)
                .done((resDomaines) => {
                    if (resDomaines) {
                        let _res = [];
                        for (let i = 0; i < resDomaines.length; i++) {

                            let domaine = new Domaine(resDomaines[i]);
                            _res.push(domaine);
                        }
                        itemsCompetences.that.domaines = {
                            all: _res
                        };
                        itemsCompetences.that.synchronized.domaines = true;
                    }
                })
                .error(function () {
                    console.log('domaine not founded');
                    itemsCompetences.that.domaines = {
                        all: []
                    };
                }).bind(this);
            http().getJson(`/competences/enseignements?idStructure=${this.idStructure}`).done(function (res) {
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
                itemsCompetences.that.enseignements = {
                    all: res
                };
                itemsCompetences.that.synchronized.enseignements = true;
                itemsCompetences.that.initCycles();
            }.bind(this));
        },
        enableWatchers: function () {
            this.$watch(() => this.$parent.lastSelectedCycle, () => {
                this.lastSelectedCycle = this.$parent.lastSelectedCycle;
                utils.safeApply(this);
            });
            this.$watch( () => this.search.keyword, (newValue, oldValue) => {
                this.search.haschange = (newValue !== oldValue);
                utils.safeApply(this);
            });
        },
        initCycles: function () {
            if (this.synchronized.enseignements && this.synchronized.domaines) {
                let domaines = _.groupBy(this.domaines.all, 'id_cycle');
                for (let index in domaines) {
                    let cycle = _.findWhere(this.source.cycles, {id_cycle: parseInt(index)});
                    if (cycle !== undefined ) {
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
            itemsCompetences.that.enseignementsFilter = {};
            itemsCompetences.that.competencesFilter = {};
            itemsCompetences.that._domaines = [];
            itemsCompetences.that.showCompetencesDomaine = {};
            itemsCompetences.that.displayFilterDomaine = false;
            for (let i = 0; i < itemsCompetences.that.enseignements.all.length; i++) {
                let currEnseignement = itemsCompetences.that.enseignements.all[i];
                itemsCompetences.that.enseignementsFilter[currEnseignement.id] = {
                    isSelected: pbInitSelected,
                    nomHtml: currEnseignement.nom
                };
                // on initialise aussi les compétences
                this.initFilterRec(currEnseignement.competences, pbInitSelected);
            }
            itemsCompetences.that._domaines = _.sortBy(this._domaines, 'code_domaine');
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
                            && itemsCompetences.that.showCompetencesDomaine[currCompetence.ids_domaine_int[0]] === true)
                        || itemsCompetences.that.showCompetencesDomaine.length === undefined) {
                        comp = _.findWhere(poCompetences.all, {id: poCompetences.all[i].id}) !== undefined;
                        if (comp !== undefined) _b = false;
                        itemsCompetences.that.competencesFilter[
                            currCompetence.id + '_' + currCompetence.id_enseignement] = {
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
                .done(() => {
                    this.opened.lightboxDeletePersoItem = false;
                    this.opened.errorDeletePersoItem = false;
                    this.getCompetences();
                    utils.safeApply(this);
                })
                .error( () => {
                    this.opened.errorDeletePersoItem = true;
                    console.log('delete not work');
                    utils.safeApply(this);
                }).bind(this);
        },

        // suppression de la personnalisation des items de compétences
        openDeletePersoItem: function () {
            this.opened.lightboxDeletePersoItem = true;
        },
        openCreateItem: function (connaissance, domaines) {
            this.itemsCompetences.that.opened.lightboxCreateItem = true;
            this.itemsCompetences.that.newItem = {
                ismanuelle: true,
                nom: undefined,
                id_parent: connaissance.id,
                id_cycle: connaissance.id_cycle,
                id_enseignement: connaissance.id_enseignement,
                id_type: 2,
                ids_domaine: connaissance.ids_domaine_int,
                id_etablissement: this.itemsCompetences.that.structure.id
            };
            this.itemsCompetences.that.printDomaines = _.clone(domaines);
            this.itemsCompetences.that.selectedDomaines = this.itemsCompetences.that.newItem.ids_domaine;
            utils.safeApply(this.itemsCompetences.that);
        }.bind(this),

        initSelectDomaine: function (domaine) {
            domaine.selected = _.contains(this.itemsCompetences.that.selectedDomaines, domaine.id);
        }.bind(this),

        selectDomaine: function (domaine) {
            if ( this.itemsCompetences.that.newItem.hasOwnProperty('id') ) {
                this.itemsCompetences.that.updatedDomaineId = domaine.id;
                this.itemsCompetences.that.saveItem(this.itemsCompetences.that.newItem, 'updateDomaine');
            }
            else {
                if (domaine.selected && !_.contains(this.itemsCompetences.that.newItem.ids_domaine, domaine.id)) {
                    this.itemsCompetences.that.newItem.ids_domaine.push(domaine.id);
                }
                else if (!domaine.selected) {
                    this.itemsCompetences.that.newItem.ids_domaine =
                        _.without(this.itemsCompetences.that.newItem.ids_domaine, domaine.id);
                }
            }
        }.bind(this),

        // Affichage des Domaines d'une compétence
        openItemDomaine: function (competence, competencesFilter, domaines) {
            itemsCompetences.that.newItem = competence;
            itemsCompetences.that.newItem.ids_domaine = competence.ids_domaine_int;
            let _c = competencesFilter[competence.id + '_' + competence.id_enseignement];
            this.itemsCompetences.that.printDomaines = _.clone(domaines);
            this.itemsCompetences.that.selectedDomaines = _c.data.ids_domaine_int;
            if (template.isEmpty('patchwork' + competence.id)) {
                template.open('patchwork' + competence.id,
                    '../../../competences/public/template/personnels/param_items/showDomaine');
            }
            else {
                template.close('patchwork' + competence.id);
            }
            if (this.itemsCompetences.that.lastCompetence !== undefined
                && this.itemsCompetences.that.lastCompetence.id !== competence.id
                && !template.isEmpty('patchwork' + this.itemsCompetences.that.lastCompetence.id)) {
                template.close('patchwork' + this.itemsCompetences.that.lastCompetence.id);
                utils.safeApply(this.itemsCompetences.that);
            }
            this.itemsCompetences.that.lastCompetence = competence;
            utils.safeApply(this.itemsCompetences.that);
        }.bind(this),

        jsonCreateItem: function (item) {
            return {
                nom: item.nom,
                id_etablissement: itemsCompetences.that.structure.id,
                id_parent: item.id_parent,
                id_type: item.id_type,
                id_cycle: item.id_cycle,
                id_enseignement: item.id_enseignement,
                ids_domaine: item.ids_domaine
            };
        },
        jsonUpdateMaskItem: function (item) {
            return {
                id: item.id,
                id_etablissement: itemsCompetences.that.structure.id,
                masque: item.masque
            };
        },
        jsonUpdateNameItem: function (item) {
            return {
                id: item.id,
                id_etablissement: itemsCompetences.that.structure.id,
                nom: item.nom
            };
        },
        jsonUpdateDomaineItem: function (item) {
            return {
                id: item.id,
                id_etablissement: itemsCompetences.that.structure.id,
                id_domaine: itemsCompetences.that.updatedDomaineId
            };
        },
        saveItem: function (item, action) {
            switch (action) {
                case 'create': {
                    http().postJson(`competences/competence`, this.jsonCreateItem(item))
                        .done(() => {
                            this.opened.lightboxCreateItem = false;
                            this.getCompetences();
                            utils.safeApply(this);
                        })
                        .error(function () {
                            this.opened.lightboxCreateItem = false;
                            console.log(' error createItem');
                        }).bind(this);
                    break;
                }
                case 'mask': {
                    console.dir('mask Off' + item.nom);
                    console.log('' );
                    http().putJson(`competences/competence`, this.jsonUpdateMaskItem(item))
                        .done(() => {
                            item.masque = !item.masque;
                            utils.safeApply(this);
                        })
                        .error(function () {
                            console.log(' error Mask Item');
                        }).bind(this);
                    break;
                }
                case 'rename': {
                    http().putJson(`competences/competence`, this.jsonUpdateNameItem(item))
                        .done(() => {
                            this.getCompetences();
                            utils.safeApply(this);
                        })
                        .error(function () {
                            console.log(' error Rename Item');
                        }).bind(this);
                    break;
                }
                case 'updateDomaine': {
                    http().putJson(`competences/competence`, this.jsonUpdateDomaineItem(item))
                        .done(() => {
                            this.getCompetences();
                            utils.safeApply(this);
                        })
                        .error(function () {
                            console.log(' error updateDomaine Item');
                        }).bind(this);
                    break;
                }
                case 'reinitItem': {
                    console.dir('reinit Item');
                    break;
                }
                default: break;
            }
        },
        trash: function (item) {
            console.dir('trash' + item.nom);
            http().delete(`competences/competence?id=${item.id}&id_etablissement=${item.id_etablissement}`)
                .done(() => {
                    this.getCompetences();
                    utils.safeApply(this);
                })
                .error(function () {
                    console.log(' error createItem');
                }).bind(this);
        }
    }
};