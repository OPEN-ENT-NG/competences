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
import fr.openent.competences.service.impl.CompetenceRepositoryEvents;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.share.impl.SqlShareService;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.DeliveryOptions;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import java.math.BigInteger;

public class Competences extends BaseServer {

    public static String COMPETENCES_SCHEMA;
    public static String VSCO_SCHEMA;

    public static JsonObject LSUN_CONFIG;
    public static JsonObject TRANSITION_CONFIG;

    public static final String NOTES_TABLE = "notes";
    public static final String ANNOTATIONS = "annotations";
    public static final String COMPETENCES_TABLE = "competences";
    public static final String PERSO_COMPETENCES_TABLE = "perso_competences";
    public static final String PERSO_COMPETENCES_ORDRE_TABLE = "perso_order_item_enseignement";
    public static final String REL_COMPETENCES_DOMAINES_TABLE = "rel_competences_domaines";
    public static final String REL_COMPETENCES_ENSEIGNEMENTS_TABLE = "rel_competences_enseignements";
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
    public static final String SERVICES_TABLE = "services";
    public static final String MODALITES_TABLE = "modalites";
    public static final String REL_DEVOIRS_GROUPES = "rel_devoirs_groupes";
    public static final String COMPETENCES_DEVOIRS = "competences_devoirs";
    public static final String ENSEIGNEMENT_COMPLEMENT = "enseignement_complement";
    public static final String LANGUES_CULTURE_REGIONALE = "langues_culture_regionale";
    public static final String ELEVE_ENSEIGNEMENT_COMPLEMENT = "eleve_enseignement_complement";
    public static final String NIVEAU_ENS_COMPLEMENT = "niveau_ens_complement";
    public static final String DISPENSE_DOMAINE_ELEVE = "dispense_domaine_eleve";
    public static final String COMPETENCE_NIVEAU_FINAL = "competence_niveau_final";
    public static final String MATIERE_TABLE = "matiere";

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
    public static final String SCHEMA_SERVICE = "eval_service";
    public static final String DEVOIR_TABLE = "devoirs";
    public static final String TRANSITION_TABLE = "transition";
    public static final String DEVOIR_SHARE_TABLE = "devoirs_shares";
    public static final String SCHEMA_COMPETENCE_CREATE = "eval_createCompetence";
    public static final String SCHEMA_COMPETENCE_UPDATE = "eval_updateCompetence";
    public static final String SCHEMA_DISPENSEDOMAINE_ELEVE_CREATE = "eval_createDispenseDomaineEleve";
    public static final String SCHEMA_CREATE_COMPETENCE_NIVEAU_FINAL = "eval_createCompetenceNiveauFinal";

    // droits
    public static final String DEVOIR_ACTION_UPDATE = "fr-openent-competences-controllers-DevoirController|updateDevoir";
    public static final String PARAM_COMPETENCE_RIGHT = "competences.paramCompetences";
    public static final String PARAM_SERVICES_RIGHT = "competences.paramServices";
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

    // Clefs usuelles
    public static final String ID_ETABLISSEMENT_KEY = "idEtablissement";
    public static final String ID_ELEVE_KEY = "idEleve";
    public static final String ID_ELEVES_KEY = "idEleves";
    public static final String ID_CLASSE_KEY = "idClasse";
    public static final String ID_CLASSES_KEY = "idClasses";
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
    public static final String FORMATIVE = "formative";
    public static final String ID_PERIODE = "id_periode";
    public static final String ID_MATIERE = "id_matiere";
    public static final String ACTION = "action";
    public static final String POSITIONNEMENT = "positionnement";
    public static final String POSITIONNEMENT_AUTO = "positionnement_auto";
    public static final String POSITIONNEMENTS_AUTO = "positionnements_auto";
    public static final String MOYENNE = "moyenne";
    public static final String MOYENNES = "moyennes";
    public static final String ELEVES = "eleves";
    public static final String APPRECIATION_CLASSE = "appreciation_classe";
    public static final String APPRECIATIONS = "appreciations";
    public static final String NOTES = "notes";
    public static final String HAS_NOTE = "hasNote";
    public static final String NN = "NN";


    @Override
	public void start() throws Exception {
        super.start();

        COMPETENCES_SCHEMA = config.getString("db-schema");
        VSCO_SCHEMA = config.getString("vsco-schema");
        LSUN_CONFIG = config.getJsonObject("lsun");
        TRANSITION_CONFIG = config.getJsonObject("transition");
        DELIVERY_OPTIONS = new DeliveryOptions()
                .setSendTimeout(TRANSITION_CONFIG.getInteger("timeout-transaction") * 1000L);

        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender notification = emailFactory.getSender();

        final EventBus eb = getEventBus(vertx);
        final Storage storage = new StorageFactory(vertx).getStorage();

		addController(new CompetencesController());
        addController(new ServicesController());
        addController(new AnnotationController());
		addController(new AppreciationController());
		addController(new BFCController(eb));
		addController(new CompetenceController(eb));
		addController(new CompetenceNoteController(eb));
		addController(new ModaliteController());
		addController(new ExportBulletinController(eb));


        // devoir controller
        DevoirController devoirController = new DevoirController(eb);
        SqlCrudService devoirSqlCrudService = new SqlCrudService(COMPETENCES_SCHEMA, DEVOIR_TABLE, DEVOIR_SHARE_TABLE, new fr.wseduc.webutils.collections.JsonArray().add("*"), new JsonArray().add("*"), true);
        devoirController.setCrudService(devoirSqlCrudService);
        devoirController.setShareService(new SqlShareService(COMPETENCES_SCHEMA, DEVOIR_SHARE_TABLE, eb, securedActions, null));
        addController(devoirController);
		addController(new DomaineController(eb));
		addController(new EnseignementController(eb));
		addController(new ExportPDFController(eb, notification, storage));
		addController(new LSUController(eb));
		addController(new NiveauDeMaitriseController());
		addController(new NoteController(eb));
		addController(new RemplacementController());
        addController(new ElementProgrammeController());
		addController(new UtilsController(storage));
        addController(new BilanPeriodiqueController(eb));

		addController(new EventBusController());

        setRepositoryEvents(new CompetenceRepositoryEvents(eb));

        addController(new ElementBilanPeriodiqueController(eb));

    }

}
