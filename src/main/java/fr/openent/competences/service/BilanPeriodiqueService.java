package fr.openent.competences.service;

import fr.openent.competences.model.Service;
import fr.openent.competences.model.achievements.AchievementsProgress;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface BilanPeriodiqueService {
    /**
     * Récupère les retards et absences d'un élève
     *
     * @param idEleve
     * @param idStructure
     * @param idClasse
     * @param eitherHandler
     */
    void getRetardsAndAbsences(String structureId, List<String> idEleves, List<String> idClasses, Handler<Either<String, JsonArray>> eitherHandler);

    /**
     * Récupères les données pour le suivi des acquis d'un élève
     *
     * @param idEtablissement
     * @param idPeriode
     * @param idEleve
     * @param idClasse
     */
    void getSuiviAcquis(final String idEtablissement, final Long idPeriode, final String idEleve,
                        final JsonArray idClasse, final List<Service> services, final JsonArray multiTeachers,
                        Handler<Either<String, JsonArray>> handler);

    /**
     * @param idEleve
     * @param idEtablissement
     * @param idClasse
     * @param typeClasse
     * @param idPeriodeString
     * @param handler
     */
    void getBilanPeriodiqueDomaineForGraph(final String idEleve, final String idEtablissement,
                                           final String idClasse, final Integer typeClasse, final String idPeriodeString,
                                           final Handler<Either<String, JsonArray>> handler);


    /**
     * Get skills validated percentage by subject for a student
     *
     * @param structureId structure identifier filter on
     * @param studentId   student identifier to filter on
     * @param periodId    period identifier to filter on
     * @param groupId     group identifier to filter on
     * @return return Future containing student SubjectSkillsValidatedPercentage inside his Achievements
     */
    Future<AchievementsProgress> getSubjectSkillsValidatedPercentage(String structureId, String studentId,
                                                                     Long periodId, String groupId);

}
