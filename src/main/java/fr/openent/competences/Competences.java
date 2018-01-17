package fr.openent.competences;

import fr.openent.competences.controllers.*;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.impl.SqlShareService;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Competences extends BaseServer {

    public static String COMPETENCES_SCHEMA;
    public static String VSCO_SCHEMA;

    public static JsonObject LSUN_CONFIG;

    public static final String NOTES_TABLE = "notes";
    public static final String ANNOTATIONS = "annotations";
    public static final String COMPETENCES_TABLE = "competences";
    public static final String COMPETENCES_NOTES_TABLE = "competences_notes";
    public static final String ENSEIGNEMENTS_TABLE = "enseignements";
    public static final String DOMAINES_TABLE = "domaines";
    public static final String REL_PROFESSEURS_REMPLACANTS_TABLE = "rel_professeurs_remplacants";
    public static final String REL_ANNOTATIONS_DEVOIRS_TABLE = "rel_annotations_devoirs";
    public static final String APPRECIATIONS_TABLE = "appreciations";
    public static final String BFC_TABLE = "bilan_fin_cycle";
    public static final String PERSO_NIVEAU_COMPETENCES_TABLE = "perso_niveau_competences";
    public static final String NIVEAU_COMPETENCES_TABLE = "niveau_competences";
    public static final String USE_PERSO_NIVEAU_COMPETENCES_TABLE = "use_perso";
    public static final String CYCLE_TABLE = "cycle";
    public static final String BFC_SYNTHESE_TABLE = "bfc_synthese";
    public static final String REL_DEVOIRS_GROUPES = "rel_devoirs_groupes";
    public static final String COMPETENCES_DEVOIRS = "competences_devoirs";
    public static final String ENSEIGNEMENT_COMPLEMENT = "enseignement_complement";
    public static final String ELEVE_ENSEIGNEMENT_COMPLEMENT = "eleve_enseignement_complement";


    public final static String SCHEMA_ANNOTATION_UPDATE = "eval_updateAnnotation";
    public final static String SCHEMA_APPRECIATIONS_CREATE = "eval_createAppreciation";
    public final static String SCHEMA_APPRECIATIONS_UPDATE = "eval_updateAppreciation";
    public final static String SCHEMA_BFC_CREATE = "eval_createBFC";
    public final static String SCHEMA_BFC_UPDATE = "eval_updateBFC";
    public final static String SCHEMA_BFCSYNTHESE_CREATE = "eval_createBfcSynthese";
    public final static String SCHEMA_NIVEAUENSCPL_CREATE = "eval_createNiveauEnsCpl";
    public static final String SCHEMA_COMPETENCE_NOTE_CREATE = "eval_createCompetenceNote";
    public static final String SCHEMA_COMPETENCE_NOTE_UPDATE = "eval_updateCompetenceNote";
    public static final String SCHEMA_DEVOIRS_UPDATE = "eval_updateDevoir";
    public static final String SCHEMA_DEVOIRS_CREATE = "eval_createDevoir";
    public final static String SCHEMA_MAITRISE_CREATE = "eval_createMaitrise";
    public static final String SCHEMA_REL_PROFESSEURS_REMPLACANTS_CREATE = "eval_createRel_professeurs_remplacants";
    public static final String SCHEMA_NOTES_CREATE = "eval_createNote";
    public static final String SCHEMA_NOTES_UPDATE = "eval_updateNote";
    public final static String SCHEMA_USE_PERSO_NIVEAU_COMPETENCE = "eval_usePersoNiveauCompetence";
    public final static String SCHEMA_MAITRISE_UPDATE = "eval_updateMaitrise";
    public final static String DEVOIR_TABLE = "devoirs";
    public final static String DEVOIR_SHARE_TABLE = "devoirs_shares";


    public final static String DEVOIR_ACTION_UPDATE = "fr-openent-competences-controllers-DevoirController|updateDevoir";

    public final static Integer MAX_NBR_COMPETENCE = 12;

    public final static String VIESCO_BUS_ADDRESS = "viescolaire";

    @Override
	public void start() {
        super.start();

        COMPETENCES_SCHEMA = container.config().getString("db-schema");
        VSCO_SCHEMA = container.config().getString("vsco-schema");
        LSUN_CONFIG = container.config().getObject("lsun");

        EmailFactory emailFactory = new EmailFactory(vertx, container, container.config());
        EmailSender notification = emailFactory.getSender();

        final EventBus eb = getEventBus(vertx);

		addController(new CompetencesController());
		addController(new AnnotationController());
		addController(new AppreciationController());
		addController(new BFCController(eb));
		addController(new CompetenceController());
		addController(new CompetenceNoteController(eb));

        // devoir controller
        DevoirController devoirController = new DevoirController(eb);
        SqlCrudService devoirSqlCrudService = new SqlCrudService(COMPETENCES_SCHEMA, DEVOIR_TABLE, DEVOIR_SHARE_TABLE, new JsonArray().addString("*"), new JsonArray().add("*"), true);
        devoirController.setCrudService(devoirSqlCrudService);
        devoirController.setShareService(new SqlShareService(COMPETENCES_SCHEMA, DEVOIR_SHARE_TABLE, eb, securedActions, null));
        addController(devoirController);
		addController(new DomaineController());
		addController(new EnseignementController());
		addController(new ExportPDFController(eb, notification));
		addController(new LSUController(eb));
		addController(new NiveauDeMaitriseController());
		addController(new NoteController(eb));
		addController(new RemplacementController());
		addController(new UtilsController());

    }

}
