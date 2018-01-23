import { Model } from 'entcore';

export class Annotation extends Model {
    libelle: string;
    libelle_court: string;

    constructor (o?) {
        super();
        if (o) this.updateData(o);
    }

    toString () {
        return this.libelle;
    }
}