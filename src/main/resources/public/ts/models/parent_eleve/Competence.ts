import {DefaultCompetence} from "../common/DefaultCompetence";

export class Competence extends DefaultCompetence {
    constructor () {
        super();
        this.collection(Competence);
    }

}