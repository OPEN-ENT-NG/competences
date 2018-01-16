/**
 * Created by anabah on 31/08/2017.
 */

import { Model } from 'entcore';
import {NiveauCompetence} from "./eval_niveau_comp";

export class Cycle extends Model {
    id_cycle: string;
    libelle: string;
    selected: boolean;
    niveauCompetencesArray: Array<NiveauCompetence>;
}