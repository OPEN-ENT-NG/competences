


export class Historique{
   id_type: number;
   moyEleveAllMatieres: number[];
   moyClasseAllMatieres: number[];
   moyGeneraleEleve: string;
   moyGeneraleClasse: string;

    constructor (pIdPeriode : number){
        this.id_type = pIdPeriode;
        this.moyClasseAllMatieres = [];
        this.moyEleveAllMatieres = [];
    }
}