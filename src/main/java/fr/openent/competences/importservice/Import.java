package fr.openent.competences.importservice;

import fr.openent.competences.model.importservice.ExercizerStudent;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

import java.util.List;

public interface Import {

    Future<Buffer> processImportFile();

    Future<List<ExercizerStudent>> run();

    Future<Buffer> parseAndFormatBuffer(Buffer resFile);

    Future<List<ExercizerStudent>> fetchDataFromBuffer(Buffer buffer);
}
