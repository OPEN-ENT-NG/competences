import { DefaultEnseignement } from "../common/DefaultEnseignement";
import { Competence } from "./Competence";


export class Enseignement extends DefaultEnseignement {

    constructor(p? : any) {
        super();
        if (p) this.updateData(p);
        this.collection(Competence);
    }
}
