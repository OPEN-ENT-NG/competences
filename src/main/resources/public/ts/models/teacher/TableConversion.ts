import { Model } from 'entcore';

export class TableConversion extends  Model {
    valmin : number;
    valmax : number;
    libelle : string;
    ordre : number;
    couleur : string;

    constructor(p? : any) {
        super();
    }
}