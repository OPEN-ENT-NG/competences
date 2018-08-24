import {Model, _, model, notify, Collection} from 'entcore';
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
        // this.collection(Classe, {
        //     sync: function () {
        //         return new Promise(async (resolve) => {
        //             if (this.classe.eleves.length() === 0) {
        //                 await this.classe.eleves.sync();
        //             }
        //             if (this.classe.periodes.length() === 0) {
        //                 await this.classe.periodes.sync();
        //             }
        //             this.synchronized.classe = true;
        //             resolve();
        //         });
        //     }
        // });

        // this.collection(ElementBilanPeriodique, {
        //     sync: async function () {
        //         try {
        //             let data = await http.get(BilanPeriodique.api.GET_ELEMENTS + "&idClasse=" + this.classe.id + "&idEnseignant=" + model.me.userId);
        //             this.elements = data.data;
        //         } catch (e) {
        //             notify.error('evaluations.elements.get.error');
        //         }
        //     }
        // });
        //
        // this.collection(ElementBilanPeriodique, {
        //     sync: async function () {
        //         try {
        //             let url = BilanPeriodique.api.GET_APPRECIATIONS + '?idPeriode=' + periode.id;
        //             for (let i = 0; i < this.elements.length; i++) {
        //                 url += "&idElement=" + this.elements[i].id;
        //             }
        //             let data = await http.get(url);
        //             this.appreciations = data.data;
        //         } catch (e) {
        //             notify.error('evaluations.appreciations.get.error');
        //         }
        //     }
        // });
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
                let url = BilanPeriodique.api.GET_ENSEIGNANTS + "?idElement=" + this.elements[0].id;
                for (let i = 1; i < data.data.length; i++) {
                    url += "&idElement=" + this.elements[i].id;
                }
                let result = await http.get(url);
                _.forEach(this.elements, (element) => {
                    let enseignants = _.findWhere(result.data, {idElement: element.id})
                    element.enseignants = enseignants.idsEnseignants;
                });
            }

        } catch (e) {
            notify.error('evaluations.elements.get.error');
        }
    }

    // async getEnseignantsOnElements (elements) {
    //     try {
    //         let url = BilanPeriodique.api.GET_ENSEIGNANTS + "&idElement=" + elements[0].id;
    //         for (let i = 1; i < elements.length; i++) {
    //             url += "&idElement=" + elements[i].id;
    //         }
    //         let data = await http.get(url);
    //         _.forEach(elements, (element) => {
    //             element.enseignantsMatieres = _.where(data.data, {id_elt_bilan_periodique: element.id});
    //         });
    //     } catch (e) {
    //         notify.error('evaluations.enseignants.get.error');
    //     }
    // }

    async syncAppreciations (elements, periode) {
        try {
            let url = BilanPeriodique.api.GET_APPRECIATIONS + '?idPeriode=' + periode.id;
            for (let i = 0; i < elements.length; i++) {
                url += "&idElement=" + elements[i].id;
            }
            let data = await http.get(url);
            this.appreciations = data.data;

            _.forEach(elements, (element) => {
                var elemsApprec = _.where(this.appreciations, {id_elt_bilan_periodique: element.id});
                _.forEach(elemsApprec, (elemApprec) => {
                    if(elemApprec.id_eleve === undefined){
                        element.appreciationClasse[periode.id] = elemApprec.commentaire;
                    }
                    else {
                        _.find(this.classe.eleves.all, function(eleve){
                            if(eleve.id === elemApprec.id_eleve){

                                if(eleve.appreciations !== undefined){

                                    if(eleve.appreciations[periode.id] !== undefined){
                                        eleve.appreciations[periode.id][element.id] = elemApprec.commentaire;
                                    } else {
                                        eleve.appreciations[periode.id] = [];
                                        eleve.appreciations[periode.id][element.id] = elemApprec.commentaire;
                                    }

                                } else {
                                    eleve.appreciations = [];
                                    eleve.appreciations[periode.id] = [];
                                    eleve.appreciations[periode.id][element.id] = elemApprec.commentaire;
                                }
                            }
                        })
                    }
                });
            });
        } catch (e) {
            notify.error('evaluations.appreciations.get.error');
        }
    }

    toJSON(periode, element, eleve?){
        let data = {
            id_periode : periode.id,
            id_element : element.id
        };
        eleve ? _.extend(data, {id_eleve : eleve.id, appreciation : eleve.appreciations[periode.id][element.id]})
            :  _.extend(data, {appreciation : element.appreciationClasse[periode.id]});

        return data;
    }

    async saveAppreciation (periode, element, eleve?) {
        try {
            eleve ? await http.post(BilanPeriodique.api.create + "?type=eleve", this.toJSON(periode, element, eleve))
                : await http.post(BilanPeriodique.api.create + "?type=classe", this.toJSON(periode, element));
        } catch (e) {
            notify.error('evaluations.appreciation.post.error');
        }
    }

    // async updateAppreciation (periode, element, eleve?) {
    //     // try {
    //     //     eleve ? await http.put(this.api.create + "?type=eleve", this.toJSON(periode, element, eleve))
    //     //         : await http.put(this.api.create + "?type=classe", this.toJSON(periode, element));
    //     // } catch (e) {
    //     //     notify.error('evaluations.appreciation.put.error');
    //     // }
    // }

}