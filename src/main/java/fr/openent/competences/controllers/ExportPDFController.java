/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.bean.Eleve;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class ExportPDFController extends ControllerHelper {
    private String assetsPath = "../..";
    private Map<String, String> skins = new HashMap<String, String>();
    protected static final Logger log = LoggerFactory.getLogger(ExportPDFController.class);


    private String node;

    /**
     * Déclaration des services
     */
    private DevoirService devoirService;
    private UtilsService utilsService;
    private BFCService bfcService;
    private DomainesService domaineService;
    private CompetenceNoteService competenceNoteService;
    private CompetencesService competencesService;
    private NiveauDeMaitriseService niveauDeMaitriseService;
    private ExportService exportService;

    public ExportPDFController(EventBus eb, EmailSender notification) {
        devoirService = new DefaultDevoirService();
        utilsService = new DefaultUtilsService();
        bfcService = new DefaultBFCService(eb);
        domaineService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        competencesService = new DefaultCompetencesService(eb);
        niveauDeMaitriseService = new DefaultNiveauDeMaitriseService();
        exportService = new DefaultExportService(eb);
    }

    /**
     * Récupère le nom des enseignants de chacune des matières puis positionne
     * les devoirs de l'élève sur les bonnes matières et enfin génère le PDF associé
     * formant le relevé de notes de l'élève.
     *
     * @param request
     * @param user l'utilisateur connecté.
     * @param matieres la liste des matières de l'élève.
     * @param classe
     * @param idUsers
     * @param devoirsJson la liste des devoirs et notes de l'élève.
     * @param periodeJson la periode
     * @param userJson l'élève
     * @param etabJson l'établissement
     */
    public void getEnseignantsMatieres(final HttpServerRequest request, final UserInfos user, final JsonArray matieres,
                                       final String classe, JsonArray idUsers, final JsonArray devoirsJson,
                                       final JsonObject periodeJson, final JsonObject userJson, final JsonObject etabJson) {

        JsonObject action = new JsonObject()
                .putString("action", "eleve.getUsers")
                .putArray("idUsers", idUsers);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                JsonArray matieresArray = new JsonArray();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray r = body.getArray("results");

                    for(int index = 0; index < matieres.size(); index++) {
                        JsonObject matiereDevoir = matieres.get(index);
                        matieresArray.add(matiereDevoir.getObject("data").getObject("data"));
                    }

                    for(int i = 0 ; i < devoirsJson.size(); i++){
                        JsonObject devoir = devoirsJson.get(i);
                        // Récupération de l'enseignant du devoir
                        JsonObject enseignantDevoir = null;
                        for(int j = 0; j < r.size(); j++) {
                            enseignantDevoir = r.get(j);
                            if(enseignantDevoir.getString("id").equals(devoir.getString("owner"))){
                                break;
                            }
                        }
                        if(enseignantDevoir != null) {
                            // Récupération de la matière
                            for (int k = 0; k < matieresArray.size(); k++) {
                                JsonObject matiereDevoir = matieresArray.get(k);
                                getDevoirsByMatiere(devoirsJson, matiereDevoir);

                                if (matiereDevoir.getString("id").equals(devoir.getString("id_matiere"))) {
                                    String firstNameEnsiegnant = enseignantDevoir.getString("firstName");
                                    String displayName = firstNameEnsiegnant.substring(0,1) + ".";
                                    displayName = displayName + enseignantDevoir.getString("name");

                                    if (matiereDevoir.getArray("displayNameEnseignant") == null) {
                                        matiereDevoir.putArray("displayNameEnseignant", new JsonArray().addString(displayName));
                                    } else {
                                        JsonArray _enseignantMatiere = matiereDevoir.getArray("displayNameEnseignant");
                                        if (!_enseignantMatiere.contains(displayName)) {
                                            _enseignantMatiere.addString(displayName);
                                            matiereDevoir.putArray("displayNameEnseignant", _enseignantMatiere);
                                        }
                                    }
                                }
                            }
                        }
                    }



                    final JsonObject templateProps = new JsonObject();

                    templateProps.putArray("matieres", matieresArray);
                    templateProps.putObject("periode", periodeJson);
                    templateProps.putObject("user", userJson.getObject("u").getObject("data"));
                    templateProps.putObject("classe", userJson.getObject("c").getObject("data"));
                    templateProps.putObject("etablissement", etabJson);
                    String templateName = "releve-eleve.pdf.xhtml";

                    String prefixPdfName = "releve-eleve";
                    prefixPdfName+= "-" + userJson.getObject("u").getObject("data").getString("displayName");
                    prefixPdfName+= "-" + userJson.getObject("c").getObject("data").getString("name");

                    String etablissementName = etabJson.getString("name");
                    etablissementName = etablissementName.trim().replaceAll(" ", "-");
                    prefixPdfName+= "-" + etablissementName;

                    genererPdf(request, templateProps, templateName, prefixPdfName);

                }else{
                    leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                }
            }
        });
    }

    /**
     * Récupère les devoirs de la matière et les positionnent sur celle ci.
     *
     * @param devoirsJson la liste de tous les devoirs de l'élève.
     * @param matiereInter la matière dont on cherche les devoirs.
     */
    private void getDevoirsByMatiere(JsonArray devoirsJson, JsonObject matiereInter) {

        JsonArray devoirsMatiereJson = new JsonArray();

        List<NoteDevoir> listeNoteDevoirs = new ArrayList<NoteDevoir>();

        // parcours des devoirs
        for (int i = 0; i < devoirsJson.size(); i++) {
            JsonObject devoirJson = devoirsJson.get(i);

            // boolean permettant de savoir s'il y a un coefficient différent de 1 sur la note
            devoirJson.putBoolean("hasCoeff", !Double.valueOf(devoirJson.getString("coefficient")).equals(new Double(1)));

            // ajout du devoir sur la matiere, si son identifiant de matière correspond bien
            if(matiereInter.getString("id").equals(devoirJson.getString("id_matiere"))) {
                devoirsMatiereJson.add(devoirJson);
                Double note = Double.valueOf(devoirJson.getString("note"));
                Double diviseur = Double.valueOf(devoirJson.getInteger("diviseur"));
                Boolean ramenerSur = devoirJson.getBoolean("ramener_sur");
                Double coefficient = Double.valueOf(devoirJson.getString("coefficient"));

                NoteDevoir noteDevoir = new NoteDevoir(note,diviseur,ramenerSur,coefficient);
                listeNoteDevoirs.add(noteDevoir);
            }
        }
        matiereInter.putArray("devoirs", devoirsMatiereJson);

        boolean hasDevoirs = !listeNoteDevoirs.isEmpty();
        matiereInter.putBoolean("hasDevoirs", hasDevoirs);

        if(hasDevoirs) {
            // calcul de la moyenne de l'eleve pour la matiere
            JsonObject moyenneMatiere = utilsService.calculMoyenne(listeNoteDevoirs, false, 20);// TODO recuper le diviseur de la matiere
            // ajout sur l'objet json
            matiereInter.putString("moyenne", moyenneMatiere.getNumber("moyenne").toString());
        }
    }

    /**
     * Genere le releve d'un eleve sous forme de PDF
     *
     */
    @Get("/releve/pdf")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void getReleveEleve(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){

                    // parametres de l'url
                    final MultiMap params = request.params();

                    final Long idPeriode;
                    if(params.get("idPeriode")!= null) {
                        try {
                            idPeriode = Long.parseLong(params.get("idPeriode"));
                        } catch (NumberFormatException e) {
                            log.error("Error : idPeriode must be a long object", e);
                            badRequest(request, e.getMessage());
                            return;
                        }
                    }
                    else{
                        idPeriode = null;
                    }

                    final String idEtablissement = params.get("idEtablissement");
                    final String idUser = params.get("idUser");


                    // TODO verifier que l'utilisateur connecte est bien l'eleve dont essaie d'acceder au releve ou que
                    // le parent connecte essaie bien d'acceder au releve d'un de ses eleves

                    // récupération de l'élève
                    utilsService.getInfoEleve(idUser, new Handler<Either<String,JsonObject>>() {

                        @Override
                        public void handle(Either<String, JsonObject> eventUser) {
                            if(eventUser.isRight()) {
                                final JsonObject userJSON = eventUser.right().getValue();

                                final String classeEleve = userJSON.getObject("u").getObject("data").getArray("classes").get(0);

                                // Récupération de la liste des devoirs de la personne avec ses notes associées
                                devoirService.listDevoirs(idUser, idEtablissement, null, null, idPeriode, new Handler<Either<String, JsonArray>>() {
                                    @Override
                                    public void handle(final Either<String, JsonArray> eventListDevoirs) {
                                        if(eventListDevoirs.isRight()){

                                            // devoirs de l'eleve (avec ses notes) sous forme d'objet JSON
                                            final JsonArray devoirsJSON = eventListDevoirs.right().getValue();
                                            final JsonArray idMatieres = new JsonArray();
                                            final JsonArray idEnseignants = new JsonArray();
                                            for (int i=0; i < devoirsJSON.size(); i++){
                                                JsonObject devoir = devoirsJSON.get(i);
                                                idMatieres.add(devoir.getValue("id_matiere"));
                                                idEnseignants.add(devoir.getValue("owner"));
                                            }
                                            // récupération de l'ensemble des matières de l'élève

                                            JsonObject action = new JsonObject()
                                                    .putString("action", "matiere.getMatieres")
                                                    .putArray("idMatieres", idMatieres);

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    JsonObject body = message.body();

                                                    if ("ok".equals(body.getString("status"))) {
                                                        JsonArray r = body.getArray("results");
                                                        final ArrayList<String> classesFieldOfStudy = new ArrayList<String>();
                                                        String key = new String();
                                                        JsonObject f = new JsonObject();
                                                        final JsonArray matieres = r;

                                                        for(int i = 0; i < r.size(); i++){
                                                            JsonObject o = r.get(i);
                                                            key = classeEleve+"$"+o.getString("externalId");
                                                            classesFieldOfStudy.add(key);
                                                        }


                                                        // recuperation etablissement
                                                        utilsService.getStructure(idEtablissement, new Handler<Either<String,JsonObject>>() {

                                                            @Override
                                                            public void handle(Either<String, JsonObject> eventStructure) {
                                                                if(eventStructure.isRight()) {
                                                                    final JsonObject etabJSON = eventStructure.right().getValue().getObject("s").getObject("data");
                                                                    final JsonObject periodeJSON= new JsonObject();

                                                                    if(null != params.get("idTypePeriode")
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
                                                                        periodeJSON.putString("libelle", libellePeriode);
                                                                    }else {
                                                                        // Construction de la période année
                                                                        periodeJSON.putString("libelle", "Ann\u00E9e");
                                                                    }
                                                                    getEnseignantsMatieres(request, user, matieres,
                                                                            classeEleve, idEnseignants, devoirsJSON,
                                                                            periodeJSON, userJSON, etabJSON);
                                                                }
                                                            }

                                                        }); // fin getPeriode



                                                    }else{
                                                        leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                    }
                                                }
                                            });

                                        }else{
                                            leftToResponse(request, eventListDevoirs.left());
                                        }

                                    } // fin handle listDevoirs
                                }); // fin lisDevoirs
                            }
                        }
                    }); // fin récupération élève
                }else{
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
     * @param classe  La liste des Eleves de la classe.
     * @return        Un JsonArray contenant les JsonObject de tous les Eleves de la classe; null si un
     *                Eleve n'est pas pret.
     *
     * @see Eleve
     */
    private JsonArray formatBFC(List<Eleve> classe) {
        JsonArray result = new JsonArray();

        for(Eleve eleve : classe) {
            if(!eleve.isReady()) {
                return null;
            }
            result.addObject(eleve.toJson());
        }

        return result;
    }

    /**
     * Ajoute le JsonObject a <code>collection</code>, et si les JsonObject de toutes les classes ont ete
     * renseignees dans <code>collection</code>, assemble tous JsonObject au sein d'un JsonArray qui sera fournit
     * au handler.
     *
     * @param key         Identifiant de la classe a ajoute dans <code>collection</code>.
     * @param value       JsonObject de la classe a ajoute dans <code>collection</code>.
     * @param collection  Map des JsonObject de toutes les classes, indexant par leur identifiant.
     * @param handler     Handler manipulant le JsonArray lorsque celui-ci est assemble.
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
            JsonArray result = new JsonArray();
            for(JsonObject classe : collection.values()) {
                result.addObject(classe);
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
     * @param classes      L'ensemble des Eleves, rassembles par classe et indexant en fonction de l'identifiant de la
     *                     classe
     * @param idStructure  L'identifiant de la structure. Necessaire afin de recuperer l'echelle de conversion.0
     * @param idPeriode    L'identifiant de la periode pour laquelle on souhaite recuperer le BFC.
     * @param handler      Handler contenant le BFC final.
     *
     * @see Eleve
     */
    private void getBFCParClasse(final Map<String, List<Eleve>> classes, final String idStructure, Long idPeriode, final Handler<Either<String, JsonArray>> handler) {

        // Contient toutes les classes sous forme JsonObject, indexant en fontion de l'identifiant de la classe
        // correspondante.
        final Map<String, JsonObject> result = new LinkedHashMap<>();

        // La map result avec les identifiants des classes, contenus dans "classes", afin de s'assurer qu'aucune ne
        // manque.
        for(String s : classes.keySet()) {
            result.put(s, null);
        }

        for (final Map.Entry<String, List<Eleve>> classe : classes.entrySet()) {

            final Map<Integer, String> libelleEchelle = new HashMap<>();
            final Map<String, Map<Long, Integer>> resultatsEleves = new HashMap<>();
            final Map<Long, Map<String, String>> domainesRacines = new LinkedHashMap<>();
            final List<String> idEleves = new ArrayList<>();

            // La liste des identifiants des Eleves de la classe est necessaire pour "buildBFC"
            for(Eleve e : classe.getValue()) {
                idEleves.add(e.getIdEleve());
            }

            competenceNoteService.getConversionNoteCompetence(idStructure, classe.getKey(), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isRight()) {
                        if(event.right().getValue().size() == 0) {
                            collectBFCEleve(classe.getKey(), new JsonObject().putString("error", "Une erreur est survenue lors de la recuperation de l'échelle de conversion pour la classe " + classe.getValue().get(0).getNomClasse() + " : aucune echelle de conversion pour cette classe."), result, handler);
                            log.error("getBFC : getConversionNoteCompetence (" + idStructure + ", " + classe.getKey() + ") : aucune echelle de conversion pour cette classe.");
                        }
                        for (int i = 0; i < event.right().getValue().size(); i++) {
                            JsonObject _o = event.right().getValue().get(i);
                            libelleEchelle.put(_o.getInteger("ordre"), _o.getString("libelle"));
                        }
                        for(Eleve e : classe.getValue()) {
                            e.setLibelleNiveau(libelleEchelle);
                        }
                        JsonArray classeResult = formatBFC(classe.getValue());
                        if (classeResult != null) { // classeResult est différent de null si tous les élèves de la classe ont tous les paramètres
                            collectBFCEleve(classe.getKey(), new JsonObject().putArray("eleves", classeResult), result, handler);
                        }
                    } else {
                        collectBFCEleve(classe.getKey(), new JsonObject().putString("error", "Une erreur est survenue lors de la recuperation de l'échelle de conversion pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                        log.error("getBFC : getConversionNoteCompetence (" + idStructure + ", " + classe.getKey() + ") : " + event.left().getValue());
                    }
                }
            });

            domaineService.getDomainesRacines(classe.getKey(), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        if(event.right().getValue().size() == 0) {
                            collectBFCEleve(classe.getKey(), new JsonObject().putString("error", "Une erreur est survenue lors de la recuperation des domaines pour la classe " + classe.getValue().get(0).getNomClasse() + " : aucun domaine racine pour cette classe."), result, handler);
                            log.error("getBFC : getDomainesRacines (" + classe.getKey() + ") : aucun domaine racine pour cette classe.");
                        }
                        JsonArray queryResult = event.right().getValue();
                        for (int i = 0; i < queryResult.size(); i++) {
                            JsonObject domaine = queryResult.get(i);
                            Map<String, String> infoDomaine = new HashMap<>();
                            infoDomaine.put("id", String.valueOf(domaine.getLong("id")));
                            infoDomaine.put("codification", domaine.getString("codification"));
                            infoDomaine.put("libelle", domaine.getString("libelle"));
                            domainesRacines.put(Long.valueOf(infoDomaine.get("id")), infoDomaine);
                        }
                        for (Eleve e : classe.getValue()) {
                            e.setDomainesRacines(domainesRacines);
                        }
                        JsonArray classeResult = formatBFC(classe.getValue());
                        if (classeResult != null) {
                            collectBFCEleve(classe.getKey(), new JsonObject().putArray("eleves", classeResult), result, handler);
                        }
                    } else {
                        collectBFCEleve(classe.getKey(), new JsonObject().putString("error", "Une erreur est survenue lors de la recuperation des domaines pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                        log.error("getBFC : getDomainesRacines (" + classe.getKey() + ") : " + event.left().getValue());
                    }
                }
            });

            bfcService.buildBFC(false, idEleves.toArray(new String[0]), classe.getKey(), idStructure, idPeriode,null, new Handler<Either<String, JsonObject>>(){
                @Override
                public void handle(final Either<String, JsonObject> event){
                        if (event.isRight()) {
                            JsonArray resultats = event.right().getValue().getArray(idEleves.get(0));
                            Map<Long, Integer> resultEleves  = new HashMap<>();
                            for (Object resultat : resultats){
                                resultEleves.put((Long)((JsonObject) resultat).getNumber("idDomaine"), (Integer)((JsonObject) resultat).getNumber("niveau"));
                            }
                            resultatsEleves.put(idEleves.get(0), resultEleves);
                            for (Eleve e : classe.getValue()) {
                                e.setNotes(resultatsEleves.get(e.getIdEleve()));
                            }
                            JsonArray classeResult = formatBFC(classe.getValue());
                            if (classeResult != null) {
                                collectBFCEleve(classe.getKey(), new JsonObject().putArray("eleves", classeResult), result, handler);
                            }
                        } else {
                            collectBFCEleve(classe.getKey(), new JsonObject().putString("error", "Une erreur est survenue lors de la recuperation des notes pour la classe : " + classe.getValue().get(0).getNomClasse() + ";\n" + event.left().getValue()), result, handler);
                            log.error("getBFC : buildBFC (Array of idEleves, " + classe.getKey() + ", " + idStructure + ") : " + event.left().getValue());
                        }
                    }
            });
        }
    }

    /**
     * Recupere l'identifiant de la structure a laquelle appartiennent les classes dont l'identifiant est passe en
     * parametre.
     *
     * @param idClasses  Tableau contenant l'identifiant des classes dont on souhaite connaitre la structure.
     * @param handler    Handler contenant l'identifiant de la structure.
     */
    private void getStructClasses(String[] idClasses, final Handler<Either<String, String>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "classe.getEtabClasses")
                .putArray("idClasses", new JsonArray(idClasses));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    JsonArray queryResult = body.getArray("results");
                    if(queryResult.size() == 0) {
                        handler.handle(new Either.Left<String, String>("Aucune classe n'a ete trouvee."));
                        log.error("getStructClasses : No classes found with these ids");
                    } else if(queryResult.size() > 1) {
                        // Il est impossible de demander un BFC pour des classes n'appartenant pas au meme etablissement.
                        handler.handle(new Either.Left<String, String>("Les classes n'appartiennent pas au meme etablissement."));
                        log.error("getStructClasses : provided classes are not from the same structure.");
                    } else {
                        JsonObject structure = queryResult.get(0);
                        handler.handle(new Either.Right<String,  String>(structure.getString("idStructure")));
                    }
                } else {
                    handler.handle(new Either.Left<String, String>(body.getString("message")));
                    log.error("getStructClasses : " + body.getString("message"));
                }
            }
        });
    }

    /**
     * Recupere l'identifiant de l'ensemble des classes de la structure dont l'identifiant est passe en parametre.
     *
     * @param idStructure  Identifiant de la structure dont on souhaite recuperer les classes.
     * @param handler      Handler contenant la liste des identifiants des classes recuperees.
     */
    private void getClassesStruct(final String idStructure, final Handler<Either<String, List<String>>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "classe.getClasseEtablissement")
                .putString("idEtablissement", idStructure);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    List<String> result = new ArrayList<>();
                    JsonArray queryResult = body.getArray("results");
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject classe = queryResult.get(i);
                        result.add(classe.getString("idClasse"));
                    }
                    handler.handle(new Either.Right<String, List<String>>(result));
                } else {
                    handler.handle(new Either.Left<String, List<String>>(body.getString("message")));
                    log.error("getClassesStruct : " + body.getString("message"));
                }
            }
        });
    }

    /**
     * Recupere l'identifiant de l'ensemble des eleves de la classe dont l'identifiant est passe en parametre.
     *
     * @param idClasses  Identifiant de la classe dont on souhaite recuperer les eleves.
     * @param handler    Handler contenant la liste des identifiants des eleves recuperees.
     */
    private void getElevesClasses(String[] idClasses, final Handler<Either<String, Map<String, List<String>>>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "classe.getElevesClasses")
                .putArray("idClasses", new JsonArray(idClasses));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    Map<String, List<String>> result = new LinkedHashMap<>();
                    JsonArray queryResult = body.getArray("results");
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject eleve = queryResult.get(i);
                        if(!result.containsKey(eleve.getString("idClasse"))) {
                            result.put(eleve.getString("idClasse"), new ArrayList<String>());
                        }
                        result.get(eleve.getString("idClasse")).add(eleve.getString("idEleve"));
                    }
                    handler.handle(new Either.Right<String, Map<String, List<String>>>(result));
                } else {
                    handler.handle(new Either.Left<String, Map<String, List<String>>>(body.getString("message")));
                    log.error("getElevesClasses : " + body.getString("message"));
                }
            }
        });
    }

    /**
     * Recupere les informations relatives a chaque eleve dont l'identifiant est passe en parametre, et cree un objet
     * Eleve correspondant a cet eleve.
     *
     * @param idEleves  Tableau contenant les identifiants des eleves dont on souhaite recuperer les informations.
     * @param handler   Handler contenant la liste des objets Eleve ainsi construit,
     *                  ou un erreur potentiellement survenue.
     *
     * @see Eleve
     */
    private void getInfoEleve(String[] idEleves, final Handler<Either<String, List<Eleve>>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "eleve.getInfoEleve")
                .putArray("idEleves", new JsonArray(idEleves));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    final Set<String> classes = new HashSet<>();
                    final List<Eleve> result = new ArrayList<>();
                    JsonArray queryResult = body.getArray("results");
                    for (int i = 0; i < queryResult.size(); i++) {
                        JsonObject eleveBase = queryResult.get(i);
                        Eleve eleveObj = new Eleve(eleveBase.getString("idEleve"),
                                eleveBase.getString("lastName"),
                                eleveBase.getString("firstName"),
                                eleveBase.getString("idClasse"),
                                eleveBase.getString("classeName"));
                        classes.add(eleveObj.getIdClasse());
                        result.add(eleveObj);
                    }

                    utilsService.getCycle(new ArrayList<>(classes), new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if(event.isRight()) {
                                JsonArray queryResult = event.right().getValue();
                                for (int i = 0; i < queryResult.size(); i++) {
                                    JsonObject cycle = queryResult.get(i);
                                    for(Eleve eleve : result) {
                                        if(Objects.equals(eleve.getIdClasse(), cycle.getString("id_groupe"))) {
                                            eleve.setCycle(cycle.getString("libelle"));
                                        }
                                    }
                                }
                                handler.handle(new Either.Right<String, List<Eleve>>(result));
                            } else {
                                handler.handle(new Either.Left<String, List<Eleve>>(event.left().getValue()));
                                log.error("getInfoEleve : getCycle : " + event.left().getValue());
                            }
                        }
                    });

                } else {
                    handler.handle(new Either.Left<String, List<Eleve>>(body.getString("message")));
                    log.error("getInfoEleve : getInfoEleve : " + body.getString("message"));
                }
            }
        });
    }

    /**
     * Recupere les parametres manquant afin de pouvoir generer le BFC dans le cas ou seul l'identifiant de la
     * structure est fourni.
     *
     * @param idStructure  Identifiant de la structure dont on souhaite generer le BFC.
     * @param handler      Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamStruct(final String idStructure,  final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {

        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();
        population.put(idStructure, new LinkedHashMap<String, List<Eleve>>());

        getClassesStruct(idStructure, new Handler<Either<String, List<String>>>() {
            @Override
            public void handle(Either<String, List<String>> event) {
                if (event.isRight()) {
                    final List<String> classes = event.right().getValue();
                    getElevesClasses(classes.toArray(new String[0]), new Handler<Either<String, Map<String, List<String>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, List<String>>> event) {
                            if (event.isRight()) {
                                for (final Map.Entry<String, List<String>> classe : event.right().getValue().entrySet()) {
                                    population.get(idStructure).put(classe.getKey(), null);
                                    getInfoEleve(classe.getValue().toArray(new String[0]), new Handler<Either<String, List<Eleve>>>() {
                                        @Override
                                        public void handle(Either<String, List<Eleve>> event) {
                                            if(event.isRight()) {
                                                population.get(idStructure).put(classe.getKey(), event.right().getValue());
                                                // Si population.get(idStructure).values() contient une valeur null,
                                                // cela signifie qu'une classe n'a pas encore recupere sa liste d'eleves
                                                if(!population.get(idStructure).values().contains(null)) {
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
     * @param idClasses  Identifiants des classes dont on souhaite generer le BFC.
     * @param handler    Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamClasses(final List<String> idClasses, final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {
        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();

        getStructClasses(idClasses.toArray(new String[0]), new Handler<Either<String, String>>() {
            @Override
            public void handle(Either<String, String> event) {
                if(event.isRight()) {
                    final String idStructure = event.right().getValue();
                    population.put(idStructure, new LinkedHashMap<String, List<Eleve>>());

                    getElevesClasses(idClasses.toArray(new String[0]), new Handler<Either<String, Map<String, List<String>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, List<String>>> event) {
                            if (event.isRight()) {
                                for (final Map.Entry<String, List<String>> classe : event.right().getValue().entrySet()) {
                                    population.get(idStructure).put(classe.getKey(), null);
                                    getInfoEleve(classe.getValue().toArray(new String[0]), new Handler<Either<String, List<Eleve>>>() {
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
     *
     * @param idEleves  Identifiants des eleves dont on souhaite generer le BFC.
     * @param handler   Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamEleves(final List<String> idEleves, final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {
        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();

        getInfoEleve(idEleves.toArray(new String[0]), new Handler<Either<String, List<Eleve>>>() {
            @Override
            public void handle(Either<String, List<Eleve>> event) {
                if(event.isRight()) {
                    final Map<String, List<Eleve>> classes = new LinkedHashMap<>();
                    for(Eleve e : event.right().getValue()) {
                        if(!classes.containsKey(e.getIdClasse())) {
                            classes.put(e.getIdClasse(), new ArrayList<Eleve>());
                        }
                        classes.get(e.getIdClasse()).add(e);
                    }
                    getStructClasses(classes.keySet().toArray(new String[0]), new Handler<Either<String, String>>() {
                        @Override
                        public void handle(Either<String, String> event) {
                            if(event.isRight()) {
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
     * Appelle {@link #getParamStruct(String, Handler)} si seul l'identifiant de la structure est fourni.
     * Appelle {@link #getParamClasses(List, Handler)} si seuls les identifiants de classes sont fournis.
     * Appelle {@link #getParamEleves(List, Handler)} si seuls les identifiants d'eleves sont fournis.
     *
     * @param idStructure  Identifiant de la structure dont on souhaite generer le BFC.
     * @param idClasses    Identifiants des classes dont on souhaite generer le BFC.
     * @param idEleves     Identifiants des eleves dont on souhaite generer le BFC.
     * @param handler      Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamBFC(final String idStructure, final List<String> idClasses, final List<String> idEleves, final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {

        if (idStructure != null) {
            getParamStruct(idStructure, new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
                @Override
                public void handle(Either<String, Map<String, Map<String, List<Eleve>>>> event) {
                    if(event.isRight()) {
                        handler.handle(new Either.Right<String, Map<String, Map<String, List<Eleve>>>>(event.right().getValue()));
                    } else {
                        handler.handle(new Either.Left<String, Map<String, Map<String, List<Eleve>>>>(event.left().getValue()));
                        log.error("getParamStruct : failed to get related idClasses and/or idEleves.");
                    }
                }
            });
        } else if (!idClasses.isEmpty()) {
            getParamClasses(idClasses, new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
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
            getParamEleves(idEleves, new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
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
     * Genere le BFC des entites passees en parametre au format PDF via la fonction {@link #genererPdf(HttpServerRequest, JsonObject, String, String)}.
     * Ces entites peuvent etre au choix un etablissement, un ou plusieurs classes, un ou plusieurs eleves.
     * Afin de prefixer le fichier PDF cree, appelle {@link DefaultUtilsService#getNameEntity(String[], Handler)} afin
     * de recuperer le nom de l'entite fournie.
     *
     * @param request
     */
    @Get("/BFC/pdf")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void getBFCEleve(final HttpServerRequest request) {

        final String idStructure = request.params().get("idStructure");
        final List<String> idClasses = request.params().getAll("idClasse");
        final List<String> idEleves = request.params().getAll("idEleve");
        final Long idPeriode = (request.params().get("idPeriode") != null) ? Long.valueOf(request.params().get("idPeriode")) : null;

        // Ou exclusif sur la presence des parametres, de facon a s'assurer qu'un seul soit renseigne.
        if(idStructure != null ^ !idClasses.isEmpty() ^ !idEleves.isEmpty()) {

            getParamBFC(idStructure, idClasses, idEleves, new Handler<Either<String, Map<String, Map<String, List<Eleve>>>>>() {
                @Override
                public void handle(Either<String, Map<String, Map<String, List<Eleve>>>> event) {
                    if(event.isRight()) {
                        final String idStructureGot = event.right().getValue().entrySet().iterator().next().getKey();
                        final Map<String, List<Eleve>> classes = event.right().getValue().entrySet().iterator().next().getValue();

                        getBFCParClasse(classes, idStructureGot, idPeriode, new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if(event.isRight()) {
                                    final JsonObject result = new JsonObject().putArray("classes", event.right().getValue());
                                    if(idStructure != null) {
                                        utilsService.getNameEntity(new String[]{idStructureGot}, new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> event) {
                                                if(event.isRight()) {
                                                    final String structureName = ((JsonObject) event.right().getValue().get(0)).getString("name").replace(" ", "_");
                                                    generateBFCExport(result, idPeriode, structureName, request);
                                                } else {
                                                    leftToResponse(request, event.left());
                                                    log.error("getNameEntity : Unable to get the name of the specified entity (idStructure).");
                                                }
                                            }
                                        });
                                    } else if (!idClasses.isEmpty()) {
                                        utilsService.getNameEntity(classes.keySet().toArray(new String[0]), new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> event) {
                                                if (event.isRight()) {
                                                    final StringBuilder classesName = new StringBuilder();
                                                    for (int i = 0; i < event.right().getValue().size(); i++) {
                                                        classesName.append(((JsonObject) event.right().getValue().get(i)).getString("name")).append("_");
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
                                        utilsService.getNameEntity(idEleves.toArray(new String[0]), new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> event) {
                                                if (event.isRight()) {
                                                    final StringBuilder elevesName = new StringBuilder();
                                                    for (int i = 0; i < event.right().getValue().size(); i++) {
                                                        elevesName.append(((JsonObject) event.right().getValue().get(i)).getString("name")).append("_");
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

    private void generateBFCExport (final JsonObject result, Long idPeriode, final String fileNamePrefix, final HttpServerRequest request) {
        if (idPeriode != null) {
            JsonObject jsonRequest = new JsonObject()
                    .putObject("headers", new JsonObject().putString("Accept-Language",
                            request.headers().get("Accept-Language")))
                    .putString("Host", getHost(request));
            JsonObject action = new JsonObject()
                    .putString("action", "periode.getLibellePeriode")
                    .putNumber("idType", idPeriode)
                    .putObject("request", jsonRequest);

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();

                    if ("ok".equals(body.getString("status"))) {
                        String periodeName = body.getString("result");
                        periodeName = periodeName.replace(" ", "_");
                        genererPdf(request, result, "BFC.pdf.xhtml", "BFC_" + fileNamePrefix + "_" + periodeName);
                    } else {
                        leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                        log.error("getPeriode : Unable to get the label of the specified entity (idPeriode).");
                    }
                }
            });
        } else {
            genererPdf(request, result, "BFC.pdf.xhtml", "BFC_" + fileNamePrefix);
        }
    }

    /**
     * Generation d'un PDF à partir d'un template xhtml
     * @param request
     * @param templateProps objet JSON contenant l'ensemble des valeurs à remplir dans le template
     * @param templateName nom du template
     * @param prefixPdfName prefixe du nom du pdf (qui sera complété de la date de génération)
     */
    private void genererPdf(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                            final String prefixPdfName) {

        final String dateDebut = new SimpleDateFormat("dd.MM.yyyy").format(new Date().getTime());
        if(templateProps.getBoolean("image")) {
            log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime()) + " -> Debut Generation Image du template " + templateName);
        } else {
            log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime()) + " -> Debut Generation PDF du template " + templateName);
        }

//        this.assetsPath = (String) vertx.sharedData().ge  tMap("server").get("assetPath");
//        this.skins = vertx.sharedData().getMap("skins");
//        final String assetsPath = this.assetsPath + "/assets/themes/" + this.skins.get(Renders.getHost(request));
//        final String templatePath = assetsPath + "/template/viescolaire/";
        final String templatePath = container.config().getObject("exports").getString("template-path");
//        final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + "/assets/themes/" + this.skins.get(Renders.getHost(request)) + "/img/";
        final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) + container.config().getString("app-address") + "/public/";

        node = (String) vertx.sharedData().getMap("server").get("node");
        if (node == null) {
            node = "";
        }
        
        vertx.fileSystem().readFile(templatePath + templateName, new Handler<AsyncResult<Buffer>>() {

            @Override
            public void handle(AsyncResult<Buffer> result) {
                if (!result.succeeded()) {
                    badRequest(request, "Error while reading template : " + templatePath + templateName);
                    log.error("Error while reading template : " + templatePath + templateName);
                    return;
                }
                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                processTemplate(request, templateProps, templateName, reader, new Handler<Writer>() {

                    @Override
                    public void handle(Writer writer) {
                        String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                        if (processedTemplate == null) {
                            badRequest(request, "Error while processing.");
                            if(templateProps != null) {
                                log.error("processing error : \ntemplateProps : " + templateProps.toString() 
                                        + "\ntemplateName : " + templateName);
                            }
                            return;
                        }
                        JsonObject actionObject = new JsonObject();
                        byte[] bytes;
                        try {
                            bytes = processedTemplate.getBytes("UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            bytes = processedTemplate.getBytes();
                            log.error(e.getMessage(), e);
                        }

                        actionObject
                                .putBinary("content", bytes)
                                .putString("baseUrl", baseUrl);
                        eb.send(node + "entcore.pdf.generator", actionObject, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> reply) {
                                JsonObject pdfResponse = reply.body();
                                if (!"ok".equals(pdfResponse.getString("status"))) {
                                    badRequest(request, pdfResponse.getString("message"));
                                    return;
                                }
                                byte[] pdf = pdfResponse.getBinary("content");

                                if(templateProps.getBoolean("image")){
                                    File pdfFile = new File(prefixPdfName + "_" + dateDebut + ".pdf");
                                    OutputStream outStream = null;
                                    try {
                                        outStream = new FileOutputStream(pdfFile);
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        outStream.write(pdf);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    try {
                                        String sourceDir = pdfFile.getAbsolutePath();
                                        File sourceFile = new File(sourceDir);
                                        while(!sourceFile.exists()){
                                            System.err.println(sourceFile.getName() + " File does not exist");
                                        }
                                        if (sourceFile.exists()) {
                                            PDDocument document = PDDocument.load(sourceDir);
                                            @SuppressWarnings("unchecked")
                                            List<PDPage> list = document.getDocumentCatalog().getAllPages();
                                            File imageFile = null;
                                            for (PDPage page : list) {
                                                BufferedImage image = page.convertToImage();
                                                int height = 150 + Integer.parseInt(templateProps.getString("nbrCompetences"))*50;
                                                BufferedImage SubImage = image.getSubimage(0, 0, 1684, height);
                                                imageFile = new File(prefixPdfName + "_" + dateDebut + ".jpg");
                                                ImageIO.write(SubImage, "jpg", imageFile);
                                            }
                                            document.close();
                                            FileInputStream fis = new FileInputStream(imageFile);
                                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                            byte[] buf = new byte[(int)imageFile.length()];
                                            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                                                bos.write(buf, 0, readNum);
                                            }
                                            byte[] bytes = bos.toByteArray();

                                            request.response().putHeader("Content-Type", "image/jpg");
                                            request.response().putHeader("Content-Disposition","attachment; filename="+prefixPdfName+"_"+dateDebut+".jpg");
                                            request.response().end(new Buffer(bytes));
                                            outStream.close();
                                            bos.close();
                                            fis.close();

                                            Files.deleteIfExists(Paths.get(pdfFile.getAbsolutePath()));
                                            Files.deleteIfExists(Paths.get(imageFile.getAbsolutePath()));
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime()) + " -> Fin Generation Image du template " + templateName);
                                } else {
                                    request.response().putHeader("Content-Type", "application/pdf");
                                    request.response().putHeader("Content-Disposition",
                                            "attachment; filename="+prefixPdfName+"_"+dateDebut+".pdf");
                                    request.response().end(new Buffer(pdf));
                                    log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime()) + " -> Fin Generation PDF du template " + templateName);
                                }
                            }
                        });
                    }
                });

            }
        });

    }

    @Get("/devoirs/print/:idDevoir/formsaisie")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void getFormsaisi(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null) {
                    MultiMap params = request.params();
                    final Long idDevoir;
                    if (params.get("idDevoir") != null ) {
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
                                if(devoirInfo.isRight()){

                                    final JsonObject devoirInfos = (JsonObject) ((Either.Right) devoirInfo).getValue();
                                    result.putString("devoirName", devoirInfos.getString("name"));
                                    result.putString("devoirCoefficient", devoirInfos.getString("coefficient"));
                                    result.putNumber("devoirDiviseur", devoirInfos.getLong("diviseur"));
                                    result.putBoolean("evaluation",devoirInfos.getBoolean("is_evaluated"));

                                    JsonObject jsonRequest = new JsonObject()
                                            .putObject("headers", new JsonObject().putString("Accept-Language",
                                                    request.headers().get("Accept-Language")))
                                            .putString("Host", getHost(request));
                                    JsonObject action = new JsonObject()
                                            .putString("action", "periode.getLibellePeriode")
                                            .putNumber("type", devoirInfos.getInteger("periodetype"))
                                            .putNumber("ordre", devoirInfos.getInteger("periodeordre"))
                                            .putObject("request", jsonRequest);
                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                        @Override
                                        public void handle(Message<JsonObject> message) {
                                            JsonObject body = message.body();

                                            result.putString("periode", body.getString("result"));

                                            JsonObject action = new JsonObject()
                                                    .putString("action", "classe.getEleveClasse")
                                                    .putString("idClasse", devoirInfos.getString("id_groupe"));

                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> message) {
                                                    JsonObject body = message.body();

                                                    if ("ok".equals(body.getString("status"))) {
                                                        result.putArray("eleves", body.getArray("results"));

                                                        JsonObject action = new JsonObject()
                                                                .putString("action", "matiere.getMatiere")
                                                                .putString("idMatiere", devoirInfos.getString("id_matiere"));

                                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                            @Override
                                                            public void handle(Message<JsonObject> message) {
                                                                JsonObject body = message.body();

                                                                if ("ok".equals(body.getString("status"))) {
                                                                    result.putString("matiere", body.getObject("result").getObject("n").getObject("data").getString("label"));

                                                                    JsonObject action = new JsonObject()
                                                                            .putString("action", "classe.getClasseInfo")
                                                                            .putString("idClasse", devoirInfos.getString("id_groupe"));

                                                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                                        @Override
                                                                        public void handle(Message<JsonObject> message) {
                                                                            JsonObject body = message.body();
                                                                            if ("ok".equals(body.getString("status"))) {
                                                                                result.putString("classeName", body.getObject("result").getObject("c").getObject("data").getString("name"));
                                                                                if(devoirInfos.getBoolean("is_evaluated") == true){
                                                                                    Integer nbrColone = (devoirInfos.getInteger("nbrcompetence") + 1 );
                                                                                    result.putString("nbrCompetences",nbrColone.toString());
                                                                                }else{
                                                                                    result.putString("nbrCompetences",devoirInfos.getInteger("nbrcompetence").toString());
                                                                                }

                                                                                if(devoirInfos.getInteger("nbrcompetence") > 0) {
                                                                                    competencesService.getDevoirCompetences(idDevoir, new Handler<Either<String, JsonArray>>() {
                                                                                        @Override
                                                                                        public void handle(Either<String, JsonArray> CompetencesObject) {
                                                                                            if(CompetencesObject.isRight()){
                                                                                                JsonArray  CompetencesOld = CompetencesObject.right().getValue();
                                                                                                JsonArray  CompetencesNew = new JsonArray();
                                                                                                Integer size =0;
                                                                                                Double ligne = new Double(0);
                                                                                                Integer lenght = 103; // le nombre de caractére max dans une ligne
                                                                                                Double height = new Double(2.2); // la hauteur d'une ligne
                                                                                                for (int i=0 ; i < CompetencesOld.size() ; i++) {
                                                                                                    JsonObject Comp = CompetencesOld.get(i);
                                                                                                    size = Comp.getString("nom").length() +10; // +10 pour "[ Cx ]"
                                                                                                    ligne += (Integer) size / lenght ;
                                                                                                    if(size%lenght > 0 ){
                                                                                                        ligne++;
                                                                                                    }
                                                                                                    Comp.putNumber("i", i+1);
                                                                                                    CompetencesNew.addObject(Comp);
                                                                                                }

                                                                                                ligne = (ligne * height) + 6; // + 6 la hauteur de la 1 ligne du tableau
                                                                                                if( ligne < 25){ // 25 est la hauteure minimal
                                                                                                    ligne = Double.parseDouble("25") ;
                                                                                                }
                                                                                                result.putString("ligne", ligne.toString()+"%");
                                                                                                if(CompetencesNew.size() > 0){
                                                                                                    result.putBoolean("hasCompetences",true);
                                                                                                }else{
                                                                                                    result.putBoolean("hasCompetences",false);
                                                                                                }
                                                                                                result.putArray("competences",CompetencesNew);
                                                                                                genererPdf(request, result , "Devoir.saisie.xhtml", "Formulaire_saisie");
                                                                                            }else{
                                                                                                log.error("Error :can not get competences devoir ");
                                                                                                badRequest(request, "Error :can not get competences devoir ");
                                                                                            }
                                                                                        }
                                                                                    });
                                                                                }else{
                                                                                    genererPdf(request, result , "Devoir.saisie.xhtml", "Formulaire_saisie");
                                                                                }
                                                                            }else{
                                                                                log.error("Error :can not get classe informations ");
                                                                                badRequest(request, "Error :can not get  classe informations");
                                                                            }
                                                                        }
                                                                    });
                                                                }else{
                                                                    log.error("Error :can not get classe info ");
                                                                    badRequest(request, "Error :can not get  classe info  ");
                                                                }
                                                            }
                                                        });
                                                    }else{
                                                        log.error("Error :can not get students ");
                                                        badRequest(request, "Error :can not get students  ");
                                                    }
                                                }
                                            });
                                        }
                                    });
                                }else{
                                    log.error("Error :can not get informations from postgres tables ");
                                    badRequest(request, "Error :can not get informations from postgres tables ");
                                }
                            }
                        });
                    } else{
                        log.error("Error : idDevoir must be a long object");
                        badRequest(request,"Error : idDevoir must be a long object");
                    }
                }else{
                    unauthorized(request);
                }

            }});
    }

    @Get("/devoirs/print/:idDevoir/cartouche")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void getCartouche(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if(user != null){
                    MultiMap params = request.params();
                    final Long idDevoir;
                    if(params.get("idDevoir")!= null ) {
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
                        if(nbrCartouche > 0 ){
                            JsonArray nbr = new JsonArray();
                            for(int j=0; j<nbrCartouche;j++){
                                nbr.add(j);
                            }
                            result.putArray("number", nbr);
                        }else{
                            result.putArray("number", new JsonArray().add("cartouche"));
                        }

                        final String byEleve = params.get("eleve");
                        final String color = params.get("color");
                        if(byEleve != null && color !=null ){
                            devoirService.getDevoirInfo(idDevoir, new Handler<Either<String, JsonObject>>() {
                                @Override
                                public void handle(Either<String, JsonObject> devoirInfo) {
                                    if(devoirInfo.isRight()){
                                        final JsonObject devoirInfos = (JsonObject) ((Either.Right) devoirInfo).getValue();
                                        SimpleDateFormat dateBfFormat = new SimpleDateFormat("yyyy-MM-dd");
                                        SimpleDateFormat dateAftFormat = new SimpleDateFormat("dd-MM-yyyy");
                                        String reformattedStr = "";
                                        ArrayList<String> classeList = new ArrayList<String>();

                                        result.putString("devoirName", devoirInfos.getString("name"));
                                        if(color.equals("true")){
                                            result.putBoolean("byColor", true);
                                        }else{
                                            result.putBoolean("byColor", false);
                                        }
                                        try {
                                            reformattedStr = dateAftFormat.format(dateBfFormat.parse( devoirInfos.getString("created")));
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        if(reformattedStr != "") {
                                            result.putString("devoirDate", reformattedStr);
                                        }else {
                                            result.putString("devoirDate",  devoirInfos.getString("created"));
                                        }
//                                        result.putBoolean("evaluation", devoirInfos.getBoolean("is_evaluated"));
                                        result.putBoolean("evaluation", true);
                                        //début
                                        classeList.add(devoirInfos.getString("id_groupe"));
                                        utilsService.getCycle(classeList , new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> cycle) {
                                                if(cycle.isRight()){
                                                    JsonObject cycleobj = cycle.right().getValue().get(0);
                                                    niveauDeMaitriseService.getNiveauDeMaitriseofCycle(cycleobj.getLong("id_cycle"), new Handler<Either<String, JsonArray>>() {
                                                        @Override
                                                        public void handle(Either<String, JsonArray> nivMaitrise) {
                                                            if(nivMaitrise.isRight()){
                                                                result.putArray("niveaux", nivMaitrise.right().getValue());
                                                                if(byEleve.equals("true")){
                                                                    result.putBoolean("byEleves", true);
                                                                    JsonObject action = new JsonObject()
                                                                            .putString("action", "classe.getEleveClasse")
                                                                            .putString("idClasse", devoirInfos.getString("id_groupe"));

                                                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                                        @Override
                                                                        public void handle(Message<JsonObject> message) {
                                                                            JsonObject body = message.body();

                                                                            if ("ok".equals(body.getString("status"))) {
                                                                                result.putArray("eleves", body.getArray("results"));
                                                                                if(devoirInfos.getInteger("nbrcompetence") > 0) {
                                                                                    competencesService.getDevoirCompetences(idDevoir, new Handler<Either<String, JsonArray>>() {
                                                                                        @Override
                                                                                        public void handle(Either<String, JsonArray> CompetencesObject) {
                                                                                            if(CompetencesObject.isRight()){
                                                                                                JsonArray  CompetencesOld = CompetencesObject.right().getValue();
                                                                                                JsonArray  CompetencesNew = new JsonArray();
                                                                                                for (int i=0 ; i < CompetencesOld.size() ; i++) {
                                                                                                    JsonObject Comp = CompetencesOld.get(i);
                                                                                                    Comp.putNumber("i", i+1);
                                                                                                    if(i==0){
                                                                                                        Comp.putBoolean("first",true);
                                                                                                    }else{
                                                                                                        Comp.putBoolean("first",false);
                                                                                                    }
                                                                                                    CompetencesNew.addObject(Comp);
                                                                                                }
                                                                                                if(CompetencesNew.size() > 0){
                                                                                                    result.putBoolean("hasCompetences",true);
                                                                                                }else{
                                                                                                    result.putBoolean("hasCompetences",false);
                                                                                                }
                                                                                                result.putString("nbrCompetences",devoirInfos.getInteger("nbrcompetence").toString());
                                                                                                result.putArray("competences",CompetencesNew);
                                                                                                genererPdf(request, result , "cartouche.pdf.xhtml", "Cartouche");
                                                                                            }else{
                                                                                                log.error("Error :can not get competences devoir ");
                                                                                                badRequest(request, "Error :can not get competences devoir ");
                                                                                            }
                                                                                        }
                                                                                    });
                                                                                }else{
                                                                                    genererPdf(request, result , "cartouche.pdf.xhtml", "Cartouche");
                                                                                }
                                                                            }else{
                                                                                log.error("Error :can not get students ");
                                                                                badRequest(request, "Error :can not get students  ");
                                                                            }
                                                                        }
                                                                    });
                                                                }else{
                                                                    result.putBoolean("byEleves", false);
                                                                    if(devoirInfos.getInteger("nbrcompetence") > 0) {
                                                                        competencesService.getDevoirCompetences(idDevoir, new Handler<Either<String, JsonArray>>() {
                                                                            @Override
                                                                            public void handle(Either<String, JsonArray> CompetencesObject) {
                                                                                if(CompetencesObject.isRight()){
                                                                                    JsonArray  CompetencesOld = CompetencesObject.right().getValue();
                                                                                    JsonArray  CompetencesNew = new JsonArray();
                                                                                    for (int i=0 ; i < CompetencesOld.size() ; i++) {
                                                                                        JsonObject Comp = CompetencesOld.get(i);
                                                                                        Comp.putNumber("i", i+1);
                                                                                        if(i==0){
                                                                                            Comp.putBoolean("first",true);
                                                                                        }else{
                                                                                            Comp.putBoolean("first",false);
                                                                                        }

                                                                                        CompetencesNew.addObject(Comp);
                                                                                    }
                                                                                    if(CompetencesNew.size() > 0){
                                                                                        result.putBoolean("hasCompetences",true);
                                                                                    }else{
                                                                                        result.putBoolean("hasCompetences",false);
                                                                                    }
                                                                                    result.putString("nbrCompetences",devoirInfos.getInteger("nbrcompetence").toString() );
                                                                                    result.putArray("competences",CompetencesNew);
                                                                                    result.putBoolean("image", Boolean.parseBoolean(request.params().get("image")));
                                                                                    genererPdf(request, result , "cartouche.pdf.xhtml", "Cartouche");
                                                                                }else{
                                                                                    log.error("Error :can not get competences devoir ");
                                                                                    badRequest(request, "Error :can not get competences devoir ");
                                                                                }
                                                                            }
                                                                        });
                                                                    }else{
                                                                        genererPdf(request, result , "cartouche.pdf.xhtml", "Cartouche");
                                                                    }
                                                                }
                                                            }else{
                                                                log.error("Error :can not get levels ");
                                                                badRequest(request, "Error :can not get levels  ");
                                                            }
                                                        }
                                                    });

                                                }else{
                                                    log.error("Error :can not get cycle ");
                                                    badRequest(request, "Error :can not get cycle  ");
                                                }



                                            }
                                        });

                                    }else{
                                        log.error("Error :can not get informations from postgres tables ");
                                        badRequest(request, "Error :can not get informations from postgres tables ");
                                    }

                                }
                            });
                        }
                    }
                    else{
                        log.error("Error : idDevoir must be a long object");
                        badRequest(request,"Error : idDevoir must be a long object");
                    }
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    @Get("/devoirs/print/:idDevoir/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportDevoir (final HttpServerRequest request) {
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
                    String idGroupe = devoir.getString("id_groupe");
                    String idEtablissement = devoir.getString("id_etablissement");

                    exportService.getExportEval(text, devoir, idGroupe, idEtablissement, request, new Handler<Either<String, JsonObject>>(){

                        @Override
                        public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                            if(stringJsonObjectEither.isRight()) {
                                try {
                                    JsonObject result = stringJsonObjectEither.right().getValue();
                                    if (json) {
                                        Renders.renderJson(request, result);
                                    } else {
                                        String fileName = result.getObject("devoir").getString("classe") + "_" + result.getObject("devoir").getString("nom").replace(' ', '_');
                                        genererPdf(request, result, "evaluation.pdf.xhtml", fileName);
                                    }
                                } catch (Error err){
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

    @Get("/releveComp/print/:idEleve/export")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExportReleveComp(final HttpServerRequest request) {
        final Boolean text = Boolean.parseBoolean(request.params().get("text"));
        final Boolean json = Boolean.parseBoolean(request.params().get("json"));
        final String idEleve = request.params().get("idEleve");
        final List<String> listIdMatieres = request.params().getAll("idMatiere");

        final JsonArray idMatieres = new JsonArray();
        for(int i = 0; i < listIdMatieres.size(); i++){
            idMatieres.add(listIdMatieres.get(i));
        }


        Long idPeriode = null;

        try {
            if(request.params().contains("idPeriode")) {
                idPeriode = Long.parseLong(request.params().get("idPeriode"));
            }
        } catch (NumberFormatException err) {
            badRequest(request, err.getMessage());
            log.error(err);
            return;
        }

        final Long finalIdPeriode = idPeriode;

        JsonObject action = new JsonObject()
                .putString("action", "eleve.getInfoEleve")
                .putArray("idEleves", new JsonArray(new String[]{idEleve}));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status")) && body.getArray("results").size() > 0) {
                    JsonObject eleve = body.getArray("results").get(0);
                    final String name = eleve.getString("lastName").toUpperCase() + " " + eleve.getString("firstName");
                    final String idClasse = eleve.getString("idClasse");
                    final String nomClasse = eleve.getString("classeName");
                    final String idEtablissement = eleve.getString("idEtablissement");

                    JsonObject action = new JsonObject()
                            .putString("action", "groupe.listGroupesEnseignementsByUserId")
                            .putString("userId", idEleve);

                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();

                            if ("ok".equals(body.getString("status"))) {
                                JsonArray result = body.getArray("results");
                                final List<String> idGroupes = new ArrayList<>();
                                final List<String> nomGroupes = new ArrayList<>();
//                                for (int i = 0; i < result.size(); i++) {
//                                    JsonObject groupe = ((JsonObject) result.get(i)).getObject("g").getObject("data");
//                                    idGroupes.add(groupe.getString("id"));
//                                    nomGroupes.add(groupe.getString("name"));
//                                }
                                idGroupes.add(idClasse);
                                nomGroupes.add(nomClasse);

                                exportService.getExportReleveComp(text, idEleve, idGroupes.toArray(new String[0]), idEtablissement, listIdMatieres, finalIdPeriode, new Handler<Either<String, JsonObject>>() {
                                    @Override
                                    public void handle(final Either<String, JsonObject> stringJsonObjectEither) {
                                        if (stringJsonObjectEither.isRight()) {
                                            try {
                                                final JsonObject result = stringJsonObjectEither.right().getValue();
                                                final JsonObject headerEleve = new JsonObject();
                                                headerEleve.putString("nom", name);
                                                headerEleve.putString("classe", nomGroupes.toString().substring(1, nomGroupes.toString().length() - 1));

                                                JsonObject action = new JsonObject()
                                                        .putString("action", "matiere.getMatieres")
                                                        .putArray("idMatieres", idMatieres);
                                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                    @Override
                                                    public void handle(Message<JsonObject> message) {
                                                        JsonObject body = message.body();

                                                        if ("ok".equals(body.getString("status"))) {
                                                            //String matiere = body.getObject("result").getObject("n").getObject("data").getString("label");
                                                            //headerEleve.putString("matiere", matiere);
                                                            JsonArray results = body.getArray("results");
                                                            String matieres = ((JsonObject)results.get(0)).getString("name");
                                                            for (int i = 1; i < results.size(); i++) {
                                                                matieres = matieres+", "+((JsonObject) results.get(i)).getString("name");
                                                            }
                                                            headerEleve.putString("matiere", matieres);

                                                            JsonObject jsonRequest = new JsonObject()
                                                                    .putObject("headers", new JsonObject().putString("Accept-Language",
                                                                            request.headers().get("Accept-Language")))
                                                                    .putString("Host", getHost(request));
                                                            JsonObject action = new JsonObject()
                                                                    .putString("action", "periode.getLibellePeriode")
                                                                    .putNumber("idType", finalIdPeriode)
                                                                    .putObject("request", jsonRequest);
                                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                                @Override
                                                                public void handle(Message<JsonObject> message) {
                                                                    JsonObject body = message.body();

                                                                    if ("ok".equals(body.getString("status"))) {
                                                                        String libellePeriode = body.getString("result")
                                                                                .replace("é", "e")
                                                                                .replace("è", "e");
                                                                        headerEleve.putString("periode", libellePeriode);
                                                                        result.getObject("header").putObject("left", headerEleve);
                                                                        if (json) {
                                                                            Renders.renderJson(request, result);
                                                                        } else {
                                                                            String fileName = name.replace(' ', '_') + "_export_competences";
                                                                            genererPdf(request, result, "releve-competences.pdf.xhtml", fileName);
                                                                        }
                                                                    } else {
                                                                        leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            leftToResponse(request, stringJsonObjectEither.left());
                                                        }
                                                    }
                                                });

                                            } catch (Error err) {
                                                leftToResponse(request, new Either.Left<>("An error occured while rendering pdf export : " + err.getMessage()));
                                            }
                                        } else {
                                            leftToResponse(request, stringJsonObjectEither.left());
                                        }
                                    }
                                });
                            } else {
                                leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                            }
                        }
                    });
                } else {
                    leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                }
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
            if(request.params().contains("idPeriode")) {
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
                    final Long idCycle = ((JsonObject) stringJsonArrayEither.right().getValue().get(0)).getLong("id_cycle");
                    JsonObject cycleObj = stringJsonArrayEither.right().getValue().get(0);

                    if (!idCycle.equals(cycleObj.getLong("id_cycle"))) {
                        leftToResponse(request, new Either.Left<>("different cycle"));
                    } else {
                        final JsonObject action = new JsonObject()
                                .putString("action", "classe.getEtabClasses")
                                .putArray("idClasses", new JsonArray(new String[]{idClasse}));

                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> message) {
                                JsonObject body = message.body();
                                if ("ok".equals(body.getString("status"))) {
                                    final String idEtablissement = ((JsonObject)body.getArray("results").get(0)).getString("idStructure");
                        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                            @Override
                            public void handle(final UserInfos user) {
                                final boolean isChefEtab = (user!= null)?("Personnel".equals(user.getType()) && user.getFunctions().containsKey("DIR")):true;
                                    if((user != null) || isChefEtab ) {
                                        bfcService.getVisibility(idEtablissement, user, new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                                                if(stringJsonArrayEither.isRight() || isChefEtab){
                                                    boolean moy = true;
                                                    if(stringJsonArrayEither.isRight()){
                                                        JsonArray result = stringJsonArrayEither.right().getValue();
                                                        moy = ((JsonObject)result.get(0)).getBoolean("visible");;
                                                    }
                                                    if(isChefEtab)
                                                        moy = true;
                                                    final boolean isHabilite = moy;

                                                    exportService.getExportRecapEval(text, idCycle, idEtablissement, new Handler<Either<String, JsonArray>>() {

                                                        @Override
                                                        public void handle(final Either<String, JsonArray> stringJsonObjectEither) {
                                                            if (stringJsonObjectEither.isRight()) {
                                                                try {
                                                                    final JsonObject result = new JsonObject();

                                                                    final JsonArray legende = stringJsonObjectEither.right().getValue();
                                                                    result.putArray("legende", legende);
                                                                    String atteint_calcule = new String("Valeurs affichées par domaine : niveau atteint + niveau calculé".getBytes(), StandardCharsets.UTF_8);
                                                                    String atteint = new String("Valeurs affichées par domaine : niveau atteint".getBytes(), StandardCharsets.UTF_8);
                                                                    result.putString("displayMoy", isHabilite ? atteint_calcule : atteint);

                                                                    final JsonObject action = new JsonObject()
                                                                            .putString("action", "classe.getEleveClasse")
                                                                            .putString("idClasse", idClasse);

                                                                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                                        @Override
                                                                        public void handle(Message<JsonObject> message) {
                                                                            JsonObject body = message.body();

                                                                            if ("ok".equals(body.getString("status"))) {
                                                                                JsonArray eleves = body.getArray("results");

                                                                                final String[] idEleves = new String[eleves.size()];
                                                                                final String[] nameEleves = new String[eleves.size()];
                                                                                for (int i = 0; i < eleves.size(); i++) {
                                                                                    idEleves[i] = ((JsonObject)eleves.get(i)).getString("id");
                                                                                    nameEleves[i] = ((JsonObject)eleves.get(i)).getString("lastName") + " " + ((JsonObject)eleves.get(i)).getString("firstName");
                                                                                }
                                                                                boolean recapEval = true;
                                                                                bfcService.buildBFC(recapEval, idEleves, idClasse, idEtablissement, finalIdPeriode, idCycle, new Handler<Either<String, JsonObject>>(){
                                                                                    @Override
                                                                                    public void handle (Either<String, JsonObject> stringMapEither){
                                                                                        if(stringMapEither.isRight()){
                                                                                            final JsonArray eleves = new JsonArray();
                                                                                            JsonObject bfc = stringMapEither.right().getValue();
                                                                                            if(bfc.size() > 0){
                                                                                                final int[] idDomaines = new int[bfc.getArray("domainesRacine").size()];
                                                                                                for(int l = 0; l <  bfc.getArray("domainesRacine").size(); l++){
                                                                                                    Long idDomaine = bfc.getArray("domainesRacine").get(l);
                                                                                                    idDomaines[l] = idDomaine.intValue();
                                                                                                }

                                                                                                for (int i = 0; i < idEleves.length; i++){
                                                                                                    JsonObject eleve = new JsonObject();
                                                                                                    JsonArray notesEleve = new JsonArray();
                                                                                                    List domainesEvalues = new ArrayList();
                                                                                                    if(bfc.containsField(idEleves[i])) {
                                                                                                        for (Object resultNote : bfc.getArray(idEleves[i])) {
                                                                                                            for (Object niveau : legende) {
                                                                                                                JsonObject note = new JsonObject();

                                                                                                                if (((JsonObject) resultNote).getNumber("niveau").toString()
                                                                                                                        .equals(((JsonObject) niveau).getLong("ordre").toString())) {
                                                                                                                    note.putNumber("id", ((JsonObject) resultNote).getNumber("idDomaine"));
                                                                                                                    note.putString("visu", ((JsonObject) niveau).getString("visu"));
                                                                                                                    note.putBoolean("nonEvalue", false);
                                                                                                                    if (isHabilite)
                                                                                                                        note.putString("moyenne", text ? "- " + ((JsonObject) resultNote).getNumber("moyenne")
                                                                                                                                : "" + ((JsonObject) resultNote).getNumber("moyenne"));

                                                                                                                    domainesEvalues.add(((JsonObject) note).getNumber("id").intValue());
                                                                                                                    notesEleve.add(note);
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                    addMaitriseNE(domainesEvalues, notesEleve, idDomaines, text);
                                                                                                    eleve.putString("id", idEleves[i]);
                                                                                                    eleve.putString("nom", nameEleves[i]);
                                                                                                    eleve.putArray("notes", sortJsonArrayById(notesEleve));
                                                                                                    eleves.add(eleve);
                                                                                                }

                                                                                                domaineService.getDomainesLibCod(idDomaines, new Handler<Either<String, JsonArray>>(){
                                                                                                    @Override
                                                                                                    public void handle(Either<String, JsonArray> stringJsonArrayEither){
                                                                                                        if(stringJsonArrayEither.isRight()){
                                                                                                            JsonArray domaines = stringJsonArrayEither.right().getValue();
                                                                                                            result.putArray("domaines", isDomaineParent(sortJsonArrayById(domaines)));
                                                                                                            result.putArray("eleves", eleves);
                                                                                                            JsonObject action = new JsonObject()
                                                                                                                    .putString("action", "classe.getClasseInfo")
                                                                                                                    .putString("idClasse", idClasse);
                                                                                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                                                                                @Override
                                                                                                                public void handle(Message<JsonObject> message) {
                                                                                                                    JsonObject body = message.body();

                                                                                                                    if ("ok".equals(body.getString("status"))) {
                                                                                                                        final String classeName = body.getObject("result").getObject("c").getObject("data").getString("name");
                                                                                                                        result.putString("classe", classeName);

                                                                                                                        JsonObject jsonRequest = new JsonObject()
                                                                                                                                .putObject("headers", new JsonObject().putString("Accept-Language", request.headers().get("Accept-Language")))
                                                                                                                                .putString("Host", getHost(request));
                                                                                                                        JsonObject action = new JsonObject()
                                                                                                                                .putString("action", "periode.getLibellePeriode")
                                                                                                                                .putNumber("idType", finalIdPeriode)
                                                                                                                                .putObject("request", jsonRequest);
                                                                                                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                                                                                                                            @Override
                                                                                                                            public void handle(Message<JsonObject> message) {
                                                                                                                                JsonObject body = message.body();

                                                                                                                                if ("ok".equals(body.getString("status"))) {
                                                                                                                                    String libellePeriode = body.getString("result")
                                                                                                                                            .replace("é", "e")
                                                                                                                                            .replace("è", "e");
                                                                                                                                    result.putString("periode", libellePeriode);
                                                                                                                                    result.putBoolean("text", text);
                                                                                                                                    result.putBoolean("isHabilite", isHabilite);
                                                                                                                                    if (json) {
                                                                                                                                        Renders.renderJson(request, result);
                                                                                                                                    } else {
                                                                                                                                        String fileName = classeName.replace(' ', '_') + "_export_recapitulatif";
                                                                                                                                        genererPdf(request, result, "recapitulatif-evaluations.pdf.xhtml", fileName);
                                                                                                                                    }
                                                                                                                                } else {
                                                                                                                                    leftToResponse(request, new Either.Left<>("periode not found")); //leftToResponse(request, new Either.Left<>(body.getString("message")));
                                                                                                                                }
                                                                                                                            }
                                                                                                                        });
                                                                                                                    } else {
                                                                                                                        leftToResponse(request, new Either.Left<>("classe not found"));
                                                                                                                    }
                                                                                                                }
                                                                                                            });
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
                                                                    });
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
                                    } else{
                                        badRequest(request);
                                    }
                                    }
                                });
                                } else {
                                    leftToResponse(request, new Either.Left<>("etab not found"));
                                }
                            }
                        });
                    }
                } else {
                    leftToResponse(request, stringJsonArrayEither.left());
                }
            }
        });
    }

    private JsonArray sortJsonArrayById(JsonArray jsonArray){
        List<JsonObject> jsonValues = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject p = jsonArray.get(i);
            jsonValues.add(p);
        }
        Collections.sort( jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "id";
            @Override
            public int compare(JsonObject a, JsonObject b) {
                Long valA = 0L;
                Long valB = 0L;
                try {
                    valA =  (Long) a.getNumber(KEY_NAME);
                    valB = (Long) b.getNumber(KEY_NAME);
                }
                catch (Exception e) {
                    //do something
                }
                return valA.compareTo(valB);
            }
        });

        JsonArray sortedJsonArray = new JsonArray();
        for (JsonObject o : jsonValues) {
            sortedJsonArray.add(o);
        }
        return sortedJsonArray;
    }

    private JsonArray isDomaineParent(JsonArray domaines){
        JsonArray newDomaines = new JsonArray();
        for( int k = 0; k < domaines.size(); k++) {
            JsonObject domaine = domaines.get(k);
            if("0".equals(domaine.getNumber("id_parent").toString()) || k == 0){
                domaine.putBoolean("isDomaineParent", true);
//                if(!"0".equals(domaine.getNumber("id_parent").toString()))
//                    domaine.putString("nomDomaine", "Domaine D" + domaine.getNumber("id_parent").toString());
//                else
//                    domaine.putString("nomDomaine", "Domaine " + domaine.getString("codification"));

            } else {
                domaine.putBoolean("isDomaineParent", false);
            }
            newDomaines.add(domaine);
        }
        return newDomaines;
    }

    private void addMaitriseNE(List domainesEvalues, JsonArray notesEleve, int[] idDomaines, boolean text){
        for (int idDomaine : idDomaines) {
            if (!domainesEvalues.contains(idDomaine)) {
                JsonObject note = new JsonObject();
                note.putNumber("id", new Long(idDomaine));
                note.putString("visu", text ? "NE" : "white");
                note.putBoolean("nonEvalue", true);
                notesEleve.add(note);
            }
        }
    }

}
