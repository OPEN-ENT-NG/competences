import { Model, Collection } from 'entcore';
import { Competence } from "../parent_eleve/Competence";

export class DefaultEnseignement extends Model {
    id;
    competences : Collection<Competence>;

    //TODO Delete when infra-front will be fixed
    updateData: (o) => void;
    collection: (type, mixin?, name?) => void;
}