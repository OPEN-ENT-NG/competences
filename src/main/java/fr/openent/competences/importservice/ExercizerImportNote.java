package fr.openent.competences.importservice;

import com.opencsv.bean.CsvToBeanBuilder;
import fr.openent.competences.constants.Field;
import fr.openent.competences.model.importservice.ExercizerStudent;
import fr.openent.competences.service.UtilsService;

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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExercizerImportNote extends ImportFile <List<ExercizerStudent>> {

    private static final String AVERAGE_RULE = "Moyenne";

    UtilsService utilsService;
    String idClasse;

    public ExercizerImportNote(HttpServerRequest request, Storage storage, String idClasse, UtilsService utilsService) {
        super(request, storage);
        this.utilsService = utilsService;
        this.idClasse = idClasse;
    }

    @Override
    public Future<List<ExercizerStudent>> run() {
        Promise<List<ExercizerStudent>> promise = Promise.promise();
        super.processImportFile()
                .compose(this::parseAndFormatBuffer)
                .compose(this::fetchDataFromBuffer)
                .compose(students -> this.resolveStudents(students, this.idClasse))
                .onSuccess(promise::complete)
                .onFailure(err -> {
                    err.printStackTrace();
                    promise.fail(err.getMessage());
                });
        return promise.future();
    }

    @Override
    public Future<Buffer> parseAndFormatBuffer(Buffer resFile) {
        Promise<Buffer> promise = Promise.promise();
        Buffer buffer = Buffer.buffer();
        RecordParser recordParser = RecordParser.newDelimited("\n", bufferedLine -> {
            String buff = bufferedLine.getString(0, bufferedLine.length() - 1);
            if (!buff.startsWith(AVERAGE_RULE)){
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

    private Future<List<ExercizerStudent>> resolveStudents(List<ExercizerStudent> students, String idClasse) {
        Promise<List<ExercizerStudent>> promise = Promise.promise();
        utilsService.getEleveClasseInfos(idClasse)
                .onSuccess(classStudents -> {
                    Map<String, JsonObject> studentsClassesMap = classStudents.stream()
                            .map(JsonObject.class::cast)
                            .collect(Collectors.toMap((student -> student.getString(Field.DISPLAYNAME).toLowerCase()), student -> student));
                    students.forEach(student -> {
                        if (studentsClassesMap.containsKey(student.getStudentName().toLowerCase())) {
                            student.setId(studentsClassesMap.get(student.getStudentName().toLowerCase()).getString(Field.ID));
                        }
                    });
                    promise.complete(students);
                })
                .onFailure(promise::fail);
        return promise.future();
    }

}
