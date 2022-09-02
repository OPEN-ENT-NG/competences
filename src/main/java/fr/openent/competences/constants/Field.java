package fr.openent.competences.constants;

public class Field {



    private Field() {
        throw new IllegalStateException("Utility class");
    }

    //id
    public static final String ID = "id";
    public static final String IDSTRUCTURE = "idStructure";
    public static final String IDETABLISSEMENT = "idEtablissement";
    public static final String IDCLASSE = "idClasse";
    public static final String ID_STRUCTURE = "id_structure";
    public static final String ID_MATIERE = "id_matiere";
    public static final String ID_SUBTOPIC = "id_subtopic";
    public static final String ID_TEACHER = "id_teacher";
    public static final String ID_TOPIC = "id_topic";
    public static final String ID_GROUP = "id_group";

    //transaction
    public static final String ACTION = "action";
    public static final String VALUES = "values";
    public static final String STATEMENT = "statement";
    public static final String PREPARED = "prepared";

    //Event bus
    public static final String OK = "ok";
    public static final String STATUS = "status";
    public static final String RESULTS = "results";

    //variables
    public static final String SAVE_BFC = "saveBFC";
    public static final String SUBCOEF = "subCoef";
    public static final String SERVICE_SUBTOPIC = "services_subtopic";
    public static final String COEFFICIENT = "coefficient" ;
    public static final String GROUPS = "groups";

    //subTopic

    public static final String IDDEVOIR = "idDevoir";

}
