import { Model, Collection, _ } from 'entcore';
import { evaluations } from './index';

export class Competence extends Model {
    competences : Collection<Competence>;
    selected : boolean;
    id : number;
    id_competence : number;
    nom : string;
    code_domaine : string;
    ids_domaine : string;
    composer : any;

    constructor () {
        super();
        this.collection(Competence);
    }

    selectChildren (bool) : Promise<any> {
        return new Promise((resolve, reject) => {
            if(this.competences.all.length !== 0){
                _.each(this.competences.all, function(child){
                    child.selected = bool;
                    child.selectChildren(bool).then(resolve);
                });
            }else{
                if (resolve && (typeof (resolve) === 'function')) {
                    resolve();
                }
            }
        });
    }

    findSelectedChildren () {
        if(this.selected === true){
            evaluations.competencesDevoir.push(this.id);
        }
        if(this.competences.all.length !== 0){
            _.each(this.competences.all, function(child){
                child.findSelectedChildren();
            });
        }
    }
}