import {_, notify} from "entcore";
import * as utils from "../utils/teacher";
import http from "axios";
import {AvisConseil} from "../models/teacher/AvisConseil";
import {AvisOrientation} from "../models/teacher/AvisOrientation";

export const opinionConference = {
    title: 'Configuration des avis conseil',
    description: 'Permet d\'ajouter / modifier / supprimer des avis conseil et orientation',
    that: undefined,
    controller: {
        init: async function () {
            console.log("opinionConference");
            this.idStructure = this.source.idStructure;
            this.typeAvis = 0;
            this.newOpinion = '';
            this.classOpinions = [];
            this.orientationOpinions = [];

            opinionConference.that = this;
            await this.runMessageLoader();

            opinionConference.that.getOpinions().then(async ({data}) => {
                opinionConference.that.classOpinions = _.where(data, {type_avis: 1});
                opinionConference.that.orientationOpinions = _.where(data, {type_avis: 2});

                await this.stopMessageLoader();
            }).catch( async (error) => {
                console.error(error);
                await this.stopMessageLoader();
            });
        },

        runMessageLoader: async function () {
            opinionConference.that.displayMessageLoader = true;
            await utils.safeApply(opinionConference.that);
        },

        stopMessageLoader: async function ( ) {
            opinionConference.that.displayMessageLoader = false;
            await utils.safeApply(opinionConference.that);
        },

        getOpinions: function () {
            try {
                return http.get(`/competences/avis/bilan/periodique?id_structure=${opinionConference.that.idStructure}`);
            } catch (e) {
                notify.error('evaluations.elements.get.error');
            }
        },

        addOpinion: async function (text) {
            let opinion;
            if(opinionConference.that.typeAvis === 1) {
                opinion = new AvisConseil(null, null, opinionConference.that.idStructure);
            } else if(opinionConference.that.typeAvis === 2) {
                opinion = new AvisOrientation(null, null, opinionConference.that.idStructure);
            }
            await opinion.createNewOpinion(text);
            opinionConference.that.cancelCreateOpinion();
            opinionConference.that.init();
        },

        updateOpinion: async function (opinion) {
            try {
                await http.put("/competences/avis/bilan/periodique?id_avis=" + opinion.id
                    + "&active=" + opinion.active + "&libelle=" + opinion.libelle);
            }
            catch (e) {
                notify.error('viescolaire.conference.opinions.service.delete.error');
            }
        },

        deleteOpinion: async function(opinion) {
            try {
                await http.delete("/competences/avis/bilan/periodique?id_avis=" + opinion.id);
                opinionConference.that.init();
            }
            catch (e) {
                notify.error('viescolaire.conference.opinions.service.delete.error');
            }
        },

        createClassOpinion: function() {
            opinionConference.that.typeAvis = 1;
            opinionConference.that.openCreateLightbox();
        },

        createOrientationOpinion: function() {
            opinionConference.that.typeAvis = 2;
            opinionConference.that.openCreateLightbox();
        },

        openCreateLightbox: function() {
            opinionConference.that.createOpinion = true;
        },

        cancelCreateOpinion: function() {
            opinionConference.that.createOpinion = false;
        }
    },
};