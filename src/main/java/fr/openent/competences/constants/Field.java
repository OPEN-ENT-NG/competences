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
    public static final String LANGUES_CULTURE_REGIONALE = "langues_culture_regionale";
    public static final String MATIERE_TABLE = "matiere";
    public static final String MODALITES_TABLE = "modalites";
    public static final String MOYENNE_FINALE_TABLE = "moyenne_finale";
    public static final String NIVEAU_COMPETENCES_TABLE = "niveau_competences";
    public static final String NIVEAU_ENS_COMPLEMENT = "niveau_ens_complement";

    public static final String NOTES_TABLE = "notes";
    public static final String PERSO_COMPETENCES_TABLE = "perso_competences";
    public static final String PERSO_NIVEAU_COMPETENCES_TABLE = "perso_niveau_competences";
    public static final String PERSO_COMPETENCES_ORDRE_TABLE = "perso_order_item_enseignement";
    public static final String REL_ANNOTATIONS_DEVOIRS_TABLE = "rel_annotations_devoirs";
    public static final String REL_COMPETENCES_DOMAINES_TABLE = "rel_competences_domaines";
    public static final String REL_COMPETENCES_ENSEIGNEMENTS_TABLE = "rel_competences_enseignements";
    public static final String REL_DEVOIRS_GROUPES_TABLE = "rel_devoirs_groupes";

    //variables
    public static final String SAVE_BFC = "saveBFC";
}
