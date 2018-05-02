package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;


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
     * @param idEleve identifiant de l'élève
     * @param user user
     * @param handler handler portant le résultat de la requête
     */
    public void deleteBFC(long idBFC, String idEleve, UserInfos user, Handler<Either<String, JsonObject>> handler);

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
     * @param recapEval indique si on est dans un export de récapitulation d'évaluation
     * @param idClasse l'id de la classe à laquelle appartient l'élève
     * @param idStructure l'id de l'établissement auquel appartient la classe
     * @param handler handler portant le résultat du calcul de moyenne
     */
    public void buildBFC(boolean recapEval, String[] idEleves, String idClasse, String idStructure, Long idPeriode, Long idCycle, Handler<Either<String, JsonObject>> handler);

    /**
     * retourne la date de creation du BFC, si null la date de modification sinon la date du jour
     * pour un idEleve
     * @param idEleve id de l'élève
     * @param handler handler portant le résultat
     */
    // public void getDateCreatedBFC(String idEleve, Handler<Either<String,JsonArray>> handler);

    /**
     * Récupère les valeurs de la table calc_millesime
     * @param handler handler portant le résultat
     */
    public void getCalcMillesimeValues(Handler<Either<String, JsonArray>> handler);

    /**
     * Active la visibilité des moyennes sur l'écran de BFC
     * @param structureId id établissement neo
     * @param user utilisateur connecté
     * @param visible 0 : caché pour tout le monde, 1 : caché pour les enseignants, 2 : visible pour tous
     * @param handler handler portant le résultat
     */
    public void setVisibility(String structureId, UserInfos user, Integer visible,
                              Handler<Either<String, JsonArray>> handler);


    /**
     *  Récupère la valeur de l'état de la visibilité des moyennes sur l'écran de BFC.
     *  0 : caché pour tout le monde, 1 : caché pour les enseignants, 2 : visible pour tous
     * @param structureId id établissement neo
     * @param user utilisateur connecté
     * @param handler handler portant le résultat
     */
    public void getVisibility(String structureId, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * donne un JsonArray avec
     * la moyenne du contrôle continu qui correspond à la somme des maîtrises obtenue pour chaque domaine Racine
     * en tenant compte de la dispense d'un domaine ou non.
     * idEleve
     * et le totalMaxBaremeBrevet = nb de domaines non dispensé x MaxBaremeBrevet
     * @param eb eventBus
     * @param idsClasses des id des  classe
     * @param handler  handler portant le résultat
     */

    public void getMoyenneControlesContinusBrevet(EventBus eb, List<String> idsClasses, final Handler<Either<String, JsonArray>> handler);
}
