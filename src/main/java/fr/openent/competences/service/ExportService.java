package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ExportService {

    public void getExportEval(final Boolean text, Boolean only_evaluation, JsonObject devoir, String idGroupe, String idEtablissement, HttpServerRequest request,
                              Handler<Either<String, JsonObject>> handler);

    public void getExportReleveComp(final Boolean text, final Boolean pByEnseignement, final String idEleve, String[] idGroupes,
                                    String[] idFunctionalGroupes, final String idEtablissement, final List<String> idMatieres,
                                    Long idPeriodeType, final Boolean isCycle, final Handler<Either<String, JsonObject>> handler);

    public void getExportRecapEval(final Boolean text, final Long idCycle, final String idEtablissement, final Handler<Either<String, JsonArray>> handler);
}
