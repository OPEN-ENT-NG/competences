package fr.openent.competences.controllers;

import com.mongodb.util.JSON;
import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.*;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;


import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;


public class BilanPeriodiqueController extends ControllerHelper{

    private final BilanPeriodiqueService bilanPeriodiqueService;
    private final DefaultSyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;
    private final DefaultAppreciationCPEService appreciationCPEService;

    public BilanPeriodiqueController (EventBus eb){
        this.eb = eb;
        bilanPeriodiqueService = new DefaultBilanPerioqueService(eb);
        syntheseBilanPeriodiqueService = new DefaultSyntheseBilanPeriodiqueService();
        appreciationCPEService = new DefaultAppreciationCPEService();

    }

    @Get("/bilan/periodique/eleve/:idEleve")
    @ApiDoc("renvoit tous les éléments pour le bilan périodique d'un élève")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getSuiviDesAcquisEleve(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                final String idEtablissement = request.params().get("idEtablissement");
                final String idPeriodeString = request.params().get("idPeriode");
                final Long idPeriode = (idPeriodeString != null)? Long.parseLong(idPeriodeString): null;
                final String idEleve = request.params().get("idEleve");
                final String idClasse = request.params().get("idClasse");
                bilanPeriodiqueService.getSuiviAcquis(idEtablissement, idPeriode, idEleve, idClasse,
                        arrayResponseHandler(request));
            }
        });

    }

    @Get("/eleve/evenements/:idEleve")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
    public void getAbsencesAndRetards (final HttpServerRequest request) {
        final String  idEleve = request.params().get("idEleve");
        bilanPeriodiqueService.getRetardsAndAbsences(idEleve,arrayResponseHandler(request));
    }

    /**
     * Récupère les synthèses de l'élève
     * @param request
     */
    @Get("/syntheseBilanPeriodique")
    @ApiDoc("Récupère la synthèse d'un élève pour une période donnée")
    @SecuredAction(value = "", type= ActionType.AUTHENTICATED)
//    @ResourceFilter(AccessAppreciationClasseFilter.class)
    public void getSyntheseBilanPeriodique(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>(){
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    syntheseBilanPeriodiqueService.getSyntheseBilanPeriodique(
                            Long.parseLong(request.params().get("id_typePeriode")),
                            request.params().get("id_eleve"),
                            defaultResponseHandler(request));
                } else {
                    badRequest(request);
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
    @SecuredAction(value = "create.appreciation.CPE.bilan.periodique", type = ActionType.WORKFLOW)
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
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getAppreciationCPE(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(final UserInfos user) {
                if (user != null) {
                    appreciationCPEService.getAppreciationCPE(
                            Long.parseLong(request.params().get("id_periode")),
                            request.params().get("id_eleve"),
                            defaultResponseHandler(request));
                } else {
                    badRequest(request);
                }
            }
        });
    }
}















