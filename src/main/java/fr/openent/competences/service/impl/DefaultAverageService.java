/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.service.impl;

import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FutureHelper;
import fr.openent.competences.service.AverageService;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.SyntheseBilanPeriodiqueService;
import fr.openent.competences.service.UtilsService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultAverageService implements AverageService {
    private final NoteService noteService;
    private final UtilsService utilsService;
    private final SyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService;

    public DefaultAverageService(NoteService noteService,
                                 UtilsService utilsService, SyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService) {
        this.noteService = noteService;
        this.utilsService = utilsService;
        this.syntheseBilanPeriodiqueService = syntheseBilanPeriodiqueService;
    }

    public Future<JsonObject> getStudentsAverageForExportPdf(String structureId, Integer periodId, String classId, Integer typeGroup,
                                                             String className, boolean isWithSummaries, Boolean withMoyGeneraleByEleve,
                                                             String language, String host) {
        Promise<JsonObject> promise = Promise.promise();
        SortedMap<String, Set<String>> mapAllidMatAndidTeachers = new TreeMap<>();
        Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve = new LinkedHashMap<>();

        HashMap<String, Future<?>> futures = new HashMap<>();
        final JsonObject data = new JsonObject();
        getPeriod(periodId, classId, structureId)
                .compose(periodes -> getSubjectAveragesStudents(periodes, classId,
                        periodId, structureId, typeGroup, className, mapAllidMatAndidTeachers, mapIdMatListMoyByEleve))
                .compose(subjectAveragesStudents -> {
                    Promise<JsonObject> promiseSubjectsEvaluated = Promise.promise();
                    data.mergeIn(subjectAveragesStudents);
                    List<String> studentIds = data.getJsonArray(Field.ELEVES, new JsonArray())
                            .stream()
                            .filter(JsonObject.class::isInstance)
                            .map(JsonObject.class::cast)
                            .map(student -> student.getString(Field.ID_ELEVE))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());


                    Future<JsonArray> futurePeriodicReportSummaries = getPeriodicReportSummaries(
                            (periodId != null ? Long.valueOf(periodId) : null), isWithSummaries,
                            studentIds, structureId);
                    noteService.getMatEvaluatedAndStat(mapAllidMatAndidTeachers, mapIdMatListMoyByEleve,
                            FutureHelper.handler(promiseSubjectsEvaluated));
                    Future<JsonObject> futureSubjectsEvaluated = promiseSubjectsEvaluated.future();

                    futures.put(Field.PERIODICREPORTSUMMARIES, futurePeriodicReportSummaries);
                    futures.put(Field.SUBJECTS, futureSubjectsEvaluated);

                    return CompositeFuture.all(futurePeriodicReportSummaries, futureSubjectsEvaluated);
                })
                .onFailure(promise::fail)
                .onSuccess(res -> {
                    JsonObject subjectsEvaluated = (JsonObject) futures.get(Field.SUBJECTS).result();
                    data.getMap().putAll(subjectsEvaluated.getMap());
                    JsonObject result = new JsonObject(data.getMap());
                    formatDataForPdf(result, subjectsEvaluated,
                            ((JsonArray) futures.get(Field.PERIODICREPORTSUMMARIES).result())
                                    .stream()
                                    .filter(JsonObject.class::isInstance)
                                    .map(JsonObject.class::cast)
                                    .collect(Collectors.toList()),
                            withMoyGeneraleByEleve);
                    promise.complete(result);
                });

        return promise.future();
    }


    private Future<JsonArray> getPeriod(Integer periodId, String classId, String structureId) {
        Promise<JsonArray> promise = Promise.promise();
        if (periodId != null) promise.complete(new JsonArray());
        else
            utilsService.getPeriodes(Collections.singletonList(classId), structureId, FutureHelper.handler(promise));
        return promise.future();
    }

    private Future<JsonObject> getSubjectAveragesStudents(JsonArray periodes, String idClasse, Integer idPeriod,
                                                          String idEtablissement, Integer typeGroup, String name,
                                                          SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                                          Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve) {
        Promise<JsonObject> promise = Promise.promise();
        if (periodes != null && !periodes.isEmpty())
            noteService.getMoysEleveByMatByYear(idEtablissement, periodes, typeGroup,
                    name, mapAllidMatAndidTeachers, mapIdMatListMoyByEleve, FutureHelper.handler(promise));
        else
            noteService.getMoysEleveByMatByPeriode(idClasse, idPeriod, idEtablissement, typeGroup,
                    name, mapAllidMatAndidTeachers, mapIdMatListMoyByEleve, false, FutureHelper.handler(promise));

        return promise.future();
    }

    private Future<JsonArray> getPeriodicReportSummaries(Long periodId, boolean isWithAppreciations, List<String> studentIds,
                                                         String structureId) {
        Promise<JsonArray> promise = Promise.promise();
        if (periodId != null && isWithAppreciations)
            syntheseBilanPeriodiqueService.getPeriodicReportSummaries(periodId, studentIds, structureId)
                    .onFailure(promise::fail)
                    .onSuccess(promise::complete);
        else promise.complete(new JsonArray());

        return promise.future();
    }

    private void formatDataForPdf(JsonObject result, JsonObject subjectsEvaluated, List<JsonObject> summaries,
                                  Boolean withMoyGeneraleByEleve) {
        orderAveragesByRank(result, subjectsEvaluated);
        setStudentsSummary(result, summaries);
        setTotalColspan(result, withMoyGeneraleByEleve);
    }

    private void orderAveragesByRank(JsonObject result, JsonObject subjectsEvaluated) {
        result.getJsonArray(Field.ELEVES, new JsonArray()).stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .forEach(eleve -> eleve.put(Field.ELEVEMOYBYMAT,
                        Utils.sortJsonArrayIntValue(Field.RANK,
                                new JsonArray(eleve.getJsonArray(Field.ELEVEMOYBYMAT, new JsonArray())
                                        .stream()
                                        .filter(JsonObject.class::isInstance)
                                        .map(JsonObject.class::cast)
                                        .map(eleveMoy -> eleveMoy.put(Field.RANK, subjectsEvaluated
                                                .getJsonArray(Field.MATIERES, new JsonArray())
                                                .stream()
                                                .filter(JsonObject.class::isInstance)
                                                .map(JsonObject.class::cast)
                                                .filter(matiere -> Objects.equals(eleveMoy.getString(Field.ID_MATIERE), matiere.getString(Field.ID)))
                                                .findFirst()
                                                .map(matiere -> matiere.getInteger(Field.RANK, 0))
                                                .orElse(0))
                                        )
                                        .collect(Collectors.toList())))));
    }

    private void setStudentsSummary(JsonObject result, List<JsonObject> summaries) {
        summaries.forEach(summary -> result.getJsonArray(Field.ELEVES, new JsonArray())
                .stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(student -> student.getString(Field.ID_ELEVE) != null &&
                        student.getString(Field.ID_ELEVE).equals(summary.getString(Field.ID_ELEVE)))
                .findFirst()
                .ifPresent(student -> student.put(Field.SUMMARY, summary.getString(Field.SYNTHESE))));
    }

    private void setTotalColspan(JsonObject result, Boolean withMoyGeneraleByEleve) {
        result.put(Field.TOTALCOLUMN,
                // 1 systematic column for students name + 1 column per subject + 1 column only if we want to show global average
                1 + result.getJsonArray(Field.MATIERES, new JsonArray()).size()
                        + (Boolean.TRUE.equals(withMoyGeneraleByEleve) ? 1 : 0)
        );
    }
}
