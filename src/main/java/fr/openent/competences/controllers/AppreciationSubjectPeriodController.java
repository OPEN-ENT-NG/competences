package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.model.AppreciationSubjectPeriodModel;
import fr.openent.competences.security.AccessReleveFilter;
import fr.openent.competences.security.utils.FilterPeriodeUtils;
import fr.openent.competences.security.utils.FilterUserUtils;
import fr.openent.competences.service.AppreciationSubjectPeriodService;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.impl.DefaultAppreciationSubjectPeriod;
import fr.openent.competences.service.impl.DefaultNoteService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class AppreciationSubjectPeriodController extends ControllerHelper {
    private final NoteService notesService;
    private final AppreciationSubjectPeriodService appreciationSubjectPeriodService;
    private final String URL = "/appreciation-subject-period";

    public AppreciationSubjectPeriodController(EventBus eb) {
        this.eb = eb;
        notesService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb);
        appreciationSubjectPeriodService = new DefaultAppreciationSubjectPeriod( Competences.COMPETENCES_SCHEMA,
                Competences.APPRECIATION_MATIERE_PERIODE_TABLE, Competences.REL_APPRECIATION_USERS_NEO, eb);
    }

    @Post(URL)
    @ApiDoc("Create an appreciation in table appreciation_matière_periode")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void createAppreciationSubjectPeriod(final HttpServerRequest request) {
        preparedUpdateOrInsertSqlAppreciationSubjectPeriod(request);
    }

    @Put(URL)
    @ApiDoc("Update an appreciation in table appreciation_matière_periode")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void updateAppreciationSubjectPeriod(final HttpServerRequest request) {
        preparedUpdateOrInsertSqlAppreciationSubjectPeriod(request);
    }

    @Delete(URL)
    @ApiDoc("Delete an appreciation in table appreciation_matière_periode")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessReleveFilter.class)
    public void deleteAppreciationSubjectPeriod(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, resource -> {
            AppreciationSubjectPeriodModel appreciationSubjectPeriod = new AppreciationSubjectPeriodModel(resource);

            RequestUtils.bodyToJson(request, result -> {
                checkAllAccessAndStartCRUD(request, resource, authorized -> {
                    if (authorized.isRight()) {
                        notesService.deleteColonneReleve(appreciationSubjectPeriod.getIdStudent(),
                                appreciationSubjectPeriod.getIdPeriod(), appreciationSubjectPeriod.getIdSubject(),
                                appreciationSubjectPeriod.getIdClassSchool(),
                                Competences.APPRECIATION_MATIERE_PERIODE_TABLE, arrayResponseHandler(request));
                    } else {
                        unauthorized(request, authorized.left().getValue());
                    }
                });
            });
        });
    }


    private void preparedUpdateOrInsertSqlAppreciationSubjectPeriod(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJson(request, resource -> {
                AppreciationSubjectPeriodModel appreciationSubjectPeriod = new AppreciationSubjectPeriodModel(resource);

                RequestUtils.bodyToJson(request, result -> {
                    final String idStructure = resource.getString("idEtablissement");
                    checkAllAccessAndStartCRUD(request, resource, authorized -> {
                        if (authorized.isRight()) {
                            appreciationSubjectPeriodService.updateOrInsertAppreciationSubjectPeriod(appreciationSubjectPeriod,
                                    user, idStructure, defaultResponseHandler(request));
                        } else {
                            unauthorized(request, authorized.left().getValue());
                        }
                    });
                });
            });
        });
    }

    private void checkAllAccessAndStartCRUD(HttpServerRequest request, JsonObject resource, Handler<Either<String, JsonArray>> handler) {
        // Vérification de l'accès à la matière
        UserUtils.getUserInfos(eb, request, user -> {
            checkAccessToSubject(request, user, resource, handler);
        });
    }

    private void checkAccessToSubject(HttpServerRequest request, UserInfos user, JsonObject resource, Handler<Either<String, JsonArray>> handler) {
        // Vérification de la date de fin de saisie
        final String idSubject = resource.getString("idMatiere");
        final String idStructure = resource.getString("idEtablissement");
        final Boolean isPeriodicReview = (resource.getBoolean("isBilanPeriodique") != null) ?
                resource.getBoolean("isBilanPeriodique") : false;
        new FilterUserUtils(user, eb).validateMatiere(request, idStructure, idSubject, isPeriodicReview,
                hasAccessToSubject -> {
                    if (!hasAccessToSubject) {
                        log.error("Not access to Subject");
                        handler.handle(new Either.Left<>("Not access to Subject"));
                    } else {
                        checkDateOfEndInput(request, user, resource, handler);
                    }
                });
    }

    private void checkDateOfEndInput(HttpServerRequest request, UserInfos user, JsonObject resource, Handler<Either<String, JsonArray>> handler) {
        //verif date fin de saisie
        final String idClassSchool = resource.getString("idClasse");
        final Long idPeriod = resource.getLong("idPeriode");
        new FilterPeriodeUtils(eb, user).validateEndSaisie(request, idClassSchool, idPeriod.intValue(), isUpdatable -> {
            if (!isUpdatable) {
                log.error("Not access to API because of end of saisie");
                handler.handle(new Either.Left<>("Not access to API because of end of saisie"));
            } else {
                handler.handle(new Either.Right<>(new JsonArray().add("ok ok it is me ")));
            }
        });
    }
}
