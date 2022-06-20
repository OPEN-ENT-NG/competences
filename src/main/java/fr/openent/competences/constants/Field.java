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

    //Event bus
    public static final String OK = "ok";
    public static final String STATUS = "status";
    public static final String RESULTS = "results";

    //db
    public static final String SUBTOPIC_TABLE = "services_subtopic";


    //model
    public static final String COEFFICIENT = "coefficient";

}
