package fr.openent.competences.service.impl;

import fr.openent.competences.bean.lsun.Donnees;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;

public interface LSUService {

    void serviceResponseOK (AtomicBoolean answer, int count, String thread, String method);

    /**
     * Permet de sélectionner parmis les disciplines sélectionnées de l'établissement, celles qui sont évaluées
     * (reférencées dans au moins une balise suiviAcquis d'un élève).
     *
     * @param idsEvaluatedDiscipline
     * @param donnees
     * @param errorsExport
     */
    void validateDisciplines(JsonArray idsEvaluatedDiscipline, Donnees donnees, JsonObject errorsExport);


    JsonArray getIdsEvaluatedDiscipline() ;

    void addIdsEvaluatedDiscipline( Object idDiscipline);

    void initIdsEvaluatedDiscipline();
}
