/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

import {model, idiom as lang, _, Behaviours} from 'entcore';
import * as utils from '../../utils/teacher';
import { BilanFinDeCycle, CompetenceNote } from './index';
import {evaluations} from "./model";

export class Utils {
    static isHeadTeacher (classe) {
        /*if (evaluations.structure.detailsUser === undefined) {
            await evaluations.structure.getDetailsOfUser();
        }*/
        return _.contains(
            _.union(evaluations.structure.detailsUser.headTeacher,
                evaluations.structure.detailsUser.headTeacherManual), classe.externalId);
    }

    static isChefEtab  (classe?) {
        let isAdmin = model.me.hasWorkflow(Behaviours.applicationsBehaviours.viescolaire.rights.workflow.adminChefEtab);
        if(classe === undefined || classe === null || classe === "" || classe === "*") {
            return isAdmin;
        }
        else {
            return isAdmin || this.isHeadTeacher(classe);

        }
    }

    static async rightsChefEtabHeadTeacherOnBilanPeriodique (classe, nameWorkFlow){
        return ( model.me.type !== 'ENSEIGNANT' && model.me.
            hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow[nameWorkFlow]))
            || ( model.me.type === 'ENSEIGNANT' && this.isHeadTeacher(classe) &&  model.me.
            hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow[nameWorkFlow]) && this.isHeadTeacher(classe));
    }

    static canSaisieProjet () {
        return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.accessProjets);
    }

    static canCreateElementBilanPeriodique () {
        return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.bilanPeriodique);
    }

    static canSaisiAppreciationCPE () {
        return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.canSaisiAppreciationCPE);
    }

    static canUpdateBFCSynthese () {
        return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.canUpdateBFCSynthese);
    }
    static canSaveCompetenceNiveauFinal () {
        return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.saveCompetenceNiveauFinal);
    }

    /**
     * Méthode récursive de l'affichage des sous domaines d'un domaine
     *
     * @param poDomaines la liste des domaines
     * @pbMesEvaluations booleen indiquant d'afficher ou non un domaine
     *
     */
    static setVisibleSousDomainesRec (poDomaines, pbVisible) {
        if(poDomaines !== null && poDomaines !== undefined) {
            for (var i = 0; i < poDomaines.all.length; i++) {
                var oSousDomaine = poDomaines.all[i];
                oSousDomaine.visible = pbVisible;
                this.setVisibleSousDomainesRec(oSousDomaine.domaines, pbVisible);
            }
        }
    }

    static setSliderOptions(poDomaine,tableConversions) {

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
                    model.trigger('refresh-slider');

                });
            }
        };

        poDomaine.slider = {
            options: {
               ticksTooltip: function(value) {
                    return String(poDomaine.moyenne);
                },
                //disabled: parseFloat(poDomaine.moyenne) === -1,
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

        if(poDomaine.dispense_eleve) {
            poDomaine.slider.options.disabled = true;
            poDomaine.slider.options.readOnly = true;
        }else{
            poDomaine.slider.options.disabled = false;
            poDomaine.slider.options.readOnly = false;
        }

        let moyenneTemp = undefined;
        // si Une valeur a été modifiée par le chef d'établissement alors on prend cette valeur
        if(poDomaine.bfc !== undefined && poDomaine.bfc.valeur !== undefined){
            moyenneTemp = poDomaine.bfc.valeur;
        }else{
            moyenneTemp = poDomaine.moyenne;
        }

        // Récupération de la moyenne convertie
        let maConvertion = utils.getMoyenneForBFC(moyenneTemp,tableConversions);

        poDomaine.slider.value = maConvertion;

        poDomaine.slider.options.getSelectionBarClass = function(value){
            if (value === -1) {
                return '#d8e0f3'
            } else {
                let ConvertionOfValue = _.find(tableConversions,{ordre: value});
                if(ConvertionOfValue !== undefined)
                    return ConvertionOfValue.couleur;
            }

        };

        poDomaine.slider.options.translate = function(value,sliderId,label){
            let l = '#label#';
            if (label === 'model') {

                l = '<b>#label#</b>';
            }

            if(value === -1) {
                return l.replace('#label#', lang.translate('evaluations.competence.unevaluated'));
            } else {
                let libelle = _.find(tableConversions,{ordre: value});
                if(libelle !== undefined)
                    return l.replace('#label#', lang.translate(libelle.libelle));
            }

        };
    }

    static getMaxEvaluationsDomaines(poDomaine, poMaxEvaluationsDomaines,tableConversions, pbMesEvaluations,
                                     bfcsParDomaine, classe) {


        // si le domaine est évalué, on ajoute les max de chacunes de ses competences
        if(poDomaine.evaluated) {
            for (let i = 0; i < poDomaine.competences.all.length; i++) {
                    let competence = poDomaine.competences.all[i];
                    if(competence.niveauFinaltoShowAllEvaluations !== undefined) {
                        poMaxEvaluationsDomaines.push(competence.niveauFinaltoShowAllEvaluations + 1);
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
                this.getMaxEvaluationsDomaines(poDomaine.domaines.all[i], poMaxEvaluationsDomaines,tableConversions,
                    pbMesEvaluations,bfcsParDomaine, classe);
            }
        }

        // mise à jour de la moyenne
        if (poMaxEvaluationsDomaines.length > 0) {
            poDomaine.moyenne = utils.average(_.without(poMaxEvaluationsDomaines,0) );
        } else {
            poDomaine.moyenne = -1;
        }

        this.setSliderOptions(poDomaine,tableConversions);

        // Chefs d'établissement

        //Si l'utilisateur n'est pas un chef d'établissement il ne peut pas modifier le slider
        if(!this.isChefEtab(classe)){
            poDomaine.slider.options.readOnly = true;
        }
    }

    static findCompetenceRec (piIdCompetence, poDomaine) {
        for (var i = 0; i < poDomaine.competences.all.length; i++) {
            // si compétences trouvée on arrete le traitement
            if(poDomaine.competences.all[i].id === piIdCompetence) {
                return poDomaine.competences.all[i];
            }
        }

        // recherche dans les sous-domaines
        if(poDomaine.domaines) {
            for(var i=0; i<poDomaine.domaines.all.length; i++) {
                let comp = this.findCompetenceRec(piIdCompetence, poDomaine.domaines.all[i]);
                if(comp !== undefined){
                    return comp;
                }
            }
        }
    }

    /**
     *For a list of Evaluations set niveauFinaltoShowAllEvaluations, niveauAtteintToShowMyEvaluations
     * and niveauFinalToShowMyEvaluations for a competence
     * @param competence
     *
     *
     */

     static setMaxCompetenceShow  ( competence) {

        //all evaluations
        // récupèrer toutes les évaluations de type non "formative"
        let allEvaluations = _.filter(competence.competencesEvaluations, (evaluation) => {
            return !evaluation.formative;
            // la competence doit être reliée à un devoir ayant un type non "formative"
        });
        if(allEvaluations !== undefined && allEvaluations.length > 0){
            let notHistorizedEvals = _.filter(allEvaluations, (evaluation) => {
                return evaluation.eval_lib_historise === false;
            });
            allEvaluations = (notHistorizedEvals.length > 0)? notHistorizedEvals : allEvaluations;
            competence.niveauFinaltoShowAllEvaluations = Utils.getNiveauMaxOfListEval(allEvaluations);
        }

        // my evaluations
        let myEvaluations = _.filter(allEvaluations, function (evaluation) {
            return evaluation.owner !== undefined && evaluation.owner === model.me.userId;
        });
        if( myEvaluations !== undefined && myEvaluations.length > 0){
            //set the max of my evaluations on this competence for "niveau atteint"
            competence.niveauAtteintToShowMyEvaluations = Utils.getNiveauMaxOfListEval(myEvaluations, true);

            //set the max of my evaluations on this competence for "niveau final"
            competence.niveauFinalToShowMyEvaluations = Utils.getNiveauMaxOfListEval(myEvaluations);
        }
    }

    /**
     *
     * @param listEval
     * @param onlyNote
     */
    static getNiveauMaxOfListEval (listEval,onlyNote? ){
        //tableau des max des Evals pour chaque matière
        if(onlyNote !== undefined && onlyNote){
            return  _.max(listEval, (e) => {
                return e.evaluation;
            }).evaluation;
        }else {
            let allmaxMats = [];
            //trier par idMatiere;
            let listEvalsByMatiere = _.groupBy(listEval, (e) => {
                return e.id_matiere;
            });
            _.mapObject(listEvalsByMatiere, (tabEvals, idMat) => {

                if (_.first(tabEvals).niveau_final !== null) {
                    allmaxMats.push(_.first(tabEvals).niveau_final);
                } else {
                    allmaxMats.push(_.max(tabEvals, (e) => {
                        return e.evaluation;
                    }).evaluation);
                }
            });

            return _.max(allmaxMats);
        }
    }
    static setCompetenceNotes(poDomaine, poCompetencesNotes, object?, classe?, tabDomaine?) {
        if (object === undefined && classe === undefined) {
            if (poDomaine.competences) {
                _.map(poDomaine.competences.all, function (competence) {
                    competence.competencesEvaluations = _.where(poCompetencesNotes, {
                        id_competence: competence.id
                    });
                    if( competence.competencesEvaluations !== undefined && competence.competencesEvaluations.length > 0){
                        Utils.setMaxCompetenceShow(competence);
                    }
                });
                if (tabDomaine !== undefined) {
                    tabDomaine.push(poDomaine);
                }
            }
        }
        else if(poDomaine.competences) {
            _.map(poDomaine.competences.all, function (competence) {
                competence.competencesEvaluations = _.where(poCompetencesNotes, {
                    id_competence: competence.id,
                    id_domaine: competence.id_domaine
                });
                if( competence.competencesEvaluations !== undefined && competence.competencesEvaluations.length > 0){
                        Utils.setMaxCompetenceShow(competence);
                }

                if (object.composer.constructor.name === 'SuiviCompetenceClasse') {
                    let mineCompetencesEvaluations = _.filter(competence.competencesEvaluations, {owner : model.me.userId});

                    // Récupère mes évaluations maximales de la compétence pour tous les élèves
                    competence.mineCompetencesEvaluations = Utils.getCompetenceEvaluations(classe, competence,mineCompetencesEvaluations);

                    // Récupère les évaluations maximales de la compétence pour tous les élèves
                    competence.competencesEvaluations = Utils.getCompetenceEvaluations(classe, competence,competence.competencesEvaluations);

                    for (let i = 0; i < classe.eleves.all.length; i++) {
                        let mine = _.findWhere(competence.mineCompetencesEvaluations, {id_eleve : classe.eleves.all[i].id,
                            owner : model.me.userId});
                        let others = _.filter(competence.competencesEvaluations, function (evaluation) {
                            return evaluation.owner !== model.me.userId; });

                        if (mine === undefined) {
                            if(competence.mineCompetencesEvaluations === undefined
                            || competence.mineCompetencesEvaluations === null) {
                                competence.mineCompetencesEvaluations = [];
                            }

                            competence.mineCompetencesEvaluations.push(new CompetenceNote({
                                evaluation: -1,
                                id_competence: competence.id,
                                id_eleve: classe.eleves.all[i].id,
                                owner: model.me.userId
                            }));
                        }

                        if (others.length === 0) {
                            if(competence.competencesEvaluations === undefined
                                || competence.competencesEvaluations === null) {
                                competence.competencesEvaluations = [];
                            }

                            competence.competencesEvaluations.push(new CompetenceNote({
                                evaluation: -1,
                                id_competence: competence.id,
                                id_eleve: classe.eleves.all[i].id
                            }));
                        }
                    }
                }
            });
        }

        if( poDomaine.domaines) {
            for (var i = 0; i < poDomaine.domaines.all.length; i++) {
                this.setCompetenceNotes(poDomaine.domaines.all[i], poCompetencesNotes, object, classe, tabDomaine);
            }
        }
    }

    /**
     * Récupère l'évaluation maximale (niveau final ou niveau atteint) pour chaque élève
     * @param classe
     * @param competence
     * @param competencesEvaluations
     * @returns {any}
     */
    static getCompetenceEvaluations(classe, competence,competencesEvaluations) {
        for (let i = 0; i < classe.eleves.all.length; i++) {
            let currentIdEleve = classe.eleves.all[i].id
            // MN-175 : On calcule par élève le niveau toutes matières confondues
            let commpetenceEvaluationsEleve = _.where(competencesEvaluations, {id_eleve: currentIdEleve});
            if (commpetenceEvaluationsEleve !== undefined && commpetenceEvaluationsEleve.length > 0) {
                // On initialise la competence evaluation finale de l'élève
                let commpetenceEvaluationEleveFinal = {
                    id_competence: competence.id,
                    id_eleve: currentIdEleve,
                    id_domaine: competence.id_domaine,
                    evaluation: 0,
                    owner: commpetenceEvaluationsEleve[0].owner
                };

                let niveauFinal = 0;
                for (var j = 0; j < commpetenceEvaluationsEleve.length; j++) {
                    let tempCommpetenceEvaluationEleve = commpetenceEvaluationsEleve[j];
                    if (tempCommpetenceEvaluationEleve.id_matiere !== undefined && tempCommpetenceEvaluationEleve.id_matiere !== null && tempCommpetenceEvaluationEleve.id_matiere !== '') {
                        if (tempCommpetenceEvaluationEleve.niveau_final !== undefined && tempCommpetenceEvaluationEleve.niveau_final !== null) {
                            // On prend le niveau final  si celui ci est supérieur
                            if (tempCommpetenceEvaluationEleve.niveau_final > niveauFinal) {
                                niveauFinal = tempCommpetenceEvaluationEleve.niveau_final;
                                commpetenceEvaluationEleveFinal.owner = tempCommpetenceEvaluationEleve.owner;
                            }
                        } else {
                            // On prend le max des évaluations si celui ci est supérieur
                            if (tempCommpetenceEvaluationEleve.evaluation > niveauFinal) {
                                niveauFinal = tempCommpetenceEvaluationEleve.evaluation;
                                commpetenceEvaluationEleveFinal.owner = tempCommpetenceEvaluationEleve.owner;
                            }
                        }
                    }
                }
                commpetenceEvaluationEleveFinal.evaluation = niveauFinal;
                // On supprime les évaluations de l'élève
                let commpetenceEvaluationsFinal = _.filter(competencesEvaluations, function (competencesEvaluation) {
                    return competencesEvaluation.id_eleve !== currentIdEleve
                });
                // On ajoute le niveau calculé
                commpetenceEvaluationsFinal.push(commpetenceEvaluationEleveFinal)
                return commpetenceEvaluationsFinal ;
            }

        }
    }

// Filtres
    /**
     * Filtre permettant de retourner l'évaluation maximum en fonction du paramètre de recherche "Mes Evaluations"
     * @param listeEvaluations Tableau d'évaluations de compétences
     * @returns {(evaluation:any)=>(boolean|boolean)} Retourne true si la compétence courante est la plus haute du tableau listeEvaluations
     */
    static isMaxEvaluation = function (listeEvaluations, $scope) {
        return function (evaluation) {
            var _evalFiltered = listeEvaluations;
            if ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true) {
                _evalFiltered = _.filter(listeEvaluations, function (competence) {
                    return competence.owner !== undefined && competence.owner === $scope.me.userId;
                });
            }

            // filtre sur les competences prises dans le calcul
            _evalFiltered = _.filter(_evalFiltered, function (competence) {
                return !competence.formative;
                // la competence doit être reliée à un devoir ayant un type non "formative"
            });

            // calcul du max parmis les competences
            let max = _.max(_evalFiltered, function (competence) {
                return competence.evaluation;
            });
            if (typeof max === 'object') {
                return evaluation.id_competences_notes === max.id_competences_notes;
            } else {
                return false;
            }
        };
    };

    static hasMaxNotFormative = function (MaCompetence, $scope) {
        let _evalFiltered = MaCompetence.competencesEvaluations;
        if ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true) {
            _evalFiltered = _.filter(MaCompetence.competencesEvaluations, function (evaluation) {
                if (evaluation.owner !== undefined && evaluation.owner === $scope.me.userId)
                    return evaluation;
            });
        }

        // filtre sur les competences prises dans le calcul
        _evalFiltered = _.filter(_evalFiltered, function (competence) {
            return !competence.formative; // la competence doit être reliée à un devoir ayant un type non "formative"
        });

        let max = _.max(_evalFiltered, function (evaluation) {
            return evaluation.evaluation;
        });
        if (typeof max === 'object') {
            return (!(max.evaluation == -1));
        } else {
            return false;
        }
    };


    /**
     * Retourne si l'utilisateur n'est pas le propriétaire de compétences
     * @param listeEvaluations Tableau d'évaluations de compétences
     * @returns {boolean} Retourne true si l'utilisateur n'est pas le propriétaire
     */
    static notEvalutationOwner = function (listeEvaluations, $scope) {
        if ($scope.suiviFilter === undefined) $scope.initFilterMine();
        if ($scope.suiviFilter.mine === 'false' || $scope.suiviFilter.mine === false) {
            return false;
        }
        let _t = _.filter(listeEvaluations, function (competence) {
            return competence.owner === undefined || competence.owner === $scope.me.userId;
        });
        return _t.length === 0;
    };

    static FilterNotEvaluated = function (MaCompetence, $scope) {
        if ($scope.selected.grey === true || ($scope.selected.grey === false && MaCompetence.masque)) {
            let _t = MaCompetence.competencesEvaluations;
            if ($scope.suiviFilter.mine === 'true' || $scope.suiviFilter.mine === true) {
                _t = _.filter(MaCompetence.competencesEvaluations, function (evaluation) {
                    if (evaluation.owner !== undefined && evaluation.owner === $scope.me.userId)
                        return evaluation;
                });
            }


            let max = _.max(_t, function (evaluation) {
                return evaluation.evaluation;
            });
            if (typeof max === 'object') {
                return (!(max.evaluation == -1));
            } else {
                return false;
            }

        } else {
            return true;
        }

    };

}