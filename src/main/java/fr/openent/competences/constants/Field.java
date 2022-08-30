package fr.openent.competences.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    //id
    public static final String ID = "id";

    //transaction
    public static final String ACTION = "action";
    public static final String VALUES = "values";
    public static final String STATEMENT = "statement";
    public static final String STATE = "state";
    public static final String MESSAGE = "message";
    public static final String INSTALLED = "installed";
    public static final String ACTIF = "actif";
    public static final String ACTIVATE = "activate";

    //Event bus
    public static final String OK = "ok";
    public static final String STATUS = "status";
    public static final String RESULTS = "results";

    //Modules
    public static final String PRESENCES = "presences";

    //variables
    public static final String SAVE_BFC = "saveBFC";
    public static final String STRUCTUREID = "structureId";
    public static final String ISSKILLAVERAGE = "isSkillAverage";
    public static final String IDCLASSE = "idClasse";
    public static final String TYPECLASSE = "typeClasse";
    public static final String IDDOMAINE = "idDomaine";
    public static final String IDPERIODE = "idPeriode";
    public static final String ERROR = "error";

    // tables
    public static final String STRUTUCTURE_OPTIONS = "structure_options";

    //colonnes
    public static final String ISAVERAGESKILLS = "is_average_skills";
    public static final String EVALUATION = "evaluation";

    //schema json
    public static final String SCHEMA_EVAL_CREATEORUPDATESTRUCTUREOPTIONISAVERAGESKILLS =
            "eval_createOrUpdateStructureOptionIsAverageSkills";



}
