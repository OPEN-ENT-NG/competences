import { http, model, _ } from 'entcore';
import { Devoir, evaluations } from './index';

export class DevoirsCollection {
    all : Devoir[];
    sync : any;
    percentDone : boolean;
    idEtablissement: string;

    get api () {
        return {
            get : '/competences/devoirs?idEtablissement=' + this.idEtablissement,
            areEvaluatedDevoirs : '/competences/devoirs/evaluations/informations?',
            done : '/competences/devoirs/done'
        }
    }

    constructor (idEtablissement : string) {
        this.idEtablissement = idEtablissement;
        this.sync =  function () {
            return new Promise((resolve) => {
                http().getJson(this.api.get).done(function (res) {
                    this.load(res);
                    if (evaluations.synchronized.matieres) {
                        DevoirsCollection.synchronizeDevoirMatiere();
                    } else {
                        evaluations.matieres.on('sync', function () {
                            DevoirsCollection.synchronizeDevoirMatiere();
                        });
                    }
                    if (evaluations.synchronized.types) {
                        evaluations.devoirs.synchronizedDevoirType();
                    } else {
                        evaluations.types.on('sync', function () {
                            evaluations.devoirs.synchronizedDevoirType();
                        });
                    }
                    evaluations.devoirs.trigger('sync');
                    if (resolve && (typeof(resolve) === 'function')) {
                        resolve(res);
                    }
                }.bind(this));
            });
        };
    }

    static synchronizeDevoirMatiere () {
        for (let i = 0; i < evaluations.devoirs.all.length; i++) {
            let matiere = evaluations.matieres.findWhere({id : evaluations.devoirs.all[i].id_matiere});
            if (matiere) evaluations.devoirs.all[i].matiere = matiere;
        }
    }

    static synchronizedDevoirType () {
        for (let i = 0 ; i < evaluations.devoirs.all.length; i++) {
            let type = evaluations.types.findWhere({id : evaluations.devoirs.all[i].id_type});
            if (type) evaluations.devoirs.all[i].type = type;
        }
    }

    areEvaluatedDevoirs (idDevoirs) : Promise<any> {
        return new Promise((resolve) => {
            let URLBuilder = "";
            for (let i=0; i<idDevoirs.length; i++){
                if(i==0)
                    URLBuilder = "idDevoir="+idDevoirs[i];
                else
                    URLBuilder += "&idDevoir="+idDevoirs[i];
            }
            http().getJson(this.api.areEvaluatedDevoirs + URLBuilder  ).done(function(data){
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve(data);
                }
            });
        });
    }


    getPercentDone (devoir?) : Promise<any> {
        return new Promise((resolve, reject) => {
            if(devoir && evaluations.structure.synchronized.devoirs) {
                let url = this.api.done + "?idDevoir="+devoir.id + "&is_evaluated=" +devoir.is_evaluated;
                url += "&idGroupe=" + devoir.id_groupe;
                url += "&has_competence=" + (devoir.competences.all.length > 0 || devoir.nbcompetences > 0);
                http().getJson(url)
                    .done((res) => {
                        let calculatedPercent = _.findWhere(res, {id : devoir.id});
                        let _devoir = _.findWhere(this.all, {id : devoir.id});
                        if (_devoir !== undefined) {
                            _devoir.percent = calculatedPercent === undefined ? 0 : calculatedPercent.percent;
                        }
                        model.trigger('apply');
                        resolve();
                    })
                    .error(() => {
                        reject();
                    });
            }
        });
    }
}