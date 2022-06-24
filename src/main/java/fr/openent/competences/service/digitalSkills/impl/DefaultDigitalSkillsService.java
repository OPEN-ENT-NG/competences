package fr.openent.competences.service.digitalSkills.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.helpers.FormateFutureEvent;
import fr.openent.competences.service.digitalSkills.ClassAppreciationDigitalSkillsService;
import fr.openent.competences.service.digitalSkills.DigitalSkillsService;
import fr.openent.competences.service.digitalSkills.StudentDigitalSkillsService;
import fr.openent.competences.service.digitalSkills.StudentAppreciationDigitalSkillsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultDigitalSkillsService implements DigitalSkillsService {
    private static final Logger log = LoggerFactory.getLogger(DefaultDigitalSkillsService.class);
    private final ClassAppreciationDigitalSkillsService classAppDigitalSkills;
    private final StudentAppreciationDigitalSkillsService studentAppDigitalSkills;
    private final StudentDigitalSkillsService studentDigitalSkills;

    public DefaultDigitalSkillsService () {
        classAppDigitalSkills = new DefaultClassAppreciationDigitalSkills(Competences.COMPETENCES_SCHEMA,
                Competences.CLASS_APPRECIATION_DIGITAL_SKILLS);
        studentAppDigitalSkills = new DefaultStudentAppreciationDigitalSkills(Competences.COMPETENCES_SCHEMA,
                Field.STUDENT_APPRECIATION_DIGITAL_SKILLS_TABLE);
        studentDigitalSkills = new DefaultStudentDigitalSkills(Competences.COMPETENCES_SCHEMA,
                Competences.STUDENT_DIGITAL_SKILLS_TABLE);
    }

    public DefaultDigitalSkillsService (ClassAppreciationDigitalSkillsService classAppDigitalSkills,
                                        StudentAppreciationDigitalSkillsService studentAppDigitalSkills,
                                        StudentDigitalSkillsService levelDigitalSkills) {
        this.classAppDigitalSkills = classAppDigitalSkills;
        this.studentAppDigitalSkills = studentAppDigitalSkills;
        this.studentDigitalSkills = levelDigitalSkills;
    }

    @Override
    public void getDigitalSkillsByStudentByClass(String idStudent, String idClass, String idStructure,
                                                 Handler<Either<String, JsonObject>> handler) {
        Future<JsonObject> getStudentAppFuture = Future.future();
        Future<JsonObject> getClassAppFuture = Future.future();
        Future<JsonArray> getEvaluatedDigitalSkillsFuture = Future.future();

        studentAppDigitalSkills.getStudentAppreciation(idStudent, idStructure, responseStudentApp ->
                FormateFutureEvent.formate("[Competences] DefaultDigitalSkills No student appreciation Digital Skills ",
                        getStudentAppFuture, responseStudentApp));
        classAppDigitalSkills.getClassAppreciation(idClass, responseClassApp ->
                FormateFutureEvent.formate("[Competences] DefaultDigitalSkills No class appreciation Digital Skills ",
                        getClassAppFuture, responseClassApp));
        studentDigitalSkills.getEvaluatedDigitalSkills(idStudent, idStructure, responseEvaluations ->
                FormateFutureEvent.formate("[Competences] DefaultDigitalSkills No evaluated Digital Skills ",
                        getEvaluatedDigitalSkillsFuture, responseEvaluations));

        CompositeFuture.all(getStudentAppFuture,getClassAppFuture, getEvaluatedDigitalSkillsFuture).setHandler(
                getHandlerAllDigitalSkills(handler, getStudentAppFuture,getClassAppFuture, getEvaluatedDigitalSkillsFuture));
    }

    private Handler<AsyncResult<CompositeFuture>> getHandlerAllDigitalSkills(
            Handler<Either<String, JsonObject>> handler, Future<JsonObject> getStudentAppFuture, Future<JsonObject> getClassAppFuture,
            Future<JsonArray> getEvaluatedDigitalSkillsFuture) {
        return allResponse -> {
            if (allResponse.failed()) {
                handler.handle(new Either.Left<>(allResponse.cause().getMessage()));
                log.info(allResponse.cause().getMessage());
            } else {
                JsonObject result = new JsonObject();

                JsonObject studentAppreciation = getStudentAppFuture.result();
                JsonObject classAppreciation = getClassAppFuture.result();
                JsonArray evaluatedDigitalSkills = getEvaluatedDigitalSkillsFuture.result();

                result.put("studentAppreciation", studentAppreciation);
                result.put("classAppreciation", classAppreciation);
                result.put("evaluatedDigitalSkills", evaluatedDigitalSkills);

                handler.handle(new Either.Right<>(result));
            }
        };
    }

    @Override
    public void getDigitalSkillsByStudent(String id_student, String id_structure,
                                          Handler<Either<String, JsonObject>> handler) {
        Future<JsonObject> getStudentAppFuture = Future.future();

        Future<JsonArray> getEvaluatedDigitalSkillsFuture = Future.future();

        studentAppDigitalSkills.getStudentAppreciation(id_student, id_structure, responseStudentApp ->
                FormateFutureEvent.formate("[Competences] DefaultDigitalSkills No student appreciation Digital Skills ",
                        getStudentAppFuture, responseStudentApp));
        studentDigitalSkills.getEvaluatedDigitalSkills(id_student, id_structure, responseEvaluations ->
                FormateFutureEvent.formate("[Competences] DefaultDigitalSkills No evaluated Digital Skills ",
                        getEvaluatedDigitalSkillsFuture, responseEvaluations));

        CompositeFuture.all(getStudentAppFuture, getEvaluatedDigitalSkillsFuture).setHandler(
                getHandlerAllDigitalSkillsStudent(handler, getStudentAppFuture, getEvaluatedDigitalSkillsFuture));
    }

    private Handler<AsyncResult<CompositeFuture>> getHandlerAllDigitalSkillsStudent(Handler<Either<String, JsonObject>> handler,
                                                                                    Future<JsonObject> getStudentAppFuture,
                                                                                    Future<JsonArray> getEvaluatedDigitalSkillsFuture) {
        return  allResponseStudent -> {
            if (allResponseStudent.failed()) {
                handler.handle(new Either.Left<>(allResponseStudent.cause().getMessage()));
                log.info(allResponseStudent.cause().getMessage());
            } else {
                JsonObject result = new JsonObject();

                JsonObject studentAppreciation = getStudentAppFuture.result();
                JsonArray evaluatedDigitalSkills = getEvaluatedDigitalSkillsFuture.result();

                result.put("studentAppreciation", studentAppreciation);
                result.put("evaluatedDigitalSkills", evaluatedDigitalSkills);

                handler.handle(new Either.Right<>(result));
            }

        };
    }

    @Override
    public void getAllDigitalSkillsByDomaine(Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder();
        query.append("SELECT ds.id as id_digital_skill, id_domaine, ds.libelle as libelle, dds.libelle as libelle_domaine")
                .append(" FROM ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DIGITAL_SKILLS_TABLE)
                .append(" ds INNER JOIN ").append(Competences.COMPETENCES_SCHEMA).append(".").append(Competences.DOMAINE_DIGITAL_SKILLS_TABLE)
                .append(" dds ON dds.id = ds.id_domaine")
                .append(" ORDER BY id_domaine, id_digital_skill");

        Sql.getInstance().prepared(query.toString(), new JsonArray(), SqlResult.validResultHandler(handler));
    }
}
