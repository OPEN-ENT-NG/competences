import * as utils from "../utils/teacher";
import {_, http} from "entcore";

export const programElements = {
    title: 'Configuration des éléments programmes',
    description: 'Permet d\'ajouter / modifier / supprimer des élements de programme',
    that: undefined,
    controller: {
        init: async function () {
            console.log(" programElement");
            this.idStructure = this.source.idStructure;
            this.cycles = this.source.cycles;
            this.cycle = null;
            this.domaine = null;
            this.domainesEnseignements = [];
            this.sousDomainesEnseignements = [];
            this.addProp = false;
            this.newProp = "";
            this.editProp = false;
            this.oldProp = "";
            programElements.that = this;
        },

        get api() {
            return {
                GET_ELEMENT_PROGRAMME_DOMAINES: `/competences/element/programme/domaines?idCycle=`,
                GET_ELEMENT_PROGRAMME_SOUS_DOMAINES: `/competences/element/programme/sous/domaines?idDomaine=`,
                GET_ELEMENT_PROGRAMME_PROPOSITIONS: `/competences/element/programme/propositions?idSousDomaine=`,
                ELEMENT_PROGRAMME_PROPOSITIONS: `/competences/element/programme/propositions`,
                DELETE_ELEMENT_PROGRAMME_PROPOSITIONS: `/competences/element/programme/propositions?idProposition=`,
            }
        },

        translate : function(key) {
            return utils.translate(key);
        },

        syncDomainesEnseignements(cycle): Promise<any> {
            return new Promise(async (resolve, reject) => {
                await this.runMessageLoader();
                http().getJson(this.api.GET_ELEMENT_PROGRAMME_DOMAINES + cycle.id_cycle)
                    .done(async (res) => {
                        this.domainesEnseignements = res;
                        await this.stopMessageLoader();
                        resolve();
                    })
                    .error(async (res) => {
                        console.error(res);
                        await this.stopMessageLoader();
                        reject();
                    })
            });
        },

        syncSousDomainesEnseignements(domaine?) : Promise<any> {
            if(domaine == null)
                domaine = programElements.that.domaine;
            else
                programElements.that.domaine = domaine;

            return new Promise( async (resolve, reject) => {
                await this.runMessageLoader();
                programElements.that.sousDomainesEnseignements = [];

                http().getJson(this.api.GET_ELEMENT_PROGRAMME_SOUS_DOMAINES + domaine.id)
                    .done(async (res) => {
                        programElements.that.sousDomainesEnseignements = res;
                        _.forEach(programElements.that.sousDomainesEnseignements, async (sousDomaine) => {
                            sousDomaine.propositions = await programElements.that.syncPropositions(sousDomaine);
                            utils.safeApply(programElements.that);
                        });
                        await this.stopMessageLoader();
                        resolve(res);
                    }).error(async () => {
                    await this.stopMessageLoader();
                    reject();
                });
            });
        },

        syncPropositions(sousDomaine) : Promise<any> {
            return new Promise((resolve, reject) => {
                http().getJson(this.api.GET_ELEMENT_PROGRAMME_PROPOSITIONS + sousDomaine.id
                    + "&idEtablissement=" + this.idStructure)
                    .done(async (res) => {
                        resolve(res);
                    }).error(async () => {
                    reject();
                })
            });
        },

        addCustomProposition(text) : Promise<any> {
            return new Promise(async (resolve, reject) => {
                await programElements.that.runMessageLoader();
                let proposition = {
                    libelle: text,
                    id_sous_domaine: this.sousDomaineSelected.id,
                    id_etablissement: this.idStructure
                };
                await http().postJson(this.api.ELEMENT_PROGRAMME_PROPOSITIONS, proposition).done(async () => {
                    programElements.that.cancelAddProp();
                    programElements.that.syncSousDomainesEnseignements();
                    resolve();
                }).error(async (error) => {
                    console.error(error);
                    programElements.that.cancelAddProp();
                    await programElements.that.stopMessageLoader();
                    reject();
                });
            });
        },

        deleteProposition(proposition) : Promise<any> {
            return new Promise(async (resolve, reject) => {
                await this.runMessageLoader();
                await http().delete(this.api.DELETE_ELEMENT_PROGRAMME_PROPOSITIONS + proposition.id).done(async () => {
                    programElements.that.syncSousDomainesEnseignements();
                    resolve();
                }).error(async (error) => {
                    console.error(error);
                    await this.stopMessageLoader();
                    reject();
                });
            });
        },

        editProposition(proposition) : Promise<any> {
            return new Promise(async (resolve, reject) => {
                console.log(programElements.that.oldProp);
                console.log(proposition.libelle);
                if(programElements.that.oldProp !== proposition.libelle){
                    await this.runMessageLoader();
                    let p = {
                        libelle: proposition.libelle,
                        id_proposition: proposition.id
                    };
                    await http().putJson(this.api.ELEMENT_PROGRAMME_PROPOSITIONS, p).done(async () => {
                        programElements.that.cancelEditProp();
                        programElements.that.syncSousDomainesEnseignements();
                        resolve();
                    }).error(async (error) => {
                        console.error(error);
                        await this.stopMessageLoader();
                        reject();
                    });
                }
            });
        },

        runMessageLoader: async function () {
            programElements.that.displayMessageLoader = true;
            await utils.safeApply(programElements.that);
        },

        stopMessageLoader: async function () {
            programElements.that.displayMessageLoader = false;
            await utils.safeApply(programElements.that);
        },

        openAddProp: async function (sousDomaine) {
            programElements.that.sousDomaineSelected = sousDomaine;
            programElements.that.addProp = true;
            await utils.safeApply(programElements.that);
        },

        cancelAddProp: async function () {
            programElements.that.addProp = false;
            programElements.that.newProp = "";
            await utils.safeApply(programElements.that);
        },

        cancelEditProp: async function () {
            programElements.that.editProp = false;
            await utils.safeApply(programElements.that);
        },

        changeOldProp: async function (proposition) {
            programElements.that.oldProp = proposition.libelle;
            await utils.safeApply(programElements.that);
        }
    },
};