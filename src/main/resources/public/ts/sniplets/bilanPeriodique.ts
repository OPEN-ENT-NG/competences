import {notify, template, _} from 'entcore';
import http from "axios";
import {evaluations} from '../models/teacher';
import * as utils from '../utils/teacher';
import {itemsCompetences} from "./itemsCompetences";

console.log("here");

export const bilanPeriodique = {
    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    that: undefined,
    controller: {
        async init() {
            bilanPeriodique.that = this;
            this.idStructure = this.source.idStructure;
            this.selected = {EPI: true, AP: false, parcours: false};
            await evaluations.sync();
            await evaluations.structure.sync();
            this.selectedElements = [];
            this.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            this.thematique = {
                code: "",
                libelle: ""
            };
            await this.getElements();
        },

        async openElementLigthbox(param?) {
            bilanPeriodique.that.emptyLightbox();
            await this.getThematique(this.getTypeElement());
            bilanPeriodique.that.classes = evaluations.structure.classes;
            bilanPeriodique.that.enseignants = evaluations.structure.enseignants;
            bilanPeriodique.that.modifElem = param;
            delete this.dataELem;

            if(param) {
                bilanPeriodique.that.dataELem = {
                    id: this.selectedElements[0].id,
                    idEtablissement: evaluations.structure.id,
                    theme: this.selectedElements[0].theme,
                    type: this.selectedElements[0].type,
                    classes: this.selectedElements[0].groupes,
                    ens_mat: this.selectedElements[0].intervenantsMatieres,
                    libelle: this.selectedElements[0].libelle,
                    description: this.selectedElements[0].description
                };
                this.dataELem = bilanPeriodique.that.dataELem;
            } else {
                bilanPeriodique.that.dataELem = {
                    idEtablissement : evaluations.structure.id,
                    classes: [],
                    ens_mat: []
                };
                this.dataELem = bilanPeriodique.that.dataELem;
            }
            bilanPeriodique.that.themeBase = {
                open: false
            };
            bilanPeriodique.that.themePerso= {
                open: false
            };
            bilanPeriodique.that.opened.lightboxCreatePE = true;
            bilanPeriodique.that.showAddtheme = false;
            template.open('lightboxCreatePE', '../../../competences/public/template/behaviours/sniplet-createProjetEducatif');
            utils.safeApply(this);
        },

        showTheme: function(param) {
            bilanPeriodique.that.showAddtheme = param;
            bilanPeriodique.that.changeThematique = false;
            if(param){
                bilanPeriodique.that.thematique.code = null;
                bilanPeriodique.that.thematique.libelle = null;
            }
        },

        openAddtheme: function(theme) {
            bilanPeriodique.that.changeThematique = true;

            bilanPeriodique.that.thematique = {
                id: theme.id,
                code: theme.code,
                libelle: theme.libelle
            };
            bilanPeriodique.that.showAddtheme = true;
        },

        /////       Création d'un thème personnalisé      /////

        async createThematique(thematique) {
            try {
                await http.post('/competences/thematique',
                    {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement()});
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.showAddtheme = false;
            } catch (e) {
                notify.error('Problème lors de la création de la thématique');
                console.error('Problème lors de la création de la thématique');
                throw e;
            }

            utils.safeApply(this);
        },


        /////       Modification d'un thème personnalisé      /////

        async updateThematique(thematique) {
            try {
                await http.put(`/competences/thematique?idThematique=${thematique.id}`,
                    {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement()});
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.showAddtheme = false;
            } catch (e) {
                notify.error('evaluations.thematique.update.error');
            }
            utils.safeApply(this);
        },


        /////       Suppression d'un thème personnalisé      /////

        async tryDeleteTheme(theme) {
            let elements = await this.getElementsOnThematique(theme.id);
            if (elements.length > 0) {
                this.opened.lightboxConfirmDeleteThemes = true;
            } else {
                await this.deleteThematique(theme);
            }
            utils.safeApply(this);
        },

        async deleteThematique(thematique) {
            try {
                await http.delete(`/competences/thematique?idThematique=${thematique.id}`);
                bilanPeriodique.that.getThematique(this.getTypeElement());
                bilanPeriodique.that.showAddtheme = false;
            }
            catch (e) {
                notify.error('evaluations.thematique.delete.error');
            }
            utils.safeApply(this);
        },

        addTheme: function(theme) {
            this.dataELem.theme = theme;
            this.themeBase.open = false;
            this.themePerso.open = false;
        },


        /////       Création d'un EPI/AP/Parcours      /////

        async createElementBilanPeriodique() {
            try {
                if (this.dataELem !== undefined && this.dataELem !== null) {
                    this.dataELem.type = this.getTypeElement();
                    if (this.dataELem.theme !== undefined && this.dataELem.theme.id !== undefined) {
                        this.dataELem.id_theme =  this.dataELem.theme.id;
                    }
                    const {data} = await http.post(`/competences/elementBilanPeriodique?type=${this.dataELem.type}`, this.dataELem);
                    this.elements.push(data);
                }
                bilanPeriodique.that.emptyLightbox();
                bilanPeriodique.that.opened.lightboxCreatePE = false;
                bilanPeriodique.that.getElements();
            } catch (e) {
                notify.error('Problème lors de la création de l\'élément du bilan périodique');
                console.error('Problème lors de la création de l\'élément du bilan périodique');
                throw e;
            }
        },

        async getThematique(type) {
            try {
                let data = await http.get(`/competences/thematique?type=${type}`);
                bilanPeriodique.that.themes = data.data;
            } catch (e) {
                notify.error('evaluations.theme.get.error');
            }
            utils.safeApply(this);
        },


        /////       Filtre les thèmes pour savoir si ce sont des EPI / AP / Parcours      /////

        filterTheme: function (param) {
            return (item) => {
                if (item.personnalise === param) {
                        return item;
                }
            }
        },

        async getElementsOnThematique(idThematique) {
            try {
                let data = await http.get(`/competences/elements/thematique?idThematique=${idThematique}`);
                return data.data;
            } catch (e) {
                notify.error('evaluations.theme.get.error');
            }
        },

        async getAppreciationsOnClasse(idClasse, idElement) {
            try {
                let data = await http.get(`/competences/elementsAppreciations?idClasse=${idClasse}&idElement=${idElement}`);
                return data.data;
            } catch (e) {
                notify.error('evaluations.appreciations.get.error');
            }
        },

        translate: function (key) {
            return utils.translate(key);
        },

        /////       Filtre les matières en fonctions de l'enseignant sélectionné      /////

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
                if (!_.findWhere(this.dataELem.ens_mat, {intervenant: param1, matiere: param2})) {
                    this.dataELem.ens_mat.push({intervenant: param1, matiere: param2});
                }
            } else {
                if (!_.contains(this.dataELem.classes, param1)) {
                    this.dataELem.classes.push(param1)
                }
            }
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


        /////       Suppression d'un EPI/AP/Parcours      /////

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
            utils.safeApply(this);
        },

        async getAppreciations(elements) {
            // try {
            //     let url = "/competences/elementsAppreciations?idEtablissement=" + evaluations.structure.id;
            //     for (var i = 0; i < elements.length; i++) {
            //         url += "&idElement=" + elements[i].id;
            //     }
            //     let data = await http.get(url);
            //     this.appreciations = data.data;
            //     utils.safeApply(this);
            // } catch (e) {
            //     notify.error('evaluations.appreciations.get.error');
            // }
        },


        /////       Modification d'un EPI/AP/Parcours      /////

        async updateElement() {
            try {
                if (this.dataELem !== undefined && this.dataELem !== null
                && this.dataELem.theme !== undefined && this.dataELem.theme.id !== undefined ) {
                    this.dataELem.id_theme =  this.dataELem.theme.id;
                }
                if (this.dataELem.description === null
                    ||  this.dataELem.description === '') {
                    delete(this.dataELem.description);
                }
                await http.put(`/competences/elementBilanPeriodique?idElement=${bilanPeriodique.that.dataELem.id}&type=${bilanPeriodique.that.dataELem.type}`, this.dataELem);
                bilanPeriodique.that.emptyLightbox();
                bilanPeriodique.that.opened.lightboxCreatePE = false;
                bilanPeriodique.that.getElements();

            } catch (e) {
                notify.error('evaluations.elements.update.error');
            }
            utils.safeApply(this);
        },


        /////       Champs obligatoire      /////

        requiredElement: function () {
            if (this.getTypeElement() === 1) {
                return this.dataELem.theme === undefined || this.dataELem.libelle === null
                    || this.dataELem.libelle === undefined || this.dataELem.libelle === ""
                    || this.dataELem.ens_mat.length < 2 || this.dataELem.classes.length === 0
            }
            else if (this.getTypeElement() === 2) {
                 return this.dataELem.libelle === null || this.dataELem.libelle === undefined
                     || this.dataELem.libelle === "" || this.dataELem.ens_mat.length < 2
                     || this.dataELem.classes.length === 0
            }
            else {
                return this.dataELem.theme === null || this.dataELem.classes.length === 0
            }
        },

        requiredTheme: function() {
            return bilanPeriodique.that.thematique.code === undefined
                    || bilanPeriodique.that.thematique.libelle === undefined
                    || bilanPeriodique.that.thematique.code === null
                    || bilanPeriodique.that.thematique.libelle === null
                    || bilanPeriodique.that.thematique.code === ''
                    || bilanPeriodique.that.thematique.libelle === ''
        },

        /////       Selection checkbox      /////

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
                if(this.itemFiltered(element)){
                    element.selected = this.search.elementAll;
                }
            });
        },

        filterItem: function () {
        return (item) => {
            return this.itemFiltered(item);
            }
        },

        itemFiltered: function (item){
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
        },


        /////       Suppression des chips enseigantns / matières et classe lors de la création des EPI/AP/Parcours      /////

        async tryDeleteChips(item) {
            item.selected = true;
            this.opened.lightboxConfirmDeleteChips = true;
        },

        async deleteChips() {
                this.ensei_matieres = this.dataELem.ens_mat;
                for (let i = 0; i < this.ensei_matieres.length; i++) {
                    if (this.ensei_matieres[i].selected) {
                        this.dataELem.ens_mat = _.without(this.dataELem.ens_mat, this.dataELem.ens_mat[i]);
                    }
                }
                bilanPeriodique.that.search.enseignant = null;
                bilanPeriodique.that.search.matiere = null;
                bilanPeriodique.that.opened.lightboxConfirmDeleteChips = false;
        },

        async tryDeleteClasse(item) {
            item.selected = true;
            this.opened.lightboxConfirmDeleteClasse = true;
        },

        async deleteClasse() {
            this.chipClasse = this.dataELem.classes;
            for(let i = 0; i < this.chipClasse.length; i++){
                if(this.chipClasse[i].selected){
                    this.dataELem.classes = _.without(this.dataELem.classes, this.dataELem.classes[i]);
                }
            }
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.opened.lightboxConfirmDeleteClasse = false;
        },

        async emptyLightbox() {
            bilanPeriodique.that.search.enseignant = null;
            bilanPeriodique.that.search.matiere = null;
            bilanPeriodique.that.search.classe = null;
            bilanPeriodique.that.dataELem = {
                idEtablissement: evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
        }

    }
}