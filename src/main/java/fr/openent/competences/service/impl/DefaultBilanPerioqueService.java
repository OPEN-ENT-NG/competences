package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.*;
import fr.openent.competences.utils.FormateFutureEvent;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.fontbox.afm.Composite;
import org.entcore.common.sql.Sql;

import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.openent.competences.Competences.TRANSITION_CONFIG;
import static org.entcore.common.sql.SqlResult.validResultHandler;

public class DefaultBilanPerioqueService implements BilanPeriodiqueService{
    private static final Logger log = LoggerFactory.getLogger(DefaultBilanPerioqueService.class);
    private final NoteService noteService;
    private final UtilsService utilsService;
    private final DevoirService devoirService;
    private final ElementProgramme elementProgramme;
    private final EventBus eb;
    private final Sql sql;

    public DefaultBilanPerioqueService (EventBus eb){
        this.eb = eb;
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        utilsService = new DefaultUtilsService();
        devoirService = new DefaultDevoirService(eb);
        elementProgramme = new DefaultElementProgramme() ;
        sql = Sql.getInstance();

    }

    @Override
    public void getRetardsAndAbsences(String idEleve, Handler<Either<String, JsonArray>> eitherHandler){
        StringBuilder query = new StringBuilder()
                .append(" SELECT * " )
                .append(" FROM viesco.absences_et_retards ")
                .append(" WHERE id_eleve = ? ");
        JsonArray params = new JsonArray().add(idEleve);

        sql.prepared(query.toString(), params, Competences.DELIVERY_OPTIONS,
                validResultHandler(eitherHandler));
    }

    public void getSuiviAcquis(final String idEtablissement, final Long idPeriode,
                                         final String idEleve, final String idClasse ,
                                         Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> subjectFuture = Future.future();
        // Récupération des matières
        devoirService.getMatiereTeacherForOneEleveByPeriode(idEleve, event -> {
            FormateFutureEvent.formate(subjectFuture,event);
        });

        // Récupération des groupes de l'élève
        Future<JsonArray> idsGroupsFuture = Future.future();
        Utils.getGroupsEleve(eb, idEleve,idEtablissement, event -> {
            FormateFutureEvent.formate(idsGroupsFuture, event);
        });


        CompositeFuture.all(subjectFuture, idsGroupsFuture).setHandler( event -> {
            if(event.succeeded()){
                Map<String,JsonObject> idsMatieresIdsTeachers = new HashMap<>();
                JsonArray idsMatieres = new fr.wseduc.webutils.collections.JsonArray();
                JsonArray idsTeachers = new fr.wseduc.webutils.collections.JsonArray();
                JsonArray responseArray = subjectFuture.result();
                JsonArray idClasseGroups = idsGroupsFuture.result();
                if(responseArray == null || responseArray.isEmpty()) {
                    handler.handle(new Either.Right<>(new JsonArray()));
                }
                else {
                    buildSubjectForSuivi(idsMatieresIdsTeachers, idsMatieres, idsTeachers, responseArray);

                    // Récupération du libelle des matières
                    Future<Map<String,JsonObject>> libelleMatiereFuture = Future.future();
                    Utils.getLibelleMatiere( eb, idsMatieres, libelleMatiereEvent -> {
                        FormateFutureEvent.formate(libelleMatiereFuture, libelleMatiereEvent);
                    });

                    // Récupération des noms et prénoms des professeurs
                    Future<Map<String,JsonObject>> lastNameAndFirstNameFuture = Future.future();
                    Utils.getLastNameFirstNameUser(eb, idsTeachers, lastNameAndFirstNameEvent -> {
                        FormateFutureEvent.formate(lastNameAndFirstNameFuture, lastNameAndFirstNameEvent);
                    });

                    CompositeFuture.all(libelleMatiereFuture, lastNameAndFirstNameFuture).setHandler( event1 -> {
                        if(event1.succeeded()) {
                            Map<String, JsonObject> idsMatLibelle = libelleMatiereFuture.result();
                            Map<String, JsonObject> teachersInfos = lastNameAndFirstNameFuture.result();
                            JsonArray idsGroups = new fr.wseduc.webutils.collections.JsonArray();
                            setSubjectLibelleAndTeachers (idEleve, idClasseGroups, idClasse, idEtablissement, idsGroups,
                                    idsMatieresIdsTeachers, idsMatieres, idsMatLibelle, teachersInfos, idPeriode, handler);

                        }
                        else{
                            handler.handle(new Either.Right<>(new JsonArray()));
                        }
                    });
                }

            }
            else {
                String error = event.cause().getMessage();
                log.error("getSuiviAcquisWithFuture " + error);
                handler.handle(new Either.Left<>(error));
            }
        });

    }

