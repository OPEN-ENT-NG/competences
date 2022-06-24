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

    public static final String REL_GROUPE_APPRECIATION_ELT_ELEVE_TABLE = "rel_groupe_appreciation_elt_eleve";
    public static final String REL_ELT_BILAN_PERIODIQUE_GROUPE_TABLE = "rel_elt_bilan_periodique_groupe";
    public static final String REL_ELT_BILAN_PERIODIQUE_INTERVENANT_MATIERE_TABLE = "rel_elt_bilan_periodique_intervenant_matiere";
    public static final String REL_PROFESSEURS_REMPLACANTS_TABLE = "rel_professeurs_remplacants";
    public static final String REL_APPRECIATION_USERS_NEO_TABLE = "rel_appreciations_users_neo";
    public static final String STUDENT_APPRECIATION_DIGITAL_SKILLS_TABLE = "student_appreciation_digital_skills";
    public static final String STUDENT_DIGITAL_SKILLS_TABLE = "student_digital_skills";
    public static final String STSFILE_TABLE = "sts_file";
    public static final String SYNTHESE_BILAN_PERIODIQUE_TABLE = "synthese_bilan_periodique";
    public static final String THEMATIQUE_BILAN_PERIODIQUE_TABLE = "thematique_bilan_periodique";
    public static final String TRANSITION_TABLE = "transition";
    public static final String USERS_TABLE = "users";
    public static final String USE_PERSO_NIVEAU_COMPETENCES_TABLE = "use_perso";
    public static final String VIESCO_ABSENCES_ET_RETARDS_TABLE = "absences_et_retards";
    public static final String VIESCO_PERIODE_TABLE = "periode";
    public final static String VIESCO_MATIERE_LIBELLE_TABLE = "subject_libelle";
    public final static String VIESCO_MODEL_MATIERE_LIBELLE_TABLE = "model_subject_libelle";
    public static final String VIESCO_MULTI_TEACHING_TABLE = "multi_teaching";
    public final static String VIESCO_MATIERE_TABLE = "matiere";
    public final static String VIESCO_SOUS_MATIERE_TABLE = "sousmatiere";
    public final static String VIESCO_TYPE_SOUS_MATIERE_TABLE = "type_sousmatiere";
    public final static String VIESCO_SERVICES_TABLE = "services";

    //variables
    public static final String SAVE_BFC = "saveBFC";
}
