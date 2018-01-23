package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface ExportService {

    public void getExportEval(final Boolean text, JsonObject devoir, String idGroupe, String idEtablissement, HttpServerRequest request,
                              Handler<Either<String, JsonObject>> handler);

    public void getExportReleveComp(final Boolean text, final String idEleve, String[] idGroupes, final String idEtablissement, final List<String> idMatieres,
                                    Long idPeriodeType, final Handler<Either<String, JsonObject>> handler);
}
