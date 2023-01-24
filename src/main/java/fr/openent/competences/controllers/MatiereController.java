package fr.openent.competences.controllers;

import fr.openent.competences.Utils;
import fr.openent.competences.enums.Common;
import fr.openent.competences.helper.ManageError;
import fr.openent.competences.model.Subject;
import fr.openent.competences.security.AccessIfMyStructure;
import fr.openent.competences.security.AdministratorRight;
import fr.openent.competences.service.MatiereService;
import fr.openent.competences.service.impl.DefaultMatiereService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.enums.subjects.SubjectOther.*;
import static fr.openent.competences.enums.subjects.SubjectKey.*;
import static org.entcore.common.http.response.DefaultResponseHandler.*;

public class MatiereController extends ControllerHelper {

    private final MatiereService matiereService;

    public MatiereController(EventBus eb) {
        matiereService = new DefaultMatiereService(eb);
    }


    @Post("/matieres/libelle/model/save")
    @ApiDoc("sauvegarde un model de libelle de matiere pour un établissement")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void setModel(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, ressource -> {
            String idStructure = ressource.getString(ID_STRUCTURE_KEY);
            String title = ressource.getString(TITLE);
            Long idModel = ressource.getLong(ID_KEY);
            JsonArray libelleMatiere = ressource.getJsonArray(SUBJECTS);
            if(idStructure != null) {
                matiereService.saveModel(idStructure, title, idModel, libelleMatiere, event -> {
                    if(event.isLeft()){
                        JsonObject error = new JsonObject().put("error", event.left().getValue());
                        Renders.renderJson(request, error, 400);
                    } else {
                        Renders.renderJson(request, event.right().getValue());
                    }
                });
            } else {
                badRequest(request);
            }
        });
    }

    @Get("/matieres/models/:idStructure")
    @ApiDoc("Retourne les models de libellé d'un établissement")
    @SecuredAction(value = "", type=ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void getModels(final HttpServerRequest request) {
        String idStructure = request.params().get(ID_STRUCTURE_KEY);
        if(idStructure != null) {
            try {
                matiereService.getModels(idStructure, null, arrayResponseHandler(request));
            }
            catch (Exception e) {
                Renders.renderError(request, new JsonObject().put("error", e.getCause()));
            }
        }
        else {
            badRequest(request);
        }
    }

    @Delete("/matieres/model/:id")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteModel(final HttpServerRequest request) {
        matiereService.deleteModeleLibelle(request.params().get("id"), arrayResponseHandler(request));
    }

    @Get("/matieres/devoirs/update")
    @ApiDoc("on met par défaut une sousMatiere à chaque devoir")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void updateDevoirs(final HttpServerRequest request) {
        matiereService.updateDevoirs(null, arrayResponseHandler(request));
    }

    @Get("/subjects/short-label/subjects")
    @ApiDoc("Get subjects with sort-labels")
    @SecuredAction(value = "", type= ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getShortLabetToSubjects(final HttpServerRequest request) {
        try{
            JsonArray idsSubjectPrepared = new JsonArray();
            String paramsSubjectsIds = request.params().get("ids");
            List<String> subjectsIds = Arrays.asList(paramsSubjectsIds.split(","));
            if(paramsSubjectsIds.isEmpty() && subjectsIds.size() == 0){
                defaultResponseHandler(request, 204);
            } else {
                for (String subjectId : subjectsIds) {
                    idsSubjectPrepared.add(subjectId);
                }
                Utils.getLibelleMatiere(eb, idsSubjectPrepared, subjectsEvent -> {
                    if(subjectsEvent.isLeft()) {
                        badRequest(request, "Error left subjects: " + subjectsEvent.left().getValue());
                    } else {
                        Renders.renderJson(request, new JsonObject().put("subjects", subjectsEvent.right().getValue()));
                    }
                });
            }
        } catch( Exception errorCatch) {
            badRequest(request, "Error in catch: " + errorCatch);
        }

    }

    @Delete("/subjects/:idStructure/id-structure/initialization-rank")
    @ApiDoc("Initialization rank all subjects with id structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void initializationRank(final HttpServerRequest request) {
        String idStructure;
        try {
        idStructure = request.params().get("idStructure");
        } catch (Exception errorWhenGetBody) {
            ManageError.requestFailError(request,
                    Common.ERROR.getString(),
                    "Error during recovery of the params",
                    errorWhenGetBody.toString());
            return;
        }

        if(idStructure.isEmpty()) badRequest(request);

        Subject subject = new Subject();
        subject.setIdStructure(idStructure);

        matiereService.removeRankOnSubject(subject,  notEmptyResponseHandler(request));
    }

    @Put("/subjects/reshuffle-rank")
    @ApiDoc("Reshuffle the order of subject after drag it one")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void organisationOrderSubject(final HttpServerRequest request) {

        RequestUtils.bodyToJson(request, subjectBody -> {
            String idStructure;
            String idSubject;
            int indexStart;
            int indexEnd;

            try {
                idStructure = subjectBody.getString(ID_STRUCTURE.getString());
                idSubject = subjectBody.getString(ID.getString());
                indexStart = subjectBody.getInteger(INDEX_START.getString());
                indexEnd =  subjectBody.getInteger(INDEX_END.getString());
            } catch (Exception errorWhenGetBody) {
                ManageError.requestFailError(request,
                        Common.ERROR.getString(),
                        "Error during recovery of the body",
                        errorWhenGetBody.toString());
                return;
            }
            String direction = indexEnd == -1 ? DRAG_UP.getString() : DRAG_DOWN.getString();
            Boolean isUp = direction.equals(DRAG_UP.getString());
                Subject.getListSubject(eb, idStructure, subjectsEvent -> {
                    if (subjectsEvent.isLeft()) {
                        ManageError.requestFailError(request,
                                Common.ERROR.getString(),
                                "Error during recovery of the listSubject",
                                subjectsEvent.left().getValue());
                        return;
                    }
                    Subject subject = new Subject(
                            idSubject,
                            idStructure,
                            (isUp ? indexStart : indexEnd)
                    );

                    List<Subject> subjects = subjectsEvent.right().getValue();

                    subjects = subjects.stream()
                            .filter(subjectFiltered -> !(subjectFiltered).getId().equals(idSubject))
                            .collect(Collectors.toList());

                    for (int i = indexStart; i < (isUp ? subjects.size() : indexEnd); i++) {
                        subjects.get(i).setRank(isUp ? i + 1 : i);
                    }

                    subjects.add(subject);
                    matiereService.updateListRank(subjects, notEmptyResponseHandler(request));
                });
            });
        }
}