    private void setSubjectLibelle(String idMatiere, JsonObject result, Map<String, JsonObject> idsMatLibelle){
        if (idsMatLibelle != null && !idsMatLibelle.isEmpty() && idsMatLibelle.containsKey(idMatiere)) {
            result.put("id_matiere", idMatiere)
                    .put("libelleMatiere", idsMatLibelle.get(idMatiere).getString("name"));
        } else {
            result.put("id_matiere", idMatiere)
                    .put("libelleMatiere", "no libelle");
            log.error("matiere non retrouve sans libelle idMatiere : " + idMatiere);
        }

    }

    private void setTeacherInfo(JsonObject result, JsonArray idsTeachers, Map<String, JsonObject> teachersInfos){
        if (idsTeachers != null) {
            JsonArray teachers = new fr.wseduc.webutils.collections.JsonArray();
            for (Object idTeacher : idsTeachers) {
                if (teachersInfos != null && !teachersInfos.isEmpty() && teachersInfos.containsKey(idTeacher)) {
                    teachers.add(teachersInfos.get(idTeacher));
                } else {
                    teachers.add(new JsonObject().put("id", idTeacher)
                            .put("firstName", "no first name").put("name", "no name"));
                    log.error("enseignant non retrouve idTeacher : " + idTeacher);
                }
            }
            result.put("teachers", teachers);
        }
    }

    private void setSubjectLibelleAndTeachers (String idEleve,JsonArray idClasseGroups, final String idClasse,
                                               String idEtablissement, JsonArray idsGroups,
                                               Map<String,JsonObject> idsMatieresIdsTeachers,
                                               JsonArray idsMatieres, Map<String, JsonObject> idsMatLibelle,
                                               Map<String, JsonObject> teachersInfos, Long idPeriode,
                                               Handler<Either<String, JsonArray>> handler) {
        if (!(idClasseGroups != null && !idClasseGroups.isEmpty())) {
            idsGroups.add(idClasse);
        } else {
            idsGroups.addAll(idClasseGroups);
        }

        // For each subject build the result
        // idMAtTeachersGroups = map<idMat, JsonObject (JsonArray(Teachers),JsonArray(Groups)>
        ArrayList<Future> subjectsFuture = new ArrayList<>();
        JsonArray results = new JsonArray();

        for (Map.Entry<String, JsonObject> idMAtTeachersGroups : idsMatieresIdsTeachers.entrySet()) {
            final JsonObject result = new JsonObject();
            String idMatiere = idMAtTeachersGroups.getKey();
            JsonObject teachersObject = idMAtTeachersGroups.getValue();
            JsonArray idsTeachers = teachersObject.getJsonArray("teachers");

            //Ajout des libellés des matières
            setSubjectLibelle(idMatiere, result, idsMatLibelle);

            // Ajout des enseignants des matières
            setTeacherInfo(result, idsTeachers, teachersInfos);

            // Récupération des élements du Programme
            Future<JsonArray> elementsProgFuture = Future.future();
            elementProgramme.getElementProgrammeClasses(
                    idPeriode, idMatiere, idsGroups,elementsProgEvent -> {
                        FormateFutureEvent.formate(elementsProgFuture, elementsProgEvent);
                    });

            // Récupération des appreciation Moyenne Finale et positionnement Finale
            Future<JsonArray> appreciationMoyFinalePosFuture = Future.future();
            noteService.getAppreciationMoyFinalePositionnement(idEleve, idMatiere, null, event -> {
                FormateFutureEvent.formate(appreciationMoyFinalePosFuture, event);
            });

            // Récupération des notes
            Future<JsonArray> notesFuture = Future.future();
            noteService.getNoteElevePeriode(null, idEtablissement, idsGroups, idMatiere, null,
                    notesEvent -> {FormateFutureEvent.formate(notesFuture, notesEvent);});

            // Récupération des compétences-notes
            Future<JsonArray> compNotesFuture =  Future.future();
            noteService.getCompetencesNotesReleve(idEtablissement, null, null, idMatiere,
                    null, idEleve, null, false, compNotesEvent -> {
                        FormateFutureEvent.formate(compNotesFuture, compNotesEvent);
                    });

            // Récupération de la moyenne finale
            Future<JsonArray> moyenneFinaleFuture = Future.future();
            noteService.getColonneReleve(null, null, idMatiere, idsGroups, "moyenne",
                    moyenneFinaleEvent -> FormateFutureEvent.formate(moyenneFinaleFuture, moyenneFinaleEvent));

            Future<String> subjectFuture = Future.future();
            subjectsFuture.add(subjectFuture);
            CompositeFuture.all(elementsProgFuture, appreciationMoyFinalePosFuture, notesFuture, compNotesFuture,
                    moyenneFinaleFuture).setHandler( event -> {
                if(event.succeeded()){
                    setElementProgramme(result, elementsProgFuture.result());
                    setAppreciationMoyFinalePositionnementEleve(result,appreciationMoyFinalePosFuture.result());
                    setMoyAndPosForSuivi(notesFuture.result(), compNotesFuture.result(),moyenneFinaleFuture.result(),
                            result, idEleve);
                    results.add(result);
                    subjectFuture.complete();
                }
                else{
                    subjectFuture.fail(event.cause().getMessage());
                }
            });
        }

        CompositeFuture.all(subjectsFuture).setHandler(event -> {
            if (event.succeeded()) {
                        String [] sortedField = new  String[1];
                        sortedField[0] = "libelleMatiere";
                        handler.handle(new Either.Right<>(
                                new DefaultUtilsService().sortArray(results,
                                        sortedField)));

                } else {
                    String error = event.cause().getMessage();
                    log.error(error);
                    handler.handle(new Either.Left<>(error));
                }
        });
    }

