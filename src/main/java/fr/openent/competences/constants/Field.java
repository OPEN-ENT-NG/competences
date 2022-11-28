package fr.openent.competences.constants;

public class Field {


    private Field() {
        throw new IllegalStateException("Utility class");
    }

    //id
    public static final String ID = "id";
    public static final String _ID = "_id";
    public static final String IDSTRUCTURE = "idStructure";
    public static final String IDETABLISSEMENT = "idEtablissement";
    public static final String IDCLASSE = "idClasse";
    public static final String ID_STRUCTURE = "id_structure";
    public static final String ID_MATIERE = "id_matiere";
    public static final String IDMATIERE = "idMatiere";
    public static final String ID_SUBTOPIC = "id_subtopic";
    public static final String ID_TEACHER = "id_teacher";
    public static final String ID_TOPIC = "id_topic";
    public static final String ID_GROUP = "id_group";
    public static final String ID_GROUPS = "id_groups";
    public static final String ID_GROUPE = "id_groupe";
    public static final String ID_TYPESOUSMATIERE = "id_type_sousmatiere";
    public static final String ID_PERIODE = "id_periode";
    public static final String IDPERIODE = "idPeriode";
    public static final String ID_ELEVE = "id_eleve";
    public static final String IDELEVE = "idEleve";
    public static final String ID_CLASSE = "id_classe";
    public static final String ID_ELEVE_MOYENNE_FINALE = "id_eleve_moyenne_finale";
    public static final String ID_SOUSMATIERE = "id_sousmatiere";
    public static final String ID_MATIERE_MOYF = "id_matiere_moyf";
    public static final String ID_ENSEIGNANT = "id_enseignant";
    public static final String CLASSID = "classId";
    public static final String DEVOIRID = "devoirId";
    public static final String PERIODEID = "periodeId";
    public static final String TEACHER_ID = "teacher_id";
    public static final String IDREPORTMODEL = "idReportModel";


    public static final String MAIN_TEACHER_ID = "main_teacher_id";
    public static final String SECOND_TEACHER_ID = "second_teacher_id";
    public static final String SUBJECT_ID = "subject_id";
    public static final String CLASS_OR_GROUP_ID = "class_or_group_id";
    public static final String IS_VISIBLE = "is_visible";

    //transaction
    public static final String ACTION = "action";
    public static final String VALUES = "values";
    public static final String STATEMENT = "statement";
    public static final String PREPARED = "prepared";
    public static final String STATE = "state";
    public static final String MESSAGE = "message";
    public static final String INSTALLED = "installed";
    public static final String ACTIF = "actif";
    public static final String ACTIVATE = "activate";
    public static final String MISSING = "missing";


    //Event bus
    public static final String OK = "ok";
    public static final String STATUS = "status";
    public static final String RESULTS = "results";
    public static final String SUBJECTID = "subjectId";
    public static final String GROUPID = "groupId";
    public static final String USERID = "userId";
    public static final String MULTITEACHING = "multiTeaching";
    public static final String GETIDMULTITEACHERS = "getIdMultiTeachers";

    //Modules
    public static final String PRESENCES = "presences";

    //variables
    public static final String NN = "NN";
    public static final String SAVE_BFC = "saveBFC";
    public static final String SUBCOEF = "subCoef";
    public static final String SERVICE_SUBTOPIC = "services_subtopic";
    public static final String COEFFICIENT = "coefficient" ;
    public static final String GROUPS = "groups";
    public static final String TEACHERS = "teachers";
    public static final String MOYENNE = "moyenne";
    public static final String HASNOTE = "hasNote";
    public static final String VALEUR = "valeur";
    public static final String IS_EVALUATED = "is_evaluated";
    public static final String OWNER = "owner";
    public static final String DIVISEUR = "diviseur";
    public static final String RAMENER_SUR = "ramener_sur";
    public static final String DEVOIRS = "devoirs";
    public static final String SOUS_MATIERES = "sous_matieres";
    public static final String _TAIL = "_tail";
    public static final String HASSOUSMATIERE = "hasSousMatiere";
    public static final String COEFF = "coeff";
    public static final String DISPLAYNAME = "displayName";
    public static final String CLASSTYPE = "classType";
    public static final String FORMATE = "formate";


    //subTopic

    public static final String IDDEVOIR = "idDevoir";

    //db
    public static final String SUBTOPIC_TABLE = "services_subtopic";
    public static final String DEVOIR_SHARE_TABLE = "devoirs_shares";


    //

    public static final String STRUCTUREID = "structureId";
    public static final String ISSKILLAVERAGE = "isSkillAverage";
    public static final String TYPECLASSE = "typeClasse";
    public static final String IDDOMAINE = "idDomaine";
    public static final String ERROR = "error";

    // tables
    public static final String STRUTUCTURE_OPTIONS = "structure_options";

    //colonne
    public static final String IS_AVERAGE_SKILLS = "is_average_skills";
    public static final String EVALUATION = "evaluation";

    //schema json
    public static final String SCHEMA_EVAL_CREATEORUPDATESTRUCTUREOPTIONISAVERAGESKILLS =
            "eval_createOrUpdateStructureOptionIsAverageSkills";



    public static final String ID_DEVOIR = "id_devoir";
    //public static final String ID_STRUCTURE = "idStructure";

    //numbers
    public static final double ROUNDER = 10.0; //Cette constante permet d'arrondir au dixième près avec la formule mathémathique adéquate.
    public static final int DIVISEUR_NOTE = 20;
}
