import {Model, _, model, notify, Collection, moment} from 'entcore';
import http from 'axios';
import {
    Periode,
    Classe,
    Structure,
    ElementBilanPeriodique,
    evaluations
} from './index';
import {AppreciationElement} from "./AppreciationElement";

export class BilanPeriodique extends  Model {
    synchronized: any;
    periode: Periode;
    classe: Classe;
    structure: Structure;
    elements: Collection<ElementBilanPeriodique>;
    appreciations : Collection<AppreciationElement>;
    endSaisie : Boolean;

    static get api() {
        return {
            GET_ELEMENTS: '/competences/elementsBilanPeriodique?idEtablissement=' + evaluations.structure.id,
            GET_ENSEIGNANTS: '/competences/elementsBilanPeriodique/enseignants',
            GET_APPRECIATIONS: '/competences/elementsAppreciations',
            create: '/competences/elementsAppreciation'
        }
    }

    constructor(periode: any, classe: Classe) {
        super();

        this.synchronized = {
            classe: false
        };

        this.periode = periode;
        this.classe = classe;

        this.structure = evaluations.structure;
    }

    syncClasse(): Promise<any> {
        return new Promise(async (resolve) => {
            if (this.classe.eleves.length() === 0) {
                await this.classe.eleves.sync();
            }
            if (this.classe.periodes.length() === 0) {
                await this.classe.periodes.sync();
            }
            this.synchronized.classe = true;
            resolve();
        });
    }

    async syncElements () {
        try {
            let data = await http.get(BilanPeriodique.api.GET_ELEMENTS + "&idClasse=" + this.classe.id + "&idEnseignant=" + model.me.userId);
            this.elements = data.data;

            if(data.data.length > 0) {
                let url = BilanPeriodique.api.GET_ENSEIGNANTS + "?idClasse=" + this.classe.id;
                for (let i = 0; i < data.data.length; i++) {
                    url += "&idElement=" + this.elements[i].id;
                }
                // if (this.classe.periodes.length() === 0) {
                //     await this.classe.periodes.sync();
                // }
                // let _p = _.findWhere(this.classe.periodes.all, {id_type: periode.id_type});
                // if(_p) {
                    let result = await http.get(url);
                    _.forEach(this.elements, (element) => {
                        let enseignants = _.findWhere(result.data, {idElement: element.id});
                        if(enseignants) {
                            element.enseignants = enseignants.idsEnseignants;
                        }
                    });
                // }
            }

        } catch (e) {
            notify.error('evaluations.elements.get.error');
        }
    }

    async syncAppreciations (elements, periode, classe) {
        try {
            let url = BilanPeriodique.api.GET_APPRECIATIONS + '?idPeriode=' + periode.id + '&idClasse=' + classe.id;
            for (let i = 0; i < elements.length; i++) {
                url += "&idElement=" + elements[i].id;
            }
            let data = await http.get(url);
            this.appreciations = data.data;

            _.forEach(elements, (element) => {
                var elemsApprec = _.where(this.appreciations, {id_elt_bilan_periodique: element.id});
                _.forEach(elemsApprec, (elemApprec) => {
                    if(elemApprec.id_eleve === undefined){
                        if(element.appreciationClasse === undefined){
                            element.appreciationClasse = [];
                        }
                        if(element.appreciationClasse[periode.id] === undefined){
                            element.appreciationClasse[periode.id] = [];
                        }
                        element.appreciationClasse[periode.id][classe.id] = elemApprec.commentaire;
                    }
                    else {
                        _.find(this.classe.eleves.all, function(eleve){
                            if(eleve.id === elemApprec.id_eleve){

                                if(eleve.appreciations === undefined){
                                    eleve.appreciations = [];
                                }
                                if(eleve.appreciations[periode.id] === undefined){
                                    eleve.appreciations[periode.id] = [];
                                }
                                eleve.appreciations[periode.id][element.id] = elemApprec.commentaire;
                            }
                        })
                    }
                });
            });
        } catch (e) {
            notify.error('evaluations.appreciations.get.error');
        }

        let period = _.findWhere(this.classe.periodes.all, {id_type: periode.id_type});
        if(period){
            this.endSaisie = moment(period.date_fin_saisie).isBefore(moment(), "days");
        }
    }

    toJSON(periode, element, eleve, classe){
        let data = {
            id_periode : periode.id,
            id_element : element.id
        };
        eleve ? _.extend(data, {id_eleve : eleve.id, appreciation : eleve.appreciations[periode.id][element.id], id_classe : classe.id})
            :  _.extend(data, {appreciation : element.appreciationClasse[periode.id][classe.id], id_classe : classe.id, externalid_classe : classe.externalId});

        return data;
    }

    async saveAppreciation (periode, element, eleve, classe) {
        try {
            eleve ? await http.post(BilanPeriodique.api.create + "?type=eleve", this.toJSON(periode, element, eleve, classe))
                : await http.post(BilanPeriodique.api.create + "?type=classe", this.toJSON(periode, element, null, classe));
        } catch (e) {
            notify.error('evaluations.appreciation.post.error');
        }
    }

}