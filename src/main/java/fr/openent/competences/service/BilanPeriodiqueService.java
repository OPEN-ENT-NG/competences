package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface BilanPeriodiqueService {
    /**
     * Récupère les retards et absences d'un élève
     * @param idEleve
     * @param eitherHandler
     */
    void getRetardsAndAbsences(String idEleve, Handler<Either<String, JsonArray>> eitherHandler);

    /**
     * Récupères les données pour le suivi des acquis d'un élève
     * @param idEtablissement
     * @param idPeriode
     * @param idEleve
     * @param idClasse
     */
    void getSuiviAcquis(final String idEtablissement, final Long idPeriode,
                        final String idEleve, final String idClasse,
                        Handler<Either<String, JsonArray>> handler);
}
