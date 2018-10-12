import { Model } from 'entcore';

export class Enseignant extends Model{
    id : string;
    displayName : string;
    name : string;
    lastName : string;

    constructor(p? : any) {
        super();
    }
}