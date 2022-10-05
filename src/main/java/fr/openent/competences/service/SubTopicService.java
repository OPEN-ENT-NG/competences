package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface SubTopicService {
    /**
     * Insert a new coefficient if it doesn't already exists . Update the coefficient if it exists
     * @param data
     * @param handler
     */
    void upsertCoefficent(JsonObject data, Handler<Either<String, JsonArray>> handler) ;

    /**
     * Get Subtopic from an id Structure
     * @param idStructure
     * @param defaultResponseHandler
     */
    void getSubtopicServices(String idStructure, Handler<Either<String, JsonArray>> defaultResponseHandler);

    /**
     * Get Subtopic from a idClass and id Structure
     * @param idStructure
     * @param idClasse
     * @param defaultResponseHandler
     */
    void getSubtopicServices(String idStructure,String idClasse, Handler<Either<String, JsonArray>> defaultResponseHandler);

    void getSubtopicServices(String idStructure, String idClasse, String idTeacher, String idMatiere, Handler<Either<String, JsonObject>> handler);

    void deleteSubtopicServices(String idMatiere, String idEnseignant, JsonArray idGroups, Handler<Either<String, JsonArray>> handler);
}
