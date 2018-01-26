import { Model, Collection, http } from 'entcore';
import { Domaine, CompetenceNote, Periode, Classe, Utils } from './index';

export class SuiviCompetenceClasse extends Model {
    domaines : Collection<Domaine>;
    competenceNotes : Collection<CompetenceNote>;
    periode : Periode;

    get api() {
        return {
            getCompetencesNotesClasse : '/competences/competence/notes/classe/',
            getArbreDomaines : '/competences/domaines?idClasse='
        }
    }

    constructor (classe : Classe, periode : any) {
        super();
        this.periode = periode;
        var that = this;

        this.collection(Domaine, {
            sync: function () {
                return new Promise((resolve, reject) => {
                    var url = that.api.getArbreDomaines + classe.id;
                    http().getJson(url).done((resDomaines) => {
                        var url = that.api.getCompetencesNotesClasse + classe.id+"/"+ classe.type_groupe;
                        if (periode !== null && periode !== undefined && periode !== '*') {
                            if(periode.id_type !== undefined)url += "?idPeriode="+periode.id_type;
                        }
                        http().getJson(url).done((resCompetencesNotes) => {
                            if(resDomaines) {
                                for(let i=0; i<resDomaines.length; i++) {
                                    var domaine = new Domaine(resDomaines[i]);
                                    that.domaines.all.push(domaine);
                                    Utils.setCompetenceNotes(domaine, resCompetencesNotes, this, classe);
                                }
                            }
                        });
                        if (resolve && typeof (resolve) === 'function') {
                            resolve();
                        }
                    });
                });
            }
        });

    }

    addEvalLibre (eleve){


    }
    findCompetence (idCompetence) {
        for(var i=0; i<this.domaines.all.length; i++) {
            var comp = Utils.findCompetenceRec(idCompetence, this.domaines.all[i].competences);
            if(comp !== undefined) {
                return comp;
            }
        }
        return false;
    }


    sync () : Promise<any> {
        return new Promise((resolve, reject) => {
            resolve();
        });
    }
}