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

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.Eleve;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.FormateFutureEvent;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.DecimalFormat;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.USE_MODEL_KEY;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class ExportPDFController extends ControllerHelper {
    private String assetsPath = "../..";
    private Map<String, String> skins = new HashMap<String, String>();
    protected static final Logger log = LoggerFactory.getLogger(ExportPDFController.class);


    /**
     * Déclaration des services
     */
    private DevoirService devoirService;
    private UtilsService utilsService;
    private BFCService bfcService;
    private DomainesService domaineService;
    private CompetenceNoteService competenceNoteService;
    private NoteService noteService;
    private CompetencesService competencesService;
    private NiveauDeMaitriseService niveauDeMaitriseService;
    private ExportService exportService;
    private BfcSyntheseService bfcSynthseService;
    private EleveEnseignementComplementService eleveEnseignementComplementService;
    private DispenseDomaineEleveService dispenseDomaineEleveService;
    private AppreciationService appreciationService;
    private ExportBulletinService exportBulletinService;
    private final Storage storage;

    public ExportPDFController(EventBus eb, EmailSender notification, Storage storage) {
        devoirService = new DefaultDevoirService(eb);
        utilsService = new DefaultUtilsService(eb);
        bfcService = new DefaultBFCService(eb);
        domaineService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb);
        competencesService = new DefaultCompetencesService(eb);
        niveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        exportService = new DefaultExportService(eb, storage);
        exportBulletinService = new DefaultExportBulletinService(eb, storage);
        bfcSynthseService = new DefaultBfcSyntheseService(Competences.COMPETENCES_SCHEMA, Competences.BFC_SYNTHESE_TABLE, eb);
        eleveEnseignementComplementService = new DefaultEleveEnseignementComplementService(Competences.COMPETENCES_SCHEMA, Competences.ELEVE_ENSEIGNEMENT_COMPLEMENT);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(Competences.COMPETENCES_SCHEMA,Competences.DISPENSE_DOMAINE_ELEVE);
        appreciationService = new DefaultAppreciationService(Competences.COMPETENCES_SCHEMA, Competences.APPRECIATIONS_TABLE);
        this.storage = storage;
    }

    /**
     * Récupère le nom des enseignants de chacune des matières puis positionne
     * les devoirs de l'élève sur les bonnes matières et enfin génère le PDF associé
     * formant le relevé de notes de l'élève.
     *
     * @param request
     * @param user        l'utilisateur connecté.
     * @param matieres    la liste des matières de l'élève.
     * @param classe
     * @param idUsers
     * @param devoirsJson la liste des devoirs et notes de l'élève.
     * @param periodeJson la periode
     * @param userJson    l'élève
     * @param etabJson    l'établissement
     */
    public void getEnseignantsMatieres(final HttpServerRequest request, final UserInfos user, final JsonArray matieres,
                                       final String classe, JsonArray idUsers, final JsonArray devoirsJson,
                                       final JsonObject periodeJson, final JsonObject userJson, final JsonObject etabJson) {

        JsonObject action = new JsonObject()
                .put("action", "eleve.getUsers")
                .put("idUsers", idUsers);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                JsonArray matieresArray = new fr.wseduc.webutils.collections.JsonArray();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray r = body.getJsonArray("results");

                    for (int index = 0; index < matieres.size(); index++) {
                        JsonObject matiereDevoir = matieres.getJsonObject(index);
                        matieresArray.add(matiereDevoir.getJsonObject("data").getJsonObject("data"));
                    }

                    for (int i = 0; i < devoirsJson.size(); i++) {
                        JsonObject devoir = devoirsJson.getJsonObject(i);
                        // Récupération de l'enseignant du devoir
                        JsonObject enseignantDevoir = null;
                        for (int j = 0; j < r.size(); j++) {
                            enseignantDevoir = r.getJsonObject(j);
                            if (enseignantDevoir.getString("id").equals(devoir.getString("owner"))) {
                                break;
                            }
                        }
                        if (enseignantDevoir != null) {
                            // Récupération de la matière
                            for (int k = 0; k < matieresArray.size(); k++) {
                                JsonObject matiereDevoir = matieresArray.getJsonObject(k);
                                getDevoirsByMatiere(devoirsJson, matiereDevoir);

                                if (matiereDevoir.getString("id").equals(devoir.getString("id_matiere"))) {
                                    String firstNameEnsiegnant = enseignantDevoir.getString("firstName");
                                    String displayName = firstNameEnsiegnant.substring(0, 1) + ".";
                                    displayName = displayName + enseignantDevoir.getString("name");

                                    if (matiereDevoir.getJsonArray("displayNameEnseignant") == null) {
                                        matiereDevoir.put("displayNameEnseignant", new fr.wseduc.webutils.collections.JsonArray().add(displayName));
                                    } else {
                                        JsonArray _enseignantMatiere = matiereDevoir.getJsonArray("displayNameEnseignant");
                                        if (!_enseignantMatiere.contains(displayName)) {
                                            _enseignantMatiere.add(displayName);
                                            matiereDevoir.put("displayNameEnseignant", _enseignantMatiere);
                                        }
                                    }
                                }
                            }
                        }
                    }


                    final JsonObject templateProps = new JsonObject();

                    templateProps.put("matieres", matieresArray);
                    templateProps.put("periode", periodeJson);
                    templateProps.put("user", userJson.getJsonObject("u").getJsonObject("data"));
                    templateProps.put("classe", userJson.getJsonObject("c").getJsonObject("data"));
                    templateProps.put("etablissement", etabJson);
                    String templateName = "releve-eleve.pdf.xhtml";

                    String prefixPdfName = "releve-eleve";
                    prefixPdfName += "-" + userJson.getJsonObject("u").getJsonObject("data").getString("displayName");
                    prefixPdfName += "-" + userJson.getJsonObject("c").getJsonObject("data").getString("name");

                    String etablissementName = etabJson.getString("name");
                    etablissementName = etablissementName.trim().replaceAll(" ", "-");
                    prefixPdfName += "-" + etablissementName;

                    exportService.genererPdf(request, templateProps, templateName, prefixPdfName, vertx, config);

                } else {
                    leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                }
            }
        }));
    }

    /**
     * Récupère les devoirs de la matière et les positionnent sur celle ci.
     *
     * @param devoirsJson  la liste de tous les devoirs de l'élève.
     * @param matiereInter la matière dont on cherche les devoirs.
     */
    private void getDevoirsByMatiere(JsonArray devoirsJson, JsonObject matiereInter) {

        JsonArray devoirsMatiereJson = new fr.wseduc.webutils.collections.JsonArray();

        List<NoteDevoir> listeNoteDevoirs = new ArrayList<NoteDevoir>();

        // parcours des devoirs
        for (int i = 0; i < devoirsJson.size(); i++) {
            JsonObject devoirJson = devoirsJson.getJsonObject(i);
            Boolean hasCoeff = devoirJson.getString("coefficient") != null;
            Double coefficient = null;
            if(hasCoeff){
                hasCoeff = !Double.valueOf(devoirJson.getString("coefficient")).equals(new Double(1));
                coefficient = Double.valueOf(devoirJson.getString("coefficient"));
            }
            // boolean permettant de savoir s'il y a un coefficient différent de 1 sur la note
            devoirJson.put("hasCoeff", hasCoeff);

            // ajout du devoir sur la matiere, si son identifiant de matière correspond bien
            if ( coefficient!=null
                    && matiereInter.getString("id").equals(devoirJson.getString("id_matiere"))) {
                devoirsMatiereJson.add(devoirJson);
                Double note = Double.valueOf(devoirJson.getString("note"));
                Double diviseur = Double.valueOf(devoirJson.getInteger("diviseur"));
                Boolean ramenerSur = devoirJson.getBoolean("ramener_sur");
                NoteDevoir noteDevoir = new NoteDevoir(note, diviseur, ramenerSur, coefficient);
                listeNoteDevoirs.add(noteDevoir);
            }
        }
        matiereInter.put("devoirs", devoirsMatiereJson);

        boolean hasDevoirs = !listeNoteDevoirs.isEmpty();
        matiereInter.put("hasDevoirs", hasDevoirs);

        if (hasDevoirs) {
            // calcul de la moyenne de l'eleve pour la matiere
            JsonObject moyenneMatiere = utilsService.calculMoyenne(listeNoteDevoirs, false, 20);// TODO recuper le diviseur de la matiere
            // ajout sur l'objet json
            if( moyenneMatiere.getLong("moyenne") != null){
                matiereInter.put("moyenne", moyenneMatiere.getLong("moyenne").toString());
            }
        }
    }

    /**
     * Genere le releve d'un eleve sous forme de PDF
     */
    @Get("/releve/pdf")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getReleveEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {

                    // parametres de l'url
                    final MultiMap params = request.params();

                    final Long idPeriode;
                    if (params.get("idPeriode") != null) {
                        try {
                            idPeriode = Long.parseLong(params.get("idPeriode"));
                        } catch (NumberFormatException e) {
                            log.error("Error : idPeriode must be a long object", e);
                            badRequest(request, e.getMessage());
                            return;
                        }
                    } else {
                        idPeriode = null;
                    }

                    final String idEtablissement = params.get("idEtablissement");
                    final String idUser = params.get("idUser");


                    // TODO verifier que l'utilisateur connecte est bien l'eleve dont essaie d'acceder au releve ou que
                    // le parent connecte essaie bien d'acceder au releve d'un de ses eleves

                    // récupération de l'élève
                    utilsService.getInfoEleve(idUser, new Handler<Either<String, JsonObject>>() {

                        @Override
                        public void handle(Either<String, JsonObject> eventUser) {
                            if (eventUser.isRight()) {
                                final JsonObject userJSON = eventUser.right().getValue();

                                final String classeEleve = userJSON.getJsonObject("u").getJsonObject("data")
                                        .getJsonArray("classes").getString(0);
                                final String idClasse = userJSON.getJsonObject("c").getJsonObject("data")
                                        .getString("id");

                                // Récupération de la liste des devoirs de la personne avec ses notes associées
                                devoirService.listDevoirs(idUser, idEtablissement, idClasse, null,
                                        idPeriode,false, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(final Either<String, JsonArray> eventListDevoirs) {
                                        if (eventListDevoirs.isRight()) {

                                            // devoirs de l'eleve (avec ses notes) sous forme d'objet JSON
                                            final JsonArray devoirsJSON = eventListDevoirs.right().getValue();
                                            final JsonArray idMatieres = new fr.wseduc.webutils.collections.JsonArray();
                                            final JsonArray idEnseignants = new fr.wseduc.webutils.collections.JsonArray();
                                            for (int i = 0; i < devoirsJSON.size(); i++) {
                                                JsonObject devoir = devoirsJSON.getJsonObject(i);
                                                idMatieres.add(devoir.getValue("id_matiere"));
                                                idEnseignants.add(devoir.getValue("owner"));
                                            }
                                            // récupération de l'ensemble des matières de l'élève

                                            JsonObject action = new JsonObject()
                                                    .put("action", "matiere.getMatieres")
                                                    .put("idMatieres", idMatieres);

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    JsonObject body = message.body();

                                                    if ("ok".equals(body.getString("status"))) {
                                                        JsonArray r = body.getJsonArray("results");
                                                        final ArrayList<String> classesFieldOfStudy = new ArrayList<String>();
                                                        String key = new String();
                                                        JsonObject f = new JsonObject();
                                                        final JsonArray matieres = r;

                                                        for (int i = 0; i < r.size(); i++) {
                                                            JsonObject o = r.getJsonObject(i);
                                                            key = classeEleve + "$" + o.getString("externalId");
                                                            classesFieldOfStudy.add(key);
                                                        }


                                                        // recuperation etablissement
                                                        utilsService.getStructure(idEtablissement, new Handler<Either<String, JsonObject>>() {

                                                            @Override
                                                            public void handle(Either<String, JsonObject> eventStructure) {
                                                                if (eventStructure.isRight()) {
                                                                    final JsonObject etabJSON = eventStructure.right().getValue().getJsonObject("s").getJsonObject("data");
                                                                    final JsonObject periodeJSON = new JsonObject();

                                                                    if (null != params.get("idTypePeriode")
                                                                            && null != params.get("ordrePeriode")) {
                                                                        final Long idTypePeriode =
                                                                                Long.parseLong(params.get("idTypePeriode"));
                                                                        final Long ordrePeriode =
                                                                                Long.parseLong(params.get("ordrePeriode"));
                                                                        StringBuilder keyI18nPeriodeType =
                                                                                new StringBuilder()
                                                                                        .append("viescolaire.periode.")
                                                                                        .append(idTypePeriode);
                                                                        String libellePeriode = I18n.getInstance()
                                                                                .translate(keyI18nPeriodeType.toString(),
                                                                                        getHost(request),
                                                                                        I18n.acceptLanguage(request));
                                                                        libellePeriode += (" " + ordrePeriode);
                                                                        periodeJSON.put("libelle", libellePeriode);
                                                                    } else {
                                                                        // Construction de la période année
                                                                        periodeJSON.put("libelle", "Ann\u00E9e");
                                                                    }
                                                                    getEnseignantsMatieres(request, user, matieres,
                                                                            classeEleve, idEnseignants, devoirsJSON,
                                                                            periodeJSON, userJSON, etabJSON);
                                                                }
                                                            }

                                                        }); // fin getPeriode


                                                    } else {
                                                        leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                    }
                                                }
                                            }));

                                        } else {
                                            leftToResponse(request, eventListDevoirs.left());
                                        }

                                    } // fin handle listDevoirs
                                }); // fin lisDevoirs
                            }
                        }
                    }); // fin récupération élève
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne un JsonArray contenant les JsonObject de chaque Eleve passe en parametre, seulement si tous les Eleves
     * sont prets.
     * Les Eleves sont pret lorsque la fonction <code>Eleve.isReady()</code> renvoit true. Dans le cas
     * contraire, la fonction s'arrete en retournant null.
     *
     * @param classe La liste des Eleves de la classe.
     * @return Un JsonArray contenant les JsonObject de tous les Eleves de la classe; null si un
     * Eleve n'est pas pret.
     * @see Eleve
     */
    private JsonArray formatBFC(List<Eleve> classe) {
        JsonArray result = new fr.wseduc.webutils.collections.JsonArray();

        for (Eleve eleve : classe) {
            if (!eleve.isReady()) {
                return null;
            }
            result.add(eleve.toJson());
        }

        return result;
    }

    /**
     * Ajoute le JsonObject a <code>collection</code>, et si les JsonObject de toutes les classes ont ete
     * renseignees dans <code>collection</code>, assemble tous JsonObject au sein d'un JsonArray qui sera fournit
     * au handler.
     *
     * @param key        Identifiant de la classe a ajoute dans <code>collection</code>.
     * @param value      JsonObject de la classe a ajoute dans <code>collection</code>.
     * @param collection Map des JsonObject de toutes les classes, indexant par leur identifiant.
     * @param handler    Handler manipulant le JsonArray lorsque celui-ci est assemble.
     */
    private void collectBFCEleve(String key, JsonObject value, Map<String, JsonObject> collection, Handler<Either<String, JsonArray>> handler) {
        if (!collection.values().contains(null)) {
            return;
        } else {
            collection.put(key, value);
        }

        // La collection est initilisee avec les identifiants de toutes les classes, associes avec une valeur null.
        // Ainsi, s'il demeure un null dans la collection, c'est que toutes les classes ne possede pas encore
        // leur JsonObject.
        if (!collection.values().contains(null)) {
            JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
            for (JsonObject classe : collection.values()) {
                result.add(classe);
            }
            handler.handle(new Either.Right<String, JsonArray>(result));
        }
    }

    /**
     * Recupere pour chaque classe l'echelle de conversion des moyennes, les domaines racines a evaluer ainsi que les
     * notes des eleves.
     * Appelle les 3 services simultanement pour chaque classe, ajoutant l'information renvoyee par le service a chaque
     * Eleve de la classe, puis appelle {@link #collectBFCEleve(String, JsonObject, Map, Handler) collectBFCEleve} avec son propre
     * handler afin de renseigner une reponse si la classe est prete a etre exporter.
     *
     * @param classes     L'ensemble des Eleves, rassembles par classe et indexant en fonction de l'identifiant de la
     *                    classe
     * @param idStructure L'identifiant de la structure. Necessaire afin de recuperer l'echelle de conversion.0
     * @param idPeriode   L'identifiant de la periode pour laquelle on souhaite recuperer le BFC.
     * @param idCycle     L'identifiant du cycle pour lequel on souhaite recuperer le BFC.
     * @param handler     Handler contenant le BFC final.
     * @see Eleve
     */
    private void getBFCParClasse(final Map<String, List<Eleve>> classes, final String idStructure, Long idPeriode, Long idCycle, final Handler<Either<String, JsonArray>> handler) {

        // Contient toutes les classes sous forme JsonObject, indexant en fontion de l'identifiant de la classe
        // correspondante.
        final Map<String, JsonObject> result = new LinkedHashMap<>();

        // La map result avec les identifiants des classes, contenus dans "classes", afin de s'assurer qu'aucune ne
        // manque.
        for (String s : classes.keySet()) {
            result.put(s, null);
        }

        for (final Map.Entry<String, List<Eleve>> classe : classes.entrySet()) {

            final Map<Integer, String> libelleEchelle = new HashMap<>();
            final Map<String, Map<Long, Integer>> resultatsEleves = new HashMap<>();

            final List<String> idEleves = new ArrayList<>();

            // La liste des identifiants des Eleves de la classe est necessaire pour "buildBFC"
            for (Eleve e : classe.getValue()) {
                idEleves.add(e.getIdEleve());
            }

            competenceNoteService.getConversionNoteCompetence(idStructure, classe.getKey(), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        if (event.right().getValue().size() == 0) {
                            collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation de l'échelle de conversion pour la classe " + classe.getValue().get(0).getNomClasse() + " : aucune echelle de conversion pour cette classe."), result, handler);
                            log.error("getBFC : getConversionNoteCompetence (" + idStructure + ", " + classe.getKey() + ") : aucune echelle de conversion pour cette classe.");
                        }
                        for (int i = 0; i < event.right().getValue().size(); i++) {
                            JsonObject _o = event.right().getValue().getJsonObject(i);
                            libelleEchelle.put(_o.getInteger("ordre"), _o.getString("libelle"));
                        }
                        for (Eleve e : classe.getValue()) {
                            e.setLibelleNiveau(libelleEchelle);
                        }
                        JsonArray classeResult = formatBFC(classe.getValue());
                        if (classeResult != null) { // classeResult est différent de null si tous les élèves de la classe ont tous les paramètres
                            collectBFCEleve(classe.getKey(), new JsonObject().put("eleves", classeResult), result, handler);
                        }
                    } else {
                        collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation de l'échelle de conversion pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                        log.error("getBFC : getConversionNoteCompetence (" + idStructure + ", " + classe.getKey() + ") : " + event.left().getValue());
                    }
                }
            });

            domaineService.getDomainesRacines(classe.getKey(), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        if (event.right().getValue().size() == 0) {
                            collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation des domaines pour la classe " + classe.getValue().get(0).getNomClasse() + " : aucun domaine racine pour cette classe."), result, handler);
                            log.error("getBFC : getDomainesRacines (" + classe.getKey() + ") : aucun domaine racine pour cette classe.");
                        }
                        final JsonArray queryResult = event.right().getValue();
                        //On récupère les domaines dispensés pour tous les élèves de la classe
                        dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(idEleves, new Handler<Either<String, Map<String, Map<Long, Boolean>>>>() {
                            @Override
                            public void handle(Either<String, Map<String, Map<Long, Boolean>>> respMap) {
                                if (respMap.isRight()) {
                                    //Map<IdEleve,Map<idDomaine,dispense>>
                                    Map<String, Map<Long, Boolean>> mapIdsElevesIdsDomainesDispenses = respMap.right().getValue();

                                    for (final Eleve e : classe.getValue()) {
                                        final Map<Long, Map<String, String>> domainesRacines = new LinkedHashMap<>();
                                        for (int i = 0; i < queryResult.size(); i++) {
                                            final JsonObject domaine = queryResult.getJsonObject(i);
                                            final Map<String, String> infoDomaine = new HashMap<>();
                                            infoDomaine.put("id", String.valueOf(domaine.getLong("id")));
                                            infoDomaine.put("codification", domaine.getString("codification"));
                                            infoDomaine.put("libelle", domaine.getString("libelle"));
                                            //On vérifie si l'id de l'élève en cours est ds la map des élève qui sont dispensés pour un domaine
                                            if (mapIdsElevesIdsDomainesDispenses.containsKey(e.getIdEleve())) {
                                                Map<Long, Boolean> idsDomainesDomaine = mapIdsElevesIdsDomainesDispenses.get(e.getIdEleve());
                                                if (idsDomainesDomaine.containsKey(Long.valueOf(infoDomaine.get("id")))) {
                                                    infoDomaine.put("dispense", String.valueOf(idsDomainesDomaine.get(Long.valueOf(infoDomaine.get("id")))));
                                                }
                                            }
                                            domainesRacines.put(Long.valueOf(infoDomaine.get("id")), infoDomaine);
                                        }
                                        e.setDomainesRacines(domainesRacines);
                                    }
                                }else {
                                    collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation des domaines dispensés pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + respMap.left().getValue()), result, handler);
                                    log.error("getBFC : mapOfDispenseDomaineByIdEleve (" + classe.getKey() + ") : " + respMap.left().getValue());
                                }
                            }
                        });

                        JsonArray classeResult = formatBFC(classe.getValue());
                        if (classeResult != null) {
                            collectBFCEleve(classe.getKey(), new JsonObject().put("eleves", classeResult), result, handler);
                        }
                    } else {
                        collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation des domaines pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                        log.error("getBFC : getDomainesRacines (" + classe.getKey() + ") : " + event.left().getValue());
                    }
                }
            });



            bfcService.buildBFC(false, idEleves.toArray(new String[0]), classe.getKey(), idStructure, idPeriode, idCycle, new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(final Either<String, JsonObject> event) {
                    if (event.isRight()) {

                        eleveEnseignementComplementService.listNiveauCplByEleves(idEleves.toArray(new String[1]), new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle (final Either <String, JsonArray> eventNCPL){
                                if (eventNCPL.isRight()) {

                                    bfcSynthseService.getBfcSyntheseByIdsEleveAndClasse(idEleves.toArray(new String[1]), classe.getKey(), new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> repSynthese) {
                                            if (repSynthese.isRight()) {
                                                Map<String, Map<String, Long>> niveauEnseignementComplementEleve = new HashMap<>();
                                                Map<String, String> syntheseEleve = new HashMap<>();
                                                // On récupère les enseignements de complément par élève
                                                JsonArray niveauEnseignementComplementEleveResultArray = eventNCPL.right().getValue();
                                                for (int i = 0; i < niveauEnseignementComplementEleveResultArray.size(); i++) {
                                                    JsonObject _o = niveauEnseignementComplementEleveResultArray.getJsonObject(i);
                                                    String id_eleve = _o.getString("id_eleve");

                                                    if (!niveauEnseignementComplementEleve.containsKey(id_eleve)) {
                                                        niveauEnseignementComplementEleve.put(id_eleve, new HashMap<String, Long>());
                                                    }
                                                    niveauEnseignementComplementEleve.get(id_eleve).put(_o.getString("libelle"), _o.getLong("niveau"));
                                                }
                                                // On récupère les synthèses des bfcs par cycle par élève
                                                JsonArray syntheseEleveResultArray = repSynthese.right().getValue();
                                                for (int i = 0; i < syntheseEleveResultArray.size(); i++) {
                                                    JsonObject _o = syntheseEleveResultArray.getJsonObject(i);
                                                    String id_eleve = _o.getString("id_eleve");

                                                    if (!syntheseEleve.containsKey(id_eleve)) {
                                                        syntheseEleve.put(id_eleve, _o.getString("texte"));
                                                    }
                                                }
                                                // On récupère les résultats par domaine par élève
                                                for (int i = 0; i <idEleves.size() ; i++) {
                                                    JsonArray resultats = event.right().getValue().getJsonArray(idEleves.get(i));
                                                    Map<Long, Integer> resultEleves = new HashMap<>();
                                                    if (resultats != null) {
                                                        for (Object resultat : resultats) {
                                                            resultEleves.put((Long) ((JsonObject) resultat).getLong("idDomaine"), (Integer) ((JsonObject) resultat).getInteger("niveau"));
                                                        }
                                                    }
                                                    resultatsEleves.put(idEleves.get(i), resultEleves);
                                                }
                                                // On modifie l'objet élève avec les informations récupérées précédemment
                                                for (Eleve e : classe.getValue()) {
                                                    e.setNotes(resultatsEleves.get(e.getIdEleve()));
                                                    e.setEnseignmentComplements(niveauEnseignementComplementEleve.get(e.getIdEleve()));
                                                    e.setSyntheseCycle(syntheseEleve.get(e.getIdEleve()));
                                                }
                                                JsonArray classeResult = formatBFC(classe.getValue());
                                                if (classeResult != null) {
                                                    collectBFCEleve(classe.getKey(), new JsonObject().put("eleves", classeResult), result, handler);
                                                }
                                            }else{
                                                collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation des notes pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                                                log.error("getBFC : buildBFC (Array of idEleves, " + classe.getKey() + ", " + idStructure + ") : " + event.left().getValue());
                                            }
                                        }
                                    });
                                } else {
                                    collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation des notes pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                                    log.error("getBFC : buildBFC (Array of idEleves, " + classe.getKey() + ", " + idStructure + ") : " + event.left().getValue());
                                }
                            }
                        });

                    } else {
                        collectBFCEleve(classe.getKey(), new JsonObject().put("error", "Une erreur est survenue lors de la recuperation des notes pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                        log.error("getBFC : buildBFC (Array of idEleves, " + classe.getKey() + ", " + idStructure + ") : " + event.left().getValue());
                    }
                }
            });


        }
    }




    /**
     * Recupere les parametres manquant afin de pouvoir generer le BFC dans le cas ou seul l'identifiant de la
     * structure est fourni.
     *
     * @param idStructure Identifiant de la structure dont on souhaite generer le BFC.
     * @param idPeriode  Identifiant de la période
     * @param handler     Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamStruct(final String idStructure,
                                final long idPeriode,
                                final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {

        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();
        population.put(idStructure, new LinkedHashMap<String, List<Eleve>>());

        Utils.getClassesStruct(eb,idStructure, new Handler<Either<String, List<String>>>() {
            @Override
            public void handle(Either<String, List<String>> event) {
                if (event.isRight()) {
                    final List<String> classes = event.right().getValue();
                    Utils.getElevesClasses(eb, classes.toArray(new String[0]), idPeriode,
                            new Handler<Either<String, Map<String, List<String>>>>() {
                                @Override
                                public void handle(Either<String, Map<String, List<String>>> event) {
                                    if (event.isRight()) {
                                        for (final Map.Entry<String, List<String>> classe : event.right().getValue().entrySet()) {
                                            population.get(idStructure).put(classe.getKey(), null);
                                            Utils.getInfoEleve(eb, classe.getValue().toArray(new String[0]),
                                                    idStructure, new Handler<Either<String, List<Eleve>>>() {
                                                        @Override
                                                        public void handle(Either<String, List<Eleve>> event) {
                                                            if (event.isRight()) {
                                                                population.get(idStructure).put(classe.getKey(), event.right().getValue());
                                                                // Si population.get(idStructure).values() contient une valeur null,
                                                                // cela signifie qu'une classe n'a pas encore recupere sa liste d'eleves
                                                                if (!population.get(idStructure).values().contains(null)) {
                                                                    handler.handle(new Either.Right<String, Map<String, Map<String, List<Eleve>>>>(population));
                                                                }
                                                            } else {
                                                                handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                                                                log.error("getParamStruct : getInfoEleve : " + event.left().getValue());
                                                            }
                                                        }
                                                    });
                                        }
                                    } else {
                                        handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                                        log.error("getParamStruct : getElevesClasses : " + event.left().getValue());
                                    }
                                }
                            });
                } else {
                    handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                    log.error("getParamStruct : getClassesStruct : " + event.left().getValue());
                }
            }
        });
    }

    /**
     * Recupere les parametres manquant afin de pouvoir generer le BFC dans le cas ou seul des identifiants de classes
     * sont fournis.
     *
     * @param idClasses Identifiants des classes dont on souhaite generer le BFC.
     * @param handler   Handler contenant les listes des eleves, indexees par classes.
     */
    private void  getParamClasses(final List<String> idClasses,
                                  final Long idPeriode,
                                  final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {
        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();

        Utils.getStructClasses(eb, idClasses.toArray(new String[0]), new Handler<Either<String, String>>() {
            @Override
            public void handle(Either<String, String> event) {
                if (event.isRight()) {
                    final String idStructure = event.right().getValue();
                    population.put(idStructure, new LinkedHashMap<String, List<Eleve>>());

                    Utils.getElevesClasses(eb, idClasses.toArray(new String[0]),
                            idPeriode,
                            new Handler<Either<String, Map<String, List<String>>>>() {
                                @Override
                                public void handle(Either<String, Map<String, List<String>>> event) {
                                    if (event.isRight()) {
                                        for (final Map.Entry<String, List<String>> classe : event.right().getValue().entrySet()) {
                                            population.get(idStructure).put(classe.getKey(), null);
                                            Utils.getInfoEleve(eb, classe.getValue().toArray(new String[0]),
                                                    idStructure, new Handler<Either<String, List<Eleve>>>() {
                                                        @Override
                                                        public void handle(Either<String, List<Eleve>> event) {
                                                            if (event.isRight()) {
                                                                population.get(idStructure).put(classe.getKey(), event.right().getValue());
                                                                // Si population.get(idStructure).values() contient une valeur null,
                                                                // cela signifie qu'une classe n'a pas encore recupere sa liste d'eleves
                                                                if (!population.get(idStructure).values().contains(null)) {
                                                                    handler.handle(new Either.Right<String, Map<String, Map<String, List<Eleve>>>>(population));
                                                                }
                                                            } else {
                                                                handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                                                                log.error("getParamClasses : getInfoEleve : " + event.left().getValue());
                                                            }
                                                        }
                                                    });
                                        }
                                    } else {
                                        handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                                        log.error("getParamClasses : getElevesClasses : " + event.left().getValue());
                                    }
                                }
                            });
                } else {
                    handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                    log.error("getParamClasses : getStructClasses : " + event.left().getValue());
                }
            }
        });
    }

    /**
     * Recupere les parametres manquant afin de pouvoir generer le BFC dans le cas ou seul des identifiants d'eleves
     * sont fournis.
     * @param idEtablissement Identifiant de l'établissement du modèle
     * @param idEleves Identifiants des eleves dont on souhaite generer le BFC.
     * @param handler  Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamEleves(final List<String> idEleves, String idEtablissement,
                                final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {
        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();

        Utils.getInfoEleve(eb, idEleves.toArray(new String[1]), idEtablissement,
                new Handler<Either<String, List<Eleve>>>() {
                    @Override
                    public void handle(Either<String, List<Eleve>> event) {
                        if (event.isRight()) {
                            final Map<String, List<Eleve>> classes = new LinkedHashMap<>();
                            for (Eleve e : event.right().getValue()) {
                                if (!classes.containsKey(e.getIdClasse())) {
                                    classes.put(e.getIdClasse(), new ArrayList<Eleve>());
                                }
                                classes.get(e.getIdClasse()).add(e);
                            }
                            Utils.getStructClasses(eb, classes.keySet().toArray(new String[0]), new Handler<Either<String, String>>() {
                                @Override
                                public void handle(Either<String, String> event) {
                                    if (event.isRight()) {
                                        population.put(event.right().getValue(), classes);
                                        handler.handle(new Either.Right<String, Map<String, Map<String, List<Eleve>>>>(population));
                                    } else {
                                        handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                                        log.error("getParamEleves : getStructClasses : " + event.left().getValue());
                                    }
                                }
                            });
                        } else {
                            handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                            log.error("getParamEleves : getInfoEleve : " + event.left().getValue());
                        }
                    }
                });

    }

    /**
     * Se charge d'appeler les methodes permettant la recuperation des parametres manquants en fonction du parametre
     * fournit.
     * Appelle  getParamStruct(String,Long, Handler)} si seul l'identifiant de la structure est fourni.
     * Appelle {@link #getParamClasses(List, Long, Handler)} si seuls les identifiants de classes sont fournis.
     * Appelle {@link #getParamEleves(List, String, Handler)} si seuls les identifiants d'eleves sont fournis.
     *
     * @param idStructure Identifiant de la structure dont on souhaite generer le BFC.
     * @param idClasses   Identifiants des classes dont on souhaite generer le BFC.
     * @param idEleves    Identifiants des eleves dont on souhaite generer le BFC.
     * @param handler     Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamBFC(final String idStructure, final List<String> idClasses, final List<String> idEleves,
                             final Long idPeriode,
                             final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {

        if (idStructure != null && (idEleves == null || idEleves.isEmpty()) ) {
            getParamStruct(idStructure, idPeriode,
                    new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, Map<String, List<Eleve>>>> event) {
                            if (event.isRight()) {
                                handler.handle(new Either.Right<String, Map<String, Map<String, List<Eleve>>>>(event.right().getValue()));
                            } else {
                                handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                                log.error("getParamStruct : failed to get related idClasses and/or idEleves.");
                            }
                        }
                    });
        } else if (!idClasses.isEmpty()) {
            getParamClasses(idClasses,
                    idPeriode,
                    new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, Map<String, List<Eleve>>>> event) {
                            if (event.isRight()) {
                                handler.handle(new Either.Right<String, Map<String, Map<String, List<Eleve>>>>(event.right().getValue()));
                            } else {
                                handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                                log.error("getParamClasses : failed to get related idStructure and/or idEleves.");
                            }
                        }
                    });
        } else if (!idEleves.isEmpty()) {
            getParamEleves(idEleves, idStructure, new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
                @Override
                public void handle(Either<String, Map<String, Map<String, List<Eleve>>>> event) {
                    if (event.isRight()) {
                        handler.handle(new Either.Right<String, Map<String, Map<String, List<Eleve>>>>(event.right().getValue()));
                    } else {
                        handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                        log.error("getParamEleves : failed to get related idStructure and/or idClasses.");
                    }
                }
            });
        } else {
            handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>("Aucun parametre renseigne."));
            log.error("getParamBFC : called with more than one null parameter.");
        }
    }

    /**
     * Genere le BFC des entites passees en parametre au format PDF via la fonction
     * Ces entites peuvent etre au choix un etablissement, un ou plusieurs classes, un ou plusieurs eleves.
     * Afin de prefixer le fichier PDF cree, appelle {@link DefaultUtilsService#getNameEntity(String[], Handler)} afin
     * de recuperer le nom de l'entite fournie.
     *
     * @param request
     */
    @Get("/BFC/pdf")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getBFCEleve(final HttpServerRequest request) {

        final String idStructure = request.params().get("idStructure");
        final List<String> idClasses = request.params().getAll("idClasse");
        final List<String> idEleves = request.params().getAll("idEleve");
        final Long idCycle = (request.params().get("idCycle") != null) ? Long.valueOf(request.params().get("idCycle")) : null;
        final Long idPeriode =
                (request.params().get("idPeriode") != null) ? Long.valueOf(request.params().get("idPeriode")) : null;

        // paramètre pour l'export des élèves
        final String idEtablissement = request.params().get(Competences.ID_ETABLISSEMENT_KEY);


        // Ou exclusif sur la presence des parametres, de facon a s'assurer qu'un seul soit renseigne.
        if (idStructure != null ^ !idClasses.isEmpty() ^ !idEleves.isEmpty()) {
            getParamBFC((idStructure)!=null? idStructure: idEtablissement, idClasses, idEleves,
                    idPeriode,
                    new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, Map<String, List<Eleve>>>> event) {
                            if (event.isRight()) {
                                final String idStructureGot = event.right().getValue().entrySet().iterator().next().getKey();
                                final Map<String, List<Eleve>> classes = event.right().getValue().entrySet().iterator().next().getValue();

                                getBFCParClasse(classes, idStructureGot, idPeriode, idCycle, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(Either<String, JsonArray> event) {
                                        if (event.isRight()) {
                                            final JsonObject result = new JsonObject().put("classes", event.right().getValue());
                                            if (idStructure != null) {
                                                utilsService.getNameEntity(new String[]{idStructureGot}, new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            final String structureName = ((JsonObject) event.right().getValue().getJsonObject(0)).getString("name").replace(" ", "_");
                                                            generateBFCExport(result, idPeriode, structureName, request);
                                                        } else {
                                                            leftToResponse(request, event.left());
                                                            log.error("getNameEntity : Unable to get the name of the specified entity (idStructure).");
                                                        }
                                                    }
                                                });
                                            } else if (!idClasses.isEmpty()) {
                                                utilsService.getNameEntity(classes.keySet().toArray(new String[1]), new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            final StringBuilder classesName = new StringBuilder();
                                                            for (int i = 0; i < event.right().getValue().size(); i++) {
                                                                classesName.append(((JsonObject) event.right().getValue().getJsonObject(i)).getString("name")).append("_");
                                                            }
                                                            classesName.setLength(classesName.length() - 1);
                                                            generateBFCExport(result, idPeriode, classesName.toString(), request);
                                                        } else {
                                                            leftToResponse(request, event.left());
                                                            log.error("getNameEntity : Unable to get the name of the specified entity (idClasses).");
                                                        }
                                                    }
                                                });
                                            } else {
                                                utilsService.getNameEntity(idEleves.toArray(new String[1]), new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if (event.isRight()) {
                                                            final StringBuilder elevesName = new StringBuilder();
                                                            for (int i = 0; i < event.right().getValue().size(); i++) {
                                                                elevesName.append(((JsonObject) event.right().getValue().getJsonObject(i)).getString("name")).append("_");
                                                            }
                                                            elevesName.setLength(elevesName.length() - 1);
                                                            generateBFCExport(result, idPeriode, elevesName.toString(), request);
                                                        } else {
                                                            leftToResponse(request, event.left());
                                                            log.error("getNameEntity : Unable to get the name of the specified entity (idEleves).");
                                                        }
                                                    }
                                                });
                                            }
                                        } else {
                                            leftToResponse(request, event.left());
                                            log.error("getBFC : Unable to get BFC for the specified parameters.");
                                        }
                                    }
                                });
                            } else {
                                leftToResponse(request, event.left());
                                log.error("getParamBFC : Unable to gather parameters, parameter unknown.");
                            }
                        }
                    });
        } else {
            leftToResponse(request, new Either.Left<>("Un seul parametre autre que la periode doit être specifie."));
            log.error("getBFCEleve : call with more than 1 parameter type (among idEleve, idClasse and idStructure).");
        }
    }

    private void generateBFCExport(final JsonObject result, Long idPeriode, final String fileNamePrefix, final HttpServerRequest request) {
        if (idPeriode != null) {
            JsonObject jsonRequest = new JsonObject()
                    .put("headers", new JsonObject().put("Accept-Language",
                            request.headers().get("Accept-Language")))
                    .put("Host", getHost(request));
            JsonObject action = new JsonObject()
                    .put("action", "periode.getLibellePeriode")
                    .put("idType", idPeriode)
                    .put("request", jsonRequest);

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if ("ok".equals(body.getString("status"))) {
                        String periodeName = body.getString("result");
                        periodeName = periodeName.replace(" ", "_");
                        exportService
                                .genererPdf(request, result,
                                        "BFC.pdf.xhtml",
                                        "BFC_" + fileNamePrefix + "_" + periodeName, vertx, config);
                    } else {
                        leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                        log.error("getPeriode : Unable to get the label of the specified entity (idPeriode).");
                    }
                }
            }));
        } else {
            exportService.genererPdf(request, result,
                    "BFC.pdf.xhtml",
                    "BFC_" + fileNamePrefix,  vertx, config);
        }
    }


    @Get("/devoirs/print/:idDevoir/formsaisie")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getFormsaisi(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    MultiMap params = request.params();
                    final Long idDevoir;
                    if (params.get("idDevoir") != null) {
                        try {
                            idDevoir = Long.parseLong(params.get("idDevoir"));
                        } catch (NumberFormatException e) {
                            log.error("Error : idDevoir must be a long object", e);
                            badRequest(request, e.getMessage());
                            return;
                        }

                        final JsonObject result = new JsonObject();

                        devoirService.getDevoirInfo(idDevoir, new Handler<Either<String, JsonObject>>() {
                            @Override
                            public void handle(final Either<String, JsonObject> devoirInfo) {
                                if (devoirInfo.isRight()) {

                                    final JsonObject devoirInfos = (JsonObject) ((Either.Right) devoirInfo).getValue();
                                    result.put("devoirName", devoirInfos.getString("name"));
                                    result.put("devoirCoefficient", devoirInfos.getString("coefficient"));
                                    result.put("devoirDiviseur", devoirInfos.getLong("diviseur"));
                                    result.put("evaluation", devoirInfos.getBoolean("is_evaluated"));

                                    JsonObject jsonRequest = new JsonObject()
                                            .put("headers", new JsonObject().put("Accept-Language",
                                                    request.headers().get("Accept-Language")))
                                            .put("Host", getHost(request));
                                    JsonObject action = new JsonObject()
                                            .put("action", "periode.getLibellePeriode")
                                            .put("type", devoirInfos.getInteger("periodetype"))
                                            .put("ordre", devoirInfos.getInteger("periodeordre"))
                                            .put("request", jsonRequest);
                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                        @Override
                                        public void handle(Message<JsonObject> message) {
                                            JsonObject body = message.body();

                                            result.put("periode", body.getString("result"));

                                            JsonObject action = new JsonObject()
                                                    .put("action", "classe.getEleveClasse")
                                                    .put("idClasse", devoirInfos.getString("id_groupe"))
                                                    .put("idPeriode", devoirInfos.getInteger("id_periode"));

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    JsonObject body = message.body();

                                                    if ("ok".equals(body.getString("status"))) {
                                                        result.put("eleves", body.getJsonArray("results"));

                                                        JsonObject action = new JsonObject()
                                                                .put("action", "matiere.getMatiere")
                                                                .put("idMatiere", devoirInfos.getString("id_matiere"));

                                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                            @Override
                                                            public void handle(Message<JsonObject> message) {
                                                                JsonObject body = message.body();

                                                                if ("ok".equals(body.getString("status"))) {
                                                                    result.put("matiere", body.getJsonObject("result").getJsonObject("n").getJsonObject("data").getString("label"));

                                                                    JsonObject action = new JsonObject()
                                                                            .put("action", "classe.getClasseInfo")
                                                                            .put("idClasse", devoirInfos.getString("id_groupe"));

                                                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                        @Override
                                                                        public void handle(Message<JsonObject> message) {
                                                                            JsonObject body = message.body();
                                                                            if ("ok".equals(body.getString("status"))) {
                                                                                result.put("classeName", body.getJsonObject("result").getJsonObject("c").getJsonObject("data").getString("name"));
                                                                                if(devoirInfos.getBoolean("is_evaluated") == true){
                                                                                    Integer nbrColone = (devoirInfos.getInteger("nbrcompetence") + 1 );
                                                                                    result.put("nbrCompetences",nbrColone.toString());
                                                                                }else{
                                                                                    result.put("nbrCompetences",devoirInfos.getInteger("nbrcompetence").toString());
                                                                                }

                                                                                if(devoirInfos.getInteger("nbrcompetence") > 0) {
                                                                                    competencesService.getDevoirCompetences(idDevoir, new Handler<Either<String, JsonArray>>() {
                                                                                        @Override
                                                                                        public void handle(Either<String, JsonArray> CompetencesObject) {
                                                                                            if(CompetencesObject.isRight()){
                                                                                                JsonArray  CompetencesOld = CompetencesObject.right().getValue();
                                                                                                final JsonArray  CompetencesNew = new fr.wseduc.webutils.collections.JsonArray();
                                                                                                Integer size =0;
                                                                                                Double ligne = new Double(0);
                                                                                                Integer lenght = 103; // le nombre de caractére max dans une ligne
                                                                                                Double height = new Double(2.2); // la hauteur d'une ligne
                                                                                                for (int i=0 ; i < CompetencesOld.size() ; i++) {
                                                                                                    JsonObject Comp = CompetencesOld.getJsonObject(i);
                                                                                                    size = Comp.getString("nom").length() +10; // +10 pour "[ Cx ]"
                                                                                                    ligne += (Integer) size / lenght ;
                                                                                                    if(size%lenght > 0 ){
                                                                                                        ligne++;
                                                                                                    }
                                                                                                    Comp.put("i", i+1);
                                                                                                    CompetencesNew.add(Comp);
                                                                                                }

                                                                                                ligne = (ligne * height) + 6; // + 6 la hauteur de la 1 ligne du tableau
                                                                                                if( ligne < 25){ // 25 est la hauteure minimal
                                                                                                    ligne = Double.parseDouble("25") ;
                                                                                                }
                                                                                                result.put("ligne", ligne.toString()+"%");
                                                                                                if(CompetencesNew.size() > 0){
                                                                                                    result.put("hasCompetences",true);
                                                                                                }else{
                                                                                                    result.put("hasCompetences",false);
                                                                                                }
                                                                                                result.put("competences",CompetencesNew);
                                                                                                exportService
                                                                                                        .genererPdf(
                                                                                                                request,
                                                                                                                result ,
                                                                                                                "Devoir.saisie.xhtml",
                                                                                                                "Formulaire_saisie",
                                                                                                                vertx, config);
                                                                                            }else{
                                                                                                log.error("Error :can not get competences devoir ");
                                                                                                badRequest(request, "Error :can not get competences devoir ");
                                                                                            }
                                                                                        }
                                                                                    });
                                                                                }else{
                                                                                    exportService.genererPdf(request,
                                                                                            result ,
                                                                                            "Devoir.saisie.xhtml",
                                                                                            "Formulaire_saisie",
                                                                                            vertx, config);
                                                                                }
                                                                            }else{
                                                                                log.error("Error :can not get classe informations ");
                                                                                badRequest(request, "Error :can not get  classe informations");
                                                                            }
                                                                        }
                                                                    }));
                                                                } else {
                                                                    log.error("Error :can not get classe info ");
                                                                    badRequest(request, "Error :can not get  classe info  ");
                                                                }
                                                            }
                                                        }));
                                                    } else {
                                                        log.error("Error :can not get students ");
                                                        badRequest(request, "Error :can not get students  ");
                                                    }
                                                }
                                            }));
                                        }
                                    }));
                                } else {
                                    log.error("Error :can not get informations from postgres tables ");
                                    badRequest(request, "Error :can not get informations from postgres tables ");
                                }
                            }
                        });
                    } else {
                        log.error("Error : idDevoir must be a long object");
                        badRequest(request, "Error : idDevoir must be a long object");
                    }
                } else {
                    unauthorized(request);
                }

            }
        });
    }

    @Get("/devoirs/print/:idDevoir/cartouche")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getCartouche(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    MultiMap params = request.params();
                    final Long idDevoir;
                    if (params.get("idDevoir") != null) {
                        try {
                            idDevoir = Long.parseLong(params.get("idDevoir"));
                        } catch (NumberFormatException e) {
                            log.error("Error : idDevoir must be a long object", e);
                            badRequest(request, e.getMessage());
                            return;
                        }

                        final JsonObject result = new JsonObject();
                        int nbrCartouche = 0;
                        try {
                            nbrCartouche = Integer.parseInt(params.get("nbr"));
                        } catch (NumberFormatException e) {
                            log.error("Error : idDevoir must be a long object", e);
                            badRequest(request, e.getMessage());
                            return;
                        }
                        if (nbrCartouche > 0) {
                            JsonArray nbr = new fr.wseduc.webutils.collections.JsonArray();
                            for (int j = 0; j < nbrCartouche; j++) {
                                nbr.add(j);
                            }
                            result.put("number", nbr);
                        } else {
                            result.put("number", new fr.wseduc.webutils.collections.JsonArray().add("cartouche"));
                        }

                        final String byEleve = params.get("eleve");
                        final String color = params.get("color");
                        if (byEleve != null && color != null) {
                            devoirService.getDevoirInfo(idDevoir, new Handler<Either<String, JsonObject>>() {
                                @Override
                                public void handle(Either<String, JsonObject> devoirInfo) {
                                    if (devoirInfo.isRight()) {
                                        final JsonObject devoirInfos = (JsonObject) ((Either.Right) devoirInfo).getValue();
                                        SimpleDateFormat dateBfFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        SimpleDateFormat dateAftFormat = new SimpleDateFormat("dd-MM-yyyy");
                                        String reformattedStr = "";
                                        ArrayList<String> classeList = new ArrayList<String>();

                                        result.put("devoirName", devoirInfos.getString("name"));
                                        if (color.equals("true")) {
                                            result.put("byColor", true);
                                        } else {
                                            result.put("byColor", false);
                                        }
                                        try {
                                            reformattedStr = dateAftFormat.format(dateBfFormat.parse(devoirInfos.getString("created")));
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        if (reformattedStr != "") {
                                            result.put("devoirDate", reformattedStr);
                                        } else {
                                            result.put("devoirDate", devoirInfos.getString("created"));
                                        }
//                                        result.put("evaluation", devoirInfos.getBoolean("is_evaluated"));
                                        result.put("evaluation", true);
                                        //début
                                        classeList.add(devoirInfos.getString("id_groupe"));
                                        utilsService.getCycle(classeList, new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> cycle) {
                                                if (cycle.isRight()) {
                                                    JsonObject cycleobj = cycle.right().getValue().getJsonObject(0);
                                                    niveauDeMaitriseService.getNiveauDeMaitriseofCycle(cycleobj.getLong("id_cycle"), new Handler<Either<String, JsonArray>>() {
                                                        @Override
                                                        public void handle(Either<String, JsonArray> nivMaitrise) {
                                                            if (nivMaitrise.isRight()) {
                                                                result.put("niveaux", nivMaitrise.right().getValue());
                                                                if (byEleve.equals("true")) {
                                                                    result.put("byEleves", true);
                                                                    JsonObject action = new JsonObject()
                                                                            .put("action", "classe.getEleveClasse")
                                                                            .put("idClasse", devoirInfos
                                                                                    .getString("id_groupe"))
                                                                            .put("idPeriode", devoirInfos
                                                                                    .getInteger("id_periode"));

                                                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                        @Override
                                                                        public void handle(Message<JsonObject> message) {
                                                                            JsonObject body = message.body();

                                                                            if ("ok".equals(body.getString("status"))) {
                                                                                result.put("eleves", body.getJsonArray("results"));
                                                                                if (devoirInfos.getInteger("nbrcompetence") > 0) {
                                                                                    competencesService.getDevoirCompetences(idDevoir, new Handler<Either<String, JsonArray>>() {
                                                                                        @Override
                                                                                        public void handle(Either<String, JsonArray> CompetencesObject) {
                                                                                            if (CompetencesObject.isRight()) {
                                                                                                JsonArray CompetencesOld = CompetencesObject.right().getValue();
                                                                                                JsonArray CompetencesNew = new fr.wseduc.webutils.collections.JsonArray();
                                                                                                for (int i = 0; i < CompetencesOld.size(); i++) {
                                                                                                    JsonObject Comp = CompetencesOld.getJsonObject(i);
                                                                                                    Comp.put("i", i + 1);
                                                                                                    if (i == 0) {
                                                                                                        Comp.put("first", true);
                                                                                                    } else {
                                                                                                        Comp.put("first", false);
                                                                                                    }
                                                                                                    CompetencesNew.add(Comp);
                                                                                                }
                                                                                                if (CompetencesNew.size() > 0) {
                                                                                                    result.put("hasCompetences", true);
                                                                                                } else {
                                                                                                    result.put("hasCompetences", false);
                                                                                                }
                                                                                                result.put("nbrCompetences", devoirInfos.getInteger("nbrcompetence").toString());
                                                                                                result.put("competences", CompetencesNew);
                                                                                                exportService
                                                                                                        .genererPdf(request,
                                                                                                                result,
                                                                                                                "cartouche.pdf.xhtml",
                                                                                                                "Cartouche",
                                                                                                                vertx, config);
                                                                                            } else {
                                                                                                log.error("Error :can not get competences devoir ");
                                                                                                badRequest(request, "Error :can not get competences devoir ");
                                                                                            }
                                                                                        }
                                                                                    });
                                                                                } else {
                                                                                    exportService.genererPdf(request,
                                                                                            result,
                                                                                            "cartouche.pdf.xhtml",
                                                                                            "Cartouche",
                                                                                            vertx, config);
                                                                                }
                                                                            } else {
                                                                                log.error("Error :can not get students ");
                                                                                badRequest(request, "Error :can not get students  ");
                                                                            }
                                                                        }
                                                                    }));
                                                                } else {
                                                                    result.put("byEleves", false);
                                                                    if (devoirInfos.getInteger("nbrcompetence") > 0) {
                                                                        competencesService.getDevoirCompetences(idDevoir, new Handler<Either<String, JsonArray>>() {
                                                                            @Override
                                                                            public void handle(Either<String, JsonArray> CompetencesObject) {
                                                                                if (CompetencesObject.isRight()) {
                                                                                    JsonArray CompetencesOld = CompetencesObject.right().getValue();
                                                                                    JsonArray CompetencesNew = new fr.wseduc.webutils.collections.JsonArray();
                                                                                    for (int i = 0; i < CompetencesOld.size(); i++) {
                                                                                        JsonObject Comp = CompetencesOld.getJsonObject(i);
                                                                                        Comp.put("i", i + 1);
                                                                                        if (i == 0) {
                                                                                            Comp.put("first", true);
                                                                                        } else {
                                                                                            Comp.put("first", false);
                                                                                        }

                                                                                        CompetencesNew.add(Comp);
                                                                                    }
                                                                                    if (CompetencesNew.size() > 0) {
                                                                                        result.put("hasCompetences", true);
                                                                                    } else {
                                                                                        result.put("hasCompetences", false);
                                                                                    }
                                                                                    result.put("nbrCompetences", devoirInfos.getInteger("nbrcompetence").toString());
                                                                                    result.put("competences", CompetencesNew);
                                                                                    result.put("image", Boolean.parseBoolean(request.params().get("image")));
                                                                                    exportService
                                                                                            .genererPdf(request,
                                                                                                    result,
                                                                                                    "cartouche.pdf.xhtml",
                                                                                                    "Cartouche",
                                                                                                    vertx, config);
                                                                                } else {
                                                                                    log.error("Error :can not get competences devoir ");
                                                                                    badRequest(request, "Error :can not get competences devoir ");
                                                                                }
                                                                            }
                                                                        });
                                                                    } else {
                                                                        exportService.genererPdf(request, result,
                                                                                "cartouche.pdf.xhtml", "Cartouche",
                                                                                vertx, config);
                                                                    }
                                                                }
                                                            } else {
                                                                log.error("Error :can not get levels ");
                                                                badRequest(request, "Error :can not get levels  ");
                                                            }
                                                        }
                                                    });

                                                } else {
                                                    log.error("Error :can not get cycle ");
                                                    badRequest(request, "Error :can not get cycle  ");
                                                }


                                            }
                                        });

                                    } else {
                                        log.error("Error :can not get informations from postgres tables ");
                                        badRequest(request, "Error :can not get informations from postgres tables ");
                                    }

                                }
                            });
                        }
                    } else {
                        log.error("Error : idDevoir must be a long object");
                        badRequest(request, "Error : idDevoir must be a long object");
                    }
                } else {
                    unauthorized(request);
                }
            }
        });
    }

    @Get("/devoirs/print/:idDevoir/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportDevoir(final HttpServerRequest request) {
        Long idDevoir = 0L;
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));


        try {
            idDevoir = Long.parseLong(request.params().get("idDevoir"));
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
        }

        devoirService.getDevoirInfo(idDevoir, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                if (stringJsonObjectEither.isRight()) {
                    JsonObject devoir = stringJsonObjectEither.right().getValue();
                    final Boolean only_evaluation = devoir.getInteger("nbrcompetence").equals(0L);
                    String idGroupe = devoir.getString("id_groupe");
                    String idEtablissement = devoir.getString("id_etablissement");

                    exportService.getExportEval(text, only_evaluation, devoir, idGroupe, idEtablissement,
                            request, new Handler<Either<String, JsonObject>>() {

                                @Override
                                public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                                    if (stringJsonObjectEither.isRight()) {
                                        try {
                                            JsonObject result = stringJsonObjectEither.right().getValue();
                                            result.put("notOnlyEvaluation", !only_evaluation);
                                            if (json) {
                                                Renders.renderJson(request, result);
                                            } else {
                                                String fileName = result.getJsonObject("devoir").getString("classe") + "_" + result.getJsonObject("devoir").getString("nom").replace(' ', '_');
                                                exportService.genererPdf(request,
                                                        result, "evaluation.pdf.xhtml",
                                                        fileName, vertx, config);
                                            }
                                        } catch (Error err) {
                                            leftToResponse(request, new Either.Left<>("An error occured while rendering pdf export : " + err.getMessage()));
                                        }
                                    } else {
                                        leftToResponse(request, stringJsonObjectEither.left());
                                    }
                                }
                            });
                } else {
                    leftToResponse(request, stringJsonObjectEither.left());
                }
            }
        });
    }

    @Get("/releveComp/print/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportReleveComp(final HttpServerRequest request) {
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean byEnseignement = Boolean.parseBoolean(request.params().get("byEnseignement"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final Boolean isCycle = Boolean.parseBoolean(request.params().get("isCycle"));
        final List<String> listIdMatieres = request.params().getAll("idMatiere");
        final String idStructure = request.params().get(Competences.ID_ETABLISSEMENT_KEY);

        final JsonArray idMatieres = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < listIdMatieres.size(); i++) {
            idMatieres.add(listIdMatieres.get(i));
        }


        Long idPeriode = null;
        String idClasse = null;
        String idEleve = null;

        try {
            if (request.params().contains("idPeriode")) {
                idPeriode = Long.parseLong(request.params().get("idPeriode"));
            }
            if (request.params().contains("idClasse")) {
                idClasse = request.params().get("idClasse");
            }
            if (request.params().contains("idEleve")) {
                idEleve = request.params().get("idEleve");
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        final Long finalIdPeriode = idPeriode;
        final String finalIdClasse = idClasse;
        final String finalIdEleve = idEleve;

        final List<String> idGroupes = new ArrayList<>();
        final Map<String, String> nomGroupes = new LinkedHashMap<>();
        final List<String> idEtablissement = new ArrayList<>();

        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieres")
                .put("idMatieres", idMatieres);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    final JsonArray results = body.getJsonArray("results");
                    String mat = ((JsonObject) results.getJsonObject(0)).getString("name");
                    for (int i = 1; i < results.size(); i++) {
                        mat = mat + ", " + ((JsonObject) results.getJsonObject(i)).getString("name");
                    }
                    final String matieres = mat;
                    JsonObject jsonRequest = new JsonObject()
                            .put("headers", new JsonObject().put("Accept-Language",
                                    request.headers().get("Accept-Language")))
                            .put("Host", getHost(request));
                    JsonObject action = new JsonObject()
                            .put("action", "periode.getLibellePeriode")
                            .put("request", jsonRequest);
                    if (!"undefined".equals(finalIdPeriode)) {
                        action.put("idType", finalIdPeriode);
                    }
                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            final JsonObject body = message.body();
                            if ("ok".equals(body.getString("status"))) {
                                String _libellePeriode = body.getString("result")
                                        .replace("é", "e")
                                        .replace("è", "e");
                                if (isCycle) {
                                    _libellePeriode = I18n.getInstance().translate("viescolaire.utils.cycle",
                                            I18n.DEFAULT_DOMAIN, Locale.FRANCE);
                                }
                                final String libellePeriode = _libellePeriode;

                                if (finalIdClasse == null) {
                                    JsonObject action = new JsonObject()
                                            .put("action", "eleve.getInfoEleve")
                                            .put(Competences.ID_ETABLISSEMENT_KEY, idStructure)
                                            .put("idEleves", new fr.wseduc.webutils.collections.JsonArray(
                                                    Arrays.asList(new String[]{finalIdEleve})));

                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                        @Override
                                        public void handle(Message<JsonObject> message) {
                                            JsonObject body = message.body();

                                            if ("ok".equals(body.getString("status")) && body.getJsonArray("results").size() > 0) {
                                                JsonObject eleve = body.getJsonArray("results").getJsonObject(0);
                                                final String nomClasse = eleve.getString("classeName");
                                                final String idClasse = eleve.getString("idClasse");
                                                final String idEtablissement = eleve.getString("idEtablissement");
                                                JsonArray idManualGroupes = UtilsConvert
                                                        .strIdGroupesToJsonArray(eleve.getValue("idManualGroupes"));
                                                JsonArray idFunctionalGroupes = UtilsConvert
                                                        .strIdGroupesToJsonArray(eleve.getValue("idGroupes"));

                                                JsonArray _idGroupes = utilsService.saUnion(idFunctionalGroupes,
                                                        idManualGroupes);
                                                String[] _iGroupesdArr = UtilsConvert.jsonArrayToStringArr(_idGroupes);

                                                final String[] idEleves = new String[1];
                                                idEleves[0] = finalIdEleve;
                                                idGroupes.add(idClasse);
                                                nomGroupes.put(eleve.getString("idEleve"), nomClasse);
                                                final Map<String, String> elevesMap = new LinkedHashMap<>();
                                                elevesMap.put(finalIdEleve, eleve.getString("lastName") + " " + eleve.getString("firstName"));
                                                final AtomicBoolean answered = new AtomicBoolean();
                                                JsonArray resultFinal = new fr.wseduc.webutils.collections.JsonArray();
                                                final Handler<Either<String, JsonObject>> finalHandler = getReleveCompetences(request, elevesMap, nomGroupes, matieres,
                                                        libellePeriode, json, answered, resultFinal);
                                                exportService.getExportReleveComp(text, byEnseignement, idEleves[0], idGroupes.toArray(new String[0]), _iGroupesdArr, idEtablissement, listIdMatieres,
                                                        finalIdPeriode, isCycle, finalHandler);
                                            } else {
                                                leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                            }
                                        }
                                    }));
                                } else {
                                    final JsonObject action = new JsonObject()
                                            .put("action", "classe.getEleveClasse")
                                            .put("idClasse", finalIdClasse)
                                            .put("idPeriode", finalIdPeriode);
                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                        @Override
                                        public void handle(Message<JsonObject> message) {
                                            if ("ok".equals(message.body().getString("status"))) {
                                                final JsonArray eleves = message.body().getJsonArray("results");
                                                final String[] idEleves = new String[eleves.size()];

                                                final Map<String, String> elevesMap = new LinkedHashMap<>();

                                                for (int i = 0; i < eleves.size(); i++) {
                                                    elevesMap.put(((JsonObject) eleves.getJsonObject(i)).getString("id"),
                                                            ((JsonObject) eleves.getJsonObject(i)).getString("lastName")
                                                                    + " " + ((JsonObject) eleves.getJsonObject(i)).getString("firstName"));
                                                    idEleves[i] = ((JsonObject) eleves.getJsonObject(i)).getString("id");
                                                }

                                                JsonObject action = new JsonObject()
                                                        .put("action", "eleve.getInfoEleve")
                                                        .put(Competences.ID_ETABLISSEMENT_KEY, idStructure)
                                                        .put("idEleves", new fr.wseduc.webutils.collections.JsonArray(
                                                                Arrays.asList(idEleves)));
                                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                                                        new Handler<Message<JsonObject>>() {
                                                            @Override
                                                            public void handle(Message<JsonObject> message) {
                                                                JsonObject body = message.body();
                                                                JsonArray result = body.getJsonArray("results");
                                                                if ("ok".equals(body.getString("status"))
                                                                        && result.size() > 0) {
                                                                    for (int i = 0; i< result.size(); i++ ) {
                                                                        JsonObject eleve = body.getJsonArray("results")
                                                                                .getJsonObject(i);
                                                                        final String nomClasse =
                                                                                eleve.getString("classeName");
                                                                        final String idClasse =
                                                                                eleve.getString("idClasse");
                                                                        idEtablissement.add(
                                                                                eleve.getString("idEtablissement"));
                                                                        idGroupes.add(idClasse);

                                                                        nomGroupes.put(((JsonObject)eleves.getJsonObject(i)).
                                                                                getString("id"),nomClasse);
                                                                    }

                                                                    final AtomicBoolean answered = new AtomicBoolean();
                                                                    JsonArray resultFinal = new fr.wseduc.webutils.collections.JsonArray();
                                                                    final Handler<Either<String, JsonObject>> finalHandler
                                                                            = getReleveCompetences(request, elevesMap,
                                                                            nomGroupes, matieres,
                                                                            libellePeriode, json, answered, resultFinal);
                                                                    for (int i = 0; i < eleves.size(); i++) {
                                                                        String [] _idGroupes = new String[1];
                                                                        _idGroupes[0] = idGroupes.get(i);

                                                                        JsonObject o = result.getJsonObject(i);

                                                                        JsonArray idManualGroupes = UtilsConvert
                                                                                .strIdGroupesToJsonArray(o.getValue("idManualGroupes"));
                                                                        JsonArray idFunctionalGroupes = UtilsConvert
                                                                                .strIdGroupesToJsonArray(o.getValue("idGroupes"));

                                                                        JsonArray idGroupes = utilsService.saUnion(idFunctionalGroupes,
                                                                                idManualGroupes);
                                                                        String[] idGroupesArr =
                                                                                UtilsConvert.jsonArrayToStringArr(idGroupes);
                                                                        exportService.getExportReleveComp(text, byEnseignement, idEleves[i],
                                                                                _idGroupes , idGroupesArr, idEtablissement.get(i),
                                                                                listIdMatieres, finalIdPeriode, isCycle, finalHandler);
                                                                    }
                                                                } else {
                                                                    leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                                }
                                                            }
                                                        }));
                                            } else {
                                                leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                            }
                                        }
                                    }));
                                }
                            } else {
                                leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                            }
                        }
                    }));
                } else {
                    leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                }
            }
        }));
    }


    @Get("/recapAppreciations/print/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportRecapAppreciations(final HttpServerRequest request) {
        final String idClasse = request.params().get("idClasse");
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));

        Integer idPeriode = null;

        try {
            if (request.params().contains("idPeriode")) {
                idPeriode = Integer.parseInt(request.params().get("idPeriode"));
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        Integer finalIdPeriode = idPeriode;

        Set<JsonObject> MatGrp = new HashSet<>();
        Map<JsonObject, String> teachers = new HashMap<>();

        Future<Map<JsonObject, String>> apprFuture = Future.future();
        Future<Map<JsonObject, Map<String, List<NoteDevoir>>>> notesFuture = Future.future();
        Future<Map<JsonObject, Map<String, NoteDevoir>>> moyennesFinalFuture = Future.future();

        Future<Map<String, String>> libMatFuture = Future.future();
        Future<Map<String, String>> libGrpFuture = Future.future();
        Future<Map<String, String>> libTeachFuture = Future.future();

        Future<Map<JsonObject, JsonObject>> moyObjectFuture = Future.future();

        JsonObject result = new JsonObject();

        Future<JsonArray> idGroupesFuture = Future.future();
        Utils.getGroupesClasse(eb, new JsonArray().add(idClasse), eventResultGroups -> {
            if (eventResultGroups.isRight()) {
                idGroupesFuture.complete(eventResultGroups.right().getValue());
            } else {
                idGroupesFuture.fail(eventResultGroups.left().getValue());
            }
        });

        Future<List<String>> idElevesFuture = Future.future();
        Utils.getIdElevesClassesGroupes(eb, idClasse, finalIdPeriode, 0, eventResultEleves -> {
            if (eventResultEleves.isRight()) {
                idElevesFuture.complete(eventResultEleves.right().getValue());
            } else {
                idElevesFuture.fail(eventResultEleves.left().getValue());
            }
        });

        CompositeFuture.all(idElevesFuture, idGroupesFuture).setHandler(event -> {
            if(event.succeeded()) {
                Set<String> idGroups = new HashSet<>(Collections.singleton(idClasse));
                idGroupesFuture.result().stream().forEach(line -> {
                    idGroups.add(((JsonObject) line).getString("id_classe"));
                    ((JsonObject) line).getJsonArray("id_groupes").getList().forEach(idGroup -> idGroups.add((String) idGroup));
                });

                appreciationService.getAppreciationClasse(idGroups.toArray(new String[0]), finalIdPeriode, null, resultAppr -> {
                    if (resultAppr.isRight()) {
                        Map<JsonObject, String> appr = new HashMap<>();

                        resultAppr.right().getValue().stream().forEach(line -> {
                            JsonObject lineObject = (JsonObject) line;

                            JsonObject key = new JsonObject();
                            key.put("id_matiere", lineObject.getString("id_matiere"));
                            key.put("id_groupe", lineObject.getString("id_classe"));

                            MatGrp.add(key);
                            appr.put(key, lineObject.getString("appreciation"));
                        });

                        apprFuture.complete(appr);
                    } else {
                        apprFuture.fail(resultAppr.left().getValue());
                    }
                });

                noteService.getNotesParElevesParDevoirs(idElevesFuture.result().toArray(new String[0]), idGroups.toArray(new String[0]), null, finalIdPeriode,
                        resultNotesEleves -> {
                            if (resultNotesEleves.isRight()) {

                                Map<JsonObject, Map<String, List<NoteDevoir>>> notes = new HashMap<>();

                                resultNotesEleves.right().getValue().stream().forEach(line -> {
                                    JsonObject lineObject = (JsonObject) line;

                                    JsonObject key = new JsonObject();
                                    key.put("id_matiere", lineObject.getString("id_matiere"));
                                    key.put("id_groupe", lineObject.getString("id_groupe"));

                                    MatGrp.add(key);

                                    if (!teachers.containsKey(key)) {
                                        teachers.put(key, lineObject.getString("owner"));
                                    }

                                    NoteDevoir note = new NoteDevoir(Double.parseDouble(lineObject.getString("valeur")),
                                            lineObject.getLong("diviseur").doubleValue(),
                                            lineObject.getBoolean("ramener_sur"),
                                            Double.parseDouble(lineObject.getString("coefficient")));

                                    if (!notes.containsKey(key)) {
                                        notes.put(key, new HashMap<>());
                                    }

                                    if (!notes.get(key).containsKey(lineObject.getString("id_eleve"))) {
                                        notes.get(key).put(lineObject.getString("id_eleve"), new ArrayList<>());
                                    }
                                    notes.get(key).get(lineObject.getString("id_eleve")).add(note);
                                });

                                notesFuture.complete(notes);

                            } else {
                                notesFuture.fail(resultNotesEleves.left().getValue());
                            }
                        });

                noteService.getMoyennesFinal(idElevesFuture.result().toArray(new String[0]), finalIdPeriode, null, idGroups.toArray(new String[0]), stringJsonArrayEither -> {
                    if (stringJsonArrayEither.isRight()) {

                        Map<JsonObject, Map<String, NoteDevoir>> moyFinal = new HashMap<>();

                        stringJsonArrayEither.right().getValue().stream().forEach(line -> {
                            JsonObject lineObject = (JsonObject) line;

                            JsonObject key = new JsonObject()
                                    .put("id_groupe", lineObject.getString("id_classe"))
                                    .put("id_matiere", lineObject.getString("id_matiere"));

                            MatGrp.add(key);

                            if (!moyFinal.containsKey(key)) {
                                moyFinal.put(key, new HashMap<>());
                            }

                            moyFinal.get(key).put(lineObject.getString("id_eleve"), new NoteDevoir(Double.parseDouble(lineObject.getString("moyenne")), false, new Double(1)));
                        });

                        moyennesFinalFuture.complete(moyFinal);
                    } else {
                        moyennesFinalFuture.fail(stringJsonArrayEither.left().getValue());
                    }
                });
            } else {
                apprFuture.fail(event.cause());
                notesFuture.fail(event.cause());
                moyennesFinalFuture.fail(event.cause());
            }
        });

        CompositeFuture.all(apprFuture, notesFuture, moyennesFinalFuture).setHandler(compositeFutureAsyncResult -> {
            if (compositeFutureAsyncResult.succeeded()) {

                Map<JsonObject, String> appr = compositeFutureAsyncResult.result().resultAt(0);
                Map<JsonObject, Map<String, List<NoteDevoir>>> notes = compositeFutureAsyncResult.result().resultAt(1);
                Map<JsonObject, Map<String, NoteDevoir>> moyennesFinales = compositeFutureAsyncResult.result().resultAt(2);

                Map<JsonObject, JsonObject> moyObjects = MatGrp.stream().collect(Collectors.toMap(val -> val, val -> new JsonObject()));

                MatGrp.stream().forEach(matGrp -> {

                    List<NoteDevoir> matGrpNotes = new ArrayList<>();

                    JsonObject moyObject = new JsonObject();

                    idElevesFuture.result().stream().forEach(idEleve -> {
                        if (moyennesFinales.containsKey(matGrp) && moyennesFinales.get(matGrp).containsKey(idEleve)) {
                            matGrpNotes.add(moyennesFinales.get(matGrp).get(idEleve));
                        } else if (notes.containsKey(matGrp) && notes.get(matGrp).containsKey(idEleve)) {
                            matGrpNotes.add(new NoteDevoir(utilsService.calculMoyenne(notes.get(matGrp).get(idEleve), false, null).getDouble("moyenne"), false, new Double(1)));
                        }
                    });

                    JsonObject resultCalc = utilsService.calculMoyenne(matGrpNotes, true, null);
                    if (resultCalc.getDouble("noteMin") > resultCalc.getDouble("moyenne")) {
                        moyObject.put("min", "");
                        moyObject.put("max", "");
                        moyObject.put("moy", "");
                    } else {
                        moyObject.put("min", resultCalc.getDouble("noteMin"));
                        moyObject.put("max", resultCalc.getDouble("noteMax"));
                        moyObject.put("moy", resultCalc.getDouble("moyenne"));
                    }
                    moyObject.put("appr", appr.get(matGrp));

                    moyObjects.put(matGrp, moyObject);
                });

                moyObjectFuture.complete(moyObjects);

                if (teachers.values().size() == 0) {
                    libTeachFuture.complete(new HashMap<>());
                } else {
                    Utils.getLastNameFirstNameUser(eb, new JsonArray(new ArrayList(teachers.values())), libTeachersEvent -> {
                        if (libTeachersEvent.isRight()) {
                            libTeachFuture.complete(libTeachersEvent.right().getValue().entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(val -> val.getKey(), val -> val.getValue().getString("firstName") + " " + val.getValue().getString("name"))));
                        } else {
                            log.error(libTeachersEvent.left().getValue());
                            libTeachFuture.fail(new Throwable(libTeachersEvent.left().getValue()));
                        }
                    });
                }

                if (MatGrp.size() == 0) {
                    libMatFuture.complete(new HashMap<>());
                } else {
                    Utils.getLibelleMatiere(eb, new JsonArray(MatGrp.stream().map(matGrp -> matGrp.getString("id_matiere")).collect(Collectors.toList())), libMatEvent -> {
                        if (libMatEvent.isRight()) {
                            libMatFuture.complete(libMatEvent.right().getValue().entrySet().stream().collect(Collectors.toMap(val -> val.getKey(), val -> val.getValue().getString("name"))));
                        } else {
                            log.error(libMatEvent.left().getValue());
                            libMatFuture.fail(new Throwable(libMatEvent.left().getValue()));
                        }
                    });
                }

                if (MatGrp.size() == 0) {
                    libGrpFuture.complete(new HashMap<>());
                } else {
                    Utils.getInfosGroupes(eb, new JsonArray(MatGrp.stream().map(matGrp -> matGrp.getString("id_groupe")).collect(Collectors.toList())), libGrpEvent -> {
                        if (libGrpEvent.isRight()) {
                            libGrpFuture.complete(libGrpEvent.right().getValue());
                        } else {
                            log.error(libGrpEvent.left().getValue());
                            libGrpFuture.fail(new Throwable(libGrpEvent.left().getValue()));
                        }
                    });
                }
            } else {
                moyObjectFuture.fail(compositeFutureAsyncResult.cause());
                libTeachFuture.fail(compositeFutureAsyncResult.cause());
                libMatFuture.fail(compositeFutureAsyncResult.cause());
                libGrpFuture.fail(compositeFutureAsyncResult.cause());
            }
        });

        Future<String> libellePeriodeFuture = Future.future();
        if(finalIdPeriode == null) {
            libellePeriodeFuture.complete("Année");
        } else {
            Utils.getLibellePeriode(eb, request, finalIdPeriode, stringStringEither -> {
                if (stringStringEither.isRight()) {
                    libellePeriodeFuture.complete(stringStringEither.right().getValue());
                } else {
                    libellePeriodeFuture.fail(stringStringEither.left().getValue());
                }
            });
        }

        Future<String> libelleClasseFuture = Future.future();
        Utils.getInfosGroupes(eb, new JsonArray().add(idClasse), stringMapEither -> {
            if (stringMapEither.isRight()) {
                libelleClasseFuture.complete(stringMapEither.right().getValue().get(idClasse));
            } else {
                libelleClasseFuture.fail(stringMapEither.left().getValue());
            }
        });

        CompositeFuture.all(libellePeriodeFuture, libelleClasseFuture, libTeachFuture, libGrpFuture, libMatFuture, moyObjectFuture).setHandler(allData -> {
            if (allData.succeeded()) {
                String libellePeriode = allData.result().resultAt(0);
                String libelleClasse = allData.result().resultAt(1);
                Map<String, String> libTeachers = allData.result().resultAt(2);
                Map<String, String> libGrp = allData.result().resultAt(3);
                Map<String, String> libMatieres = allData.result().resultAt(4);
                Map<JsonObject, JsonObject> moyObject = allData.result().resultAt(5);

                JsonArray data = new JsonArray(

                        moyObject.entrySet().stream().map(entry -> {
                            JsonObject newMoy = new JsonObject();
                            newMoy.put("mat", libMatieres.get(entry.getKey().getString("id_matiere")));
                            newMoy.put("prof", libTeachers.get(teachers.get(entry.getKey())));
                            newMoy.put("grp", libGrp.get(entry.getKey().getString("id_groupe")));
                            newMoy.mergeIn(entry.getValue());

                            return newMoy;
                        }).collect(Collectors.toList()));

                result.put("data", data);
                result.put("periode", libellePeriode);
                result.put("classe", libelleClasse);

                if(json) {
                    Renders.renderJson(request, result);
                } else {
                    String fileName = result.getString("classe") + "_export_appreciation";
                    exportService.genererPdf(request, result,
                            "export_appreciations-classe.pdf.xhtml", fileName, vertx, config);
                }
            } else {
                leftToResponse(request, new Either.Left<>(allData.cause().getMessage()));
            }
        });
    }

    @Get("/recapEval/print/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportRecapEval(final HttpServerRequest request) {
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final String idClasse = request.params().get("idClasse");

        Long idPeriode = null;

        try {
            if (request.params().contains("idPeriode")) {
                idPeriode = Long.parseLong(request.params().get("idPeriode"));
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        final Long finalIdPeriode = idPeriode;

        utilsService.getCycle(Arrays.asList(idClasse), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                if (stringJsonArrayEither.isRight()) {
                    final Long idCycle = ((JsonObject) stringJsonArrayEither.right().getValue()
                            .getJsonObject(0)).getLong("id_cycle");
                    JsonObject cycleObj = stringJsonArrayEither.right().getValue().getJsonObject(0);

                    if (!idCycle.equals(cycleObj.getLong("id_cycle"))) {
                        leftToResponse(request, new Either.Left<>("different cycle"));
                    } else {
                        final JsonObject action = new JsonObject()
                                .put("action", "classe.getEtabClasses")
                                .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(Arrays
                                        .asList(new String[]{idClasse})));

                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                                new Handler<Message<JsonObject>>() {
                                    @Override
                                    public void handle(Message<JsonObject> message) {
                                        JsonObject body = message.body();
                                        if ("ok".equals(body.getString("status"))) {
                                            final String idEtablissement = ((JsonObject) body.getJsonArray("results")
                                                    .getJsonObject(0)).getString("idStructure");
                                            UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                                                @Override
                                                public void handle(final UserInfos user) {
                                                    final boolean isChefEtab;
                                                    if(user != null) {
                                                        isChefEtab =  new WorkflowActionUtils().hasRight(user,
                                                                WorkflowActions.ADMIN_RIGHT.toString());
                                                    }
                                                    else {
                                                        isChefEtab = false;
                                                    }
                                                    WorkflowActionUtils.hasHeadTeacherRight(user,
                                                            new JsonArray().add(idClasse), null, null,
                                                            null, null, null,
                                                            new Handler<Either<String, Boolean>>() {
                                                                @Override
                                                                public void handle(Either<String, Boolean> event) {
                                                                    Boolean isHeadTeacher;
                                                                    if(event.isLeft()) {
                                                                        isHeadTeacher = false;
                                                                    }
                                                                    else {
                                                                        isHeadTeacher = event.right().getValue();
                                                                    }
                                                                    getExportRecapUtils (user,idEtablissement,idCycle,
                                                                            text,json,idClasse,finalIdPeriode,
                                                                            request ,isChefEtab || isHeadTeacher);
                                                                }
                                                            });

                                                }
                                            });
                                        } else {
                                            leftToResponse(request, new Either.Left<>("etab not found"));
                                        }
                                    }
                                }));
                    }
                } else {
                    leftToResponse(request, stringJsonArrayEither.left());
                }
            }
        });
    }

    private void getExportRecapUtils (final UserInfos user, final String idEtablissement, final Long idCycle,
                                      final Boolean text, final Boolean json, final String idClasse,
                                      final Long finalIdPeriode, final HttpServerRequest request ,
                                      final Boolean isChefEtab) {

        if ((user != null) || isChefEtab) {
            //idVisibility = 1 pour la visibilité de la moyBFC
            bfcService.getVisibility(idEtablissement,1,
                    user, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                            if (stringJsonArrayEither.isRight() || isChefEtab) {
                                boolean moy = false;
                                Number state = null;
                                if (stringJsonArrayEither.isRight()) {
                                    JsonArray result = stringJsonArrayEither.right().getValue();
                                    JsonObject obj = (JsonObject) result.getJsonObject(0);
                                    state = (obj).getInteger("visible");
                                }
                                if (state != null
                                        && Competences.BFC_AVERAGE_VISIBILITY_NONE.equals(state)) {
                                    moy = false;
                                } else if (state != null && isChefEtab &&
                                        Competences.BFC_AVERAGE_VISIBILITY_FOR_ADMIN_ONLY
                                                .equals(state)) {
                                    moy = true;
                                } else if (state != null &&
                                        Competences.BFC_AVERAGE_VISIBILITY_FOR_ALL
                                                .equals(state)) {
                                    moy = true;
                                }
                                final boolean isHabilite = moy;

                                exportService.getExportRecapEval(text, idCycle, idEtablissement,
                                        new Handler<Either<String, JsonArray>>() {

                                            @Override
                                            public void handle(final Either<String, JsonArray> stringJsonObjectEither) {
                                                if (stringJsonObjectEither.isRight()) {
                                                    try {
                                                        final JsonObject result = new JsonObject();

                                                        final JsonArray legende = stringJsonObjectEither.right().getValue();
                                                        result.put("legende", legende);
                                                        String atteint_calcule =
                                                                new String(("Valeurs affichées par domaine : niveau atteint " +
                                                                        "+ niveau calculé").getBytes(), StandardCharsets.UTF_8);
                                                        String atteint =
                                                                new String("Valeurs affichées par domaine : niveau atteint"
                                                                        .getBytes(), StandardCharsets.UTF_8);
                                                        result.put("displayMoy", isHabilite ? atteint_calcule : atteint);

                                                        final JsonObject action = new JsonObject()
                                                                .put("action", "classe.getEleveClasse")
                                                                .put("idClasse", idClasse)
                                                                .put("idPeriode", finalIdPeriode);

                                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                                                                new Handler<Message<JsonObject>>() {
                                                                    @Override
                                                                    public void handle(Message<JsonObject> message) {
                                                                        JsonObject body = message.body();

                                                                        if ("ok".equals(body.getString("status"))) {
                                                                            JsonArray eleves = body.getJsonArray("results");

                                                                            final String[] idEleves = new String[eleves.size()];
                                                                            final String[] nameEleves = new String[eleves.size()];
                                                                            for (int i = 0; i < eleves.size(); i++) {
                                                                                idEleves[i] = ((JsonObject) eleves.getJsonObject(i))
                                                                                        .getString("id");
                                                                                nameEleves[i] = ((JsonObject) eleves.getJsonObject(i))
                                                                                        .getString("lastName") + " "
                                                                                        + ((JsonObject) eleves.getJsonObject(i))
                                                                                        .getString("firstName");
                                                                            }
                                                                            boolean recapEval = true;
                                                                            bfcService.buildBFC(recapEval, idEleves, idClasse,
                                                                                    idEtablissement, finalIdPeriode, idCycle,
                                                                                    new Handler<Either<String, JsonObject>>() {
                                                                                        @Override
                                                                                        public void handle(Either<String, JsonObject> stringMapEither) {
                                                                                            if (stringMapEither.isRight()) {
                                                                                                final JsonArray eleves = new fr.wseduc.webutils.collections.JsonArray();
                                                                                                JsonObject bfc = stringMapEither.right().getValue();
                                                                                                if (bfc.size() > 0) {
                                                                                                    final int[] idDomaines = new int[bfc.getJsonArray("domainesRacine").size()];
                                                                                                    for (int l = 0; l < bfc.getJsonArray("domainesRacine").size(); l++) {
                                                                                                        Long idDomaine = bfc.getJsonArray("domainesRacine").getLong(l);
                                                                                                        idDomaines[l] = idDomaine.intValue();
                                                                                                    }

                                                                                                    for (int i = 0; i < idEleves.length; i++) {
                                                                                                        JsonObject eleve = new JsonObject();
                                                                                                        JsonArray notesEleve = new fr.wseduc.webutils.collections.JsonArray();
                                                                                                        List domainesEvalues = new ArrayList();
                                                                                                        if (bfc.containsKey(idEleves[i])) {
                                                                                                            for (Object resultNote : bfc.getJsonArray(idEleves[i])) {
                                                                                                                for (Object niveau : legende) {
                                                                                                                    JsonObject note = new JsonObject();

                                                                                                                    if (((JsonObject) resultNote).getValue("niveau").toString()
                                                                                                                            .equals(((JsonObject) niveau).getLong("ordre").toString())) {
                                                                                                                        note.put("id", ((JsonObject) resultNote).getInteger("idDomaine"));
                                                                                                                        note.put("visu", ((JsonObject) niveau).getString("visu"));
                                                                                                                        note.put("nonEvalue", false);
                                                                                                                        String moyCalcule = new DecimalFormat("#0.00").format(((JsonObject) resultNote).getDouble("moyenne").doubleValue());
                                                                                                                        if (isHabilite)
                                                                                                                            note.put("moyenne", text ? "- " + moyCalcule
                                                                                                                                    : "" + moyCalcule);

                                                                                                                        domainesEvalues.add(((JsonObject) note).getInteger("id").intValue());
                                                                                                                        notesEleve.add(note);
                                                                                                                    }
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                        addMaitriseNE(domainesEvalues, notesEleve, idDomaines, text);
                                                                                                        eleve.put("id", idEleves[i]);
                                                                                                        eleve.put("nom", nameEleves[i]);
                                                                                                        eleve.put("notes", sortJsonArrayById(notesEleve));
                                                                                                        eleves.add(eleve);
                                                                                                    }

                                                                                                    domaineService.getDomainesLibCod(idDomaines, new Handler<Either<String, JsonArray>>() {
                                                                                                        @Override
                                                                                                        public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                                                                                                            if (stringJsonArrayEither.isRight()) {
                                                                                                                JsonArray domaines = stringJsonArrayEither.right().getValue();
                                                                                                                result.put("domaines", isDomaineParent(sortJsonArrayById(domaines)));
                                                                                                                result.put("eleves", eleves);
                                                                                                                JsonObject action = new JsonObject()
                                                                                                                        .put("action", "classe.getClasseInfo")
                                                                                                                        .put("idClasse", idClasse);
                                                                                                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                                                                    @Override
                                                                                                                    public void handle(Message<JsonObject> message) {
                                                                                                                        JsonObject body = message.body();

                                                                                                                        if ("ok".equals(body.getString("status"))) {
                                                                                                                            final String classeName = body.getJsonObject("result").getJsonObject("c").getJsonObject("data").getString("name");
                                                                                                                            result.put("classe", classeName);

                                                                                                                            JsonObject jsonRequest = new JsonObject()
                                                                                                                                    .put("headers", new JsonObject().put("Accept-Language", request.headers().get("Accept-Language")))
                                                                                                                                    .put("Host", getHost(request));
                                                                                                                            JsonObject action = new JsonObject()
                                                                                                                                    .put("action", "periode.getLibellePeriode")
                                                                                                                                    .put("idType", finalIdPeriode)
                                                                                                                                    .put("request", jsonRequest);
                                                                                                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                                                                                @Override
                                                                                                                                public void handle(Message<JsonObject> message) {
                                                                                                                                    JsonObject body = message.body();

                                                                                                                                    if ("ok".equals(body.getString("status"))) {
                                                                                                                                        String libellePeriode = body.getString("result")
                                                                                                                                                .replace("é", "e")
                                                                                                                                                .replace("è", "e");
                                                                                                                                        result.put("periode", libellePeriode);
                                                                                                                                        result.put("text", text);
                                                                                                                                        result.put("isHabilite", isHabilite);
                                                                                                                                        if (json) {
                                                                                                                                            Renders.renderJson(request, result);
                                                                                                                                        } else {
                                                                                                                                            String fileName = classeName.replace(' ', '_') + "_export_recapitulatif";
                                                                                                                                            exportService.genererPdf(request, result,
                                                                                                                                                    "recapitulatif-evaluations.pdf.xhtml", fileName, vertx, config);
                                                                                                                                        }
                                                                                                                                    } else {
                                                                                                                                        leftToResponse(request, new Either.Left<>("periode not found")); //leftToResponse(request, new Either.Left<>(body.getString("message")));
                                                                                                                                    }
                                                                                                                                }
                                                                                                                            }));
                                                                                                                        } else {
                                                                                                                            leftToResponse(request, new Either.Left<>("classe not found"));
                                                                                                                        }
                                                                                                                    }
                                                                                                                }));
                                                                                                            } else {
                                                                                                                leftToResponse(request, stringJsonArrayEither.left());
                                                                                                            }
                                                                                                        }
                                                                                                    });
                                                                                                } else {
                                                                                                    leftToResponse(request, new Either.Left<>("eval not found"));
                                                                                                }
                                                                                            } else {
                                                                                                leftToResponse(request, new Either.Left<>("bfc not found"));
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        } else {
                                                                            leftToResponse(request, new Either.Left<>("eleves not found"));
                                                                        }
                                                                    }
                                                                }));
                                                    } catch (Error err) {
                                                        leftToResponse(request, new Either.Left<>("An error occured while rendering pdf export : " + err.getMessage()));
                                                    }
                                                } else {
                                                    leftToResponse(request, stringJsonObjectEither.left());
                                                }
                                            }
                                        });
                            } else {
                                leftToResponse(request, stringJsonArrayEither.left());
                            }
                        }
                    });
        } else {
            badRequest(request);
        }
    }

    private Handler<Either<String, JsonObject>> getReleveCompetences(final HttpServerRequest request,
                                                                     final Map<String, String> elevesMap,
                                                                     final Map<String, String> nomGroupes,
                                                                     final String matieres,
                                                                     final String libellePeriode, final Boolean json,
                                                                     final AtomicBoolean answered,
                                                                     final JsonArray result) {

        final AtomicInteger elevesDone = new AtomicInteger();
        final AtomicInteger elevesAdd = new AtomicInteger();

        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(final Either<String, JsonObject> stringJsonArrayEither) {
                if (!answered.get()) {
                    if (stringJsonArrayEither.isRight()) {
                        try {
                            JsonObject res = stringJsonArrayEither.right().getValue();
                            Boolean noDevoir =  res.getBoolean("noDevoir");
                            if (!noDevoir) {
                                final JsonObject headerEleve = new JsonObject();
                                final JsonObject _headerEleve = stringJsonArrayEither.right().getValue();
                                final String idEleve = stringJsonArrayEither.right().getValue()
                                        .getString("idEleve");
                                if (elevesMap.containsKey(idEleve)) {
                                    stringJsonArrayEither.right().getValue()
                                            .put("nom", elevesMap.get(idEleve));
                                    headerEleve.put("nom", elevesMap.get(idEleve));
                                }
                                headerEleve.put("classe", nomGroupes.get(idEleve));
                                headerEleve.put("matiere", matieres);
                                headerEleve.put("periode", libellePeriode);
                                JsonObject header = _headerEleve.getJsonObject("header");
                                if (header != null) {
                                    header.put("left", headerEleve);
                                }
                                result.add(_headerEleve);
                                elevesAdd.addAndGet(1);
                            }

                            if (elevesDone.addAndGet(1) == elevesMap.size()) {
                                answered.set(true);
                                JsonObject resultFinal = new JsonObject();
                                resultFinal.put("eleves", sortJsonArrayByName(result));
                                if ( 0 == result.size()){
                                    leftToResponse(request,
                                            new Either.Left<>("getExportReleveComp : No exams " +
                                                    "on given period and/or material."));
                                }
                                else if (json) {
                                    Renders.renderJson(request, result);
                                } else {
                                    final String idEleve = stringJsonArrayEither.right().getValue()
                                            .getString("idEleve");
                                    final String _nomGroupe = nomGroupes.get(idEleve);
                                    String fileName = elevesDone.get() == 1 ? elevesMap.get(idEleve)
                                            .replace(' ', '_') + "_export_competences"
                                            : _nomGroupe.replace(' ', '_') + "_export_competences";
                                    exportService.genererPdf(request, resultFinal,
                                            "releve-competences.pdf.xhtml", fileName, vertx, config);
                                }

                            }
                        } catch (Error err) {
                            leftToResponse(request,
                                    new Either.Left<>("An error occured while rendering pdf export : "
                                            + err.getMessage()));
                        }
                    }
                } else {
                    answered.set(true);
                    leftToResponse(request, stringJsonArrayEither.left());
                }
            }
        };
    }

    private JsonArray sortJsonArrayById(JsonArray jsonArray) {
        List<JsonObject> jsonValues = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject p = jsonArray.getJsonObject(i);
            jsonValues.add(p);
        }
        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "id";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                Long valA = 0L;
                Long valB = 0L;
                try {
                    valA = (Long) a.getLong(KEY_NAME);
                    valB = (Long) b.getLong(KEY_NAME);
                } catch (Exception e) {
                    //do something
                }
                return valA.compareTo(valB);
            }
        });

        JsonArray sortedJsonArray = new fr.wseduc.webutils.collections.JsonArray();
        for (JsonObject o : jsonValues) {
            sortedJsonArray.add(o);
        }
        return sortedJsonArray;
    }

    private JsonArray sortJsonArrayByName(JsonArray jsonArray) {
        List<JsonObject> jsonValues = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject p = jsonArray.getJsonObject(i);
            if (!p.getBoolean("noDevoir")) {
                jsonValues.add(p);
            }

        }
        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "nom";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    valA = a.getString(KEY_NAME);
                    valB = b.getString(KEY_NAME);
                } catch (Exception e) {
                    //do something
                }
                return valA.compareTo(valB);
            }
        });

        JsonArray sortedJsonArray = new fr.wseduc.webutils.collections.JsonArray();
        for (JsonObject o : jsonValues) {

            sortedJsonArray.add(o);
        }
        return sortedJsonArray;
    }

    private JsonArray isDomaineParent(JsonArray domaines) {
        JsonArray newDomaines = new fr.wseduc.webutils.collections.JsonArray();
        for (int k = 0; k < domaines.size(); k++) {
            JsonObject domaine = domaines.getJsonObject(k);
            if ("0".equals(domaine.getInteger("id_parent").toString()) || k == 0) {
                domaine.put("isDomaineParent", true);
//                if(!"0".equals(domaine.getNumber("id_parent").toString()))
//                    domaine.put("nomDomaine", "Domaine D" + domaine.getNumber("id_parent").toString());
//                else
//                    domaine.put("nomDomaine", "Domaine " + domaine.getString("codification"));

            } else {
                domaine.put("isDomaineParent", false);
            }
            newDomaines.add(domaine);
        }
        return newDomaines;
    }

    private void addMaitriseNE(List domainesEvalues, JsonArray notesEleve, int[] idDomaines, boolean text) {
        for (int idDomaine : idDomaines) {
            if (!domainesEvalues.contains(idDomaine)) {
                JsonObject note = new JsonObject();
                note.put("id", new Long(idDomaine));
                note.put("visu", text ? "NE" : "white");
                note.put("nonEvalue", true);
                notesEleve.add(note);
            }
        }
    }

    @Post("/export/bulletins")
    @SecuredAction(value = "export.bulletins.periodique", type = ActionType.WORKFLOW)
    public void exportBulletins(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject params) {
                Long idPeriode = params.getLong(ID_PERIODE_KEY);
                JsonArray idStudents = params.getJsonArray("idStudents");

                Boolean showBilanPerDomaines = params.getBoolean("showBilanPerDomaines");
                Boolean threeLevel = params.getBoolean("threeLevel");
                Boolean threeMoyenneClasse = params.getBoolean("threeMoyenneClasse");
                Boolean threeMoyenneEleve = params.getBoolean("threeMoyenneEleve");
                Boolean threePage = params.getBoolean("threePage");

                String idClasse = params.getString(ID_CLASSE_KEY);
                String idEtablissement = params.getString(ID_STRUCTURE_KEY);
                Boolean useModel = params.getBoolean(USE_MODEL_KEY);

                // On récupère le tableau de conversion des compétences notes pour Lire le positionnement
                Future<JsonArray> tableauDeConversionFuture = Future.future();
                competenceNoteService.getConversionNoteCompetence(idEtablissement, idClasse,
                        tableau -> FormateFutureEvent.formate(tableauDeConversionFuture, tableau));

                // On récupère les informations basic des élèves (nom, Prenom, niveau, date de naissance,  ...)
                JsonObject action = new JsonObject()
                        .put("action", "eleve.getInfoEleve")
                        .put(ID_ETABLISSEMENT_KEY, idEtablissement)
                        .put("idEleves", idStudents);
                Future<JsonArray> elevesFuture = Future.future();
                eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                        message -> {
                            if ("ok".equals(message.body().getString("status"))) {
                                elevesFuture.complete( message.body().getJsonArray("results"));

                            }
                            else {
                                elevesFuture.fail(message.body().getString("message"));
                            }
                        }));
                // Si on doit utiliser un model de libelle, On le récupère
                Future<JsonArray> modelsLibelleFuture = Future.future();
                Long idModel;
                if(useModel) {
                    idModel = params.getLong("idModel");
                    new DefaultMatiereService(eb).getModels(idEtablissement, idModel, models -> {
                        FormateFutureEvent.formate(modelsLibelleFuture, models);
                    });
                }
                else {
                    modelsLibelleFuture.complete(new JsonArray());
                }
                // Lorsqu'on a le suivi des Acquis et le tableau de conversion, on lance la récupération
                // complète des données de l'export
                CompositeFuture.all(tableauDeConversionFuture, elevesFuture, modelsLibelleFuture)
                        .setHandler( (event -> {
                            if (event.succeeded()) {
                                final JsonArray eleves = elevesFuture.result();
                                final JsonObject classe =
                                        new JsonObject().put("tableauDeConversion", tableauDeConversionFuture.result());

                                if(useModel) {
                                    JsonArray models = modelsLibelleFuture.result();
                                    if(!models.isEmpty()){
                                        models = models.getJsonObject(0).getJsonArray(SUBJECTS);
                                    }
                                    classe.put("models", models);
                                }

                                final Map<String, JsonObject> elevesMap = new LinkedHashMap<>();
                                final AtomicBoolean answered = new AtomicBoolean();
                                final Handler<Either<String, JsonObject>> finalHandler
                                        = exportBulletinService.getFinalBulletinHandler(request, elevesMap, vertx,
                                        config, eleves.size(), answered, params);

                                exportBulletinService.buildDataForStudent(request, answered, eleves,
                                         elevesMap,  idPeriode,  params, classe,  showBilanPerDomaines, finalHandler);
                            }
                            else {
                                Renders.notFound(request, event.cause().getMessage());
                            }
                        }
                        ));

            }
        });
    }

    @Get("/suiviClasse/tableau/moyenne/:idClasse/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void exportBulletinMoyennneOnly(HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos eventUser) {
                if (eventUser == null) {
                    unauthorized(request);
                } else {

                    String idClasse = request.params().get("idClasse");

                    Boolean withMoyGeneraleByEleve = Boolean.valueOf(request.params().get("withMoyGeneraleByEleve"));
                    Boolean withMoyMinMaxByMat = Boolean.valueOf(request.params().get("withMoyMinMaxByMat"));
                    Boolean text = Boolean.parseBoolean(request.params().get("text"));

                    Integer idPeriode = null;
                    try {
                        if (request.params().contains("idPeriode")) {
                            idPeriode = Integer.parseInt(request.params().get("idPeriode"));
                        }
                    } catch (NumberFormatException err) {
                        badRequest(request, err.getMessage());
                        log.error(err);
                        return;
                    }
                    final Integer idPeriodeFinal = idPeriode;

                    SortedMap<String, Set<String>> mapAllidMatAndidTeachers = new TreeMap<>();
                    Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve = new LinkedHashMap<>();

                    noteService.getMoysEleveByMat(idClasse, idPeriode,
                            mapAllidMatAndidTeachers,
                            mapIdMatListMoyByEleve ,
                            new Handler<Either<String, JsonObject>>() {
                                @Override
                                public void handle(Either<String, JsonObject> event) {

                                    if(!event.isRight()){
                                        leftToResponse(request, event.left());
                                        log.error(event.left());
                                    }else {
                                        JsonObject resultEleves = event.right().getValue();

                                        noteService.getMatEvaluatedAndStat(mapAllidMatAndidTeachers, mapIdMatListMoyByEleve, new Handler<Either<String, JsonObject>>() {
                                            @Override
                                            public void handle(Either<String, JsonObject> event) {

                                                if(!event.isRight()) {
                                                    leftToResponse(request, event.left());
                                                }else {

                                                    JsonObject resultMatieres = event.right().getValue();

                                                    resultEleves.getMap().putAll(resultMatieres.getMap());
                                                    JsonObject result = new JsonObject(resultEleves.getMap());

                                                    if(idPeriodeFinal != null) {

                                                        Utils.getLibellePeriode(eb, request, idPeriodeFinal, new Handler<Either<String, String>>() {
                                                            @Override
                                                            public void handle(Either<String, String> event) {
                                                                if (!event.isRight()) {
                                                                    leftToResponse(request, event.left());
                                                                } else {
                                                                    String libellePeriode = event.right().getValue();

                                                                    result.put("periode", libellePeriode);


                                                                    String prefix = result.getJsonArray("eleves").getJsonObject(0).getString("nameClasse");
                                                                    result.put("nameClass", prefix);
                                                                    prefix += "_" + libellePeriode;
                                                                    result.put("withMoyGeneraleByEleve", withMoyGeneraleByEleve);
                                                                    result.put("withMoyMinMaxByMat", withMoyMinMaxByMat);
                                                                    result.put("text", text);
                                                                    exportService.genererPdf(request,
                                                                            result,
                                                                            "recap_moys_eleves_par_matiere_classe.pdf.xhtml",
                                                                            prefix,
                                                                            vertx,
                                                                            config);

                                                                }
                                                            }
                                                        });
                                                    }else{
                                                        result.put("periode", "Année");
                                                        String prefix = result.getJsonArray("eleves").getJsonObject(0).getString("nameClass");
                                                        result.put("nameClass", prefix);
                                                        prefix += "_" + "Année";
                                                        result.put("withMoyGeneraleByEleve", withMoyGeneraleByEleve);
                                                        result.put("withMoyMinMaxByMat", withMoyMinMaxByMat);
                                                        result.put("text", text);
                                                        exportService.genererPdf(request,
                                                                result,
                                                                "recap_moys_eleves_par_matiere_classe.pdf.xhtml",
                                                                prefix,
                                                                vertx,
                                                                config);

                                                    }
                                                }
                                            }
                                        });
                                    }
                                }
                            });

                }
            }

        });
    }
}