import { Model, Collection } from 'entcore';
import { Competence } from './index';

export class Enseignement extends Model {
    competences : Collection<Competence>;
    id;

    constructor () {
        super();
        this.collection(Competence);
    }
}
