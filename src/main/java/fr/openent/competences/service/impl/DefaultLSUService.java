package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.lsun.CodeDomaineSocle;
import fr.openent.competences.bean.lsun.Discipline;
import fr.openent.competences.bean.lsun.Donnees;
import fr.openent.competences.service.LSUService;
import fr.openent.competences.service.UtilsService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.SqlStatementsBuilder;

import java.util.*;
import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fr.openent.competences.Competences.ID_ELEVE;
import static fr.openent.competences.Competences.ID_ELEVE_KEY;
import static fr.openent.competences.Competences.ID_PERIODE;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultLSUService implements LSUService {
    public static final String DISCIPLINE_KEY = "DIS_";
    private static final Logger log = LoggerFactory.getLogger(DefaultExportBulletinService.class);
    public static final String LSU_UNHEEDED_STUDENTS_TABLE = "eleves_ignores_lsu";
    private JsonArray idsEvaluatedDiscipline;
    private UtilsService utilsService;
    protected EventBus eb;

    private static final String TIME = "Time";
    private static final String MESSAGE = "message";
    public DefaultLSUService(EventBus eb){
        utilsService = new DefaultUtilsService();
        this.eb = eb;
    }


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
        if(correctEvaluatedDisciplines.getDiscipline().isEmpty()) {
            errorsExport.put("emptyDiscipline", true);
        }
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

    public void getIdClassIdCycleValue(List<String> classIds, final Handler<Either<String, List<Map>>> handler) {
        utilsService.getCycle(classIds, new Handler<Either<String, JsonArray>>() {
            int count = 0;
            AtomicBoolean answer = new AtomicBoolean(false);
            final String thread = "classIds -> " + classIds.toString();
            final String method = "getIdClassIdCycleValue";
            @Override
            public void handle(Either<String, JsonArray> response) {
                if (response.isRight()) {
                    JsonArray cycles = response.right().getValue();
                    Map mapIclassIdCycle = new HashMap<>();
                    Map mapIdCycleValue_cycle = new HashMap<>();
                    List<Map> mapArrayList = new ArrayList<>();
                    try {
                        for (int i = 0; i < cycles.size(); i++) {
                            JsonObject o = cycles.getJsonObject(i);
                            if(o.getString("id_groupe")!=null &&o.getLong("id_cycle")!=null && o.getLong("value_cycle")!=null) {
                                mapIclassIdCycle.put(o.getString("id_groupe"), o.getLong("id_cycle"));
                                mapIdCycleValue_cycle.put(o.getLong("id_cycle"), o.getLong("value_cycle"));
                            }else {
                                throw new Exception ("Erreur idGroupe, idCycle et ValueCycle null");
                            }
                        }
                        mapArrayList.add(mapIclassIdCycle);
                        mapArrayList.add(mapIdCycleValue_cycle);
                    }catch(Exception e){
                        handler.handle(new Either.Left<String, List<Map>>(" Exception " + e.getMessage()));
                        log.error("catch Exception in getCycle" + e.getMessage());
                    }
                    answer.set(true);
                    serviceResponseOK(answer, count, thread, method);
                    handler.handle(new Either.Right<String, List<Map>>(mapArrayList));
                } else {
                    String error =  response.left().getValue();
                    count ++;
                    serviceResponseOK(answer, count, thread, method);
                    if (error!=null && error.contains(TIME)){
                        utilsService.getCycle(classIds, this);
                    }
                    else {
                        handler.handle(new Either.Left<String, List<Map>>(
                                " getValueCycle : error when collecting Cycles " + error));
                        log.error("method getIdClassIdCycleValue an error occured when collecting Cycles " + error);
                    }
                }
            }
        });
    }



    /**
     * méthode qui permet de construire une Map avec id_domaine et son code_domaine (domaine de hérarchie la plus haute)
     * @param idsClass liste des idsClass
     * @param handler contient la map<IdDomaine,Code_domaine> les codes domaines : codes des socles communs au cycle
     */
    @Override
    public void getMapIdClassCodeDomaineById(List<String> idsClass, Handler<Either<String, Map<String,Map<Long, String>>>> handler) {

        List<Future> listFutureClass = new ArrayList<>();
        Map<String,Map<Long,String>> mapIdClassCodesDomaines = new HashMap<>();
        for(String idClass : idsClass) {
            Future classFuture = Future.future();
            listFutureClass.add(classFuture);
            JsonObject action = new JsonObject()
                    .put("action", "user.getCodeDomaine")
                    .put("idClass", idClass);

            eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                int count = 0;
                AtomicBoolean answer = new AtomicBoolean(false);
                final String thread = "classIds -> " + idsClass.toString();
                final String method = "getMapCodeDomaineById";

                @Override
                public void handle(Message<JsonObject> message) {
                    JsonObject body = message.body();
                    if ("ok".equals(body.getString("status"))) {
                        JsonArray domainesJson = message.body().getJsonArray("results");
                        Map<Long, String> mapDomaines = new HashMap<>();
                        try {
                            for (int i = 0; i < domainesJson.size(); i++) {
                                JsonObject o = domainesJson.getJsonObject(i);
                                if (CodeDomaineSocle.valueOf(o.getString("code_domaine")) != null) {
                                    mapDomaines.put(o.getLong("id_domaine"), o.getString("code_domaine"));
                                }
                            }
                            //la mapDomaines n'est renvoyee que si elle contient les 8 codes domaine du socle commun
                            if (mapDomaines.size() == CodeDomaineSocle.values().length) {
                                mapIdClassCodesDomaines.put(idClass,mapDomaines);
                                classFuture.complete();
                                // log for time-out
                                answer.set(true);
                                serviceResponseOK(answer, count, thread, method);

                            } else {
                                throw new Exception("getMapCodeDomaine : map incomplete");
                            }
                        } catch (Exception e) {

                            if (e instanceof IllegalArgumentException) {
                                handler.handle(new Either.Left<String, Map<String,Map<Long, String>>>("code_domaine en base de données non valide"));
                            } else {
                                handler.handle(new Either.Left<String, Map<String,Map<Long, String>>>("getMapCodeDomaineById : "));
                                log.error("getMapCodeDomaineById : " + e.getMessage());
                            }
                            // log for time-out
                            answer.set(true);
                            serviceResponseOK(answer, count, thread, method);
                        }
                    } else {
                        String error = body.getString(MESSAGE);
                        count++;
                        serviceResponseOK(answer, count, thread, method);
                        if (error != null && error.contains(TIME)) {
                            eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                    handlerToAsyncHandler(this));
                        } else {
                            handler.handle(new Either.Left<String, Map<String,Map<Long, String>>>(
                                    "getMapCodeDomaineById : error when collecting codeDomaineById : " + error));
                            log.error("method getMapCodeDomaineById an error occured when collecting CodeDomaineById " +
                                    error);
                        }
                    }
                }
            }));
        }
        CompositeFuture.all(listFutureClass).setHandler(event -> {
            handler.handle(new Either.Right<String, Map<String,Map<Long, String>>>(mapIdClassCodesDomaines));
        });
    }

    private void prepareStatements( SqlStatementsBuilder statements, JsonArray idsStudents, Long idPeriode,
                                   String idClasse, String query){
        for (Object idStudent: idsStudents) {
            JsonArray values = new JsonArray().add((String) idStudent).add(idClasse);
            if(idPeriode != null){
                values.add(idPeriode);
            }
            statements.prepared(query, values);
        }
    }
    public void addUnheededStudents(JsonArray idsStudents, Long idPeriode, String idClasse,
                                    final Handler<Either<String, JsonArray>> handler){
        SqlStatementsBuilder statements = new SqlStatementsBuilder();
        String query = " INSERT INTO " + Competences.EVAL_SCHEMA + "." + LSU_UNHEEDED_STUDENTS_TABLE
                + " (id_eleve, id_classe, id_periode, created) "
                + " VALUES (?, ?, " + ((idPeriode != null) ? " ?,": "-1," ) + " NOW()) "
                + " ON CONFLICT (id_eleve, id_classe,id_periode) DO UPDATE SET created = NOW() ";

       prepareStatements(statements, idsStudents, idPeriode, idClasse, query);

        Sql.getInstance().transaction(statements.build(), validResultHandler(handler));
    }
    public void remUnheededStudents(JsonArray idsStudents, Long idPeriode, String idClasse,
                                    final Handler<Either<String, JsonArray>> handler){

        SqlStatementsBuilder statements = new SqlStatementsBuilder();
        String query = " DELETE FROM " + Competences.EVAL_SCHEMA + "." + LSU_UNHEEDED_STUDENTS_TABLE
                + " WHERE id_eleve =? AND  id_classe = ? "
                + ((idPeriode!=null)?" AND id_periode =? ;": " AND id_periode =-1 ;");

        prepareStatements(statements, idsStudents, idPeriode, idClasse, query);

        Sql.getInstance().transaction(statements.build(), validResultHandler(handler));
    }

    public void getUnheededStudents(JsonArray idPeriodes, JsonArray idClasses,
                                    final  Handler<Either<String, JsonArray>> handler){
        StringBuilder query = new StringBuilder().append(" SELECT * ").append(" FROM ")
                .append(Competences.EVAL_SCHEMA).append(".").append(LSU_UNHEEDED_STUDENTS_TABLE)
                .append(" WHERE id_classe IN ").append(Sql.listPrepared(idClasses.getList()))
                .append(" AND id_periode ");

        if(idPeriodes != null && !idPeriodes.isEmpty()) {
            query.append(" IN ").append(Sql.listPrepared(idPeriodes.getList()));
        }
        else{
            query.append(" = -1 ");
        }

        JsonArray values = new JsonArray().addAll(idClasses);
        if(idPeriodes != null){
            values.addAll(idPeriodes);
        }
        Sql.getInstance().prepared(query.toString(), values, Competences.DELIVERY_OPTIONS,
                SqlResult.validResultHandler(handler));
    }
    public void getUnheededStudents(JsonArray idPeriodes, JsonArray idClasses, String idStructure,
                                    final Handler<Either<String, JsonArray>> handler){

        getUnheededStudents(idPeriodes, idClasses, response -> {
            if(response.isLeft()){
                handler.handle(new Either.Left<>(response.left().getValue()));
            }
            else {
                JsonArray unheededStudents = response.right().getValue();
                if(unheededStudents == null || unheededStudents.isEmpty()) {
                    handler.handle(new Either.Right<>(new JsonArray()));
                }
                else {
                    Map<String, List<JsonObject>> ignoredInfos =  ((List<JsonObject>)unheededStudents.getList())
                            .stream().collect(Collectors.groupingBy(  o ->  o.getString("id_eleve")));

                    JsonObject action = new JsonObject()
                            .put("action", "eleve.getInfoEleve")
                            .put(Competences.ID_ETABLISSEMENT_KEY, idStructure)
                            .put("idEleves", new JsonArray(Arrays.asList(ignoredInfos.keySet().toArray())));

                    eb.send(Competences.VIESCO_BUS_ADDRESS, action,
                            handlerToAsyncHandler( studentsInfo ->  {
                                JsonObject body = studentsInfo.body();
                                if (!"ok".equals(body.getString("status"))) {
                                    handler.handle(new Either.Left<>(body.getString(MESSAGE)));
                                }
                                else {
                                    JsonArray students = body.getJsonArray("results");
                                    JsonArray results = new JsonArray();
                                    // On ne récupère que les élèves des classes où ils  sont ignorés
                                    students.getList().forEach( s -> {
                                        JsonObject student = (JsonObject)s;
                                        String idClasse = student.getString("idClasse");
                                        JsonArray studentIgnoredInfos = new JsonArray();
                                        List<JsonObject> studentIgnoredInfo = ignoredInfos
                                                .get(student.getString(ID_ELEVE_KEY));
                                        for(int i = 0; i<studentIgnoredInfo.size(); i++){
                                            String idClaasStudentIgnored = studentIgnoredInfo.get(i).getString("id_classe");
                                            JsonObject studentIgnored = new JsonObject();
                                            if(idClaasStudentIgnored.equals(idClasse)){
                                                studentIgnored = student;
                                                studentIgnoredInfos.add(studentIgnoredInfo.get(i));
                                            }else {//ignored Student was in old class and idClaasStudentIgnored = id of old Class
                                                //oldClasses ={ idClasseOld1 : nameOldClasse1, ...}
                                                String oldClasse = student.getJsonObject("oldClasses").getString(idClaasStudentIgnored);
                                               if(oldClasse != null){

                                                   studentIgnored.put("idEleve",student.getString("idClasse"));
                                                   studentIgnored.put("firstName",student.getString("firstName"));
                                                   studentIgnored.put("lastName",student.getString("lastName"));
                                                   studentIgnored.put("idClasse",studentIgnoredInfo.get(i).getString("id_classe"));
                                                   studentIgnored.put("classeName", student.getJsonObject("oldClasses").getString(idClaasStudentIgnored));
                                                   studentIgnoredInfos.add(studentIgnoredInfo.get(i));
                                               }
                                            }
                                            if(!studentIgnoredInfos.isEmpty()) {
                                                studentIgnored.put("ignoredInfos", studentIgnoredInfos);
                                                results.add(studentIgnored);
                                            }
                                        }

                                    });
                                    handler.handle(new Either.Right<>(results));
                                }
                            }));

                }
            }
        });

    }

    public void getStudents(final List<String> classids, Future<Message<JsonObject>> studentsFuture,
                            AtomicInteger count, AtomicBoolean answer, final String thread, final String method){

        JsonObject action = new JsonObject()
                .put("action", "user.getElevesRelatives")
                .put("idsClass", new fr.wseduc.webutils.collections.JsonArray(classids));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> message) {
                        JsonObject body = message.body();
                        if (!("ok".equals(body.getString("status"))
                                && body.getJsonArray("results").size() != 0)) {
                            String error = body.getString(MESSAGE);
                            count.addAndGet(1);
                            serviceResponseOK(answer, count.get(), thread, method);
                            if (error != null && error.contains(TIME)) {
                                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                        handlerToAsyncHandler(this));
                            } else {
                                String failure = "getBaliseEleves : error when collecting Eleves " + error;
                                studentsFuture.fail(failure);
                                log.error("method getBaliseEleves an error occured when collecting Eleves " + error);
                            }
                        }
                        else {
                            studentsFuture.complete(message);
                        }
                    }
                }));
    }
    /**
     * get Deleted Student in Postgres ( in personne_supp table)
     * @param periodesByClass map requested periodes by class
     * @param handler response
     */
    @Override
    public void getDeletedStudentsPostgres(final Map<String, JsonArray> periodesByClass,
                                           Map<String, JsonObject> mapDeleteStudent,
                                          Handler<Either<String, Map<String, JsonObject>>> handler) {

        if(periodesByClass != null && periodesByClass.size()>0){

            final List<Future> futuresList = new ArrayList<>();

            for(Map.Entry<String,JsonArray> classPeriodes : periodesByClass.entrySet()){
                Future<JsonArray> futureDeletedStudentsByClass = Future.future();
                futuresList.add(futureDeletedStudentsByClass);

                String beginingPeriode = Utils.getPeriode(classPeriodes.getValue(),true);

                JsonObject action = new JsonObject()
                        .put("action", "eleve.getDeletedStudentByPeriodeByClass")
                        .put("idClass",classPeriodes.getKey())
                        .put("beginningPeriode",beginingPeriode);
                final String finalBeginingPeriode = beginingPeriode;
                eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                        handlerToAsyncHandler(new Handler<Message<JsonObject>>() {

                            AtomicBoolean answer = new AtomicBoolean(false);
                            AtomicInteger count = new AtomicInteger(0);
                            String thread = "("  + finalBeginingPeriode + ", "+ classPeriodes.getKey()+ " )";
                            String method = "getDeletedStudentsPostgres";

                            @Override
                            public void handle(Message<JsonObject> message) {

                                if("ok".equals(message.body().getString("status"))){

                                    JsonArray deletedStudentsPostgresByClasse = message.body().getJsonArray("results");

                                    if(deletedStudentsPostgresByClasse != null && deletedStudentsPostgresByClasse.size()>0){

                                        for(int j = 0 ; j < deletedStudentsPostgresByClasse.size(); j++){

                                            JsonObject deleteStudent = deletedStudentsPostgresByClasse.getJsonObject(j);

                                            //set map<IdEleve,JsonObject of deleted student
                                            if(mapDeleteStudent.containsKey(deleteStudent.getString("id_user"))){
                                                JsonObject deletedStudentMap = mapDeleteStudent.get(deleteStudent.getString("id_user"));

                                                JsonArray deleteDateClass = deleteStudent.getJsonArray("delete_date_id_class");

                                                for(int k=0 ; k < deleteDateClass.size(); k++){
                                                    deletedStudentMap.getJsonArray("delete_date_id_class")
                                                            .add(deleteDateClass.getJsonObject(k));
                                                }

                                            }else{
                                                mapDeleteStudent.put(deleteStudent.getString("id_user"),deleteStudent);
                                            }
                                        }
                                    }
                                    //log for time-out
                                    answer.set(true);
                                    serviceResponseOK(answer, count.get(),thread,method);
                                    futureDeletedStudentsByClass.complete();
                                }else{
                                    String error =  message.body().getString(MESSAGE);
                                    if (error!=null && error.contains(TIME)){
                                        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                                                handlerToAsyncHandler(this));
                                    }
                                    else {
                                        // log for time-out
                                        answer.set(true);
                                        futureDeletedStudentsByClass.complete();
                                    }
                                    serviceResponseOK(answer, count.incrementAndGet(), thread, method);
                                }


                            }
                        }));

            }

            CompositeFuture.all(futuresList).setHandler(event -> {
                if(event.failed()){
                    String error = event.cause().getMessage();
                    log.info(error);
                    handler.handle(new Either.Left<>(error));
                }else{
                    handler.handle(new Either.Right<>(mapDeleteStudent));
                }
            });
        }
    }

    @Override
    public void getAllStudentWithRelatives(String idStructure, List<String> idsClass, List<String> idsDeletedStudent, Handler<Either<String, JsonArray>> handler) {

        JsonObject action = new JsonObject()
                .put("action", "user.getAllElevesWithTheirRelatives")
                .put("idStructure",idStructure)
                .put("idsClass",new fr.wseduc.webutils.collections.JsonArray(idsClass))
                .put("idsDeletedStudent",new fr.wseduc.webutils.collections.JsonArray(idsDeletedStudent));
        eb.send(Competences.VIESCO_BUS_ADDRESS,action, Competences.DELIVERY_OPTIONS,handlerToAsyncHandler(new Handler<Message<JsonObject>>() {
            AtomicInteger count = new AtomicInteger(0);
            String thread = "idsresponsable -> " + action.encode();
            String method = "getAllStudentWithRelatives";
            AtomicBoolean answer = new AtomicBoolean(false);

            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if("ok".equals(body.getString("status"))){
                    JsonArray allStudentsWithRelative = body.getJsonArray("results");

                    handler.handle(new Either.Right<>(allStudentsWithRelative));

                    answer.set(true);
                    serviceResponseOK(answer,count.get(),thread,method);
                }else{
                    String error = body.getString(MESSAGE);
                    serviceResponseOK(answer,count.incrementAndGet(),thread,method);
                    if(error != null && error.contains(TIME)){
                        eb.send(Competences.VIESCO_BUS_ADDRESS, action,Competences.DELIVERY_OPTIONS, handlerToAsyncHandler(this));
                    }else{
                        handler.handle(new Either.Left<>("get all students : error when collecting Students : "+ error));
                        log.error("method getAllStudentWithRelatives error when collecting Students : "+ error);
                    }
                }
            }
        }));
    }
    public int nbIgnoredTimes(String idEleve, String idClasse, Map<String,JsonArray> periodesByClass,
                          Map<Long, JsonObject> periodeUnheededStudents){
        AtomicInteger nbIgnoredTimes = new AtomicInteger(0);
        if(periodesByClass.containsKey(idClasse)) {
            JsonArray periodes = periodesByClass.get(idClasse);
            periodes.getList().forEach(periode -> {
                if (periodeUnheededStudents != null) {
                    JsonObject unheededPeriode = periodeUnheededStudents
                            .get(Long.valueOf(((JsonObject) periode).getInteger("id_type")));
                    if (unheededPeriode != null) {
                        JsonArray unheededClass = unheededPeriode.getJsonArray(idClasse);
                        if (unheededClass != null && unheededClass.contains(idEleve)) {
                            nbIgnoredTimes.incrementAndGet();
                        }
                    }
                }
            });
        }
        return nbIgnoredTimes.get();
    }

    public Boolean isIgnorated(String idEleve, String idClasse, Long idPeriode,
                               Map<Long, JsonObject> periodeUnheededStudents){
        Boolean ignorated = false;
        if (periodeUnheededStudents != null && periodeUnheededStudents.containsKey(idPeriode)) {
            JsonObject unheededPeriode = periodeUnheededStudents.get(idPeriode);
            if(unheededPeriode != null) {
                JsonArray unheededClass = unheededPeriode.getJsonArray(idClasse);
                if(unheededClass != null && unheededClass.contains(idEleve)){
                    ignorated = true;
                }
            }
        }

        return ignorated;
    }

    public void setLsuUnheededStudents(Future<JsonArray> ignoredStudentFuture,
                                       Map<Long, JsonObject> periodeUnheededStudents ){
        ignoredStudentFuture.result().getList().forEach( ignoredStudent -> {
            JsonObject ignoredStudentJs = ((JsonObject)ignoredStudent);
            String idEleve = ignoredStudentJs.getString(ID_ELEVE);
            Long idPeriode = ignoredStudentJs.getLong(ID_PERIODE);
            String idClasse = ignoredStudentJs.getString("id_classe");
            if (!periodeUnheededStudents.containsKey(idPeriode)) {
                periodeUnheededStudents.put(idPeriode, new JsonObject());
            }
            JsonObject classesStudentsPeriode = periodeUnheededStudents.get(idPeriode);
            if(!classesStudentsPeriode.containsKey(idClasse)){
                periodeUnheededStudents.get(idPeriode).put(idClasse, new JsonArray());
            }
            periodeUnheededStudents.get(idPeriode).getJsonArray(idClasse).add(idEleve);
        });
    }

    public JsonArray filterUnheededStrudentsForBfc(JsonArray students, JsonArray unheededStudents ){
        JsonArray results = new JsonArray();
        Map<String, JsonArray> unheededStudentByClass = new HashMap<>();
        unheededStudents.getList().forEach( ignoredStudent -> {
            JsonObject ignoredStudentJs = ((JsonObject)ignoredStudent);
            String idEleve = ignoredStudentJs.getString(ID_ELEVE);
            String idClasse = ignoredStudentJs.getString("id_classe");

            if(!unheededStudentByClass.containsKey(idClasse)){
                unheededStudentByClass.put(idClasse, new JsonArray());
            }
            if(!unheededStudentByClass.get(idClasse).contains(idEleve)) {
                unheededStudentByClass.get(idClasse).add(idEleve);
            }
        });

        students.getList().forEach( student -> {
            JsonObject studentJson = (JsonObject) student;
            String idClasse = studentJson.getString("idClass");
            String idEleve = studentJson.getString("idNeo4j");
            Boolean isIgnorated = false;
            if(unheededStudentByClass.containsKey(idClasse)){
                if(unheededStudentByClass.get(idClasse).contains(idEleve)){
                    isIgnorated = true;
                }
            }
            if(!isIgnorated){
                results.add(studentJson);
            }
        });

        return results;
    }
}
