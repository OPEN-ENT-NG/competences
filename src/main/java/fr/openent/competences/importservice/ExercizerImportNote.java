package fr.openent.competences.importservice;

import com.opencsv.bean.CsvToBeanBuilder;
import fr.openent.competences.bean.ExercizerStudent;
import fr.openent.competences.helpers.FileHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import org.entcore.common.storage.Storage;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class ExercizerImportNote extends ImportFile {

    public ExercizerImportNote(HttpServerRequest request, Storage storage) {
        super(request, storage);
    }

    @Override
    public Future<JsonObject> run() {
        Promise<JsonObject> promise = Promise.promise();
        super.processImportFile()
                .compose(this::parseAndFormatBuffer)
                .compose(exercizerBuffer -> {
                    fetchDataFromBuffer(exercizerBuffer);
                    return Future.succeededFuture();
                })
                .onSuccess(res -> {

                })
                .onFailure(err -> {
                    err.printStackTrace();
                });
        return promise.future();
    }

    private Future<Buffer> parseAndFormatBuffer(Buffer resFile) {
        Promise<Buffer> promise = Promise.promise();
        Buffer buffer = Buffer.buffer();
        RecordParser recordParser = RecordParser.newDelimited("\n", bufferedLine -> {
            if (!bufferedLine.getString(0, 7).equals("Moyenne"))
                buffer.appendBuffer(bufferedLine);
        });
        recordParser.handle(resFile);
        recordParser.endHandler(event -> promise.complete(buffer));

        return promise.future();
    }

    private void fetchDataFromBuffer(Buffer buffer) {
        Reader reader = new InputStreamReader(new ByteArrayInputStream(buffer.getBytes()));
        List<ExercizerStudent> students = new CsvToBeanBuilder<ExercizerStudent>(reader)
                .withType(ExercizerStudent.class)
                .withSeparator(';')
                .build()
                .parse();
        for (ExercizerStudent student : students) {
            System.out.println(student.getStudentName());
            System.out.println(student.getNote());
            //System.out.println(student.getAnnotation());*/
        }
    }
}
