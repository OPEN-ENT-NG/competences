import {Model, IModel, _, model, notify, http as httpCore, Collection} from 'entcore';
import http from 'axios';
import {
    Periode,
    Classe,
    Structure,
    ElementBilanPeriodique,
    evaluations
} from './index';

export class BilanPeriodique extends  Model {
    synchronized: any;
    periode: Periode;
    classe: Classe;
    structure: Structure;
    elements: Collection<ElementBilanPeriodique>;

    get api() {
        return {
            get: '/competences/elementsBilanPeriodique?idEtablissement=' + evaluations.structure.id,
            GET_APPRECIATIONS: '/competences/elementsBilanPeriodique?idEtablissement=' + evaluations.structure.id
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

        // this.collection(ElementBilanPeriodique, {
        //     sync: function () {
        //         return new Promise((resolve) => {
        //             let url = this.api.get + "&idClasse=" + this.classe + "&idEnseignant=" + model.me.id;
        //             httpCore().getJson(url).done((res) => {
        //                 if (res) {
        //                     this.elementsBilanPeriodique = res;
        //                 }
        //                 if (resolve && typeof (resolve) === 'function') {
        //                     resolve();
        //                 }
        //             });
        //         });
        //     }
        // });
    }

    syncClasse(): Promise<any> {
        return new Promise(async (resolve, reject) => {
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
            let data = await http.get(this.api.get + "&idClasse=" + this.classe.id + "&idEnseignant=" + model.me.userId);
            this.elements = data.data;
        } catch (e) {
            notify.error('evaluations.elements.get.error');
        }
    }

}