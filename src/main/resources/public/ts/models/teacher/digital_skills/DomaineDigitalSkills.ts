import {Model, notify} from "entcore";
import { http } from 'entcore-toolkit';
import {EvaluatedDigitalSkills} from "./EvaluatedDigitalSkills";

export class DomaineDigitalSkills extends Model {
    id: number;
    libelle: string;
    digitalSkills: Array<EvaluatedDigitalSkills>;

    constructor (id, libelle) {
        super();
        this.id = id;
        this.libelle = libelle;
        this.digitalSkills = [];
    }
}