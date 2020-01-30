package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

public interface STSFileService extends CrudService {

    /**
     * create sts file
     * @param oSTSFile object with name_file, id_structure and content
     * @param handler return response : id and creation_date
     */
    void create (JsonObject oSTSFile, Handler<Either<String,JsonObject>> handler);

    /**
     * get sts files
     * @param id_etablissement id structure
     * @param handler return response
     */
    void getSTSFile (String id_etablissement, Handler<Either<String, JsonArray>> handler);
}
