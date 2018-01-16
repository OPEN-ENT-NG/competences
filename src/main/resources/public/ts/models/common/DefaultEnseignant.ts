import { Model } from 'entcore';

export class DefaultEnseignant extends Model {
    nom: string;
    prenom: string;
    id: string;

    //TODO Delete when infra-front will be fixed
    updateData: (o) => void;
}