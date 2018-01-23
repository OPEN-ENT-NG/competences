import { Model } from 'entcore';
import { SousMatiere } from './index';

export class Matiere extends Model {

    constructor () {
        super();
        this.collection(SousMatiere);
    }
}