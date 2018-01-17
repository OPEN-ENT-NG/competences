import { Model } from 'entcore';

export class Responsable extends Model {
    id: string;
    externalId : string;
    displayName : string;
    selected : boolean;

}
