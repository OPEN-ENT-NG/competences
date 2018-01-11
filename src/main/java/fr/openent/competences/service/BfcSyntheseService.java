package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by agnes.lapeyronnie on 03/11/2017.
 */
public interface BfcSyntheseService extends CrudService {
    /**
     * crée la synthese d'un eleve
     * @param synthese
     * @param user
     * @param handler
     */
    public void createBfcSynthese(JsonObject synthese, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * met à jour la synthese d'un eleve
     *
     * @param id
     * @param synthese
     * @param handler
     */

   public void updateBfcSynthese(String id, JsonObject synthese, Handler<Either<String, JsonObject>> handler);

    /**
     * supprime la synthese d'un eleve
     * @param id
     * @param handler
     */

   void deleteBfcSynthese(String id, Handler<Either<String, JsonObject>> handler);

    /**
     * recupere la synthese d'un eleve
     * @param idEleve
     * @param handler
     */
    public void getBfcSyntheseByEleve(String idEleve, Integer idCycle, Handler<Either<String, JsonObject>> handler);

    /**
     *
     * @param idsEleve tableau des idsEleve pour un cycle donné
     * @param idCycle cycle des élèves sélectionnés
     * @param handler
     */
    public void getBfcSyntheseByIdsEleve(String[] idsEleve, Long idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * return idCycle
     * @param IdEleve
     * @param handler
     */
    public void getIdCycleWithIdEleve(String IdEleve, Handler<Either<String, Integer>> handler);

}
