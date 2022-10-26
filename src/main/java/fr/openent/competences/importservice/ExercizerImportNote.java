package fr.openent.competences.importservice;

import com.opencsv.bean.CsvToBeanBuilder;
import fr.openent.competences.Competences;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.model.importservice.ExercizerStudent;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultNoteService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import io.vertx.core.CompositeFuture;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExercizerImportNote extends ImportFile {

    UtilsService utilsService;
    NoteService noteService;

    public ExercizerImportNote(HttpServerRequest request, Storage storage, UtilsService utilsService) {
        super(request, storage);
        this.utilsService = utilsService;
        this.noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE);
    }

    @Override
    public Future<List<ExercizerStudent>> run() {
        Promise<List<ExercizerStudent>> promise = Promise.promise();
        super.processImportFile()
                .compose(this::parseAndFormatBuffer)
                .compose(this::fetchDataFromBuffer)
                .onSuccess(promise::complete)
                .onFailure(Throwable::printStackTrace);
        return promise.future();
    }

    @Override
    public Future<Boolean> sql(String idClasse, String idDevoir, List<ExercizerStudent> students) {
        Promise<Boolean> promise = Promise.promise();
        utilsService.getClasseDisplaynames(idClasse)
                .compose(classStudents -> {
                    Boolean hasNoStudentConflict = true;
                    Promise<Boolean> insertOrUpdatePromiseAll = Promise.promise();
                    List<Future> futures = new ArrayList<>();
                    for(ExercizerStudent student: students){
                        JsonObject j = classStudents.stream()
                                .map(JsonObject.class::cast).filter(dn -> dn.getString("displayName").equalsIgnoreCase(student.getStudentName()))
                                .findFirst().orElse(null);
                        if(j != null){
                            Promise<JsonObject> insertOrUpdatePromise = Promise.promise();
                            futures.add(insertOrUpdatePromise.future());
                            noteService.insertOrUpdateDevoirNote(idDevoir, j.getString("id"), student.getNote(), FutureHelper.handlerJsonObject(insertOrUpdatePromise));
                        }
                        else{
                            hasNoStudentConflict = false;
                        }
                    }
                    Boolean finalHasStudentConflict = hasNoStudentConflict;
                    CompositeFuture.all(futures)
                            .onSuccess(res -> {
                                insertOrUpdatePromiseAll.complete(finalHasStudentConflict);
                            })
                            .onFailure(res -> {
                                insertOrUpdatePromiseAll.fail(res.getMessage());
                            });
                    return insertOrUpdatePromiseAll.future();
                })
                .onSuccess(promise::complete)
                .onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public Future<Buffer> parseAndFormatBuffer(Buffer resFile) {
        Promise<Buffer> promise = Promise.promise();
        Buffer buffer = Buffer.buffer();
        RecordParser recordParser = RecordParser.newDelimited("\n", bufferedLine -> {
            String buff = bufferedLine.getString(0, bufferedLine.length() - 1);
            if (!buff.startsWith("Moyenne")){
                Pattern patt = Pattern.compile("[0-9],[0-9]");
                Matcher m = patt.matcher(buff);
                StringBuffer sb = new StringBuffer(buff.length());
                while (m.find()) {
                    String text = m.group();
                    m.appendReplacement(sb, Matcher.quoteReplacement(text.replace(',', '.')));
                }
                m.appendTail(sb);
                buffer.appendString(sb.toString()).appendString("\n");
            }
        });
        recordParser.handle(resFile);
        promise.complete(buffer);
        return promise.future();
    }

    @Override
    public Future<List<ExercizerStudent>> fetchDataFromBuffer(Buffer buffer) {
        Promise<List<ExercizerStudent>> promise = Promise.promise();
        Reader reader = new InputStreamReader(new ByteArrayInputStream(buffer.getBytes()));
        List<ExercizerStudent> students = new CsvToBeanBuilder<ExercizerStudent>(reader)
                .withType(ExercizerStudent.class)
                .withSeparator(';')
                .build()
                .parse();
        promise.complete(students);
        return promise.future();
    }
}
