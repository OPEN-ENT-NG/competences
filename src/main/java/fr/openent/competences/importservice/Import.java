package fr.openent.competences.importservice;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

public interface Import <T> {

    Future<Buffer> processImportFile();

    Future<T> run();

    Future<Buffer> parseAndFormatBuffer(Buffer resFile);

    Future<T> fetchDataFromBuffer(Buffer buffer);
}
