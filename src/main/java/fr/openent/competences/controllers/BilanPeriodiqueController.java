package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.constants.Field;
import fr.openent.competences.model.Service;
import fr.openent.competences.model.Structure;
import fr.openent.competences.model.SubTopic;
import fr.openent.competences.security.*;
import fr.openent.competences.service.BilanPeriodiqueService;
import fr.openent.competences.service.impl.*;
import fr.openent.competences.utils.MultiTeachersUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.openent.competences.service.impl.DefaultUtilsService.setServices;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;


public class BilanPeriodiqueController extends ControllerHelper{

    private final BilanPeriodiqueService bilanPeriodiqueService;
    private final DefaultSyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;
    private final DefaultAppreciationCPEService appreciationCPEService;
    private final DefaultAvisConseilService avisConseilService;
    private final DefaultAvisOrientationService avisOrientationService;
    private final DefaultUtilsService utilsService;

    public BilanPeriodiqueController (EventBus eb){
        this.eb = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
        appreciationCPEService = new DefaultAppreciationCPEService();
        avisConseilService = new DefaultAvisConseilService();
        avisOrientationService = new DefaultAvisOrientationService();
        utilsService = new DefaultUtilsService(eb);
    }

    @Get("/bilan/periodique/eleve/:idEleve")
    @ApiDoc("renvoit tous les éléments pour le bilan périodique d'un élève")
    @SecuredAction(value="access.conseil.de.classe", type=ActionType.WORKFLOW)
    public void getSuiviDesAcquisEleve(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, userInfos -> {
            final String idEtablissement = request.params().get("idEtablissement");
            final String idPeriodeString = request.params().get("idPeriode");
            final Long idPeriode = (idPeriodeString != null) ? Long.parseLong(idPeriodeString) : null;
            final String idEleve = request.params().get("idEleve");
            final String idClasse = request.params().get("idClasse");

            Utils.getGroupesClasse(eb, new JsonArray().add(idClasse), responseGroupsClass -> {
                if(responseGroupsClass.isLeft()) {
                    String error = responseGroupsClass.left().getValue();
                    log.error("[Competence] BilanPeriodiqueController at getSuiviDesAcquisEleve : getGroupesClasse " + error);
                    badRequest(request);
                } else {
                    JsonArray groupsClassResult = responseGroupsClass.right().getValue();
                    JsonArray idGroupClasse = new JsonArray()
                            .add(idClasse);

                    if(groupsClassResult != null && !groupsClassResult.isEmpty()){
                        idGroupClasse.addAll(groupsClassResult.getJsonObject(0).getJsonArray("id_groupes"));
                    }

                    Future<JsonArray> periodesFuture = utilsService.getPeriodes(idGroupClasse.getList(), idEtablissement);
                    Future<List<SubTopic>> subTopicCoefFuture = utilsService.getSubTopicCoeff(idEtablissement, idClasse);

                    Future<JsonArray> servicesFuture = Future.future();
                    utilsService.getServices(idEtablissement, idGroupClasse,
                            servicesEvent -> formate(servicesFuture, servicesEvent));

                    Future<JsonArray> multiTeachersFuture = Future.future();
                    utilsService.getAllMultiTeachers(idEtablissement, idGroupClasse, multiTeachersEvent -> formate(multiTeachersFuture, multiTeachersEvent));

                    CompositeFuture.all(servicesFuture, multiTeachersFuture, subTopicCoefFuture, periodesFuture).setHandler(futuresEvent -> {
                        if (futuresEvent.failed()) {
                            String error = futuresEvent.cause().getMessage();
                            log.error(error);
                            badRequest(request);
                        } else {
                            List<Object> periodes = (periodesFuture.result().stream().filter(obj ->
                                    ((JsonObject) obj).getLong(Field.ID_TYPE).equals(idPeriode)).collect(Collectors.toList()));

                            JsonArray servicesJsonArray = servicesFuture.result();
                            JsonArray multiTeachers = multiTeachersFuture.result();
                            multiTeachers = MultiTeachersUtils.filterSubtitute(periodes, multiTeachers);
                            List<SubTopic> subTopics = subTopicCoefFuture.result();
                            Structure structure = new Structure();
                            structure.setId(idEtablissement);
                            List<Service> services = new ArrayList<>();
                            setServices(structure, servicesJsonArray, services,subTopics);
                            bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve, idGroupClasse,
                                    services, multiTeachers, arrayResponseHandler(request));
                        }
                    });
                }
            });
        });
    }

    @Get("/eleve/evenements/:idEleve")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessChildrenParentFilterStructureId.class)
    public void getAbsencesAndRetards(final HttpServerRequest request) {
        final String idEleve = request.params().get("idEleve");
        final String idStructure = request.params().get("idEtablissement");
        final String idClasse = request.params().get("idClasse");
        bilanPeriodiqueService.getRetardsAndAbsences(idStructure, Collections.singletonList(idEleve),
                Collections.singletonList(idClasse), arrayResponseHandler(request));
    }

    /**
     * Récupère la synthèses de l'élève sur une période donnée
     * @param request
     */
    @Get("/syntheseBilanPeriodique")
    @ApiDoc("Récupère la synthèse d'un élève pour une période donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessConseilDeClasseStructureId.class)
    public void getSyntheseBilanPeriodique(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>(){
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final Long idTypePeriode = Long.parseLong(request.params().get("id_typePeriode"));
                    final String idEleve = request.params().get("id_eleve");
                    final String idStructure = request.params().get("id_structure");

                    syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(idTypePeriode, idEleve, idStructure,
                            arrayResponseHandler(request));
                } else {
                    badRequest(request);
                }
            }
        });
    }

    /**
     * Récupère les synthèses et avis de l'élève sur l'année
     * @param request
     */
    @Get("/bilan/periodique/datas/avis/synthses")
    @ApiDoc("Récupère les synthèses et avis de l'élève sur l'année")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessConseilDeClasseEtablissementId.class)
    public void getSynthesesAvisBilanPeriodique(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>(){
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    if(request.params().get("idEleve") != null && request.params().get("idEtablissement") != null){
                        String idEleve = request.params().get("idEleve");
                        String idStructure = request.params().get("idEtablissement");
                        Future<JsonArray> libelleAvisFuture = Future.future();
                        avisConseilService.getLibelleAvis(null, idStructure, event -> {
                            formate(libelleAvisFuture, event);
                        });

                        Future<JsonArray> getSynthesesFuture = Future.future();
                        syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(null, idEleve, idStructure, event -> {
                            formate(getSynthesesFuture, event);
                        });

                        Future<JsonArray> getAvisConseilFuture = Future.future();
                        avisConseilService.getAvisConseil(idEleve,null,idStructure,event -> {
                            formate(getAvisConseilFuture, event);
                        });

                        Future<JsonArray> getAvisOrientationFuture = Future.future();
                        avisOrientationService.getAvisOrientation(idEleve,null,idStructure,event -> {
                            formate(getAvisOrientationFuture, event);
                        });

                        CompositeFuture.all(libelleAvisFuture, getSynthesesFuture,getAvisConseilFuture,getAvisOrientationFuture).setHandler(event -> {
                            if(event.succeeded()){
                                JsonArray libelleAvis = libelleAvisFuture.result();
                                JsonArray syntheses = getSynthesesFuture.result();
                                JsonArray avisConseil = getAvisConseilFuture.result();
                                JsonArray avisOrientation = getAvisOrientationFuture.result();
                                JsonObject result = new JsonObject();

                                JsonObject avisPerso = new JsonObject().put("id", 0).put("libelle","-- Personnalisé --")
                                        .put("type_avis", 0).put("active",true);
                                libelleAvis.add(avisPerso);

                                result.put("libelleAvis",libelleAvis).put("syntheses",syntheses).put("avisConseil",avisConseil).put("avisOrientation",avisOrientation);

                                Renders.renderJson(request,result);
                            } else {
                                String error = event.cause().getMessage();
                                log.error("getSynthesesAvisBilanPeriodique " + error);
                                Renders.badRequest(request);
                            }
                        });

                    }else{
                        log.debug("Not all informations that we need to get synthesis and avis : idEleve & idEtablissement");
                        Renders.badRequest(request);
                    }
                } else {
                    badRequest(request);
                }
            }
        });
    }

    @Post("/syntheseBilanPeriodiqueWorkflow")
    @ApiDoc("Méthode crée uniquement pour gérer le droit workflow pour la méthode suivante : createOrUpdateSyntheseBilanPeriodique")
    @SecuredAction(value = "create.synthese.bilan.periodique", type = ActionType.WORKFLOW)
    public void syntheseBilanPeriodiqueWorkflow(final HttpServerRequest request) {
        badRequest(request);
    }

    /**
     * Créer une synthese avec les données passées en POST
     *
     * @param request
     */
    @Post("/syntheseBilanPeriodique")
    @ApiDoc("Créer ou mettre à jour une synthèse du bilan périodique d'un élève pour une période donnée")
    @ResourceFilter(CreateSyntheseBilanPeriodiqueFilter.class)
    public void createOrUpdateSyntheseBilanPeriodique(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    String validator = pathPrefix + Competences.SCHEMA_SYNTHESE_CREATE;
                    RequestUtils.bodyToJson(request, validator, new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject synthese) {
                            final Long idTypePeriode = synthese.getLong("id_typePeriode");
                            final String idEleve = synthese.getString("id_eleve");
                            final String idStructure = synthese.getString("id_structure");
                            final String syntheseValue = synthese.getString("synthese");
                            syntheseBilanPeriodiqueService.createOrUpdateSyntheseBilanPeriodique(idTypePeriode,
                                    idEleve, idStructure, syntheseValue, defaultResponseHandler(request));
                        }
                    });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    /**
     * Créer une appreciation CPE avec les données passées en POST
     *
     * @param request
     */
    @Post("/appreciation/CPE/bilan/periodique")
    @ApiDoc("Créer ou mettre à jour une appreciation CPE du bilan périodique d'un élève pour une période donnée")
    @SecuredAction(value = "create.appreciation.CPE.bilan.periodique", type=ActionType.WORKFLOW)
    public void createOrUpdateAppreciationCPE(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    String validator = pathPrefix + Competences.SCHEMA_APPRECIATION_CPE_CREATE;
                    RequestUtils.bodyToJson(request, validator,
                            new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject appreciation) {
                                    final Long idPeriode = appreciation.getLong("id_periode");
                                    final String idEleve = appreciation.getString("id_eleve");
                                    appreciationCPEService.createOrUpdateAppreciationCPE(
                                            idPeriode,
                                            idEleve,
                                            appreciation.getString("appreciation"),
                                            DefaultResponseHandler.defaultResponseHandler(request));
                                }
                            });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    /**
     * Récupère les appreciations CPE de l'élève
     *
     * @param request
     */
    @Get("/appreciation/CPE/bilan/periodique")
    @ApiDoc("Récupère l'appreciation CPE du bilan périodique d'un élève pour une période donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessCPEConseilDeClasse.class)
    public void getAppreciationCPE(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    appreciationCPEService.getAppreciationCPE(Long.parseLong(request.params().get("id_periode")),
                            request.params().get("id_eleve"), defaultResponseHandler(request));
                } else {
                    badRequest(request);
                }
            }
        });
    }

    /**
     * Retourne la liste des avis prédéfinis du conseil de classe du bilan périodique
     * @param request
     */
    @Get("/avis/bilan/periodique")
    @ApiDoc("Retourne la liste des avis prédéfinis du conseil de classe du bilan périodique")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessConseilDeClasseStructureId.class)
    public void getLibelleAvis(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if(user != null){
                    String strTypeAvis = request.params().get("type_avis");
                    Long longTypeAvis = null;
                    if (strTypeAvis != null && strTypeAvis != "") {
                        longTypeAvis = Long.parseLong(request.params().get("type_avis"));
                    }
                    String idStructure = request.params().get("id_structure");
                    avisConseilService.getLibelleAvis(
                            longTypeAvis,
                            idStructure,
                            arrayResponseHandler(request));
                }else{
                    unauthorized(request);
                }
            }
        });
    }

    @Post("/avis/bilan/periodique")
    @ApiDoc("Créer un avis de conseil de classe")
    @SecuredAction(value = "", type=ActionType.RESOURCE)
    @ResourceFilter(CreateAvisConseilBilanPeriodique.class)
    public void createOpinion(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    String validator = pathPrefix + Competences.SCHEMA_CREATE_OPINION;
                    RequestUtils.bodyToJson(request, validator,
                            new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject opinion) {
                                    final Long typeAvis = opinion.getLong("type_avis");
                                    final String libelle = opinion.getString("libelle");
                                    final String idStructure = opinion.getString("id_etablissement");
                                    avisConseilService.createOpinion(
                                            typeAvis,
                                            libelle,
                                            idStructure,
                                            DefaultResponseHandler.defaultResponseHandler(request));
                                }
                            });
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Delete("/avis/bilan/periodique")
    @ApiDoc("Supprime un avis de conseil de classe")
    @SecuredAction(value = "create.avis.conseil.bilan.periodique", type=ActionType.WORKFLOW)
    public void deleteOpinion(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final Long idAvis = Long.parseLong(request.params().get("id_avis"));
                    avisConseilService.deleteOpinion(idAvis, DefaultResponseHandler.defaultResponseHandler(request));
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Put("/avis/bilan/periodique")
    @ApiDoc("Mets à jour un avis de conseil de classe")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(CreateAvisConseilBilanPeriodique.class)
    public void updateOpinion(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    final Long idAvis = Long.parseLong(request.params().get("id_avis"));
                    final boolean active = Boolean.parseBoolean(request.params().get("active"));
                    final String libelle = request.params().get("libelle");
                    avisConseilService.updateOpinion(idAvis, active, libelle,
                            DefaultResponseHandler.defaultResponseHandler(request));
                } else {
                    log.debug("User not found in session.");
                    Renders.unauthorized(request);
                }
            }
        });
    }

    @Post("/setAvisWorfklow")
    @ApiDoc("Méthode crée uniquement pour gérer le droit workflow pour la méthode suivante : setAvisConseil / setAvisOrientation")
    @SecuredAction(value = "set.avis.conseil.orientation.bilan.periodique", type = ActionType.WORKFLOW)
    public void setAvisWorfklow(final HttpServerRequest request) {
        badRequest(request);
    }

    /**
     * Récupère les avis de conseil de classe de l'élève
     *
     * @param request
     */
    @Get("/avis/conseil")
    @ApiDoc("Récupère l'avis de conseil d'un élève pour une période donnée")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getAvisConseil(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    avisConseilService.getAvisConseil(request.params().get("id_eleve"),
                            Long.parseLong(request.params().get("id_periode")), request.params().get("id_structure"),
                            arrayResponseHandler(request));
                } else {
                    badRequest(request);
                }
            }
        });
    }

    /**
     * Ajoute un avis du conseil de classe avec les données passées en POST
     *
     * @param request
     */
    @Post("/avis/conseil")
    @ApiDoc("Créer ou mettre à jour un avis de conseil d'un élève pour une période donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SetAvisConseilFilter.class)
    public void setAvisConseil(final HttpServerRequest request) {
        String validator = pathPrefix + Competences.SCHEMA_AVIS_CONSEIL_ORIENTATION_BILAN_PERIODIQUE;
        RequestUtils.bodyToJson(request, validator, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject idAvisClasse) {
                final Long idPeriode = idAvisClasse.getLong("id_periode");
                final String idEleve = idAvisClasse.getString("id_eleve");
                final Long idAvis = idAvisClasse.getLong("id_avis_conseil_bilan");
                final String idStructure = idAvisClasse.getString("id_structure");
                avisConseilService.createOrUpdateAvisConseil(idEleve, idPeriode, idAvis, idStructure,
                        defaultResponseHandler(request));
            }
        });
    }

    @Delete("/avis/conseil")
    @ApiDoc("Supprimer un avis de conseil d'un élève")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SetAvisConseilFilter.class)
    public void deleteAvisConseil(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user -> {
            if(user != null) {
                final Long idPeriode = Long.parseLong(request.params().get("id_periode"));
                final String idEleve = request.params().get("id_eleve");
                final String idStructure = request.params().get("id_structure");
                avisConseilService.deleteAvisConseil(idPeriode, idEleve, idStructure,
                        defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }

    /**
     * Récupère les orientations du conseil de classe de l'élève
     *
     * @param request
     */
    @Get("/avis/orientation")
    @ApiDoc("Récupère l'avis d'orientation d'un élève pour une période donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessConseilDeClasseStructureId.class)
    public void getAvisOrientation(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    avisOrientationService.getAvisOrientation(request.params().get("id_eleve"),
                            Long.parseLong(request.params().get("id_periode")), request.params().get("id_structure"),
                            arrayResponseHandler(request));
                } else {
                    badRequest(request);
                }
            }
        });
    }

    /**
     * Ajoute un avis du conseil de classe avec les données passées en POST
     *
     * @param request
     */
    @Post("/avis/orientation")
    @ApiDoc("Créer ou mettre à jour un avis d'orientation d'un élève pour une période donnée")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SetAvisConseilFilter.class)
    public void setAvisOrientation(final HttpServerRequest request) {
        String validator = pathPrefix + Competences.SCHEMA_AVIS_CONSEIL_ORIENTATION_BILAN_PERIODIQUE;
        RequestUtils.bodyToJson(request, validator, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject idAvisClasse) {
                final Long idPeriode = idAvisClasse.getLong("id_periode");
                final String idEleve = idAvisClasse.getString("id_eleve");
                final Long idAvis = idAvisClasse.getLong("id_avis_conseil_bilan");
                final String idStructure = idAvisClasse.getString("id_structure");
                avisOrientationService.createOrUpdateAvisOrientation(idEleve, idPeriode, idAvis, idStructure,
                        defaultResponseHandler(request));
            }
        });
    }

    @Delete("/avis/orientation")
    @ApiDoc("Supprimer un avis d'orientation d'un élève")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SetAvisConseilFilter.class)
    public void deleteAvisOrientation(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user -> {
            if(user != null) {
                final Long idPeriode = Long.parseLong(request.params().get("id_periode"));
                final String idEleve = request.params().get("id_eleve");
                final String idStructure = request.params().get("id_structure");
                avisOrientationService.deleteAvisOrientation(idPeriode, idEleve, idStructure,
                        defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        });
    }
}

