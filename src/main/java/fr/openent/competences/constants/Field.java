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
    public static final String DEVOIR_SHARE_TABLE = "devoirs_shares";
    public static final String DIGITAL_SKILLS_TABLE = "digital_skills";
    public static final String DOMAINE_DIGITAL_SKILLS_TABLE = "domaines_digital_skills";
    public static final String DISPENSE_DOMAINE_ELEVE = "dispense_domaine_eleve";
    public static final String DOMAINES_TABLE = "domaines";

    public static final String ELEMENT_PROGRAMME_TABLE = "element_programme";
    public static final String ELEVE_ENSEIGNEMENT_COMPLEMENT = "eleve_enseignement_complement";
    public static final String ELEVES_IGNORES_LSU_TABLE = "eleves_ignores_lsu";
    public static final String ELT_BILAN_PERIODIQUE_TABLE = "elt_bilan_periodique";
    public static final String ENSEIGNEMENT_COMPLEMENT = "enseignement_complement";
    public static final String ENSEIGNEMENTS_TABLE = "enseignements";
    //variables
    public static final String SAVE_BFC = "saveBFC";
}
