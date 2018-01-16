/**
 * Created by anabah on 09/05/2017.
 */
import { Model } from 'entcore';


export class DefaultEleve extends Model {
    id: string;
    firstName: string;
    lastName: string;
    idClasse: string;
    displayName: string;

    //TODO Delete when infra-front will be fixed
    updateData: (o) => void;
}