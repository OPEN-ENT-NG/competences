import { Model, Collection } from 'entcore';
import { Competence } from "../parent_eleve/Competence";

export class DefaultEnseignement extends Model {
    id;
    competences : Collection<Competence>;
}