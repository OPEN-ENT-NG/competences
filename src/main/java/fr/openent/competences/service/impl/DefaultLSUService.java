package fr.openent.competences.service.impl;

import fr.openent.competences.bean.lsun.Discipline;
import fr.openent.competences.bean.lsun.Donnees;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultLSUService implements LSUService {
    public static final String DISCIPLINE_KEY = "DIS_";
    private static final Logger log = LoggerFactory.getLogger(DefaultExportBulletinService.class);
    private JsonArray idsEvaluatedDiscipline;

    public void serviceResponseOK (AtomicBoolean answer, int count, String thread, String method) {
        if (count > 1 ) {
            log.info("[ "+ method + " ] : " + thread + " TIME OUT " + count);
        }
        if(!answer.get()) {
            answer.set(true);
            log.info(" -------[" + method + "]: " + thread + " FIN " );
        }
    }

    @Override
    public void validateDisciplines(JsonArray idsEvaluatedDiscipline,  Donnees donnees, JsonObject errorsExport) {

        // Récupération des disciplines évaluées
        List<Discipline> disciplines = donnees.getDisciplines().getDiscipline().stream().filter(
                discipline ->
                        idsEvaluatedDiscipline.contains(discipline.getId().substring(DISCIPLINE_KEY.length(),
                                discipline.getId().length())) )
                        .collect(Collectors.toList());


        // Vérification de l'unicité des codes des disciplines évaluées
        Map<String, Long> counted = disciplines.stream()
                .collect(Collectors.groupingBy((Discipline::getCode), Collectors.counting()));

        Donnees.Disciplines correctEvaluatedDisciplines = new Donnees.Disciplines();
        counted.forEach((code, occurrences) -> {
            if (occurrences > 1) {
                String errorCodeKey = "errorCode";
                if(!errorsExport.containsKey(errorCodeKey)){
                    errorsExport.put(errorCodeKey, new JsonArray());
                }
                errorsExport.getJsonArray(errorCodeKey).getList().addAll(
                        disciplines.stream().filter( discipline -> discipline.getCode().equals(code))
                                .collect(Collectors.toList()));
            }
            else {
                correctEvaluatedDisciplines.getDiscipline().addAll(
                        disciplines.stream().filter( discipline -> discipline.getCode().equals(code))
                                .collect(Collectors.toList()));
            }
        });

        donnees.setDisciplines(correctEvaluatedDisciplines);

    }

    public JsonArray getIdsEvaluatedDiscipline() {
        return idsEvaluatedDiscipline;
    }

    public void addIdsEvaluatedDiscipline( Object idDiscipline) {
        if(!idsEvaluatedDiscipline.contains(idDiscipline)){
            idsEvaluatedDiscipline.add(idDiscipline);
        }
    }

    public void initIdsEvaluatedDiscipline(){
        idsEvaluatedDiscipline = new JsonArray();
    }

}
