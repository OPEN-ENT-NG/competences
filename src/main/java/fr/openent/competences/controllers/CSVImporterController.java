package fr.openent.competences.controllers;

import com.opencsv.bean.CsvToBeanBuilder;
import fr.openent.competences.bean.ExercizerStudent;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.storage.Storage;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

public class CSVImporterController extends ControllerHelper {

    private final Storage storage;

    public CSVImporterController(Storage storage) {
        //this.CSVImporterService = new DefaultCSVImporterService(Competences.COMPETENCES_SCHEMA);
        this.storage = storage;
    }

    @Post("/csv/:id/exercizer/import")
    @ApiDoc("Set notes of a devoir by importing a CSV.")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void importExercizerCSV(final HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                renderError(request);
                return;
            }
            String fileId = entries.getString("_id");
            String filename = entries.getJsonObject("metadata").getString("filename");
            parseCsv(request, fileId, filename);
        });
    }

    /**
     * Parse CSV file
     *
     * @param request Http request
     * @param filename    Directory path
     */
//    private void parseCsv(final HttpServerRequest request, final String path, Buffer content) {
    private void parseCsv(final HttpServerRequest request, final String fileId, String filename) {
        storage.readFile(fileId,event -> {
            Reader reader = new InputStreamReader(
                    new ByteArrayInputStream(event.getBytes()));
            List<ExercizerStudent> students = new CsvToBeanBuilder<ExercizerStudent>(reader)
                    .withType(ExercizerStudent.class).withSeparator(';').build().parse();
            for(ExercizerStudent student: students){
                System.out.println(student.getStudentName());
                System.out.println(student.getNote());
                //System.out.println(student.getAnnotation());
            }
        });
    }
}
