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
            // this.empty = {
            //     libelle: [],
            //     code: [],
            //     themes: [],
            // };
            this.thematique = {
                code: "",
                libelle: ""
            };
            await this.getElements();
        },

        async openElementLigthbox(param?) {
            await this.getThematique(this.getTypeElement());
            bilanPeriodique.that.classes = evaluations.structure.classes;
            bilanPeriodique.that.enseignants = evaluations.structure.enseignants;
            bilanPeriodique.that.modifElem = param;

            if(param){
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
            } else {
                this.dataELem = {
                    idEtablissement : evaluations.structure.id,
                    classes: [],
                    ens_mat: []
                };
            }
            bilanPeriodique.that.opened.lightboxCreatePE = true;
            template.open('lightboxCreatePE', '../../../competences/public/template/behaviours/sniplet-createProjetEducatif');
            utils.safeApply(this);
            console.log("lightboxCreatePE this.classes", this.classes)
            console.log("$scope", this)

        },

        /////       Création d'un thème personnalisé      /////

        async createThematique(thematique) {
            try {
                await http.post('/competences/thematique',
                    {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement()});
                this.getThematique(this.getTypeElement());
                this.showAddtheme = false;
            } catch (e) {
                notify.error('Problème lors de la création de la thématique');
                console.error('Problème lors de la création de la thématique');
                throw e;
            }
            utils.safeApply(this);
        },

        openAddtheme: function(theme) {
            bilanPeriodique.that.changeThematique = true;
            this.thematique.code = theme.code;
            this.thematique.libelle = theme.libelle;
            bilanPeriodique.that.showAddtheme = true;
        },

        /////       Modification d'un thème personnalisé      /////

        async updateThematique(thematique) {
            try {
                await http.put(`/competences/thematique?idThematique=${thematique.id}`,
                    {code: thematique.code, libelle: thematique.libelle, type: this.getTypeElement()});

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
            }
            catch (e) {
                notify.error('evaluations.thematique.delete.error');
            }
            utils.safeApply(this);
        },

        addTheme: function(theme) {
            // bilanPeriodique.that.libelle = theme.libelle;
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
                this.opened.lightboxCreatePE = false;
                bilanPeriodique.that.getElements();
                // this.emptyLightbox();
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
                if (this.getTypeElement() !== 3) {
                    let data = await http.get(`/viescolaire/matieres?idEnseignant=${enseignant.id}&idEtablissement=${evaluations.structure.id}&isEnseignant=${true}`);
                    this.matieres = data.data;
                    utils.safeApply(this);
                }
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
        pushEnseignant: function (enseignant) {
            if (this.getTypeElement() == 3) {
                if (!_.findWhere(this.dataELem.ens_mat, {intervenant: enseignant})) {
                    this.dataELem.ens_mat.push({intervenant: enseignant});
                }
            }
        },
        // emptyLightbox: function() {
        //     this.dataELem
        //     this.empty
        //     this.selectedElements
        // },

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
        },

        async getAppreciations(elements) {
            try {
                let url = "/competences/elementsAppreciations?idEtablissement=" + evaluations.structure.id;
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
                await http.put(`/competences/elementBilanPeriodique?idElement=${this.dataELem.id}&type=${this.dataELem.type}`, this.dataELem);
                this.getElements();
            } catch (e) {
                notify.error('evaluations.elements.update.error');
            }
            utils.safeApply(this);
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

        /////       Suppression des chips enseigantns / matières et classe lors de la création des EPI/AP/Parcours      /////

        async tryDeleteChips(item) {
            item.selected = true;
            this.opened.lightboxConfirmDeleteChips = true;
        },

        async deleteChips() {
            this.ensei_matieres = this.dataELem.ens_mat;
            for(let i = 0; i < this.ensei_matieres.length; i++){
                if(this.ensei_matieres[i].selected){
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

    }
}