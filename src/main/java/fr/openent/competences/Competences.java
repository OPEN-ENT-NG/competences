/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences;

import fr.openent.competences.controllers.*;
import fr.openent.competences.service.impl.ArchiveWorker;
import fr.openent.competences.service.impl.BulletinWorker;
import fr.openent.competences.service.impl.CompetenceRepositoryEvents;
import fr.openent.competences.service.impl.CompetencesTransitionWorker;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.impl.SqlShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Competences extends BaseServer {

    public static String COMPETENCES_SCHEMA = "notes";

    public static String VSCO_SCHEMA = "viesco";
    public final static String EVAL_SCHEMA = "notes";

    public static JsonObject LSUN_CONFIG;
    public static JsonObject TRANSITION_CONFIG;
    public static JsonObject NODE_PDF_GENERATOR;

    public static final String ANNOTATIONS = "annotations";
    public static final String APPRECIATION_CLASSE_TABLE = "appreciation_classe";
    public static final String APPRECIATION_CPE_BILAN_PERIODIQUE = "appreciation_cpe_bilan_periodique";
    public static final String APPRECIATION_ELT_BILAN_PERIODIQUE_CLASSE_TABLE = "appreciation_elt_bilan_periodique_classe";
    public static final String APPRECIATION_ELT_BILAN_PERIODIQUE_ELEVE_TABLE = "appreciation_elt_bilan_periodique_eleve";
    public static final String APPRECIATION_MATIERE_PERIODE_TABLE = "appreciation_matiere_periode";
    public static final String APPRECIATIONS_TABLE = "appreciations";
    public static final String AVIS_CONSEIL_DE_CLASSE_TABLE = "avis_conseil_de_classe";
    public static final String AVIS_CONSEIL_ORIENTATION_TABLE ="avis_conseil_orientation";

    public static final String BFC_SYNTHESE_TABLE = "bfc_synthese";
    public static final String BFC_TABLE = "bilan_fin_cycle";

    public static final String BULLETIN_PARAMETERS_TABLE ="bulletin_parameters";

    public static final String COMPETENCE_NIVEAU_FINAL = "competence_niveau_final";
    public static final String COMPETENCE_NIVEAU_FINAL_ANNUEL = "competence_niveau_final_annuel";
    public static final String COMPETENCES_TABLE = "competences";
    public static final String COMPETENCES_DEVOIRS = "competences_devoirs";
    public static final String COMPETENCES_NOTES_TABLE = "competences_notes";
    public static final String CYCLE_TABLE = "cycle";

    public static final String DEVOIR_TABLE = "devoirs";
    public static final String DEVOIR_SHARE_TABLE = "devoirs_shares";
    public static final String DISPENSE_DOMAINE_ELEVE = "dispense_domaine_eleve";
    public static final String DOMAINES_TABLE = "domaines";

    public static final String ELEMENT_PROGRAMME_TABLE = "element_programme";
    public static final String ELEVE_ENSEIGNEMENT_COMPLEMENT = "eleve_enseignement_complement";
    public static final String ELEVES_IGNORES_LSU_TABLE = "eleves_ignores_lsu";
    public static final String ELT_BILAN_PERIODIQUE_TABLE = "elt_bilan_periodique";
    public static final String ENSEIGNEMENT_COMPLEMENT = "enseignement_complement";
    public static final String ENSEIGNEMENTS_TABLE = "enseignements";


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
    public static final String REL_DEVOIRS_GROUPES = "rel_devoirs_groupes";
    public static final String REL_GROUPE_APPRECIATION_ELT_ELEVE_TABLE = "rel_groupe_appreciation_elt_eleve";
    public static final String REL_ELT_BILAN_PERIODIQUE_GROUPE_TABLE = "rel_elt_bilan_periodique_groupe";
    public static final String REL_ELT_BILAN_PERIODIQUE_INTERVENANT_MATIERE_TABLE = "rel_elt_bilan_periodique_intervenant_matiere";
    public static final String REL_PROFESSEURS_REMPLACANTS_TABLE = "rel_professeurs_remplacants";
    public static final String REL_APPRECIATION_USERS_NEO = "rel_appreciations_users_neo";


    public static final String STSFILE_TABLE = "sts_file";
    public static final String SYNTHESE_BILAN_PERIODIQUE_TABLE = "synthese_bilan_periodique";

    public static final String THEMATIQUE_BILAN_PERIODIQUE_TABLE = "thematique_bilan_periodique";
    public static final String TRANSITION_TABLE = "transition";

    public static final String USERS = "users";

    public static final String USE_PERSO_NIVEAU_COMPETENCES_TABLE = "use_perso";

    public static final String VSCO_ABSENCES_ET_RETARDS = "absences_et_retards";
    public static final String VSCO_PERIODE = "periode";
    public final static String VSCO_MATIERE_LIBELLE_TABLE = "subject_libelle";
    public final static String VSCO_MODEL_MATIERE_LIBELLE_TABLE = "model_subject_libelle";
    public final static String VSCO_MATIERE_TABLE = "matiere";
    public final static String VSCO_SOUS_MATIERE_TABLE = "sousmatiere";
    public final static String VSCO_SERVICES_TABLE = "services";

    public static final String SCHEMA_ANNOTATION_UPDATE = "eval_updateAnnotation";
    public static final String SCHEMA_APPRECIATIONS_CREATE = "eval_createAppreciation";
    public static final String SCHEMA_APPRECIATIONS_UPDATE = "eval_updateAppreciation";
    public static final String SCHEMA_APPRECIATION_ELEVE_CREATE = "eval_createAppreciationEleve";
    public static final String SCHEMA_APPRECIATION_CLASSE_CREATE = "eval_createAppreciationClasse";
    public static final String SCHEMA_THEMATIQUE_BILAN_PERIODIQUE = "eval_createThematique_bilan_periodique";
    public static final String SCHEMA_EPI_BILAN_PERIODIQUE = "eval_createEpiBilanPeriodique";
    public static final String SCHEMA_AP_BILAN_PERIODIQUE = "eval_createApBilanPeriodique";
    public static final String SCHEMA_PARCOURS_BILAN_PERIODIQUE = "eval_createParcoursBilanPeriodique";
    public static final String SCHEMA_SYNTHESE_CREATE = "eval_createSynthese";
    public static final String SCHEMA_APPRECIATION_CPE_CREATE = "eval_createAppreciationCPE";
    public static final String SCHEMA_AVIS_CONSEIL_BILAN_PERIODIQUE = "eval_createAvisConseil";
    public static final String SCHEMA_AVIS_ORIENTATION_BILAN_PERIODIQUE = "eval_createAvisOrientation";
    public static final String SCHEMA_CREATE_OPINION = "eval_createOpinion";


    public static final String SCHEMA_APPRECIATIONS_CLASSE = "eval_createOrUpdateAppreciationClasse";

    public static final String SCHEMA_BFC_CREATE = "eval_createBFC";
    public static final String SCHEMA_BFC_UPDATE = "eval_updateBFC";
    public static final String SCHEMA_BFCSYNTHESE_CREATE = "eval_createBfcSynthese";
    public static final String SCHEMA_NIVEAUENSCPL_CREATE = "eval_createNiveauEnsCpl";
    public static final String SCHEMA_COMPETENCE_NOTE_CREATE = "eval_createCompetenceNote";
    public static final String SCHEMA_COMPETENCE_NOTE_UPDATE = "eval_updateCompetenceNote";
    public static final String SCHEMA_DEVOIRS_UPDATE = "eval_updateDevoir";
    public static final String SCHEMA_DEVOIRS_CREATE = "eval_createDevoir";
    public static final String SCHEMA_MAITRISE_CREATE = "eval_createMaitrise";
    public static final String SCHEMA_REL_PROFESSEURS_REMPLACANTS_CREATE = "eval_createRel_professeurs_remplacants";
    public static final String SCHEMA_NOTES_CREATE = "eval_createNote";
    public static final String SCHEMA_NOTES_UPDATE = "eval_updateNote";
    public static final String SCHEMA_USE_PERSO_NIVEAU_COMPETENCE = "eval_usePersoNiveauCompetence";
    public static final String SCHEMA_MAITRISE_UPDATE = "eval_updateMaitrise";
    public static final String SCHEMA_COMPETENCE_CREATE = "eval_createCompetence";
    public static final String SCHEMA_COMPETENCE_UPDATE = "eval_updateCompetence";
    public static final String SCHEMA_DISPENSEDOMAINE_ELEVE_CREATE = "eval_createDispenseDomaineEleve";
    public static final String SCHEMA_CREATE_COMPETENCE_NIVEAU_FINAL = "eval_createCompetenceNiveauFinal";
    public static final String SCHEMA_CREATE_STSFILE = "eval_createSTSFile";

    // droits
    public static final String DEVOIR_ACTION_UPDATE = "fr-openent-competences-controllers-DevoirController|updateDevoir";
    public static final String PARAM_COMPETENCE_RIGHT = "competences.paramCompetences";
    public static final String PARAM_SERVICES_RIGHT = "viesco.paramServices";
    public static final String CAN_UPDATE_BFC_SYNTHESE_RIGHT = "competences.canUpdateBFCSynthese";
    public static final String PARAM_LINK_GROUP_CYCLE_RIGHT = "competences.paramLinkGroupCycle";
    public static final String CAN_UPDATE_RETARDS_AND_ABSENCES = "competences.canUpdateRetardsAndAbsences";
    public static final String CAN_ACCESS_EXPORT_BULLETIN = "export.bulletins.periodique";

    // Constantes
    public static final Integer MAX_NBR_COMPETENCE = 12;
    public static final String VIESCO_BUS_ADDRESS = "viescolaire";
    public static final Number BFC_AVERAGE_VISIBILITY_NONE = 0;
    public static final Number BFC_AVERAGE_VISIBILITY_FOR_ADMIN_ONLY = 1;
    public static final Number BFC_AVERAGE_VISIBILITY_FOR_ALL = 2;
    public static  DeliveryOptions DELIVERY_OPTIONS ;

    //LSU
    public static final Integer POSITIONNEMENT_ZERO = 0;
    public static final String LIEN_PERE = "PERE";
    public static final String LIEN_MERE = "MERE";
    public static final String LIEN_TUTEUR = "TUTEUR";
    public static final String LIEN_FAMILLE = "AUTRE MEMBRE DE LA FAMILLE";
    public static final String LIEN_SOCIALE = "AIDE SOCIALE A L'ENFANT";
    public static final String LIEN_AUTRE = "AUTRE LIEN";
    public static final String LIEN_ELEVE = "ELEVE LUI-MEME";
    public static final String LIEN_FRATRIE =  "FRATRIE";
    public static final String LIEN_ASCENDANT = "ASCENDANT";
    public static final String LIEN_EDUCATEUR = "EDUCATEUR";
    public static final String LIEN_ASSISTANT_FAMILIAL =  "ASSISTANT FAMILIAL";
    public static final String LIEN_GARDE_ENFANT = "GARDE d'ENFANT";
    public static final String ELEMENT_PROGRAMME_DEFAULT = "Aucun élément travaillé pour cette matière.";

    // Clefs usuelles
    public static final String ID_ETABLISSEMENT_KEY = "idEtablissement";
    public static final String ID_ELEVE_KEY = "idEleve";
    public static final String ID_ELEVES_KEY = "idEleves";
    public static final String ID_CLASSE_KEY = "idClasse";
    public static final String ID_CLASSES_KEY = "idClasses";
    public static final String ID_STRUCTURES_KEY = "idStructures";
    public static final String TYPE_CLASSE_KEY = "typeClasse";
    public static final String ID_PERIODE_KEY = "idPeriode";
    public static final String ID_MATIERE_KEY = "idMatiere";
    public static final String CLASSE_NAME_KEY = "classeName";
    public static final String LAST_NAME_KEY = "lastName";
    public static final String FIRST_NAME_KEY = "firstName";
    public static final String DISPLAY_NAME_KEY = "displayName";
    public static final String COMPETENCES_NOTES_KEY = "competencesNotes";
    public static final String ELEMENT_PROGRAMME_KEY = "elementProgramme";
    public static final String LEVEL = "level";

    public static final String NAME = "name";
    public static final String ID_ELEVE = "id_eleve";
    public static final String ID_CLASSE = "id_classe";
    public static final String FORMATIVE = "formative";
    public static final String ID_PERIODE = "id_periode";
    public static final String ID_MATIERE = "id_matiere";
    public static final String OWNER = "owner";
    public static final String ACTION = "action";
    public static final String POSITIONNEMENT = "positionnement";
    public static final String POSITIONNEMENT_AUTO = "positionnement_auto";
    public static final String POSITIONNEMENTS_AUTO = "positionnements_auto";
    public static final String MOYENNE = "moyenne";
    public static final String MOYENNE_NON_NOTE = "moyenne_non_note";
    public static final String MOYENNES = "moyennes";
    public static final String ELEVES = "eleves";
    public static final String APPRECIATION_CLASSE = "appreciation_classe";
    public static final String APPRECIATIONS = "appreciations";
    public static final String APPRECIATIONS_ELEVE = "appreciations_eleve";
    public static final String NOTES = "notes";
    public static final String HAS_NOTE = "hasNote";
    public static final String NN = "NN";
    public static final String ID_KEY = "id";
    public static final String ID_STRUCTURE_KEY = "idStructure";
    public static final String ID_STUDENTS_KEY = "idStudents";
    public static final String EXTERNAL_ID_SUBJECT = "external_id_subject";
    public static final String EXTERNAL_ID_KEY = "externalId";
    public static final String TITLE =  "title";
    public static final String LIBELLE =  "libelle";
    public static final String CODE = "code";
    public static final String SUBJECTS = "subjects";
    public static final String STATUS = "status";
    public static final String RESULTS = "results";
    public static final String RESULT = "result";
    public static final String OK = "ok";
    public static final String MESSAGE = "message";
    public static final String LIBELLE_MATIERE = "libelleMatiere";
    public static final String BLANK_SPACE = " ";
    public static final String UNDERSCORE = "_";
    public static final String ORDRE = "ordre";
    public static final String ID_PARENT_KEY = "idParent";
    public static  String TEMPLATE_PATH;

    @Override
    public void start() throws Exception {
        super.start();

        COMPETENCES_SCHEMA = config.getString("db-schema");
        VSCO_SCHEMA = config.getString("vsco-schema");
        LSUN_CONFIG = config.getJsonObject("lsun");
        TRANSITION_CONFIG = config.getJsonObject("transition");
        DELIVERY_OPTIONS = new DeliveryOptions()
                .setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L);
        NODE_PDF_GENERATOR = config.getJsonObject("node-pdf-generator");
        TEMPLATE_PATH =  FileResolver.absolutePath(config.getJsonObject("exports")
                .getString("template-path")).toString();
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender notification = emailFactory.getSender();

        final EventBus eb = getEventBus(vertx);
        final Storage storage = new StorageFactory(vertx).getStorage();

        // Controller
        addController(new CompetencesController());
        addController(new AnnotationController());
        addController(new AppreciationController());
        addController(new BFCController(eb, storage));
        addController(new CompetenceController(eb));
        addController(new CompetenceNoteController(eb));
        addController(new ModaliteController());
        addController(new ExportBulletinController(eb,storage));
        addController(new DomaineController(eb));
        addController(new EnseignementController(eb));
        addController(new ExportPDFController(eb, notification, storage));
        addController(new LSUController(eb));
        addController(new NiveauDeMaitriseController());
        addController(new NoteController(eb));
        addController(new DevoirRemplacementController());
        addController(new ElementProgrammeController());
        addController(new UtilsController(storage,eb));
        addController(new BilanPeriodiqueController(eb));
        addController(new MatiereController(eb));
        addController(new ElementBilanPeriodiqueController(eb));
        addController(new ReportModelPrintExportController());
        addController(new YearTransitionController());
        addController(new AppreciationSubjectPeriodController(eb));
        // Devoir Controller
        DevoirController devoirController = new DevoirController(eb);
        SqlCrudService devoirSqlCrudService = new SqlCrudService(COMPETENCES_SCHEMA, DEVOIR_TABLE, DEVOIR_SHARE_TABLE,
                new fr.wseduc.webutils.collections.JsonArray().add("*"), new JsonArray().add("*"), true);
        devoirController.setCrudService(devoirSqlCrudService);
        devoirController.setShareService(new SqlShareService(COMPETENCES_SCHEMA, DEVOIR_SHARE_TABLE, eb, securedActions,
                null));
        addController(devoirController);

        EventBusController eventBusController = new EventBusController(securedActions);
        SqlCrudService eventBusSqlCrudService = new SqlCrudService(COMPETENCES_SCHEMA, DEVOIR_TABLE, DEVOIR_SHARE_TABLE,
                new fr.wseduc.webutils.collections.JsonArray().add("*"), new JsonArray().add("*"), true);
        eventBusController.setCrudService(eventBusSqlCrudService);
        eventBusController.setShareService(new SqlShareService(COMPETENCES_SCHEMA, DEVOIR_SHARE_TABLE, eb, securedActions,
                null));
        addController(eventBusController);

        // Repository Events
        setRepositoryEvents(new CompetenceRepositoryEvents(eb));

        // Worker
        log.info("WORKER : " + ArchiveWorker.class.getSimpleName());
        vertx.deployVerticle(ArchiveWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        log.info("WORKER : " + CompetencesTransitionWorker.class.getSimpleName());
        vertx.deployVerticle(CompetencesTransitionWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        log.info("WORKER : " + BulletinWorker.class.getSimpleName());
        vertx.deployVerticle(BulletinWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
    }

    public static void launchTransitionWorker(EventBus eb, JsonObject params, boolean isHTTP) {
        eb.send(CompetencesTransitionWorker.class.getSimpleName(), params.put("isHTTP", isHTTP),
                new DeliveryOptions().setSendTimeout(1000 * 1000L), handlerToAsyncHandler(eventExport -> {
                            if(!eventExport.body().getString("status").equals("ok"))
                                launchTransitionWorker(eb, params, isHTTP);
                            log.info("Ok calling worker " + eventExport.body().toString());
                        }
                ));
    }

}
