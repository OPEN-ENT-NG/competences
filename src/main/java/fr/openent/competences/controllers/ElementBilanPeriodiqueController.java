package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.AccessElementBilanPeriodiqueFilter;
import fr.openent.competences.security.CreateElementBilanPeriodique;
import fr.openent.competences.security.utils.AccessThematiqueBilanPeriodique;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import java.util.*;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;

public class ElementBilanPeriodiqueController extends ControllerHelper {

    private final DefaultElementBilanPeriodiqueService defaultElementBilanPeriodiqueService;

    public ElementBilanPeriodiqueController() {
        defaultElementBilanPeriodiqueService = new DefaultElementBilanPeriodiqueService();
    }

    /**
     * Créer une thématique avec les données passées en POST
     * @param request
     */
    @Post("/thematique")
    @ApiDoc("Créer une thématique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void createThematique(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix +
                Competences.SCHEMA_THEMATIQUE_BILAN_PERIODIQUE, resource -> defaultElementBilanPeriodiqueService.insertThematiqueBilanPeriodique(resource,
                        defaultResponseHandler(request)));
    }

    /**
     * Retourne les thématiques correspondant au type passé en paramètre
     * @param request
     */
    @Get("/thematique")
    @ApiDoc("Retourne les thématiques correspondant au type passé en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessThematiqueBilanPeriodique.class)
    public void getThematiques(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                final String idEtablissement = request.params().get("idEtablissement");
                if(user != null && user.getStructures().contains(idEtablissement)){
                    defaultElementBilanPeriodiqueService.getThematiqueBilanPeriodique(
                            Long.parseLong(request.params().get("type")),
                            idEtablissement,
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne les éléments correspondant à la thématique passée en paramètre
     * @param request
     */
    @Get("/elements/thematique")
    @ApiDoc("Retourne les éléments correspondant à la thématique passée en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessThematiqueBilanPeriodique.class)
    public void getElementsOnThematique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getElementsOnThematique(
                            request.params().get("idThematique"),
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Créer les élèments du bilan périodique avec les données passées en paramètre
     * @param request
     */
    @Post("/elementBilanPeriodique")
    @ApiDoc("Créer une élément bilan périodique")
    @SecuredAction("create.element.bilan.periodique")
    public void createElementBilanPeriodique(final HttpServerRequest request){
        String schema = getElementSchema(request.params().get("type"));

        if(schema != null){
            RequestUtils.bodyToJson(request, pathPrefix + schema, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resource) {
                    defaultElementBilanPeriodiqueService.insertElementBilanPeriodique(resource,
                            defaultResponseHandler(request));
                }
            });
        } else {
            Renders.renderJson(request, new JsonObject()
                    .put("error", "element type not found"), 400);
        }
    }

    /**
     * Mettre à jour l'élèment du bilan périodique avec les données passées en paramètre
     * @param request
     */
    @Put("/elementBilanPeriodique")
    @ApiDoc("Mettre à jour l'élèment bilan périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void updateElementBilanPeriodique(final HttpServerRequest request){

        String schema = getElementSchema(request.params().get("type"));

        if(schema != null){
            RequestUtils.bodyToJson(request, pathPrefix + schema, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resource) {
//                    if(Boolean.parseBoolean(request.params().get("hasAppreciations")))
                    defaultElementBilanPeriodiqueService.getGroupesElementBilanPeriodique(
                            request.params().get("idElement"),
                            new Handler<Either<String, JsonArray>> () {
                        @Override
                        public void handle(Either<String, JsonArray> event){
                            if(event.isRight()){
                                JsonArray classes = event.right().getValue();

                                JsonArray newClass = resource.getJsonArray("classes");
                                List<String> newClasses = new ArrayList<String>();
                                for(Object c : newClass){
                                    JsonObject classe = (JsonObject) c;
                                    newClasses.add(classe.getString("id"));
                                }
                                //pour toutes les classes présentes sur l'élèment actuellement
                                List<String> deletedClasses = new ArrayList<String>();
                                for(Object c : classes){
                                    JsonObject classe = (JsonObject) c;
                                    //si la classe présente sur l'élèment actuellement n'est pas dans la liste des nouvelles classes alors c'est une classe à supprimer
                                    if(!newClasses.contains(classe.getString("id_groupe"))){
                                        deletedClasses.add(classe.getString("id_groupe"));
                                    }
                                }
                                // on récupère les appréciations sur l'élément liées aux classes supprimées
                                defaultElementBilanPeriodiqueService.getApprecBilanPerClasse(
                                        deletedClasses, null,
                                        request.params().getAll("idElement"),
                                        new Handler<Either<String, JsonArray>> () {
                                            @Override
                                            public void handle(Either<String, JsonArray> event) {
                                                if (event.isRight()) {
                                                    //je ferai un service qui supprimera l'appreciation
                                                    JsonArray apprecClasseOnDeletedClasses = event.right().getValue();

                                                    defaultElementBilanPeriodiqueService.getApprecBilanPerEleve(
                                                            deletedClasses, null,
                                                            request.params().getAll("idElement"), null,
                                                            new Handler<Either<String, JsonArray>> () {
                                                                @Override
                                                                public void handle(Either<String, JsonArray> event) {
                                                                    if (event.isRight()) {
                                                                        JsonArray apprecEleveOnDeletedClasses = event.right().getValue();
                                                                        List<String> idsEleves = new ArrayList<>();

                                                                        if(apprecEleveOnDeletedClasses.size() > 0){// Si j'ai des appréciations sur élèves à supprimer
                                                                            //pour chaque appréciation, je récupère l'élève propriétaire
                                                                            for(Object a : apprecEleveOnDeletedClasses){
                                                                                JsonObject appreciation = (JsonObject) a;
                                                                                idsEleves.add(appreciation.getString("id_eleve"));
                                                                            }
                                                                            // je cherche la liste des classes/groupes des elèves propriétaires
                                                                            JsonObject action = new JsonObject()
                                                                                    .put("action", "user.getUsers")
                                                                                    .put("idUsers", idsEleves);

                                                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                                                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                                        @Override
                                                                                        public void handle(Message<JsonObject> message) {
                                                                                            JsonObject body = message.body();

                                                                                            if ("ok".equals(body.getString("status"))) {
                                                                                                JsonArray users = body.getJsonArray("results");

                                                                                                // map qui à un idUser associe une map de idClass -> externalIdClass de toutes les classes/groupes de l'élève
                                                                                                Map<String, Map<String, String>> usersMap = new HashMap<String, Map<String, String>>();
                                                                                                for(Object u : users) {
                                                                                                    JsonObject user = (JsonObject) u;
                                                                                                    Map<String, String> classesMap = new HashMap<String, String>();

                                                                                                    JsonArray idClasses = user.getJsonArray("currentClassIds");
                                                                                                    idClasses.addAll(user.getJsonArray("currentGroupIds"));

                                                                                                    JsonArray externalIdClasses = user.getJsonArray("currentClassExternalIds");
                                                                                                    externalIdClasses.addAll(user.getJsonArray("currentGroupExternalIds"));

                                                                                                    for(int i = 0; i < idClasses.size(); i++) {
                                                                                                        classesMap.put(idClasses.getString(i), externalIdClasses.getString(i));
                                                                                                    }

                                                                                                    usersMap.put(user.getString("id"), classesMap);
                                                                                                }

                                                                                                JsonArray apprecDeletedConcurrent = new JsonArray();
                                                                                                JsonArray apprecDeletedAlone = new JsonArray();
                                                                                                for(Object a : apprecEleveOnDeletedClasses){
                                                                                                    JsonObject apprec = (JsonObject) a;

                                                                                                    //si l'élève sur l'appreciation appartient à une des nouvelles classes alors j'ajoute l'appreciation à
                                                                                                    //apprecDeletedConcurrent pour supprimer les relations entre l'appreciation et toutes les deleted classes
                                                                                                    //
                                                                                                    //si l'élève sur l'appreciation n'appartient à aucune des nouvelles classes alors j'ajoute l'appreciation à
                                                                                                    //apprecDeletedAlone pour supprimer l'appréciation et les relations entre l'appreciation et toutes les deleted classes
                                                                                                    Map<String, String> studentClasses = usersMap.get(apprec.getString("id_eleve"));
                                                                                                    try {
                                                                                                        if(Collections.disjoint(studentClasses.keySet(), newClasses)){
                                                                                                            apprecDeletedAlone.add(apprec);
                                                                                                        } else {
                                                                                                            apprecDeletedConcurrent.add(apprec);
                                                                                                        }
                                                                                                    } catch (NullPointerException err) {
                                                                                                        badRequest(request, err.getMessage());
                                                                                                        log.error(err);
                                                                                                    }
                                                                                                }
                                                                                                JsonObject apprecDeleted = new JsonObject()
                                                                                                        .put("apprecDeletedAlone", apprecDeletedAlone)
                                                                                                        .put("apprecDeletedConcurrent", apprecDeletedConcurrent);
                                                                                                defaultElementBilanPeriodiqueService.updateElementBilanPeriodique(
                                                                                                        Long.parseLong(request.params().get("idElement")), resource,
                                                                                                        apprecClasseOnDeletedClasses, apprecDeleted,
                                                                                                        deletedClasses, defaultResponseHandler(request));
                                                                                            } else{
                                                                                                leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                                                            }
                                                                                        }
                                                                                    }));
                                                                        } else { // Si je n'ai pas d'appréciations sur élèves à supprimer
                                                                            defaultElementBilanPeriodiqueService.updateElementBilanPeriodique(
                                                                                    Long.parseLong(request.params().get("idElement")), resource,
                                                                                    apprecClasseOnDeletedClasses, null,
                                                                                    deletedClasses,defaultResponseHandler(request));
                                                                        }
                                                                    } else{
                                                                        Renders.renderJson(request, new JsonObject()
                                                                                .put("error", "error while retreiving students appreciations"), 400);
                                                                    }
                                                                }
                                                            });
                                                } else{
                                                    Renders.renderJson(request, new JsonObject()
                                                            .put("error", "error while retreiving classes appreciations"), 400);
                                                }
                                            }
                                        });
                            } else{
                                leftToResponse(request, event.left());
                            }
                        }
                    });
                }
            });
        } else {
            JsonObject error = (new JsonObject()).put("error", "element type not found");
            Renders.renderJson(request, error, 400);
        }
    }

    private String getElementSchema(String type){
        String schema= "";
        switch (type) {
            case "1" :
                schema = Competences.SCHEMA_EPI_BILAN_PERIODIQUE;
                break;
            case "2" :
                schema = Competences.SCHEMA_AP_BILAN_PERIODIQUE;
                break;
            case "3" :
                schema = Competences.SCHEMA_PARCOURS_BILAN_PERIODIQUE;
                break;
            case "eleve" :
                schema = Competences.SCHEMA_APPRECIATION_ELEVE_CREATE;
                break;
            case "classe" :
                schema = Competences.SCHEMA_APPRECIATION_CLASSE_CREATE;
                break;
            default :
                schema = null;
        }
        return schema;
    }

    /**
     * Retourne les élèments du bilan périodique
     * @param request
     */
    @Get("/elementsBilanPeriodique")
    @ApiDoc("Retourne les élèments du bilan périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void getElementBilanPeriodique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getElementBilanPeriodique(
                            Boolean.parseBoolean(request.params().get("visu"))
                                    ? null : request.params().get("idEnseignant"),
                            request.params().get("idClasse"),
                            request.params().get("idEtablissement"),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.isRight()) {
                                        JsonArray result = event.right().getValue();

                                        List<String> idMatieres = new ArrayList<>();
                                        List<String> idClasses = new ArrayList<>();
                                        List<String> idUsers = new ArrayList<>();

                                        for(Object r : result){
                                            JsonObject element = (JsonObject)r;

                                            String[] arrayIdClasses = element.getString("groupes").split(",");
                                            JsonArray jsonArrayIntsMats = element.getJsonArray("intervenants_matieres");

                                            for(int i = 0; i < arrayIdClasses.length; i++){
                                                if(!idClasses.contains(arrayIdClasses[i])){
                                                    idClasses.add(arrayIdClasses[i]);
                                                }
                                            }

                                            if(jsonArrayIntsMats != null){
                                                for(Object o : jsonArrayIntsMats){
                                                    JsonArray jsonArrayIntMat = (JsonArray) o;
                                                    String[] arrayIntMat = jsonArrayIntMat.getString(1).split(",");
                                                    if(!idUsers.contains(arrayIntMat[0])){
                                                        idUsers.add(arrayIntMat[0]);
                                                    }
                                                    if(arrayIntMat.length > 1 && !idMatieres.contains(arrayIntMat[1])){
                                                        idMatieres.add(arrayIntMat[1]);
                                                    }
                                                }
                                            }
                                        }

                                        // récupération des noms des matières
                                        JsonObject action = new JsonObject()
                                                .put("action", "matiere.getMatieres")
                                                .put("idMatieres", new fr.wseduc.webutils.collections.JsonArray(idMatieres));

                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                    @Override
                                                    public void handle(Message<JsonObject> message) {
                                                        JsonObject body = message.body();

                                                        if ("ok".equals(body.getString("status"))) {
                                                            JsonArray matieres = body.getJsonArray("results");
                                                            Map<String, String> matieresMap = new HashMap<String, String>();

                                                            for(Object o : matieres){
                                                                JsonObject matiere = (JsonObject)o;
                                                                matieresMap.put(matiere.getString("id"), matiere.getString("name"));
                                                            }

                                                            // récupération des noms des classes/groupes
                                                            JsonObject action = new JsonObject()
                                                                    .put("action", "classe.getClassesInfo")
                                                                    .put("idClasses", new fr.wseduc.webutils.collections.JsonArray(idClasses));

                                                            eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                                                                    handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                        @Override
                                                                        public void handle(Message<JsonObject> message) {
                                                                            JsonObject body = message.body();

                                                                            if ("ok".equals(body.getString("status"))) {
                                                                                JsonArray classes = body.getJsonArray("results");
                                                                                Map<String, String> classesNameMap = new HashMap<String, String>();
                                                                                Map<String, String> classesExternalIdMap = new HashMap<String, String>();
                                                                                for(Object o : classes){
                                                                                    JsonObject classe = (JsonObject)o;
                                                                                    classesNameMap.put(classe.getString("id"), classe.getString("name"));
                                                                                    classesExternalIdMap.put(classe.getString("id"), classe.getString("externalId"));
                                                                                }

                                                                                // récupération des noms des intervenants
                                                                                JsonObject action = new JsonObject()
                                                                                        .put("action", "user.getUsers")
                                                                                        .put("idUsers", idUsers);

                                                                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                                                                    @Override
                                                                                    public void handle(Message<JsonObject> message) {
                                                                                        JsonObject body = message.body();

                                                                                        if ("ok".equals(body.getString("status"))) {
                                                                                            JsonArray users = body.getJsonArray("results");
                                                                                            Map<String, String> usersMap = new HashMap<String, String>();
                                                                                            for(Object o : users){
                                                                                                JsonObject user = (JsonObject)o;
                                                                                                usersMap.put(user.getString("id"), user.getString("displayName"));
                                                                                            }

                                                                                            JsonArray parsedElems = new fr.wseduc.webutils.collections.JsonArray();
                                                                                            for(Object o  : result){
                                                                                                JsonObject element = (JsonObject) o;
                                                                                                JsonObject parsedElem = new JsonObject();

                                                                                                parsedElem.put("id", element.getInteger("id"));
                                                                                                parsedElem.put("type", element.getInteger("type_elt_bilan_periodique"));

                                                                                                if(element.getString("intitule") != null){
                                                                                                    parsedElem.put("libelle", element.getString("intitule"));
                                                                                                    parsedElem.put("description", element.getString("description"));
                                                                                                }

                                                                                                if(element.getInteger("id_thematique") != null){
                                                                                                    JsonObject theme = new JsonObject();
                                                                                                    theme.put("id", element.getInteger("id_thematique"));
                                                                                                    theme.put("libelle", element.getString("libelle"));
                                                                                                    parsedElem.put("theme", theme);
                                                                                                }

                                                                                                String[] arrayIdGroupes = element.getString("groupes").split(",");
                                                                                                JsonArray groupes = new fr.wseduc.webutils.collections.JsonArray();

                                                                                                for(int i = 0; i < arrayIdGroupes.length; i++){
                                                                                                    JsonObject groupe = new JsonObject();
                                                                                                    groupe.put("id", arrayIdGroupes[i]);
                                                                                                    groupe.put("name", classesNameMap.get(arrayIdGroupes[i]));
                                                                                                    groupe.put("externalId", classesExternalIdMap.get(arrayIdGroupes[i]));
                                                                                                    groupes.add(groupe);
                                                                                                }
                                                                                                parsedElem.put("groupes", groupes);

                                                                                                if(element.getJsonArray("intervenants_matieres") != null){

                                                                                                    JsonArray intMat = element.getJsonArray("intervenants_matieres");
                                                                                                    JsonArray intervenantsMatieres = new fr.wseduc.webutils.collections.JsonArray();

                                                                                                    for(int i = 0; i < intMat.size(); i++){
                                                                                                        String[] intMatArray = intMat.getJsonArray(i).getString(1).split(",");
                                                                                                        JsonObject intervenantMatiere = new JsonObject();

                                                                                                        JsonObject intervenant = new JsonObject();
                                                                                                        intervenant.put("id", intMatArray[0]);
                                                                                                        intervenant.put("displayName", usersMap.get(intMatArray[0]));
                                                                                                        intervenantMatiere.put("intervenant", intervenant);
                                                                                                        if(intMatArray.length > 1 ){
                                                                                                            JsonObject matiere = new JsonObject();
                                                                                                            matiere.put("id", intMatArray[1]);
                                                                                                            matiere.put("name", matieresMap.get(intMatArray[1]));
                                                                                                            intervenantMatiere.put("matiere", matiere);
                                                                                                        }

                                                                                                        intervenantsMatieres.add(intervenantMatiere);
                                                                                                    }
                                                                                                    parsedElem.put("intervenantsMatieres", intervenantsMatieres);
                                                                                                }

                                                                                                parsedElems.add(parsedElem);
                                                                                            }
                                                                                            Renders.renderJson(request, parsedElems);
                                                                                        } else{
                                                                                            leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                                                        }
                                                                                    }
                                                                                }));
                                                                            } else{
                                                                                leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                                            }
                                                                        }
                                                                    }));
                                                        } else{
                                                            leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                        }
                                                    }
                                                }));
                                    } else{
                                        leftToResponse(request, event.left());
                                    }
                                }
                            });

                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne les thématiques correspondant au type passé en paramètre
     * @param request
     */
    @Get("/elementsBilanPeriodique/enseignants")
    @ApiDoc("Retourne les thématiques correspondant au type passé en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void getEnseignantsElements(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getEnseignantsElementsBilanPeriodique(
                            request.params().getAll("idElement"),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.isRight()) {
                                        JsonArray result = event.right().getValue();

                                        List<String> idUsers = new ArrayList<>();
                                        Map<Long, List<String>> ensElemMap = new HashMap<Long, List<String>>();

                                        for (Object o : result) {
                                            JsonObject ensMat = (JsonObject) o;
                                            if (!idUsers.contains(ensMat.getString("id_intervenant"))) {
                                                idUsers.add(ensMat.getString("id_intervenant"));
                                            }
                                            if (!ensElemMap.containsKey(ensMat.getLong("id_elt_bilan_periodique"))) {
                                                ensElemMap.put(ensMat.getLong("id_elt_bilan_periodique"), new ArrayList<>());
                                            }
                                            ensElemMap.get(ensMat.getLong("id_elt_bilan_periodique")).add(ensMat.getString("id_intervenant"));
                                        }

                                        // récupération des noms des intervenants
                                        JsonObject action = new JsonObject()
                                                .put("action", "user.getUsers")
                                                .put("idUsers", idUsers);

                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> message) {
                                                JsonObject body = message.body();

                                                if ("ok".equals(body.getString("status"))) {
                                                    JsonArray users = body.getJsonArray("results");
                                                    Map<String, String> usersMap = new HashMap<String, String>();
                                                    for(Object o : users){
                                                        JsonObject user = (JsonObject)o;
                                                        usersMap.put(user.getString("id"), user.getString("displayName"));
                                                    }
                                                    JsonArray resultat = new JsonArray();

                                                    for(Map.Entry<Long, List<String>> entry : ensElemMap.entrySet()) {
                                                        JsonObject enseignantsElem = new JsonObject();
                                                        enseignantsElem.put("idElement", entry.getKey());
                                                        JsonArray ens = new fr.wseduc.webutils.collections.JsonArray();
                                                        for(Object o : entry.getValue()){
                                                            String idEns = (String)o;
                                                            ens.add(usersMap.get(idEns));
                                                        }
                                                        enseignantsElem.put("idsEnseignants", ens);
                                                        resultat.add(enseignantsElem);
                                                    }
                                                    Renders.renderJson(request, resultat);
                                                } else {
                                                    leftToResponse(request, new Either.Left<String, Object>(body.getString("message")));
                                                }
                                            }
                                        }));
                                    } else {
                                        leftToResponse(request, event.left());
                                    }
                                }
                            });
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne les classes correspondant à l'enseignant en paramètre
     * @param request
     */
    @Get("/elementsBilanPeriodique/classes")
    @ApiDoc("Retourne les classes correspondant à l'enseignant")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void getClassesElementBilanPeriodique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    final String idStructure = request.params().get("idStructure");
                    defaultElementBilanPeriodiqueService.getClassesElementsBilanPeriodique(
                            idStructure, user.getUserId(),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.isRight()) {
                                        JsonArray externalIdElementBilanPeriodique = event.right().getValue();
                                        JsonObject action = new JsonObject()
                                                .put("action", "classe.listAllGroupes")
                                                .put("idStructure", idStructure);
                                        // On récupère la liste des classes de l'établissement
                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                                            @Override
                                            public void handle(Message<JsonObject> message) {
                                                  JsonObject body = message.body();
                                                if ("ok".equals(body.getString("status"))) {
                                                    JsonArray listGroupesEtablissement = body.getJsonArray("results");
                                                    JsonArray jsonArrayResultat = new fr.wseduc.webutils.collections.JsonArray();
                                                    if(listGroupesEtablissement.size() > 0){
                                                        for (int i = 0; i < listGroupesEtablissement.size(); i++) {
                                                            JsonObject vGroupe = listGroupesEtablissement.getJsonObject(i).getJsonObject("m").getJsonObject("data");
                                                            for (int j = 0; j < externalIdElementBilanPeriodique.size(); j++) {
                                                                String idGroupe = externalIdElementBilanPeriodique.getJsonObject(j).getString("id_groupe");
                                                                if(idGroupe.equalsIgnoreCase(vGroupe.getString("id"))){
                                                                    JsonArray vTypeClasse = listGroupesEtablissement.getJsonObject(i).getJsonObject("m").getJsonObject("metadata").getJsonArray("labels");
                                                                    if(vTypeClasse.contains("Class")){
                                                                        vGroupe.put("type_groupe",0);
                                                                    } else if (vTypeClasse.contains("FunctionalGroup")){
                                                                        vGroupe.put("type_groupe",1);
                                                                    } else{
                                                                        vGroupe.put("type_groupe",2);
                                                                    }
                                                                    jsonArrayResultat.add(vGroupe);
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        Renders.renderJson(request, jsonArrayResultat);
                                                    }
                                                } else {
                                                    log.warn("getClassesElementBilanPeriodique :  erreur lors de la récupération des groupes/classes : id Etablissement : " + idStructure);
                                                    Renders.renderJson(request, new JsonObject()
                                                            .put("error", "error while retreiving classes getClassesElementBilanPeriodique"), 400);
                                                }
                                            }
                                        }));

                                    } else {
                                        leftToResponse(request, event.left());
                                    }
                                }
                            });
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Supprimer des élèments du bilan périodique dont les ids sont passés en paramètre
     * @param request
     */
    @Delete("/elementsBilanPeriodique")
    @ApiDoc("Supprimer des éléments du bilan périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void deleteElementBilanPeriodique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.deleteElementBilanPeriodique(
                            request.params().getAll("idElement"),
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Mettre à jour une thématique
     * @param request
     */
    @Put("/thematique")
    @ApiDoc("Mettre à jour une thématique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void updateThematique(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix +
                Competences.SCHEMA_THEMATIQUE_BILAN_PERIODIQUE, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject resource) {
                defaultElementBilanPeriodiqueService.updateThematique(
                        request.params().get("idThematique"),resource,
                        defaultResponseHandler(request));
            }
        });
    }

    /**
     * Supprimer une thématique
     * @param request
     */
    @Delete("/thematique")
    @ApiDoc("Supprimer une thématique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateElementBilanPeriodique.class)
    public void deleteThematique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.deleteThematique(
                            request.params().get("idThematique"),
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Retourne les appreciations liées au élèments du bilan périodiques
     * (et à la classe) passés en paramètre
     * @param request
     */
    @Get("/elementsAppreciations")
    @ApiDoc("Retourne les appreciations liées au élèments du bilan périodiques passés en paramètre")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void getAppreciations(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getApprecBilanPerClasse(
                            request.params().getAll("idClasse"),
                            request.params().get("idPeriode"),
                            request.params().getAll("idElement"),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if(event.isRight()){
                                        JsonArray apprecClasses = event.right().getValue();
                                        defaultElementBilanPeriodiqueService.getApprecBilanPerEleve(
                                                request.params().getAll("idClasse"),
                                                request.params().get("idPeriode"),
                                                request.params().getAll("idElement"),
                                                request.params().get("idEleve"),
                                                new Handler<Either<String, JsonArray>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonArray> event) {
                                                        if(event.isRight()){
                                                            JsonArray apprecEleves = event.right().getValue();
                                                            Renders.renderJson(request, apprecClasses.addAll(apprecEleves));
                                                        } else {
                                                            Renders.renderJson(request, new JsonObject()
                                                                    .put("error", "error while retreiving students appreciations"), 400);
                                                        }
                                                    }
                                                });
                                    } else {
                                        Renders.renderJson(request, new JsonObject()
                                                .put("error", "error while retreiving classes appreciations"), 400);
                                    }

                                }
                            });
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    /**
     * Créer une appréciation avec les données passées en POST depuis l'écran de la saisie de projet
     * @param request
     */
    @Post("/elementsAppreciationsSaisieProjet")
    @ApiDoc("Créer une appréciation")
    @SecuredAction("create.appreciation.saisie.projets")
    public void createAppreciationSaisieProjet(final HttpServerRequest request){
        createApprec(request);
    }

    /**
     * Créer une appréciation avec les données passées en POST depuis l'écran du bilan périoque
     * @param request
     */
    @Post("/elementsAppreciationBilanPeriodique")
    @ApiDoc("Créer une appréciation")
    @SecuredAction("create.appreciation.bilan.periodique")
    public void createAppreciation(final HttpServerRequest request){
        createApprec(request);
    }

    private void createApprec(final HttpServerRequest request){
        String schema = getElementSchema(request.params().get("type"));

        if(schema != null){
            UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                @Override
                public void handle(final UserInfos user) {
                    if(user != null){
                        RequestUtils.bodyToJson(request, pathPrefix + schema,
                                new Handler<JsonObject>() {
                                    @Override
                                    public void handle(JsonObject resource) {
                                        List<String> idsElements = new ArrayList<String>();
                                        idsElements.add(resource.getInteger("id_element").toString());
                                        new FilterUserUtils(user, eb).validateElement(idsElements,
                                                resource.getString("id_classe"), new Handler<Boolean>() {
                                                    @Override
                                                    public void handle(final Boolean isValid) {
                                                        if (isValid) {
                                                            new FilterUserUtils(user, eb).validateEleve(resource.getString("id_eleve"),
                                                                    resource.getString("id_classe"), new Handler<Boolean>() {
                                                                        @Override
                                                                        public void handle(final Boolean isValid) {
                                                                            if (isValid) {
                                                                                defaultElementBilanPeriodiqueService.getGroupesElementBilanPeriodique(
                                                                                        resource.getInteger("id_element").toString(),
                                                                                        new Handler<Either<String, JsonArray>> () {
                                                                                            @Override
                                                                                            public void handle(Either<String, JsonArray> event){
                                                                                                if(event.isRight()){
                                                                                                    defaultElementBilanPeriodiqueService.insertOrUpdateAppreciationElement(
                                                                                                            resource.getString("id_eleve"),
                                                                                                            resource.getString("id_classe"),
                                                                                                            resource.getString("externalid_classe"),
                                                                                                            new Long(resource.getInteger("id_periode")),
                                                                                                            new Long(resource.getInteger("id_element")),
                                                                                                            resource.getString("appreciation"),
                                                                                                            event.right().getValue(),
                                                                                                            defaultResponseHandler(request));
                                                                                                } else {
                                                                                                    leftToResponse(request, event.left());
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            } else {
                                                                                unauthorized(request);
                                                                            }
                                                                        }
                                                                    });
                                                        } else {
                                                            unauthorized(request);
                                                        }
                                                    }
                                                });
                                    }
                                });
                    } else{
                        unauthorized(request);
                    }
                }
            });
        } else {
            Renders.renderJson(request, new JsonObject()
                    .put("error", "appreciation type not found"), 400);
        }
    }
}
