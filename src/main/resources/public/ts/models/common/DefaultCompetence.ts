import { Model, Collection } from 'entcore';
import { Competence } from "../teacher/eval_teacher_mdl";

export class DefaultCompetence extends Model {
    competences: Collection<Competence>;
    selected: boolean;
    id: number;
    id_competence: number;
    nom: string;
    code_domaine: string;
    ids_domaine: string;
    composer: any;

    //TODO Delete when infra-front will be fixed
    collection:  (type, mixin?, name?) => void;
    updateData: (o) => void;
}