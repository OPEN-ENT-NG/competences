package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.AccessElementBilanPeriodiqueFilter;
import fr.openent.competences.security.CreateElementBilanPeriodique;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getThematiques(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getThematiqueBilanPeriodique(
                            Long.parseLong(request.params().get("type")),
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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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

//    /**
//     * Retourne les appréciations correspondant à la classe passée en paramètre
//     * @param request
//     */
//    @Get("/appreciations/classe")
//    @ApiDoc("Retourne les appréciations correspondant à la classe passée en paramètre")
//    @SecuredAction(value = "", type = ActionType.RESOURCE)
//    @ResourceFilter(CreateElementBilanPeriodique.class)
//    public void getAppreciationsOnClasse(final HttpServerRequest request){
//        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
//            @Override
//            public void handle(UserInfos user) {
//                if(user != null){
//                    defaultElementBilanPeriodiqueService.getApprecClasseOnClasse(
//                            request.params().get("idClasse"),
//                            request.params().get("idElement"),
//                            new Handler<Either<String, JsonArray>>() {
//                                @Override
//                                public void handle(Either<String, JsonArray> event) {
//                                    if (event.isRight()) {
//                                        JsonArray apprecClasses = event.right().getValue();
//                                        defaultElementBilanPeriodiqueService.getApprecEleveOnClasse(
//                                                request.params().get("idClasse"),
//                                                request.params().get("idElement"),
//                                                new Handler<Either<String, JsonArray>>() {
//                                                    @Override
//                                                    public void handle(Either<String, JsonArray> event) {
//                                                        if (event.isRight()) {
//                                                            JsonArray apprecEleves = event.right().getValue();
//                                                            Renders.renderJson(request, apprecClasses.addAll(apprecEleves));
//                                                        } else {
//                                                            Renders.renderJson(request, new JsonObject()
//                                                                    .put("error", "error while retreiving students appreciations"), 400);
//                                                        }
//                                                    }
//                                                });
//                                    } else {
//                                        Renders.renderJson(request, new JsonObject()
//                                                .put("error", "error while retreiving classes appreciations"), 400);
//                                    }
//                                }
//                            });
//                }else{
//                    unauthorized(request);
//                }
//            }
//        });
//    }

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
                    defaultElementBilanPeriodiqueService.updateElementBilanPeriodique(Long.parseLong(request.params().get("idElement")), resource,
                            defaultResponseHandler(request));
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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
//    @SecuredAction(value = "", type = ActionType.RESOURCE)
//    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void getElementBilanPeriodique(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getElementBilanPeriodique(
                            request.params().get("idEnseignant"),
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
     * Retourne les appreciations liées au élèments du bilan périodiques (et à la classe) passés en paramètre
     * @param request
     */
    @Get("/elementsAppreciations")
    @ApiDoc("Retourne les appreciations liées au élèments du bilan périodiques passés en paramètre")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
//    @SecuredAction(value = "", type = ActionType.RESOURCE)
//    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void getAppreciations(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    defaultElementBilanPeriodiqueService.getApprecBilanPerClasse(
                            request.params().get("idClasse"),
                            request.params().get("idPeriode"),
                            request.params().getAll("idElement"),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if(event.isRight()){
                                        JsonArray apprecClasses = event.right().getValue();

                                        defaultElementBilanPeriodiqueService.getApprecBilanPerEleve(
                                                request.params().get("idClasse"),
                                                request.params().get("idPeriode"),
                                                request.params().getAll("idElement"),
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
     * Créer une appréciation avec les données passées en POST
     * @param request
     */
    @Post("/elementsAppreciation")
    @ApiDoc("Créer une appréciation")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
//    @SecuredAction(value = "", type = ActionType.RESOURCE)
//    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
    public void createAppreciation(final HttpServerRequest request){
        String schema = getElementSchema(request.params().get("type"));

        if(schema != null){
            RequestUtils.bodyToJson(request, pathPrefix + schema, new Handler<JsonObject>() {
                @Override
                public void handle(JsonObject resource) {
                    defaultElementBilanPeriodiqueService.getGroupesElementBilanPeriodique(
                            resource.getInteger("id_element").toString(),
                            new Handler<Either<String, JsonArray>> () {
                            @Override
                                    public void handle(Either<String, JsonArray> event){
                                if(event.isRight()){
                                    defaultElementBilanPeriodiqueService.insertOrUpdateAppreciationElement(
                                            resource.getString("id_eleve"),
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
                }
            });
        } else {
            Renders.renderJson(request, new JsonObject()
                    .put("error", "appreciation type not found"), 400);
        }
    }

//    /**
//     * Mettre à jour une appréciation avec les données passées en POST
//     * @param request
//     */
//    @Put("/elementsAppreciation")
//    @ApiDoc("Mettre à jour une appréciation")
//    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
////    @SecuredAction(value = "", type = ActionType.RESOURCE)
////    @ResourceFilter(AccessElementBilanPeriodiqueFilter.class)
//    public void updateAppreciation(final HttpServerRequest request){
//
//        String schema = getElementSchema(request.params().get("type"));
//
//        if(schema != null){
//            RequestUtils.bodyToJson(request, pathPrefix + schema, new Handler<JsonObject>() {
//                @Override
//                public void handle(JsonObject resource) {
//                    defaultElementBilanPeriodiqueService.updateAppreciationBilanPeriodique(
//                            new Long(resource.getInteger("id_appreciation")),
//                            resource.getString("appreciation"),
//                            request.params().get("type"),
//                            defaultResponseHandler(request));
//                }
//            });
//        } else {
//            Renders.renderJson(request, new JsonObject()
//                    .put("error", "appreciation type not found"), 400);
//        }
//    }
}
