package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created by vogelmt on 29/03/2017.
 */
public interface BFCService extends CrudService {
    /**
     * Créer un BFC pour un élève
     * @param bfc objet contenant les informations relative au BFC
     * @param user utilisateur
     * @param handler handler portant le résultat de la requête
     */
    public void createBFC(final JsonObject bfc, final UserInfos user, final Handler<Either<String, JsonObject>> handler);

    /**
     * Mise à jour d'un BFC pour un élève
     * @param data appreciation à mettre à jour
     * @param user utilisateur
     * @param handler handler portant le resultat de la requête
     */
    public void updateBFC(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'un BFC pour un élève
     * @param idBFC identifiant de la note
     * @param user user
     * @param handler handler portant le résultat de la requête
     */
    public void deleteBFC(Long idBFC, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Récupère les BFCs d'un élève pour chaque domaine
     * @param idEleves
     * @param idEtablissement
     * @param idCycle
     * @param handler
     */
    public void getBFCsByEleve(String[] idEleves, String idEtablissement, Long idCycle, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les moyennes par domaines des élève dont l'id est passé en paramètre.
     * La map retournee a pour clé l'id de l'élève, et contient une autre map qui contient, pour chaque id de domaine racine, la moyenne simplifiée (
     * @param idEleves id des élèves dont on souhaite obtenir les moyennes pour le BFC
     * @param idClasse l'id de la classe à laquelle appartient l'élève
     * @param idStructure l'id de l'établissement auquel appartient la classe
     * @param handler handler portant le résultat du calcul de moyenne
     */
    public void buildBFC(String[] idEleves, String idClasse, String idStructure, Long idPeriode, Long idCycle, Handler<Either<String, Map<String, Map<Long, Integer>>>> handler);

    /**
     * retourne la date de creation du BFC, si null la date de modification sinon la date du jour
     * pour un idEleve
     * @param idEleve
     * @param handler
     */
    // public void getDateCreatedBFC(String idEleve, Handler<Either<String,JsonArray>> handler);

    /**
     * Récupère les valeurs de la table calc_millesime
     * @param handler
     */
    public void getCalcMillesimeValues(Handler<Either<String, JsonArray>> handler);
}
