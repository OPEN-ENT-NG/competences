package fr.openent.competences.constants;

public class Field {



    private Field() {
        throw new IllegalStateException("Utility class");
    }

    //id
    public static final String ID = "id";
    public static final String _ID = "_id";
    public static final String IDDOMAINE = "idDomaine";
    public static final String IDSTRUCTURE = "idStructure";
    public static final String STRUCTUREID = "structureId";
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
    public static final String ID_GROUPES = "id_groupes";
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
    public static final String IDENSEIGNANT = "idEnseignant";
    public static final String CLASSID = "classId";
    public static final String DEVOIRID = "devoirId";
    public static final String IDDEVOIR = "idDevoir";
    public static final String ID_DEVOIR = "id_devoir";
    public static final String DELETED_DATE = "deleted_date";
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
    public static final String ENTERED_END_DATE = "entered_end_date";
    public static final String TIMESTAMP_DT = "timestamp_dt";
    public static final String TIMESTAMP_FN = "timestamp_fn";
    public static final String ID_TYPE = "id_type";
    public static final String PERIODEID = "periodeId";
    public static final String TEACHER_ID = "teacher_id";
    public static final String IDREPORTMODEL = "idReportModel";
    public static final String SUBJECTID = "subjectId";
    public static final String GROUPID = "groupId";
    public static final String USERID = "userId";
    public static final String IDTYPEPERIODE = "idTypePeriode";


    public static final String MAIN_TEACHER_ID = "main_teacher_id";
    public static final String SECOND_TEACHER_ID = "second_teacher_id";
    public static final String SUBJECT_ID = "subject_id";
    public static final String CLASS_OR_GROUP_ID = "class_or_group_id";


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
    public static final String RESULT = "result";
    public static final String RESULTS = "results";

    public static final String MULTITEACHING = "multiTeaching";
    public static final String GETIDMULTITEACHERS = "getIdMultiTeachers";
    //Modules
    public static final String PRESENCES = "presences";

    //variables
    public static final String NN = "NN";
    public static final String SAVE_BFC = "saveBFC";
    public static final String SUBCOEF = "subCoef";
    public static final String SERVICE_SUBTOPIC = "services_subtopic";
    public static final String COEFFICIENT = "coefficient";
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
    public static final String IS_VISIBLE = "is_visible";
    public static final String dateFormateYYYYMMDDTHHMMSS  = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String IS_ANNOTATION = "is_annotation";
    public static final String NOTE = "note";
    public static final String LIBELLE_COURT = "libelle_court";
    public static final String HASDIVISEUR = "hasDiviseur";
    public static final String DATA = "data";
    public static final String ORDREPERIODE = "ordrePeriode";
    public static final String LIBELLE = "libelle";
    public static final String ANNEE = "Ann\u00E9e";
    public static final String TYPEGROUPE = "typeGroupe";
    public static final String NAME = "name";
    public static final String WITHMOYGENERALEBYELEVE = "withMoyGeneraleByEleve";
    public static final String WITHMOYMINMAXBYMAT = "withMoyMinMaxByMat";
    public static final String TEXT = "text";
    public static final String ELEVES = "eleves";
    public static final String ELEVEMOYBYMAT = "eleveMoyByMat";
    public static final String MATIERES = "matieres";
    public static final String RANK = "rank";
    public static final String PERIODE = "periode";
    public static final String NAMECLASSE = "nameClasse";
    public static final String NAMECLASS = "nameClass";
    //

    public static final String ISSKILLAVERAGE = "isSkillAverage";
    public static final String TYPECLASSE = "typeClasse";

    public static final String ERROR = "error";

    //colonne
    public static final String IS_AVERAGE_SKILLS = "is_average_skills";
    public static final String EVALUATION = "evaluation";

    //schema json
    public static final String SCHEMA_EVAL_CREATEORUPDATESTRUCTUREOPTIONISAVERAGESKILLS =
            "eval_createOrUpdateStructureOptionIsAverageSkills";

    //schemas
    public static final String DB_SCHEMA = "db-schema";
    public static final String DB_VIESCO_SCHEMA = "vsco-schema";

    // TABLE
    public static final String APPRECIATION_CLASSE_TABLE = "appreciation_classe";
    public static final String APPRECIATION_ELT_BILAN_PERIODIQUE_CLASSE_TABLE = "appreciation_elt_bilan_periodique_classe";
    public static final String APPRECIATION_ELT_BILAN_PERIODIQUE_ELEVE_TABLE = "appreciation_elt_bilan_periodique_eleve";
    public static final String APPRECIATION_MATIERE_PERIODE_TABLE = "appreciation_matiere_periode";
    public static final String APPRECIATIONS_TABLE = "appreciations";
    public static final String AVIS_CONSEIL_DE_CLASSE_TABLE = "avis_conseil_de_classe";
    public static final String AVIS_CONSEIL_BILAN_PERIODIQUE_TABLE = "avis_conseil_bilan_periodique";
    public static final String AVIS_CONSEIL_ORIENTATION_TABLE ="avis_conseil_orientation";
    public static final String BFC_SYNTHESE_TABLE = "bfc_synthese";
    public static final String BFC_TABLE = "bilan_fin_cycle";
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
    public static String IS_COTEACHING = "is_coteaching";

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
    public static final String REL_GROUPE_APPRECIATION_ELT_ELEVE_TABLE = "rel_groupe_appreciation_elt_eleve";
    public static final String REL_ELT_BILAN_PERIODIQUE_GROUPE_TABLE = "rel_elt_bilan_periodique_groupe";
    public static final String REL_ELT_BILAN_PERIODIQUE_INTERVENANT_MATIERE_TABLE = "rel_elt_bilan_periodique_intervenant_matiere";
    public static final String REL_PROFESSEURS_REMPLACANTS_TABLE = "rel_professeurs_remplacants";
    public static final String REL_APPRECIATION_USERS_NEO_TABLE = "rel_appreciations_users_neo";

    public static final String STRUTUCTURE_OPTIONS = "structure_options";
    public static final String STUDENT_APPRECIATION_DIGITAL_SKILLS_TABLE = "student_appreciation_digital_skills";
    public static final String STUDENT_DIGITAL_SKILLS_TABLE = "student_digital_skills";
    public static final String STSFILE_TABLE = "sts_file";
    public static final String SUBTOPIC_TABLE = "services_subtopic";
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

    //numbers
    public static final double ROUNDER = 10.0; //Cette constante permet d'arrondir au dixième près avec la formule mathémathique adéquate.
    public static final int DIVISEUR_NOTE = 20;


    //UserType
    public static final String TEACHER = "Teacher";
}
