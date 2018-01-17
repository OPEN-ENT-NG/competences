import { Model } from 'entcore';

export class Enseignant extends Model{
    id : string;
    displayName : string;

    constructor(p? : any) {
        super();
    }
}