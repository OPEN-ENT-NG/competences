package fr.openent.competences.utils;

import fr.openent.competences.Competences;
import fr.openent.competences.folders.FolderExporterZip;
import fr.openent.competences.helpers.FormateFutureEvent;
import fr.openent.competences.model.PdfFile;
import fr.openent.competences.model.Folder;
import fr.openent.competences.service.impl.ArchiveWorker;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Competences.ACTION;
import static fr.openent.competences.Competences.FIRST_NAME_KEY;
import static fr.openent.competences.Competences.LAST_NAME_KEY;
import static fr.openent.competences.Utils.isNull;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.*;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.Renders.getHost;

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

    private static void clearCompletedTable(){
        String query = "DELETE FROM " + Competences.EVAL_SCHEMA
                + ".arhive_bulletins_complet WHERE true ;";
        Sql.getInstance().prepared(query, new JsonArray(), Competences.DELIVERY_OPTIONS,
                event -> {
                    JsonObject body = event.body();
                    if(!body.getString(STATUS).equals(OK)){
                        String message = body.getString(MESSAGE);
                        log.error("[clearCompletedTable] :: " + message);
                    }
                    else{
                        JsonArray structures = body.getJsonArray(RESULTS);
                        log.info("[clearCompletedTable] :: " + structures.encode());
                    }
                });
    }

    private static void clearArchiveTable(JsonArray ids, String table, Handler<Either<String,JsonObject>> handler){
        String query = "DELETE FROM notes." + table+ " WHERE id_file IN "+ Sql.listPrepared(ids.getList()) + " ;";
        Sql.getInstance().prepared(query, ids, Competences.DELIVERY_OPTIONS,
                SqlResult.validUniqueResultHandler(handler));
        if(table.equals(ARCHIVE_BULLETIN_TABLE)){
            clearCompletedTable();
        }
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

        filename += classeName;
        filename += separator;
        filename += student.getString(LAST_NAME_KEY);
        filename += separator;
        filename += student.getString(FIRST_NAME_KEY);
        filename += separator;
        filename += periode;
        filename += end;
        return filename;
    }

    public static void generateArchiveBulletin(EventBus eb, HttpServerRequest request) {
        final JsonObject action = new JsonObject()
                .put("action", "structure.getStructuresActives")
                .put("module","notes");
        eb.send(VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>(){
            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray idStructures = event.body().getJsonArray("results");
                JsonObject action = new JsonObject()
                        .put(ACTION, ArchiveWorker.ARCHIVE_BULLETIN)
                        .put(HOST, getHost(request))
                        .put(ACCEPT_LANGUAGE, I18n.acceptLanguage(request))
                        .put(X_FORWARDED_FOR, request.headers().get(X_FORWARDED_FOR) == null)
                        .put(ID_STRUCTURES_KEY,idStructures)
                        .put(PATH, request.path());

                eb.send(ArchiveWorker.class.getSimpleName(), action, Competences.DELIVERY_OPTIONS);
                Renders.ok(request);
//                request.response().setStatusMessage("ok").setStatusCode(201).end();
            }
        }));
    }

    public static void getArchiveBFCZip(String idStructure, HttpServerRequest request, EventBus eb, Storage storage, Vertx vertx) {
        getListToDownloadBFCSQL(idStructure,eb,storage, vertx,request);
    }



    public static void getArchiveBulletinZip(String idStructure, HttpServerRequest request, EventBus eb, Storage storage, Vertx vertx) {
        getListToDownloadSQL(idStructure,eb,storage, vertx,request);
    }
    private static void getListToDownloadBFCSQL(String idStructure, EventBus eb, Storage storage, Vertx vertx, HttpServerRequest request) {
        String query = "SELECT id_classe,id_etablissement, id_eleve,id_file , file_name as name from " + COMPETENCES_SCHEMA + ".archive_bfc where id_etablissement =  ?";
        JsonArray params = new JsonArray().add(idStructure);
        executeSqlRequest(eb, storage, vertx, request, query, params);
    }

    private static void getListToDownloadSQL(String idStructure, EventBus eb, Storage storage, Vertx vertx, HttpServerRequest request) {
        String query = "SELECT id_classe,id_etablissement, id_eleve,id_file , file_name as name from " + COMPETENCES_SCHEMA + ".archive_bulletins where id_etablissement =  ?";
        JsonArray params = new JsonArray().add(idStructure);

        executeSqlRequest(eb, storage, vertx, request, query, params);
    }

    private static void executeSqlRequest(EventBus eb, Storage storage, Vertx vertx, HttpServerRequest request, String query, JsonArray params) {
        List<PdfFile> listPdf = new ArrayList<>();
        Sql.getInstance().prepared(query, params, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                JsonArray results =  event.body().getJsonArray("results");
                for(int i = 0 ; i < results.size() ; i ++ ){
                    JsonArray result = results.getJsonArray(i);
                    PdfFile bpdf = new PdfFile(result.getString(0),result.getString(1),result.getString(2),result.getString(3),result.getString(4));
                    listPdf.add(bpdf);
                }
                if(listPdf.size() > 0 ) {
                    createFolders(listPdf, eb, storage, vertx, request);
                }else{
                    request.response().setStatusCode(204).setStatusMessage("No data to export").end();
                }
            }
        });
    }

    private static void createFolders(List<PdfFile> listBulletin, EventBus eb, Storage storage, Vertx vertx, HttpServerRequest request) {
        String idStructure = listBulletin.get(0).getId_structure();
        String query = "Match (s:Structure{id:{idStructure} }) return s.name as name";
        Neo4j.getInstance().execute(query, new JsonObject().put(ID_STRUCTURE_KEY, idStructure), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                Folder structureFolder =  new Folder(event.body().getJsonArray("result").getJsonObject(0).getString("name"));
                structureFolder.setId_folder(idStructure);
                createClassesFolders(structureFolder,listBulletin,eb, storage, vertx, request);

            }
        });
    }

    private static void createClassesFolders(Folder structureFolder, List<PdfFile> listBulletin, EventBus eb, Storage storage, Vertx vertx, HttpServerRequest request) {
        List<String> idsClasses = new ArrayList<>();
        for(int i = 0; i < listBulletin.size();i++){
            String idClasse = listBulletin.get(i).getId_class();
            if(!idsClasses.contains(idClasse)) {
                idsClasses.add(idClasse);
            }
        }

        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getClassesInfo")
                .put("idClasses", idsClasses);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(handleGetClassesInfo(structureFolder, listBulletin, eb, storage, vertx, request)));


    }

    private static Handler<Message<JsonObject>> handleGetClassesInfo(Folder structureFolder, List<PdfFile> listBulletin, EventBus eb, Storage storage, Vertx vertx, HttpServerRequest request) {
        return message -> {
            JsonObject body = message.body();
            List<Future> futures = new ArrayList<>();

            if (OK.equals(body.getString(STATUS))) {
                JsonArray results = body.getJsonArray(RESULTS);
                for(int i = 0 ; i < results.size();i++){
                    Future<Folder> folderFuture = Future.future();
                    futures.add(folderFuture);

                }

                CompositeFuture.all(futures).setHandler(event -> {
                    if (event.succeeded()) {
                        event.result().list().forEach(f ->{
                            ((Folder)f).setId_parent(structureFolder.getId_folder());
                            structureFolder.addFolder((Folder)f);
                        });
                        FolderExporterZip zipBuilder = new FolderExporterZip(storage, vertx.fileSystem(), false);
                        List<JsonObject> files = new ArrayList<>();
                        files = getAllFilesAndFolders(structureFolder);

                        zipBuilder.exportAndSendZip(structureFolder.toJsonObject(),files , request, true).setHandler(zipEvent -> {
                            if (zipEvent.failed()) {
                                request.response().setStatusCode(500).end();
                            }else{
                                log.info("jobs done");
                            }
                        });
                    }
                });

                for(int i = 0 ; i < results.size();i++){
                    Folder classFolder = new Folder(results.getJsonObject(i).getString("name"));
                    classFolder.setId_folder(results.getJsonObject(i).getString("id"));
                    createStudentFoldersForClass(structureFolder,classFolder,listBulletin,eb,getHandlerFolder(futures.get(i)));
                }

            }

        };
    }

    private static List<JsonObject> getAllFilesAndFolders(Folder structureFolder) {
        return structureFolder.getAllFilesAndFolders();
    }

    private static void createStudentFoldersForClass(Folder structureFolder, Folder classFolder, List<PdfFile> listBulletin, EventBus eb, Handler<Either<String, Folder>> handlerFolder) {
        List<String> idsStudents = new ArrayList<>();
        for (int i = 0; i < listBulletin.size(); i++) {
            PdfFile pdfFile = listBulletin.get(i);
            String idClasse = pdfFile.getId_class();
            if (idClasse.equals(classFolder.getId_folder())) {
                String idStudent = pdfFile.getId_student();
                if (!idsStudents.contains(idStudent)) {
                    idsStudents.add(idStudent);
                }
            }
        }

        JsonObject action = new JsonObject()
                .put(ACTION, "eleve.getInfoEleve")
                .put("idEleves", idsStudents)
                .put("idEtablissement",listBulletin.get(0).getId_structure());
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (OK.equals(body.getString(STATUS))) {
                JsonArray results= body.getJsonArray(RESULTS);
                for (int i = 0 ; i < results.size(); i++){
                    JsonObject result = results.getJsonObject(i);
                    Folder studentFolder = new Folder(result.getString("firstName") + "_" + result.getString("lastName"));
                    studentFolder.setId_folder(result.getString("idEleve"));
                    studentFolder.setId_parent(classFolder.getId_folder());
                    addPdfToStudent(studentFolder,listBulletin);
                    classFolder.addFolder(studentFolder);
                }
                handlerFolder.handle(new Either.Right<>(classFolder));

            }
        }));


    }

    private static void addPdfToStudent( Folder studentFolder, List<PdfFile> listBulletin) {
        for (int i = 0; i < listBulletin.size(); i++) {
            PdfFile pdfFile = listBulletin.get(i);
            String idStudent = pdfFile.getId_student();
            if(idStudent.equals(studentFolder.getId_folder())){
                pdfFile.setId_parent(studentFolder.getId_folder());
                studentFolder.addBulletin(pdfFile);
            }
        }
    }

    private static Handler<Either<String, Folder>> getHandlerFolder(Future<Folder> serviceFuture) {
        return event -> {
            if (event.isRight()) {
                serviceFuture.complete(event.right().getValue());
            } else {
                serviceFuture.fail(event.left().getValue());
            }
        };
    }


}
