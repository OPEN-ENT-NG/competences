package fr.openent.competences.service;

import fr.openent.competences.model.Subject;
import fr.wseduc.webutils.Either;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import io.vertx.core.Handler;

import java.util.List;
import java.util.Map;

public interface MatiereService extends CrudService {
     void getLibellesCourtsMatieres(Boolean wantMapCodeLibelle,Handler<Either<String, Map<String,String>>> handler);
     /**
      * Sauvegarde un model de libelle de matiere
      * @param idStructure
      * @param title
      * @param idModel
      * @param libelleMatiere
      * @param handler
      */
     void saveModel(String idStructure, String title, Long idModel, JsonArray libelleMatiere,
                    Handler<Either<String, JsonObject>> handler);

     /**
      * Récupère les modèles de libellé de matiere d'un établissement
      * @param idStructure
      * @param idModel
      * @param handler
      */
     void getModels(String idStructure, Long idModel, Handler<Either<String, JsonArray>> handler);

     /**
      *
      * @param idModel
      * @param handler
      */
     void deleteModeleLibelle(String idModel, Handler<Either<String, JsonArray>> handler);

     /**
      * Récupère les sous Matières d'une matière
      * @param idMatiere
      * @param handler
      */
     void getSousMatieres(String idMatiere, String idStructure, Handler<Either<String, JsonArray>> handler);

     /**
      * get underSubject for one struture and one subject
      * @param subjectId subject Id
      * @param structureId  structure id
      * @return future contains response
      */
     Future<JsonArray> getUnderSubjects(String subjectId, String structureId);

     Future<JsonArray> getMatieresEtab(String idStructure);
     /**
      *  @deprecated Use @link {#getMatieresEtab(String idStructure)}
      * Récupération des Matières de l'établissement
      * @param idEtablissement
      * @param handler
      */
     @Deprecated
     void getMatieresEtab(String idEtablissement, Handler<Either<String, JsonArray>> handler);

     /**
      * Met à jour les devoirs en choisissant une sousMAtiere par défaut au devoir contenant des matières avec
      * sous matières
      * @param idsMatieres
      * @param handler
      */
     void updateDevoirs(JsonArray idsMatieres, Handler<Either<String, JsonArray>> handler);

     /**
      * Reverse rank between both subjects in neo4j
      * @param subject fist subject update
      * @param handler return id of subjects
      */
     void removeRankOnSubject(Subject subject, Handler<Either<String, JsonObject>> handler);

     /**
      * Update rank subjects in neo4j
      * @param subjects fist subject update
      * @param handler return id of subject
      */
     void updateListRank(List<Subject> subjects, Handler<Either<String, JsonObject>> handler);
}
