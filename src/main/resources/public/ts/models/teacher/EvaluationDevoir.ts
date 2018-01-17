import { Model } from 'entcore';

export class EvaluationDevoir extends  Model {
    nbreval : number;
    id : string;
    evaluation : number;
    typeeval : string;

    constructor(p? : any) {
        super();
    }
}