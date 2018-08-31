package fr.openent.competences.service;


import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;

import java.util.List;

public interface CompetenceNiveauFinalService extends CrudService {

    /**
     * create and update a competence_niveau_final
     * @param niveauFinal niveauFinal to create or to update (with id_periode, id_eleve, id_competence, id_matiere and
     *                    id_classe)
     * @param handler Function returning data
     */
    void setNiveauFinal(JsonObject niveauFinal, Handler<Either<String,JsonObject>> handler);

    /**
     * delete niveaufinal
     * @param niveauFinal niveauFinal to delete (with id_periode, id_eleve, id_competence, id_matiere and id_classe)
     * @param handler Function returning data
     */
    void deleteNiveauFinal(JsonObject niveauFinal, Handler<Either<String,JsonObject>> handler);

    /**
     * get the niveau final by  a periode, a student, a competence, a subject and a class
     * @param id_periode
     * @param id_eleve
     * @param ids_matieres
     * @param id_classe
     * @param handler
     */
    void getNiveauFinalByEleve(Long id_periode, String id_eleve, List<String> ids_matieres, String id_classe, Handler<Either<String, JsonArray>> handler);

}
