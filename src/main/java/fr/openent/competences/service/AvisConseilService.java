package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface AvisConseilService {

    /**
     * Retourne la liste des avis et orientation du conseil de classe.
     * @param typeAvis type d'avis
     * @param handler Handler de retour
     */
    public void getLibelleAvis(Long typeAvis, String idStructure, Handler<Either<String, JsonArray>> handler);

    /**
     * Créer un avis de conseil de classe / orientation
     * @param typeAvis 1 - Conseil de classe / 2 - Conseil d'orientation
     * @param libelle Libelle de l'avis
     * @param idStructure id de l'etablissement
     */
    public void createOpinion (Long typeAvis, String libelle, String idStructure,
                               Handler<Either<String, JsonObject>> handler);

    /**
     * Mets à jour un avis de conseil de classe / orientation
     * @param idAvis id de l'avis
     */
    public void updateOpinion (Long idAvis, boolean active, String libelle,
                               Handler<Either<String, JsonObject>> handler);

    /**
     * Supprime un avis de conseil de classe / orientation
     * @param idAvis id de l'avis
     */
    public void deleteOpinion (Long idAvis, Handler<Either<String, JsonObject>> handler);

    /**
     * Selectionner un avis de conseil de classe d'un élève pour une période donnée
     * @param idEleve id eleve
     * @param idPeriode id periode
     * @param id_avis_conseil_bilan id de l'avis
     * @param idStructure id de l'établissement où l'avis est saisi
     * @param handler handler portant le résultat de la requête
     */
    public void createOrUpdateAvisConseil(String idEleve, Long idPeriode, Long id_avis_conseil_bilan, String idStructure,
                                          Handler<Either<String, JsonObject>> handler);

    /**
     * Récupérer un avis de conseil de classe d'un élève pour une période donnée
     * @param idEleve id eleve
     * @param idPeriode id periode
     * @param idStructure id de l'établissement où l'avis est saisi
     * @param handler handler portant le résultat de la requête
     */
    public void getAvisConseil(String idEleve, Long idPeriode, String idStructure,
                               Handler<Either<String, JsonArray>> handler);

}
