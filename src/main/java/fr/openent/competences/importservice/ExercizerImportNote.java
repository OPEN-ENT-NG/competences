package fr.openent.competences.importservice;

import fr.openent.competences.helpers.FileHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;

public class ExercizerImportNote extends ImportFile {

    public ExercizerImportNote(HttpServerRequest request, Storage storage) {
        super(request, storage);
    }

    @Override
    public Future<JsonObject> run() {
        Promise<JsonObject> promise = Promise.promise();
        super.processImportFile()
                .compose(resFile -> {
                    // read en CSV avec tes colonnes spécifiques (p-e à externaliser dans ImportFile)
                    // read CSV avec ton buffer (Recordparser)
                    return Future.succeededFuture();
                })
                .onSuccess(res -> {

                })
                .onFailure(err -> {

                });
        return promise.future();
    }


    private void parseCsv(final HttpServerRequest request, final String fileId, String filename) {

        FileHelper.readFile(this.storage, fileId)
                .onSuccess(res -> {
//                    Reader reader = new InputStreamReader(
//                            new ByteArrayInputStream(event.getBytes()));
//                    List<ExercizerStudent> students = new CsvToBeanBuilder<ExercizerStudent>(reader)
//                            .withType(ExercizerStudent.class).withSeparator(';').build().parse();
//                    for(ExercizerStudent student: students){
//                        System.out.println(student.getStudentName());
//                        System.out.println(student.getNote());
//                        //System.out.println(student.getAnnotation());
//                    }
                })
                .onFailure(err -> {

                });
    }
}
