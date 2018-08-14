import {notify, template, _} from 'entcore';
import http from "axios";
import {evaluations} from '../models/teacher';
import * as utils from '../utils/teacher';

console.log("here");

export const bilanPeriodique = {
    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    controller: {
        async init() {
            this.idStructure = this.source.idStructure;
            this.selected = {EPI: true, AP: false, parcours: false};
            await evaluations.sync();
            await evaluations.structure.sync();
            this.selectedElements = [];
            // this.libelleTheme = null;
            this.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            this.empty = {
                libelle: [],
                code: [],
                themes: [],
            };
            await this.getElements();
        },

        async openElementLigthbox(param?) {
            await this.getThematique(this.getTypeElement());
            this.classes = evaluations.structure.classes;
            this.enseignants = evaluations.structure.enseignants;
            this.modifElem = param;

            if(param){
                this.dataELem = {
                    theme: this.selectedElements[0].theme,
                    idEtablissement: evaluations.structure.id,
                    classes: this.selectedElements[0].groupes,
                    ens_mat: this.selectedElements[0].intervenantsMatieres,
                    libelle: this.selectedElements[0].libelle,
                    description: this.selectedElements[0].description
                };
            } else {
                this.dataELem = {
                    idEtablissement : evaluations.structure.id,
                    classes: [],
                    ens_mat: []
                };
            }
            this.opened.lightboxCreatePE = true;
            template.open('lightboxCreatePE', '../../../competences/public/template/behaviours/sniplet-createProjetEducatif');
            utils.safeApply(this);
            console.log("lightboxCreatePE this.classes", this.classes)
            console.log("$scope", this)

        },

        async createThematique(thematique) {
            try {
                if (thematique !== undefined && thematique !== null) {
                    if (thematique.code !== undefined && thematique.code !== null
                        && thematique.libelle !== undefined && thematique.libelle !== null) {
                        await http.post('/competences/thematique',
                            {code: thematique.code, libelle: thematique.libelle, type: 1});
                    }
                    this.getThematique(this.getTypeElement());
                    this.showAddtheme = false;
                    this.emptyLightbox();
                }
            } catch (e) {
                notify.error('Problème lors de la création de la thématique');
                console.error('Problème lors de la création de la thématique');
                throw e;
            }
            utils.safeApply(this);
        },

        async createElementBilanPeriodique() {
            try {
                if (this.dataELem !== undefined && this.dataELem !== null) {
                    this.dataELem.type = this.getTypeElement();
                    const {data} = await http.post(`/competences/elementBilanPeriodique?type=${this.dataELem.type}`, this.dataELem);
                    this.elements.push(data);
                }
                this.opened.lightboxCreatePE = false;
                this.getElements();
                this.emptyLightbox();
            } catch (e) {
                notify.error('Problème lors de la création de l\'élément du bilan périodique');
                console.error('Problème lors de la création de l\'élément du bilan périodique');
                throw e;
            }
            utils.safeApply(this);
        },

        async getThematique(type) {
            try {
                let data = await http.get(`/competences/thematique?type=${type}`);
                this.themes = data.data;
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.theme.get.error');
            }
        },

        translate: function (key) {
            return utils.translate(key);
        },

        async syncMatieresEnseignant(enseignant) {
            try {
                let data = await http.get(`/viescolaire/matieres?idEnseignant=${enseignant.id}&idEtablissement=${evaluations.structure.id}&isEnseignant=${true}`);
                this.matieres = data.data;
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.matiere.get.error');
            }
        },

        pushData: function (param1, param2?) {
            if (param2) {
                if (!_.findWhere(this.dataELem.ens_mat, {ens: param1, mat: param2})) {
                    this.dataELem.ens_mat.push({ens: param1, mat: param2});
                }

            } else {
                if (!_.contains(this.dataELem.classes, param1)) {
                    this.dataELem.classes.push(param1)
                }
            }
        },

        emptyLightbox: function() {
            this.data
            this.empty
            this.selectedElements
        },

        async getElements() {
            try {
                let {data} = await http.get(`/competences/elementsBilanPeriodique?idEtablissement=${evaluations.structure.id}`);
                this.elements = data;
                this.$apply();
            } catch (e) {
                notify.error('evaluations.elements.get.error');
            }
        },

        getTypeElement: function () {
            let type = null;
            if (this.selected.EPI) type = 1;
            if (this.selected.AP) type = 2;
            if (this.selected.parcours) type = 3;
            return type;
        },

        async tryDeleteElements(elements) {
            await this.getAppreciations(elements);
            if (this.appreciations !== undefined) {
                (this.appreciations.length > 0) ? this.opened.lightboxConfirmDeleteElements = true
                    : await this.deleteElements(elements);
            } else {
                await this.deleteElements(elements);
            }
            utils.safeApply(this);
        },

        async deleteElements(elements) {
            try {
                let url = "/competences/elementsBilanPeriodique?idEtablissement=" + evaluations.structure.id;
                for (var i = 0; i < elements.length; i++) {
                    url += "&idElement=" + elements[i].id;
                }
                await http.delete(url);
                _.forEach(elements, (element) => {
                    element.selected = false;
                });
                this.getElements();
            } catch (e) {
                notify.error('evaluations.elements.delete.error');
            }
        },

        async getAppreciations(elements) {
            try {
                let url = "/competences/appreciations?idEtablissement=" + evaluations.structure.id;
                for (var i = 0; i < elements.length; i++) {
                    url += "&idElement=" + elements[i].id;
                }
                let data = await http.get(url);
                this.appreciations = data.data;
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.appreciations.get.error');
            }
        },

        async updateElement(element) {
            try {
                await http.put(`/competences/elementBilanPeriodique?idElement=${element.id}&type=${this.dataELem.type}`, this.dataELem);
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.elements.update.error');
            }
        },

        selectUnselectElement: function (element) {
            if (!_.contains(this.selectedElements, element)) {
                this.selectedElements.push(element);
            } else {
                this.selectedElements = _.without(this.selectedElements, element);
            }
        },

        checkSelectedElements: function (elements) {
            this.selectedElements = _.filter(elements, function (element) {
                return element.selected === true;
            });
            if (this.selectedElements.length === 0) {
                this.opened.lightboxConfirmDeleteElements = false;
            }
            return this.selectedElements;
        },

        selectAllElements: function (elements) {
            this.search.elementAll = !this.search.elementAll;
            _.forEach(elements, (element) => {
                element.selected = this.search.elementAll;
            });
        },

        filterItem: function () {
        return (item) => {
                if (this.selected.EPI) {
                    if (item.theme && item.libelle)
                    return item;
                }
                else if (this.selected.AP) {
                    if (!item.theme && item.libelle)
                        return item;
                }
                else if (this.selected.parcours) {
                    if (item.theme && !item.libelle)
                        return item;
                }

            }
        },

        selectUnselectChip: function (element) {
            if (!_.contains(this.selectedChips, element)) {
                this.selectedChips.push(element);
            } else {
                this.selectedChips = _.without(this.selectedChips, element);
            }
        },

        openToggle: function (ens_mat) {
            this.selectedChips = _.filter(ens_mat, function (element) {
                return element.selected === true;
            })
            return this.selectedChips;
        },

        async deleteEnsMat(ens_mat) {
            _.forEach(ens_mat, (element) => {
                element.selected = false;
            });
            console.log("delete");
        },

        addTheme: function (theme) {
            this.libelleTheme = theme.libelle;
            this.dataELem.theme = theme.id;
            }
    }
}