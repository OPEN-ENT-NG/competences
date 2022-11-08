package fr.openent.competences.helpers;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.impl.*;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.ID_ETABLISSEMENT;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExportEvaluationHelper {

    protected static final Logger log = LoggerFactory.getLogger(ExportEvaluationHelper.class);

    public static void formatDevoir(JsonObject devoir, String language, String host, Map<String, Object> devoirMap) {

        String[] date = devoir.getString("date")
                .substring(0, devoir.getString("date").indexOf(" ")).split("-");
        devoirMap.put("date", date[2] + '/' + date[1] + '/' + date[0]);

        devoirMap.put(ID_KEY, devoir.getLong(ID_KEY));
        devoirMap.put("nom", devoir.getString("name"));
        devoirMap.put("coeff", devoir.getString("coefficient"));
        devoirMap.put("sur", Double.valueOf(devoir.getString(Field.DIVISEUR)));
        devoirMap.put("periode", I18n.getInstance().translate("viescolaire.periode." + devoir.getLong("periodetype").toString(), language,host)
                + " " + String.valueOf(devoir.getLong("periodeordre")));
        devoirMap.put("sousMatiere", devoir.getString("libelle", ""));


    }



    public static Future getStudents(JsonObject devoir , EventBus eb, String idGroupe , JsonArray eleves){
        Future future = Future.future();
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getElevesClasses")
                .put(ID_CLASSES_KEY, new JsonArray().add(idGroupe))
                .put(ID_PERIODE_KEY, devoir.getLong(ID_PERIODE));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS, handlerToAsyncHandler( message -> {
            JsonObject body = message.body();
            if (OK.equals(body.getString(STATUS))) {
                eleves.addAll(body.getJsonArray(RESULTS));
                future.complete();
            }
            else{
                String error = "getDevoirsInfos : devoir > cannot  getStudents " + idGroupe;
                log.error(error + "\n" + body.encode() + "\n");
                future.fail(error);
            }

        }));
        return future;
    }

    public static Future getClasseDevoir(JsonObject devoir , Map<String, Object> devoirMap, EventBus eb) {
        Future future = Future.future();
        JsonObject action = new JsonObject()
                .put(ACTION, "classe.getClasseInfo")
                .put(ID_CLASSE_KEY, devoir.getString("id_groupe"));

        eb.send(Competences.VIESCO_BUS_ADDRESS, action,  DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (OK.equals(body.getString(STATUS))) {
                devoirMap.put("classe", body.getJsonObject(RESULT).getJsonObject("c").getJsonObject("data")
                        .getString(NAME));
                future.complete();
            }
            else{
                String error = "getDevoirsInfos : devoir '" + devoirMap.get(ID_KEY) + "), couldn't get class name.";
                log.error(error);
                future.fail(error);
            }
        }));
        return future;
    }

    public  static Future getMatiereDevoir(JsonObject devoir, Map<String, Object> devoirMap, EventBus eb){

        Future future = Future.future();
        JsonObject matiereAction = new JsonObject()
                .put(ACTION, "matiere.getMatiere")
                .put(ID_MATIERE_KEY, devoir.getString(ID_MATIERE));

        eb.send(Competences.VIESCO_BUS_ADDRESS, matiereAction, DELIVERY_OPTIONS, handlerToAsyncHandler(message -> {
            JsonObject body = message.body();

            if (OK.equals(body.getString(STATUS))) {
                devoirMap.put("matiere", body.getJsonObject(RESULT).getJsonObject("n").getJsonObject("data")
                        .getString("label"));
                future.complete();
            } else {
                String error = "getDevoirsInfos : devoir '" +devoirMap.get(ID_KEY)+ "), couldn't get matiere name.";
                log.error(error);
                future.fail(error);
            }
        }));
        return  future;
    }

    public  static Future getNiveauDeMaitriseDevoir(JsonObject devoir, Boolean onlyEvaluation, JsonArray maitrises) {

        Future<JsonArray> maitriseFuture = Future.future();

        if(onlyEvaluation) {
            maitriseFuture.complete(new JsonArray());
        }else {
            final String idEtablissement = devoir.getString(ID_ETABLISSEMENT);
            final Long idCycle = devoir.getLong("id_cycle");
            new DefaultNiveauDeMaitriseService().getNiveauDeMaitrise(idEtablissement, idCycle,
                    event -> {
                        if(event.isRight()) {
                            maitrises.addAll(event.right().getValue());
                            maitriseFuture.complete();
                        }
                        else {
                            String error = event.left().getValue();
                            log.error(error);
                            maitriseFuture.fail(error);
                        }
                    });
        }
        return maitriseFuture;
    }
   


    public static Future getCompetencesNotesDevoir(Long idDevoir, Boolean onlyEvaluation, JsonArray competencesNotes) {
        Future future = Future.future();
        if (onlyEvaluation) {
            future.complete();
        }
        else {
            new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE)
                    .getCompetencesNotesDevoir(idDevoir, event -> {
                        if(event.isLeft()){
                            future.fail(event.left().getValue());
                        }
                        else{
                            competencesNotes.addAll(event.right().getValue());
                            future.complete();
                        }
                    });
        }
        return future;
    }

    public static Future getDevoirCompetences(Long idDevoir, Boolean onlyEvaluation,
                                              EventBus eb, JsonArray competences) {
        Future future = Future.future();
        if (onlyEvaluation) {
            future.complete();
        }
        else {
            new DefaultCompetencesService(eb).getDevoirCompetences(idDevoir, null, event -> {
                if(event.isLeft()){
                    future.fail(event.left().getValue());
                }
                else{
                    competences.addAll(event.right().getValue());
                    future.complete();
                }
            });
        }
        return future;
    }

    public static Future listNotes(Long idDevoir, EventBus eb, JsonArray notes) {
        Future future = Future.future();
        new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE, eb)
                .listNotesParDevoir(idDevoir, event -> {
                    if(event.isLeft()){
                        future.fail(event.left().getValue());
                    }
                    else{
                        notes.addAll(event.right().getValue());
                        future.complete();
                    }
                });
        return future;
    }

    public static Future listAnnotations(String idEtablissement, JsonArray annotations) {
        Future future = Future.future();
        new DefaultAnnotationService(Competences.COMPETENCES_SCHEMA, Competences.REL_ANNOTATIONS_DEVOIRS_TABLE)
                .listAnnotations(idEtablissement, event -> {
                    if(event.isLeft()){
                        future.fail(event.left().getValue());
                    }
                    else{
                        annotations.addAll(event.right().getValue());
                        future.complete();
                    }
                });
        return future;
    }


    public static JsonObject formatJsonObjectExportDevoir(final Boolean text, final Boolean usePerso,
                                                          final JsonObject devoir,
                                                    final Map<String, JsonObject> eleves,
                                                    final Map<String, JsonObject> maitrises,
                                                    final Map<String, JsonObject> competences,
                                                    final Map<String, JsonObject> notes,
                                                    final Map<String, JsonObject> annotations,
                                                    final Map<String, Map<String, JsonObject>> competenceNotes) {

        JsonObject result = new JsonObject();
        result.put("text", text);

        Map<String, String> competenceIndice = new LinkedHashMap<>();
        int i = 1;
        for (Map.Entry<String, JsonObject> competence : competences.entrySet()) {
            competenceIndice.put("[C" + String.valueOf(i) + "]", String.valueOf(competence.getValue().getLong("id_competence")));
            i++;
        }

        //Devoir
        devoir.remove(ID_KEY);
        result.put("devoir", devoir);

        //Maitrise
        JsonArray maitrisesArray = new fr.wseduc.webutils.collections.JsonArray();
        for (JsonObject maitrise : maitrises.values()) {
            JsonObject _maitrise = new JsonObject();

            _maitrise.put("libelle", maitrise.getString("libelle") != null
                    ? maitrise.getString("libelle") : maitrise.getString("default_lib"));

            _maitrise.put("visu", text ? getMaitrise(maitrise.getString("lettre"),
                    String.valueOf(maitrise.getLong(ORDRE))) : maitrise.getString("default"));

            if(usePerso && !text)
                _maitrise.put("persoColor", maitrise.getString("couleur"));

            maitrisesArray.add(_maitrise);
        }
        result.put("maitrise", maitrisesArray);

        //Competences
        JsonArray competencesArray = new fr.wseduc.webutils.collections.JsonArray();
        for (Map.Entry<String, String> competence : competenceIndice.entrySet()) {
            competencesArray.add(competence.getKey() + " " + competences.get(competence.getValue()).getString("code_domaine") + " " + competences.get(competence.getValue()).getString("nom"));
        }
        result.put("competence", competencesArray);

        //Eleves
        JsonArray elevesArray = new fr.wseduc.webutils.collections.JsonArray();

        //Header
        JsonObject headerEleves = new JsonObject();
        headerEleves.put("header", "");
        headerEleves.put("note", "Note");
        headerEleves.put("competenceNotes", new fr.wseduc.webutils.collections.JsonArray());
        for (String indice : competenceIndice.keySet()) {
            headerEleves.getJsonArray("competenceNotes").add(indice);
        }
        result.put("elevesHeader", headerEleves);

        //Body
        for (Map.Entry<String, JsonObject> eleve : eleves.entrySet()) {
            JsonObject eleveObject = new JsonObject();
            eleveObject.put("header", eleve.getValue().getString("lastName").toUpperCase() + " " + eleve.getValue().getString("firstName"));

            String note = "";
            Boolean hasAnnotation = false;
            if (notes.containsKey(eleve.getKey())) {
                if (notes.get(eleve.getKey()).getString("appreciation") != null && !notes.get(eleve.getKey()).getString("appreciation").equals("")) {
                    eleveObject.put("appreciation", notes.get(eleve.getKey()).getString("appreciation"));
                    eleveObject.put("appreciationColspan", competences.size() + 1);
                }
                if (notes.get(eleve.getKey()).getLong("id_annotation") != null) {
                    note = annotations.get(String.valueOf(notes.get(eleve.getKey()).getLong("id_annotation"))).getString("libelle_court");
                    hasAnnotation = true;
                } else {
                    note = notes.get(eleve.getKey()).getString("valeur");
                }
            }
            eleveObject.put("note", note);


            JsonArray competenceNotesEleves = new fr.wseduc.webutils.collections.JsonArray();
            for (String competence : competenceIndice.values()) {
                if (hasAnnotation) {
                    competenceNotesEleves.add("");
                } else if (competenceNotes.containsKey(eleve.getKey()) && competenceNotes.get(eleve.getKey()).containsKey(competence)) {
                    Map<String, JsonObject> competenceNotesEleve = competenceNotes.get(eleve.getKey());
                    String evaluation = String.valueOf(competenceNotesEleve.get(competence).getLong("evaluation"));

                    JsonObject _competenceNotes = new JsonObject();
                    _competenceNotes.put("visu", text ? getMaitrise(maitrises.get(String.valueOf(Integer.valueOf(evaluation) + 1)).getString("lettre"), String.valueOf(Integer.valueOf(evaluation) + 1))
                            : maitrises.get(String.valueOf(Integer.valueOf(evaluation) + 1)).getString("default"));

                    if(usePerso && !text)
                        _competenceNotes.put("persoColor", maitrises.get(String.valueOf(Integer.valueOf(evaluation) + 1)).getString("couleur"));

                    competenceNotesEleves.add(_competenceNotes);
                } else {
                    JsonObject _competenceNotes = new JsonObject();
                    _competenceNotes.put("visu", text ? getMaitrise(maitrises.get("0").getString("lettre"), "0")
                            : getMaitrise(maitrises.get("0").getString("default"), "0"));

                    if(usePerso && !text)
                        _competenceNotes.put("persoColor", maitrises.get(String.valueOf("0")).getString("couleur"));

                    competenceNotesEleves.add(_competenceNotes);
                }
                eleveObject.put("competenceNotes", competenceNotesEleves);
            }
            elevesArray.add(eleveObject);
        }
        result.put("eleves", elevesArray);

//        result.put("height", String.valueOf(calcNumbLine(result)) + "%");

        return result;
    }

}
