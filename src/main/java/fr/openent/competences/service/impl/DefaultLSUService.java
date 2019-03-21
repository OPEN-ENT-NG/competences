package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.bean.lsun.CodeDomaineSocle;
import fr.openent.competences.service.LSUService;
import fr.openent.competences.service.UtilsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicBoolean;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultLSUService implements LSUService {

    private static final Logger log = LoggerFactory.getLogger(DefaultExportBulletinService.class);
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

}
