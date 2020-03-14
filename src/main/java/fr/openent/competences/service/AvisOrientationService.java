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
     * @param idStructure id de l'établissement où l'avis a été saisi
     * @param handler handler portant le résultat de la requête
     */
    public void createOrUpdateAvisOrientation(String idEleve, Long idPeriode, Long id_avis_conseil_bilan, String idStructure, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupérer un avis d'orientation de classe d'un élève pour une période donnée
     * @param idEleve id eleve
     * @param idPeriode id periode
     * @param idStructure id de l'établissement où l'avis a été saisi
     * @param handler handler portant le résultat de la requête
     */
    public void getAvisOrientation(String idEleve, Long idPeriode, String idStructure,
                                   Handler<Either<String, JsonArray>> handler);

}
