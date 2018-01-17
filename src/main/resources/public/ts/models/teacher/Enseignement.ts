import { Model, Collection } from 'entcore';
import { Competence } from './index';

export class Enseignement extends Model {
    competences : Collection<Competence>;
    id;

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
        this.collection(Competence);
    }
}
