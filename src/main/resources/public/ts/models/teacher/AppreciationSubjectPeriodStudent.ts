import {http} from "entcore";

export class AppreciationSubjectPeriodStudent {
    private id:Number;
    public idStudent:String;
    public appreciation:String;
    private isDeleted = false;
    private idSubject:String;
    private idClass:String;
    private idPeriod:String;
    private idStructure:String;
    private URL_API = "/competences/appreciation-subject-period";

    constructor(appreciationSubjectPeriodStudent = undefined) {
        if(appreciationSubjectPeriodStudent){
            this.id = appreciationSubjectPeriodStudent.id;
            this.idStudent = appreciationSubjectPeriodStudent.idStudent;
            this.appreciation = appreciationSubjectPeriodStudent.appreciation;
            this.idSubject = appreciationSubjectPeriodStudent.idSubject;
            this.idClass = appreciationSubjectPeriodStudent.idClass;
            this.idPeriod = appreciationSubjectPeriodStudent.idPeriod;
            this.idStructure = appreciationSubjectPeriodStudent.idStructure;
        }
    }

    public async post():Promise<void>{
        await http().postJson(this.URL_API, this.toJSON())
    }

    public async put():Promise<void>{
        await http().putJson(this.URL_API, this.toJSON())
    }

    public async delete():Promise<void>{
        if(this.appreciation.length === 0) this.isDeleted = true;
        await http().deleteJson(this.URL_API, this.toJSON());
    }

    private toJSON():any{
        return {
            idMatiere: this.idSubject,
            idClasse: this.idClass,
            idEtablissement: this.idStructure,
            idPeriode: this.idPeriod,
            idEleve: this.idStudent,
            appreciation_matiere_periode: this.appreciation,
            delete: this.isDeleted,
        };
    }
}