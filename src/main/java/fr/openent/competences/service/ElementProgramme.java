package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface ElementProgramme {

    /**
     * Créée ou met à jour un élément du programme (si élément du  programme déjà existant
     * @param userId
     * @param idPeriode
     * @param idMatiere
     * @param idClasse
     * @param texte
     * @param handler
     */
    public void setElementProgramme(String userId, Long idPeriode, String idMatiere, String idClasse,String texte, Handler<Either<String, JsonArray>> handler);

    /**
     * Récupère un élément du programme
     * @param idPeriode
     * @param idMatiere
     * @param idClasse
     * @param handler
     */
    public void getElementProgramme(Long idPeriode, String idMatiere, String idClasse, Handler<Either<String, JsonObject>> handler);

    /**
     * @param handler
     */
    public void getDomainesEnseignement(Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    public void getSousDomainesEnseignement(Handler<Either<String, JsonArray>> handler);

    /**
     * @param handler
     */
    public void getPropositions(Handler<Either<String, JsonArray>> handler);
}
