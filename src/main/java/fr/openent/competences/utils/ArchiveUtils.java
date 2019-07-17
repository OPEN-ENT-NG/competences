package fr.openent.competences.utils;

import fr.openent.competences.Competences;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.*;

public class ArchiveUtils {

    public static final String ARCHIVE_BULLETIN_TABLE = "archive_bulletins";
    public static final String ARCHIVE_BFC_TABLE = "archive_bfc";
    private static final Logger log = LoggerFactory.getLogger(ArchiveUtils.class);

    private static void getIdFileArchive(String idEleve, String idClasse, Long idPeriode, String table, Boolean isCycle,
                                  Handler<Either<String,JsonObject>> handler){
        String query = " SELECT id_file " +
                " FROM notes." + table +
                " WHERE id_eleve=? AND id_classe = ? AND " + (isCycle?"id_cycle=? ":"id_periode=? ") +
                " ORDER by created  DESC limit 1;";
        JsonArray values = new JsonArray().add(idEleve).add(idClasse).add(idPeriode);
        Sql.getInstance().prepared(query, values, Competences.DELIVERY_OPTIONS,
                SqlResult.validUniqueResultHandler(handler));

    }

    private static void clearArchiveTable(JsonArray ids, String table, Handler<Either<String,JsonObject>> handler){
        String query = "DELETE FROM notes." + table+ " WHERE id_file IN "+ Sql.listPrepared(ids.getList()) + " ;";
        Sql.getInstance().prepared(query, ids, Competences.DELIVERY_OPTIONS,
                SqlResult.validUniqueResultHandler(handler));
    }

    private static void getAllIdFileArchive(String table, Handler<Either<String,JsonObject>> handler){
        String query = " SELECT array_agg(id_file) as ids FROM notes." + table+ " ;";
        JsonArray values = new JsonArray();
        Sql.getInstance().prepared(query, values, Competences.DELIVERY_OPTIONS,
                SqlResult.validUniqueResultHandler(handler));
    }

    public static void deleteAll(String table, Storage storage, Handler<JsonObject> handler){
        Future<JsonObject> idFileFuture = Future.future();
        getAllIdFileArchive(table, event -> {
            FormateFutureEvent.formate(idFileFuture, event);
            JsonArray results = idFileFuture.result().getJsonArray("ids");
            if(results == null){
                handler.handle(new JsonObject().put(RESULT, "NO files archived "));
                return;
            }
            JsonArray removesFiles = new JsonArray();
            results.stream().forEach(
                    files -> removesFiles.add(((JsonArray)files).getValue(1)));

            if(idFileFuture.failed() ||  removesFiles == null) {
                String error = (removesFiles == null) ? idFileFuture.cause().getMessage() : " no result";
                log.error("deleteAll : " + error);
                handler.handle(new JsonObject().put(ERROR, error));
                return;
            }
            storage.removeFiles(removesFiles, remove -> {
                log.info(" [Remove Archives " + table + "] " + remove.encode());
                clearArchiveTable(removesFiles, table, deleteEvent -> {
                    JsonObject response = new JsonObject().put(RESULT,remove.encode());
                    if (deleteEvent.isRight()) {
                        handler.handle(response.put("deleteOK", deleteEvent.right().getValue()));
                    }
                    else {
                        handler.handle(response.put("deleteKO", deleteEvent.left().getValue()));
                    }
                });

            });
        });
    }

    private static void getArchiveBulletin(final String idEleve, final String idClasse, final Long idPeriode,
                                           Storage storage, String table, Boolean isCycle,
                                           Handler<Either<String, Buffer>> bufferEither){

        Future<JsonObject> idFileFuture = Future.future();
        getIdFileArchive(idEleve, idClasse, idPeriode,table, isCycle, event -> {
            FormateFutureEvent.formate(idFileFuture, event);
            JsonObject result = idFileFuture.result();
            if(idFileFuture.failed() ||  result == null){
                String error = (result == null)? idFileFuture.cause().getMessage() : " no result";

                log.error("get" + table + " : " + error);
                bufferEither.handle(new Either.Left<>(error));
                return;
            }
            storage.readFile(result.getString("id_file"), fileBuffer -> {
                bufferEither.handle(new Either.Right<>(fileBuffer));
            });
        });

    }

    public static void getArchiveBulletin(final String idEleve, final String idClasse, final Long idPeriode,
                                          Storage storage, String table, Boolean isCycle,
                                          final HttpServerRequest request) {


        getArchiveBulletin(idEleve, idClasse, idPeriode, storage, table, isCycle, bufferEither -> {
            if (bufferEither.isLeft()) {
                Renders.notFound(request, bufferEither.left().getValue());
            } else {
                Buffer file = bufferEither.right().getValue();
                if (file == null) {
                    Renders.notFound(request, "in table");
                    return;
                }
                request.response().putHeader("Content-Type", "application/pdf");
                request.response().putHeader("Content-Disposition",
                        "attachment; filename=archive.pdf");
                request.response().end(file);
                log.info(new SimpleDateFormat("HH:mm:ss:S").format(new Date().getTime())
                        + " -> Fin Generation PDF du template ");
            }
        });
    }
    public static String getFileNameForStudent(JsonObject student){
        String filename = StringUtils.EMPTY_STRING;
        String separator = "_";
        String end = ".pdf";

        String classeName = student.getString(CLASSE_NAME_TO_SHOW);
        if(isNull(classeName)){
            classeName =  student.getString("nomClasse");
        }
        String periode = student.getString(PERIODE);

        if(isNull(periode)){
            periode = student.getString("cycle");
        }

        filename += student.getString(STRUCTURE);
        filename += separator;
        filename += classeName;
        filename += separator;
        filename += student.getString(LAST_NAME_KEY);
        filename += separator;
        filename += student.getString(FIRST_NAME_KEY);
        filename += separator;
        filename += periode;
        filename += separator;
        filename += student.getString(Competences.ID_ELEVE_KEY);
        filename += end;
        return filename;
    }
}
