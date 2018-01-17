import { Model } from 'entcore';

export class Annotation extends Model {
    libelle: string;
    libelle_court: string;

    //TODO Delete when infra-front will be fixed
    collection:  (type, mixin?, name?) => void;
    updateData: (o) => void;

    constructor (o?) {
        super();
        if (o) this.updateData(o);
    }

    toString () {
        return this.libelle;
    }
}