    private void buildSubjectForSuivi(Map<String,JsonObject> idsMatieresIdsTeachers, JsonArray idsMatieres,
                                      JsonArray idsTeachers, JsonArray responseArray){

        for (int i = 0; i < responseArray.size(); i++) {
            JsonObject responseObject = responseArray.getJsonObject(i);

            if (!idsMatieresIdsTeachers.containsKey(responseObject.getString("id_matiere"))) {
                idsMatieres.add(responseObject.getString("id_matiere"));
                if (!idsTeachers.contains(responseObject.getString("owner"))) {
                    idsTeachers.add(responseObject.getString("owner"));
                }
                JsonArray teachers = new fr.wseduc.webutils.collections.JsonArray()
                        .add(responseObject.getString("owner"));

                idsMatieresIdsTeachers.put(responseObject.getString("id_matiere"),
                        new JsonObject().put("teachers", teachers));
            } else {//on récupère le JsonObject de la cle idmatiere en cours
                JsonArray teachers = idsMatieresIdsTeachers
                        .get(responseObject.getString("id_matiere"))
                        .getJsonArray("teachers");

                teachers.add(responseObject.getString("owner"));
                idsTeachers.add(responseObject.getString("owner"));
            }
        }


    }


    private void setElementProgramme(final JsonObject result, final JsonArray eltsProg) {
        String elementsProg = new String();
        if (eltsProg != null && eltsProg.size() > 0) {
            for (int i = 0; i < eltsProg.size(); i++) {
                if (elementsProg.isEmpty()) {
                    elementsProg = eltsProg
                            .getJsonObject(i)
                            .getString("texte");
                } else {
                    elementsProg += " "
                            + eltsProg
                            .getJsonObject(i)
                            .getString("texte");
                }

            }
        }

        result.put("elementsProgramme", elementsProg);
    }

