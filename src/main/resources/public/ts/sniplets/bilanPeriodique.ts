import {notify, template} from 'entcore';
import http from "axios";
import {evaluations} from '../models/teacher';
import * as utils from '../utils/teacher';

console.log("here");

export const bilanPeriodique = {
    title: 'Bilan périodique',
    description: 'Permet de paramétrer les éléments nécessaires à la construction des bilans périodiques',
    controller: {
        async init () {
            this.idStructure = this.source.idStructure;
            this.selected = {EPI : true, AP : false, parcours : false};
            await evaluations.sync();
            await evaluations.structure.sync();

            this.data = {
                idEtablissement : evaluations.structure.id,
                classes: [],
                ens_mat: []
            };
            // await this.getElements();
        },
        async openCreatePE () {
            await this.getThematique (1);
            this.classes = evaluations.structure.classes;
            this.enseignants = evaluations.structure.enseignants;
            this.opened.lightboxCreatePE = true;
            template.open('lightboxCreatePE', '../../../competences/public/template/behaviours/sniplet-createProjetEducatif');
            utils.safeApply(this);
        },
        async createThematique (thematique) {
            try {
                if(thematique !== undefined && thematique !== null) {
                    if(thematique.code !== undefined && thematique.code !== null
                        && thematique.libelle !== undefined && thematique.libelle !== null){
                        await http.post('/competences/thematique',
                            {code: thematique.code, libelle: thematique.libelle, type: 1});
                    }
                }
            } catch (e) {
                notify.error('Problème lors de la création');
                console.error('Problème lors de la création');
                throw e;
            }
        },
        async createElementBilanPeriodique () {
            try {
                if(this.data !== undefined && this.data !== null) {
                    if(this.data.theme !== undefined && this.data.theme !== null
                        && this.data.libelle !== undefined && this.data.libelle !== null
                        && this.data.description !== undefined && this.data.description !== null
                        && this.data.classes.length > 0 && this.data.ens_mat.length >= 2){
                        this.data.type = 2;
                        await http.post('/competences/elementBilanPeriodique', this.data);
                    }
                }
            } catch (e) {
                notify.error('Problème lors de la création');
                console.error('Problème lors de la création');
                throw e;
            }
        },
        async updateThematique (): Promise<void> {
            await http.put(this.api.update,this.toJSON());
        },
        async getThematique (type) {
            try {
                let {data} = await http.get(`/competences/thematique?type=${type}`);
                this.themes = data;
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.theme.get.error');
            }
        },
        translate : function (key) {
            return utils.translate(key);
        },
        async syncMatieresEnseignant  (enseignant) {
            try {
                let {data} = await http.get(`/viescolaire/matieres?idEnseignant=${enseignant.id}&idEtablissement=${evaluations.structure.id}&isEnseignant=${true}`);
                this.matieres = data;
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.matiere.get.error');
            }
        },
        pushData : function(param1, param2?) {
            if(param2){
                let ens_mat = {ens: param1, mat: param2}
                this.data.ens_mat.push(ens_mat)
            } else {
                this.data.classes.push(param1)
            }
        },
        async getElements () {
            try {
                let {data} = await http.get(`/competences/elementsBilanPeriodique?idEtablissement=${evaluations.structure.id}`);
                this.elements = data;
                utils.safeApply(this);
            } catch (e) {
                notify.error('evaluations.elements.get.error');
            }
        },
    }
}