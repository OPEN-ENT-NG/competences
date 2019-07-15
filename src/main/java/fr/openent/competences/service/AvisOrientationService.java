package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface AvisOrientationService {

    /**
     * Selectionner un avis d'orientation de classe d'un élève pour une période donnée
     * @param idEleve id eleve
     * @param idPeriode id periode
     * @param id_avis_conseil_bilan id de l'avis
     * @param handler handler portant le résultat de la requête
     */
    public void createOrUpdateAvisOrientation(String idEleve, Long idPeriode, Long id_avis_conseil_bilan, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupérer un avis d'orientation de classe d'un élève pour une période donnée
     * @param idEleve id eleve
     * @param idPeriode id periode
     * @param handler handler portant le résultat de la requête
     */
    public void getAvisOrientation(String idEleve, Long idPeriode, Handler<Either<String, JsonObject>> handler);

}