    private void  setAppreciationMoyFinalePositionnementEleve(final JsonObject result,
                                                              final JsonArray allAppMoyPosi){
        JsonArray appreciations = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray moyennesFinales = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray positionnements = new fr.wseduc.webutils.collections.JsonArray();
        if( allAppMoyPosi != null){

            Map<Integer, JsonArray> mapIdPeriodeAppreciations = new HashMap<>();
            for(int i = 0; i < allAppMoyPosi.size(); i++){
                JsonObject appMoyPosi = allAppMoyPosi.getJsonObject(i);
                if(appMoyPosi.getString("appreciation_matiere_periode") != null ) {

                    if(!mapIdPeriodeAppreciations.containsKey(
                            appMoyPosi.getInteger("id_periode_appreciation"))){

                        mapIdPeriodeAppreciations.put(appMoyPosi.getInteger("id_periode_appreciation"),
                                new fr.wseduc.webutils.collections.JsonArray().add(
                                        new JsonObject().put("idClasse",
                                                appMoyPosi.getString("id_classe_appreciation"))
                                                .put("appreciation",
                                                        appMoyPosi.getString("appreciation_matiere_periode"))));

                    }else {
                        Integer idPeriode = appMoyPosi.getInteger("id_periode_appreciation");
                        JsonArray appreciationsByIdPeriode = mapIdPeriodeAppreciations.get(idPeriode);
                        JsonObject appResponse = new JsonObject().put("idClasse",
                                appMoyPosi.getString("id_classe_appreciation"))
                                .put("appreciation",appMoyPosi.getString("appreciation_matiere_periode"));

                        if(!appreciationsByIdPeriode.contains(appResponse)){

                            mapIdPeriodeAppreciations.put(idPeriode, appreciationsByIdPeriode.add(appResponse) );
                        }
                    }
                }
                //on récupère la moyenne finale de l'élève pour sa classe principale = idClasse passé en
                // paramètre
                //moyennesFinales
                if( appMoyPosi.getString("moyenne_finale") != null) {
                    JsonObject moyenne_finale = new JsonObject();
                    //if(appMoyPosi.getString("id_classe_moyfinale").equals(idClasse)) {
                    // dans le contexte d'un matiere on est sensé n'avoir qu'une moyenne finale
                    // qui est soit sur un groupe soit sur une classe
                    moyenne_finale.put("id_periode",
                            appMoyPosi.getInteger("id_periode_moyenne_finale"))

                            .put("moyenneFinale",
                                    Double.valueOf(appMoyPosi.getString("moyenne_finale")));
                    if(!moyennesFinales.contains(moyenne_finale)){
                        moyennesFinales.add(moyenne_finale);
                    }
                    //}
                }
                //Pour le positionnement on ne peut en avoir qu'un par matière
                //le positionnement n'est pas enregistré par classe
                if(appMoyPosi.getInteger("positionnement_final") != null){
                    JsonObject positionnement = new JsonObject();
                    positionnement.put("id_periode",
                            appMoyPosi.getInteger("id_periode_positionnement"))

                            .put("positionnementFinal",
                                    appMoyPosi.getInteger("positionnement_final"));

                    if(!positionnements.contains(positionnement)){
                        positionnements.add(positionnement);
                    }
                }
            }
            if(!mapIdPeriodeAppreciations.isEmpty()) {
                for (Map.Entry<Integer, JsonArray> idPeriodeApp : mapIdPeriodeAppreciations.entrySet()) {

                    appreciations.add(new JsonObject().put("id_periode",
                            idPeriodeApp.getKey()).put("appreciationByClasse", idPeriodeApp.getValue()));
                }
            }
        }

        result.put("appreciations",appreciations);
        result.put("positionnementsFinaux", positionnements);
        result.put("moyennesFinales",moyennesFinales);
    }


    private void setMoyAndPosForSuivi (JsonArray notes, JsonArray compNotes, JsonArray moyFinalesEleves,
                                       JsonObject result, String idEleve) {
        JsonArray idsEleves = new fr.wseduc.webutils.collections.JsonArray();
        HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                noteService.calculMoyennesEleveByPeriode(notes, result, idEleve, idsEleves);
        noteService.calculPositionnementAutoByEleveByMatiere(compNotes, result,false);
        noteService.calculAndSetMoyenneClasseByPeriode(idsEleves,moyFinalesEleves,
                notesByDevoirByPeriodeClasse, result);

    }
}
