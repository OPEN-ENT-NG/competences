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

    // TABLE
    public static final String BFC_ARCHIVE_TABLE = "archive_bfc";
    public static final String BULLETIN_ARCHIVE_TABLE = "archive_bulletins";
    public static final String CLASS_APPRECIATION_DIGITAL_SKILLS = "class_appreciation_digital_skills";
    public static final String COMPETENCE_NIVEAU_FINAL = "competence_niveau_final";
    public static final String COMPETENCE_NIVEAU_FINAL_ANNUEL = "competence_niveau_final_annuel";
    public static final String COMPETENCES_TABLE = "competences";
    public static final String COMPETENCES_DEVOIRS = "competences_devoirs";
    public static final String COMPETENCES_NOTES_TABLE = "competences_notes";
    public static final String CYCLE_TABLE = "cycle";
    public static final String DEVOIR_TABLE = "devoirs";

    //variables
    public static final String SAVE_BFC = "saveBFC";
}
