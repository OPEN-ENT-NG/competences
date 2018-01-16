import { DefaultEnseignant } from "../common/DefaultEnseignant";

export class Enseignant extends DefaultEnseignant {

    constructor(p? : any) {
        super();
        if (p) this.updateData(p)
    }
}
