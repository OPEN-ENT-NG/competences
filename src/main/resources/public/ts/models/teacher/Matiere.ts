import { Model } from 'entcore';
import { SousMatiere } from './index';

export class Matiere extends Model {
    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
    where: (params) => any;

    constructor () {
        super();
        this.collection(SousMatiere);
    }
}