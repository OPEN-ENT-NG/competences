package fr.openent.competences.constants;

public class Field {

    private Field() {
        throw new IllegalStateException("Utility class");
    }

    // ID
    public static final String ID = "id";

    // TRANSACTION
    public static final String ACTION = "action";
    public static final String VALUES = "values";
    public static final String STATEMENT = "statement";

    // EVENT BUS
    public static final String OK = "ok";
    public static final String STATUS = "status";
    public static final String RESULTS = "results";

    //variables
    public static final String SAVE_BFC = "saveBFC";
    // TABLE
    public static final String APPRECIATION_CLASSE_TABLE = "appreciation_classe";
    public static final String APPRECIATION_ELT_BILAN_PERIODIQUE_CLASSE_TABLE = "appreciation_elt_bilan_periodique_classe";
    public static final String APPRECIATION_ELT_BILAN_PERIODIQUE_ELEVE_TABLE = "appreciation_elt_bilan_periodique_eleve";
    public static final String APPRECIATION_MATIERE_PERIODE_TABLE = "appreciation_matiere_periode";
    public static final String APPRECIATIONS_TABLE = "appreciations";
    public static final String AVIS_CONSEIL_DE_CLASSE_TABLE = "avis_conseil_de_classe";
    public static final String AVIS_CONSEIL_ORIENTATION_TABLE ="avis_conseil_orientation";
    public static final String BFC_SYNTHESE_TABLE = "bfc_synthese";
    public static final String BFC_TABLE = "bilan_fin_cycle";
    public static final String BFC_ARCHIVE_TABLE = "archive_bfc";

}
