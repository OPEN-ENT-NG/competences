package fr.openent.competences.service;

import fr.wseduc.webutils.Either;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import io.vertx.core.Handler;

import java.util.Map;

public interface MatiereService extends CrudService {
     void getLibellesCourtsMatieres(Handler<Either<String, Map<String,String>>> handler);
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
}
