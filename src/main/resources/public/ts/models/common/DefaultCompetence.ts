import { Model, Collection } from 'entcore';
import { Competence } from "../teacher";

export class DefaultCompetence extends Model {
    competences: Collection<Competence>;
    selected: boolean;
    id: number;
    id_competence: number;
    nom: string;
    code_domaine: string;
    ids_domaine: string;
    composer: any;
}