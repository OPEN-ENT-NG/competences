package fr.openent.competences;

import fr.openent.competences.bean.Eleve;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.*;

public class Utils {

    protected static final Logger log = LoggerFactory.getLogger(Utils.class);
    protected static  UtilsService utilsService = new DefaultUtilsService();
    
    /**
     * Recupere l'identifiant de la structure a laquelle appartiennent les classes dont l'identifiant est passe en
     * parametre.
     *
     * @param idClasses Tableau contenant l'identifiant des classes dont on souhaite connaitre la structure.
     * @param handler   Handler contenant l'identifiant de la structure.
     */
    public static void getStructClasses(EventBus eb, String[] idClasses, final Handler<Either<String, String>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "classe.getEtabClasses")
                .putArray("idClasses", new JsonArray(idClasses));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();

                if ("ok".equals(body.getString("status"))) {
                    JsonArray queryResult = body.getArray("results");
                    if (queryResult.size() == 0) {
                        handler.handle(new Either.Left<String, String>("Aucune classe n'a ete trouvee."));
                        log.error("getStructClasses : No classes found with these ids");
                    } else if (queryResult.size() > 1) {
                        // Il est impossible de demander un BFC pour des classes n'appartenant pas au meme etablissement.
                        handler.handle(new Either.Left<String, String>("Les classes n'appartiennent pas au meme etablissement."));
                        log.error("getStructClasses : provided classes are not from the same structure.");
                    } else {
                        JsonObject structure = queryResult.get(0);
                        handler.handle(new Either.Right<String, String>(structure.getString("idStructure")));
                    }
                } else {
                    handler.handle(new Either.Left<String, String>(body.getString("message")));
                    log.error("getStructClasses : " + body.getString("message"));
                }
            }
        });
    }


    public static void getIdElevesClassesGroupes(EventBus eb, final String  idGroupe, final int indiceBoucle, final Handler<Either<String, List<String>>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "classe.getEleveClasse")
                .putString("idClasse", idGroupe);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                List<String> idEleves = new ArrayList<String>();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray queryResult = body.getArray("results");
                    if(queryResult != null) {
                        for (int i =0; i< queryResult.size(); i++) {
                            idEleves.add(((JsonObject)queryResult.get(i)).getString("id"));
                        }
                    }
                    handler.handle(new Either.Right<String, List<String>>(idEleves));

                } else {
                    handler.handle(new Either.Left<String, List<String>>(body.getString("message")));
                    log.error("Error :can not get students of groupe : " + idGroupe);
                }
            }
        });

    }


    /**
     * Recupere l'identifiant de l'ensemble des eleves de la classe dont l'identifiant est passe en parametre.
     *
     * @param idClasses Identifiant de la classe dont on souhaite recuperer les eleves.
     * @param handler   Handler contenant la liste des identifiants des eleves recuperees.
     */
    public static void getElevesClasses(EventBus eb, String[] idClasses, final Handler<Either<String, Map<String, List<String>>>> handler) {
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
                        if (!result.containsKey(eleve.getString("idClasse"))) {
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
     * @param idEleves Tableau contenant les identifiants des eleves dont on souhaite recuperer les informations.
     * @param handler  Handler contenant la liste des objets Eleve ainsi construit,
     *                 ou un erreur potentiellement survenue.
     * @see Eleve
     */
    public static void getInfoEleve(EventBus eb, String[] idEleves, final Handler<Either<String, List<Eleve>>> handler) {
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
                            if (event.isRight()) {
                                JsonArray queryResult = event.right().getValue();
                                for (int i = 0; i < queryResult.size(); i++) {
                                    JsonObject cycle = queryResult.get(i);
                                    for (Eleve eleve : result) {
                                        if (Objects.equals(eleve.getIdClasse(), cycle.getString("id_groupe"))) {
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
     * Recupere l'identifiant de l'ensemble des classes de la structure dont l'identifiant est passe en parametre.
     *
     * @param idStructure Identifiant de la structure dont on souhaite recuperer les classes.
     * @param handler     Handler contenant la liste des identifiants des classes recuperees.
     */
   public static void getClassesStruct(EventBus eb, final String idStructure, final Handler<Either<String, List<String>>> handler) {
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
}
