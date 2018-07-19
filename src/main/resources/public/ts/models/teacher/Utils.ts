import {model, idiom as lang, _, Behaviours} from 'entcore';
import * as utils from '../../utils/teacher';
import { BilanFinDeCycle, CompetenceNote } from './index';
import {evaluations} from "./model";

export class Utils {
    static isHeadTeacher (classe) {
        return _.contains(
            _.union(evaluations.structure.detailsUser.headTeacher,
                evaluations.structure.detailsUser.headTeacherManual), classe.externalId);
    }

    static isChefEtab (classe?) {
        let isAdmin = model.me.hasWorkflow(Behaviours.applicationsBehaviours.viescolaire.rights.workflow.adminChefEtab);
        if(classe === undefined) {
            return isAdmin;
        }
        else {
            return isAdmin || this.isHeadTeacher(classe);

        }
    }


    static canUpdateBFCSynthese () {
        return model.me.hasWorkflow(Behaviours.applicationsBehaviours.competences.rights.workflow.canUpdateBFCSynthese);
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
                var competencesEvaluations = poDomaine.competences.all[i].competencesEvaluations;
                var _evalFiltered = competencesEvaluations;

                // filtre sur les compétences évaluées par l'enseignant
                if (pbMesEvaluations) {
                    _evalFiltered = _.filter(competencesEvaluations, function (competence) {
                        return competence.owner !== undefined && competence.owner === model.me.userId;
                    });
                }

                // filtre sur les competences prises dans le calcul
                _evalFiltered = _.filter(_evalFiltered, function (competence) {
                    return !competence.formative; // la competence doit être reliée à un devoir ayant un type non "formative"
                });

                if (_evalFiltered && _evalFiltered.length > 0) {
                    // TODO récupérer la vrai valeur numérique :
                    // par exemple 0 correspond à rouge ce qui mais ça correspond à une note de 1 ou 0.5 ou 0 ?
                    poMaxEvaluationsDomaines.push(_.max(_evalFiltered, function (_t) {
                        return _t.evaluation;
                    }).evaluation + 1);
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

    static setCompetenceNotes(poDomaine, poCompetencesNotes, object, classe) {
        if(poDomaine.competences) {
            _.map(poDomaine.competences.all, function (competence) {
                competence.competencesEvaluations = _.where(poCompetencesNotes, {
                    id_competence: competence.id,
                    id_domaine: competence.id_domaine
                });
                if (object.composer.constructor.name === 'SuiviCompetenceClasse') {
                    for (var i = 0; i < classe.eleves.all.length; i++) {
                        var mine = _.findWhere(competence.competencesEvaluations, {id_eleve : classe.eleves.all[i].id, owner : model.me.userId});
                        var others = _.filter(competence.competencesEvaluations, function (evaluation) { return evaluation.owner !== model.me.userId; });
                        if (mine === undefined)
                            competence.competencesEvaluations.push(new CompetenceNote({evaluation : -1, id_competence: competence.id, id_eleve : classe.eleves.all[i].id, owner : model.me.userId}));
                        if (others.length === 0)
                            competence.competencesEvaluations.push(new CompetenceNote({evaluation : -1, id_competence: competence.id, id_eleve : classe.eleves.all[i].id}));
                    }
                }
            });
        }

        if( poDomaine.domaines) {
            for (var i = 0; i < poDomaine.domaines.all.length; i++) {
                this.setCompetenceNotes(poDomaine.domaines.all[i], poCompetencesNotes, object, classe);
            }
        }
    }
}