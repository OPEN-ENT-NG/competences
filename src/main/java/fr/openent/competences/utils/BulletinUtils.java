package fr.openent.competences.utils;

import fr.openent.competences.Competences;
import fr.openent.competences.helpers.FormateFutureEvent;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;

import static fr.openent.competences.Competences.*;

public class BulletinUtils {

    public static final String STORAGE_BULLETIN_TABLE = "archive_bulletins";
    private static final Logger log = LoggerFactory.getLogger(BulletinUtils.class);

    public static void saveIdBulletin(String idEleve, String idClasse, String externalIdClasse,
                                      String idEtablissement, Long idPeriode, String idFile, String name,
                                      String idParent, String idYear, Handler<Either<String, JsonObject>> handler){
        JsonArray values = new JsonArray().add(idClasse).add(idEleve).add(idEtablissement).add(externalIdClasse)
                .add(idPeriode).add(idFile).add(name).add(idParent != null ? idParent : "NULL").add(idYear).add(idFile);

        String query = "INSERT INTO " + COMPETENCES_SCHEMA + "." + STORAGE_BULLETIN_TABLE +
                " (id_classe, id_eleve, id_etablissement, external_id_classe, id_periode, id_file, file_name, id_parent, id_annee)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT (id_classe, id_eleve, id_etablissement, external_id_classe, id_periode, id_parent, id_annee)" +
                " DO UPDATE SET id_file = ?;";

        Sql.getInstance().prepared(query, values, Competences.DELIVERY_OPTIONS, result -> {
            JsonObject body = result.body();
            if (!"ok".equals(body.getString(STATUS))) {
                handler.handle(new Either.Left<>(body.getString(MESSAGE)));
            }
            else{
                log.info("saveIdBulletin : " + idEleve + " " + externalIdClasse + " " + name + " " + idFile);
                handler.handle(new Either.Right<>(body));
            }
        });
    }

    public static Handler<Either<String, JsonObject>> saveBulletinHandler(final String idEleve, final String idClasse,
                                                                          final String externalIdClasse,
                                                                          final String idEtablissement, final Long idPeriode,
                                                                          Handler<Either<String, JsonObject>> handler){
        String noFileStored = "No file stored: (eleve : " + idEleve + ", classe : " + idClasse + ", periode  : "
                + idPeriode + ", externalIdClasse : "+ externalIdClasse + ", idEtablissement : " + idEtablissement + ")";
        return saveEvent -> {
            if (saveEvent.isRight()) {
                log.debug("bulletin stored: (eleve : " + idEleve + ", classe : " + idClasse + ", " +
                        "periode : " + idPeriode + ") ");
            }
            else{
                log.error(noFileStored);
            }
            handler.handle(new Either.Right<>(new JsonObject()));
        };
    }

    private static void getIdFile(String idEleve, String idClasse, Long idPeriode, String idEtablissement,
                                  String idYear, Handler<Either<String, JsonArray>> handler){
        String query = "SELECT id_file, id_parent FROM " + COMPETENCES_SCHEMA + "." + STORAGE_BULLETIN_TABLE +
                " WHERE id_eleve = ? AND id_classe = ? AND id_periode = ? AND id_etablissement = ? AND id_annee = ?" +
                " ORDER by created DESC;";
        JsonArray values = new JsonArray().add(idEleve).add(idClasse).add(idPeriode).add(idEtablissement).add(idYear);
        Sql.getInstance().prepared(query, values, Competences.DELIVERY_OPTIONS,
                SqlResult.validResultHandler(handler));
    }

    private static void getBulletin(final String idEleve, final String idClasse, final Long idPeriode,
                                    String idEtablissement, String idParent, String idYear, Storage storage,
                                    Handler<Either<String, Buffer>> bufferEither){
        Future<JsonArray> idFileFuture = Future.future();
        getIdFile(idEleve, idClasse, idPeriode, idEtablissement, idYear, event -> {
            FormateFutureEvent.formate(idFileFuture, event);
            JsonArray result = idFileFuture.result();
            if(idFileFuture.failed() || result == null || result.size() == 0){
                String error = (result == null) ? idFileFuture.cause().getMessage() : "no result";

                if(result != null && result.size() == 0)
                    log.error("error get bulletin storage : " + error);
                bufferEither.handle(new Either.Left<>(error));
                return;
            }
            JsonObject value = result.getJsonObject(0);
            if(idParent != null) {
                JsonArray resultFilterParent = new JsonArray();
                for(Object res : result){
                    JsonObject resJson = (JsonObject) res;
                    if(resJson.getString("id_parent") != null && !resJson.getString("id_parent").equals("NULL")
                            && resJson.getString("id_parent").equals(idParent))
                        resultFilterParent.add(resJson);
                }
                if(resultFilterParent.size() > 0)
                    value = resultFilterParent.getJsonObject(0);
            }
            storage.readFile(value.getString("id_file"), fileBuffer -> {
                bufferEither.handle(new Either.Right<>(fileBuffer));
            });
        });
    }

    public static void getBulletin(final String idEleve, final String idClasse, final Long idPeriode,
                                   String idEtablissement, String idParent, String idYear,
                                   Storage storage, final HttpServerRequest request) {
        getBulletin(idEleve, idClasse, idPeriode, idEtablissement, idParent, idYear, storage, bufferEither -> {
            if (bufferEither.isLeft()) {
                Renders.noContent(request);
            } else {
                Buffer file = bufferEither.right().getValue();
                if (file == null) {
                    Renders.noContent(request);
                    return;
                }
                request.response().putHeader("Content-Type", "application/pdf");
                request.response().putHeader("Content-Disposition","attachment; filename=bulletin.pdf");
                request.response().end(file);
            }
        });
    }

    public static String getIdParentForStudent(JsonObject eleve) {
        if(eleve.getBoolean("getResponsable") && eleve.getJsonArray("responsables").size() > 1) {
            JsonArray responsables = eleve.getJsonArray("responsables");
            boolean isDivorcedParents = !(responsables.getJsonObject(0).getString("address")
                    .equals(responsables.getJsonObject(1).getString("address")));
            if(isDivorcedParents){
                for(Object responsable : responsables){
                    JsonObject responsableJson = (JsonObject) responsable;
                    if(responsableJson.getString("address").equals(eleve.getJsonArray("responsableLibelle").getString(1))
                            && responsableJson.getString("lastNameRelative").equals(eleve.getString("responsableLastName"))){
                        return responsableJson.getString("externalIdRelative");
                    }
                }
            }
        }

        return null;
    }
}
