package fr.openent.competences.helpers;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.service.impl.DefaultCompetencesService;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.Competences.LIBELLE;
import static fr.openent.competences.Utils.isNotNull;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class FormSaisieHelper {
    public static Future getPeriodeForFormaSaisie(JsonObject devoirInfos, String language, String host,
                                                  JsonObject result, EventBus eb) {
        Future periodeFuture = Future.future();
        JsonObject jsonRequest = new JsonObject()
                .put("headers", new JsonObject().put("Accept-Language", language))
                .put("Host", host);
        JsonObject action = new JsonObject()
                .put("action", "periode.getLibellePeriode")
                .put("type", devoirInfos.getInteger("periodetype"))
                .put("ordre", devoirInfos.getInteger("periodeordre"))
                .put("request", jsonRequest);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                result.put("periode", body.getString("result"));
                periodeFuture.complete();
            }
            else {
                periodeFuture.fail("Error :can not get periode devoir ");
            }
        }));

        return periodeFuture;
    }

    public static Future getStudentsForFormSaisie(JsonObject devoirInfos, JsonObject result, EventBus eb){
        Future studentsFuture = Future.future();
        JsonObject action = new JsonObject()
                .put("action", "classe.getEleveClasse")
                .put("idClasse", devoirInfos.getString("id_groupe"))
                .put("idPeriode", devoirInfos.getInteger("id_periode"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if ("ok".equals(body.getString("status"))) {
                result.put("eleves", Utils.sortElevesByDisplayName(body.getJsonArray("results")));
                studentsFuture.complete();
            }
            else {
                studentsFuture.fail("Error :can not get students ");
            }
        }));
        return studentsFuture;
    }


    public static  Future getSubjectsFuture(JsonObject devoirInfos, JsonObject result, EventBus eb){
        Future subjectFuture = Future.future();
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatiere")
                .put("idMatiere", devoirInfos.getString("id_matiere"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(
                message -> {
                    JsonObject body = message.body();

                    if ("ok".equals(body.getString("status"))) {
                        result.put("matiere", body.getJsonObject("result").getJsonObject("n").getJsonObject("data")
                                .getString("label"));
                        subjectFuture.complete();
                    } else {
                        subjectFuture.fail("Error :can not get classe info ");
                    }
                }));
        return subjectFuture;
    }

    public static  Future getClasseFuture(JsonObject devoirInfos, JsonObject result, EventBus eb) {
        Future classeFuture = Future.future();
        JsonObject action = new JsonObject()
                .put("action", "classe.getClasseInfo")
                .put("idClasse", devoirInfos.getString("id_groupe"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();
            if ("ok".equals(body.getString("status"))) {
                result.put("classeName", body.getJsonObject("result").getJsonObject("c").getJsonObject("data")
                        .getString("name"));
                classeFuture.complete();
            } else {
                classeFuture.fail("Error :can not get classe informations ");
            }
        }));
        return classeFuture;
    }

    public static Future getCompFuture (Long idDevoir, JsonObject devoirInfos, JsonObject result, EventBus eb) {
        Future compFuture = Future.future();
        if(devoirInfos.getInteger("nbrcompetence") > 0) {
            new DefaultCompetencesService(eb).getDevoirCompetences(idDevoir,
                    CompetencesObject -> {
                        if(CompetencesObject.isRight()){
                            JsonArray CompetencesOld = CompetencesObject.right().getValue();
                            final JsonArray  CompetencesNew = new fr.wseduc.webutils.collections.JsonArray();
                            Double ligne = new Double(0);
                            Integer length = 103; // le nombre de caractére max dans une ligne
                            Double height = new Double(2.2); // la hauteur d'une ligne
                            for (int i=0 ; i < CompetencesOld.size() ; i++) {
                                JsonObject Comp = CompetencesOld.getJsonObject(i);
                                Integer size = Comp.getString("nom").length() +10; // +10 pour "[ Cx ]"
                                ligne +=  size / length ;
                                if(size%length > 0 ){
                                    ligne++;
                                }
                                Comp.put("i", i+1);
                                CompetencesNew.add(Comp);
                            }

                            ligne = (ligne * height) + 6; // + 6 la hauteur de la 1 ligne du tableau
                            if( ligne < 25){ // 25 est la hauteure minimal
                                ligne = Double.parseDouble("25") ;
                            }
                            result.put("ligne", ligne.toString()+"%");
                            if(CompetencesNew.size() > 0){
                                result.put("hasCompetences",true);
                            }else{
                                result.put("hasCompetences",false);
                            }
                            result.put("competences",CompetencesNew);
                            compFuture.complete();
                        }else{
                            compFuture.fail("Error :can not get competences devoir ");
                        }
                    });
        }else{
            compFuture.complete();
        }

        return compFuture;
    }

    public static void formatDevoirsInfos(JsonObject devoirInfos, JsonObject result) {
        String[] date = devoirInfos.getString("date")
                .substring(0, devoirInfos.getString("date").indexOf(" ")).split("-");
        result.put("date", date[2] + '/' + date[1] + '/' + date[0]);
        result.put("devoirName", devoirInfos.getString("name"));
        result.put("devoirCoefficient", devoirInfos.getString("coefficient"));
        result.put("devoirDiviseur", devoirInfos.getLong("diviseur"));
        result.put("evaluation", devoirInfos.getBoolean("is_evaluated"));
        String libelleSoumatiere = devoirInfos.getString(LIBELLE);
        result.put("sousMatiere", libelleSoumatiere);
        result.put("hasSousMatiere", isNotNull(libelleSoumatiere));
        if(devoirInfos.getBoolean("is_evaluated") == true){
            Integer nbrColone = (devoirInfos.getInteger("nbrcompetence") + 1 );
            result.put("nbrCompetences",nbrColone.toString());
        }else{
            result.put("nbrCompetences",devoirInfos.getInteger("nbrcompetence").toString());
        }
    }
}
