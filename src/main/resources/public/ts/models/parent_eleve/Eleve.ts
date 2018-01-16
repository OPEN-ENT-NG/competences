import {DefaultEleve} from "../common/DefaultEleve";
import {Classe} from "./Classe";

export class Eleve extends DefaultEleve {
    classe: Classe;
    idStructure: string;

    constructor(o?: any) {
        super();
        if (o) this.updateData(o);
    }
}