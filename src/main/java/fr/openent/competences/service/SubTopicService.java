package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
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
     * @return
     */
    Future<JsonArray> getSubtopicServices(String idStructure);

    /**
     * Get Subtopic from a idClass and id Structure
     * @param idStructure
     * @param idClasse
     */
    Future<JsonArray> getSubtopicServices(String idStructure, String idClasse);

    /**
     * Get subtopic coeffs from an array idsClasse and id Structure
     * @param idStructure
     * @param idsClasse
     */
    Future<JsonArray> getSubtopicServices(String idStructure, JsonArray idsClasse);

    Future<JsonObject> getSubtopicServices(String idStructure, String idClasse, String idTeacher, String idMatiere);

    void deleteSubtopicServices(String idMatiere, String idEnseignant, JsonArray idGroups, Handler<Either<String, JsonArray>> handler);
}
