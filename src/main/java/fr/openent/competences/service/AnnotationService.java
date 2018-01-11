package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface AnnotationService extends CrudService {

    /** Récupère les annotations d'un établissement
     * @param idEtab identifiant d'établissement
     * @param handler handler portant le resultat de la requête
     */
    public void listAnnotations(String idEtab, Handler<Either<String, JsonArray>> handler);

    /**
     * Création d'une appréciation
     * @param appreciation
     * @param user
     * @param handler
     */
    public void createAppreciation(JsonObject appreciation, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * MAJ d'une appréciation
     * @param data
     * @param user
     * @param handler
     */
    public void updateAppreciation(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler) ;

    /**
     * Suppression d'une appréciation
     * @param idAppreciation
     * @param user
     * @param handler
     */
    public void deleteAppreciation(Long idAppreciation, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Créee une anntotation à partir d'un élève et un devoir donné
     * @param idDevoir
     * @param idAnnotation
     * @param idEleve
     * @param handler
     */
    public void createAnnotationDevoir(Long idDevoir, Long idAnnotation, String idEleve, Handler<Either<String, JsonObject>> handler);

    /**
     * Modifie une anntotation à partir d'un élève et un devoir donné
     * @param idDevoir
     * @param idAnnotation
     * @param idEleve
     * @param handler
     */
    public void updateAnnotationDevoir(Long idDevoir, Long idAnnotation, String idEleve, Handler<Either<String, JsonObject>> handler);

    /**
     * Supprime une anntotation à partir d'un élève et un devoir donné
     * @param idDevoir
     * @param idEleve
     * @param handler
     */
    public void deleteAnnotation(Long idDevoir, String idEleve, Handler<Either<String, JsonObject>> handler);
}
