/**
 * Created by agnes.lapeyronnie on 19/09/2017.
 */
import { Collection, _ } from 'entcore';
import { Responsable, Classe } from '../Teacher/eval_teacher_mdl';

export class LSU {
    responsables : Collection<Responsable>;
    classes : Array<Classe>;//sans les groupes
    structureId : string;


    constructor (structureId : string, classes : Array<Classe>, responsables : Collection<Responsable>){
        this.structureId = structureId ;
        this.classes = classes;
        this.responsables =_.clone(responsables) ;

    }

    export () {
        let url = "/competences/exportLSU/lsu?idStructure=" + this.structureId;

            for(var i=0; i<this.classes.length;i++) {
                if(this.classes[i].selected){
                    url += "&idClasse=" + this.classes[i].id;
                }
            }

          /*  _.each(_.where(this.classes.all, {selected: true}), (classe) => {
                    url += "&idClasse=" + this.classes.all[i].id;

            });*/

            for(let i=0 ; i < this.responsables.all.length ; i++){
                if(this.responsables.all[i].selected){
                url+="&idResponsable=" + this.responsables.all[i].id;
                }
            }

        location.replace(url);
    }

}