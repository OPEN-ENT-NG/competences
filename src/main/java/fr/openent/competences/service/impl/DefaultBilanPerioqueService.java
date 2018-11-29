package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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

    @Override
    public void getSuiviAcquis(final String idEtablissement, final Long idPeriode,
                               final String idEleve, final String idClasse ,
                               Handler<Either<String, JsonArray>> handler) {

        //On récupère les matières de l'élève avec les enseignants de chaque matière
        devoirService.getMatiereTeacherForOneEleveByPeriode(idEleve, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> response) {
                if( !response.isRight() ){
                    String error = response.left().getValue();
                    handler.handle(new Either.Left<>(error));

                }
                else {
                    JsonArray responseArray = response.right().getValue();
                    if( !(responseArray != null && responseArray.size() > 0 )){
                        //si pas d'évaluation on retourne le tableau vide
                        handler.handle(new Either.Right<>(new JsonArray()));
                    }
                    else {
                        //1-get idsMatière of a student with idsTeacher(owner)
                        Map<String,JsonObject> idsMatieresIdsTeachers = new HashMap<>();
                        JsonArray idsMatieres = new fr.wseduc.webutils.collections.JsonArray();
                        JsonArray idsTeachers = new fr.wseduc.webutils.collections.JsonArray();

                        for(int i = 0; i < responseArray.size(); i++) {
                            JsonObject responseObject = responseArray.getJsonObject(i);

                            if(!idsMatieresIdsTeachers.containsKey( responseObject.getString("id_matiere"))){
                                idsMatieres.add(responseObject.getString("id_matiere"));
                                if(!idsTeachers.contains(responseObject.getString("owner"))) {
                                    idsTeachers.add(responseObject.getString("owner"));
                                }
                                JsonArray teachers= new fr.wseduc.webutils.collections.JsonArray()
                                        .add( responseObject.getString("owner"));

                                idsMatieresIdsTeachers.put( responseObject.getString("id_matiere"),
                                        new JsonObject().put("teachers",teachers));
                            }
                            else {//on récupère le JsonObject de la cle idmatiere en cours
                                JsonArray teachers = idsMatieresIdsTeachers
                                        .get( responseObject.getString("id_matiere"))
                                        .getJsonArray("teachers");

                                teachers.add(responseObject.getString("owner"));
                                idsTeachers.add(responseObject.getString("owner"));
                            }
                        }
                        //2-get subject's Name

                        Utils.getLibelleMatiere( eb, idsMatieres, new Handler<Either<String, Map<String, JsonObject>>>() {
                            @Override
                            public void handle( Either<String, Map<String, JsonObject>> responseMatiere ) {

                                if(responseMatiere.isRight()){

                                    Map<String, JsonObject> idsMatLibelle = responseMatiere.right().getValue();

                                    //3-get user's lastName and firstName
                                    getUserLastNameAndFirstName(idEtablissement, idPeriode, idEleve,  idClasse,
                                            idsMatieresIdsTeachers, idsMatieres, idsTeachers,
                                            idsMatLibelle, eb, handler);
                                }
                                else {
                                    log.error("matiere.getMatieres : " + responseMatiere.left().getValue());
                                    String error = "failed get name 's subject " + responseMatiere.left().getValue();
                                    handler.handle(new Either.Left<>(error));
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private void getUserLastNameAndFirstName( final String idEtablissement, final Long idPeriode,
                                              final String idEleve, final String idClasse ,
                                              Map<String,JsonObject> idsMatieresIdsTeachers,
                                              JsonArray idsMatieres,JsonArray idsTeachers,
                                              Map<String, JsonObject> idsMatLibelle,
                                              EventBus eb,
                                              Handler<Either<String, JsonArray>> handler ){
        Utils.getLastNameFirstNameUser(eb, idsTeachers,
                new Handler<Either<String, Map<String, JsonObject>>>() {
                    @Override
                    public void handle( Either<String, Map<String, JsonObject>>
                                                responseTeacherInfo ) {
                        if(!responseTeacherInfo.isRight()) {
                            log.error("bilanPeriodiqueController ");
                            log.error("getUsers lastName and firstName 's teacher :"
                                    + responseTeacherInfo.left().getValue());
                            String error = "failed get firstName and lastName 's teachers "
                                    + responseTeacherInfo.left().getValue();
                            handler.handle(new Either.Left<>(error));
                        }
                        else {
                            Map<String, JsonObject> teachersInfos =
                                    responseTeacherInfo.right().getValue();
                            //on récupère les groups de la classe
                            Utils.getGroupesClasse(eb, new fr.wseduc.webutils.collections
                                            .JsonArray()
                                            .add(idClasse),
                                    new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(
                                                Either<String, JsonArray> responseQuerry) {
                                            //List qui contient la idClasse + tous les ids groupes
                                            // de la classe
                                            JsonArray idsGroups = new fr.wseduc.webutils.collections.JsonArray();

                                            if( !responseQuerry.isRight()) {
                                                String error = responseQuerry.left().getValue();
                                                log.error(error);
                                                handler.handle(new Either.Left<>(error));
                                            }
                                            else {

                                                JsonArray idClasseGroups = responseQuerry.right().getValue();
                                                if( !(idClasseGroups != null && !idClasseGroups.isEmpty() )) {
                                                    idsGroups.add(idClasse);
                                                }
                                                else{
                                                    idsGroups.add(idClasseGroups.getJsonObject(0)
                                                            .getString("id_classe"));
                                                    idsGroups.addAll(idClasseGroups.getJsonObject(0)
                                                            .getJsonArray("id_groupes"));
                                                }

                                                final JsonArray results = new fr.wseduc.webutils
                                                        .collections.JsonArray();
                                                final AtomicInteger compteurMatiere =
                                                        new AtomicInteger(idsMatieresIdsTeachers.size());

                                                //4-for each subject build the result
                                                //idMAtTeachersGroups = map<idMat,JsonObject
                                                // (JsonArray(Teachers),JsonArray(Groups)>
                                                for (Map.Entry<String, JsonObject>
                                                        idMAtTeachersGroups
                                                        : idsMatieresIdsTeachers.entrySet()) {
                                                    final JsonObject result = new JsonObject();
                                                    String idMatiere = idMAtTeachersGroups.getKey();
                                                    JsonObject teachersObject = idMAtTeachersGroups
                                                            .getValue();
                                                    JsonArray idsTeachers = teachersObject
                                                            .getJsonArray("teachers");
                                                    //5-add subject

                                                    result.put("id_matiere", idMatiere)
                                                            .put("libelleMatiere",
                                                                    idsMatLibelle.get(idMatiere).getString("name"));

                                                    //6-add teachers infos
                                                    if (idsTeachers != null) {
                                                        JsonArray teachers = new fr.wseduc.webutils
                                                                .collections.JsonArray();
                                                        for (Object idTeacher : idsTeachers) {
                                                            teachers.add(teachersInfos.get(idTeacher));
                                                        }
                                                        result.put("teachers", teachers);
                                                    }

                                                    //7-addElementProgramme
                                                    elementProgramme.getElementProgrammeClasses(
                                                            idPeriode, idMatiere, idsGroups,
                                                            new Handler<Either<String, JsonArray>>(){
                                                                @Override
                                                                public void handle(
                                                                        Either<String, JsonArray> responseEltProg) {
                                                                    if (responseEltProg.isRight()) {
                                                                        JsonArray eltsProg =
                                                                                responseEltProg
                                                                                        .right().getValue();
                                                                        //on récupère tous les élts du programme
                                                                        // de tous les classes et groupes de élèves
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
                                                                        //8-addAppréciation //moyenneFinale
                                                                        // positionnementModifié(pour une mat)
                                                                        addElemFinalEleve(idEleve, idMatiere,
                                                                                idPeriode, idClasse, idsGroups,
                                                                                idEtablissement, handler,
                                                                                result, results, compteurMatiere);


                                                                    } else {
                                                                        String error = (String)
                                                                                responseEltProg.left().getValue();
                                                                        log.error(error);
                                                                        handler.handle(new Either.Left<>(error));
                                                                        return;
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    private void addElemFinalEleve( final String idEleve, final String idMatiere,
                                    final Long idPeriode,
                                    final String idClasse,
                                    final JsonArray idsGroups,
                                    final String idEtablissement,
                                    Handler<Either<String, JsonArray>> handler,
                                    final JsonObject result,
                                    final JsonArray results,
                                    final AtomicInteger compteurMatiere){

        noteService.getAppreciationMoyFinalePositionnement(idEleve, idMatiere, null,
                new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> response) {

                if(response.isRight()){
                    JsonArray allAppMoyPosi = response.right().getValue();
                    JsonArray appreciations = new fr.wseduc.webutils.collections.JsonArray();
                    JsonArray moyennesFinales = new fr.wseduc.webutils.collections.JsonArray();
                    JsonArray positionnements = new fr.wseduc.webutils.collections.JsonArray();
                    if( allAppMoyPosi != null){

                        Map<Integer, JsonArray> mapIdPeriodeAppreciations = new HashMap<Integer,JsonArray>();
                        for(int i = 0; i < allAppMoyPosi.size(); i++){
                            JsonObject appMoyPosi = allAppMoyPosi.getJsonObject(i);
                            if(appMoyPosi.getString("appreciation_matiere_periode") != null ) {

                                if(!mapIdPeriodeAppreciations.containsKey(
                                        appMoyPosi.getInteger("id_periode_appreciation"))){

                                    mapIdPeriodeAppreciations.put(appMoyPosi.getInteger("id_periode_appreciation"),
                                            new fr.wseduc.webutils.collections.JsonArray().add(
                                                    new JsonObject().put("idClasse",appMoyPosi.getString("id_classe_appreciation"))
                                            .put("appreciation",appMoyPosi.getString("appreciation_matiere_periode"))));

                                }else {
                                    Integer idPeriode = appMoyPosi.getInteger("id_periode_appreciation");
                                    JsonArray appreciationsByIdPeriode = mapIdPeriodeAppreciations.get(idPeriode);
                                    JsonObject appResponse = new JsonObject().put("idClasse",appMoyPosi.getString("id_classe_appreciation"))
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
                                if(appMoyPosi.getString("id_classe_moyfinale").equals(idClasse)) {
                                    moyenne_finale.put("id_periode",
                                            appMoyPosi.getInteger("id_periode_moyenne_finale"))

                                            .put("moyenneFinale",
                                                    Double.valueOf(appMoyPosi.getString("moyenne_finale")));
                                    if(!moyennesFinales.contains(moyenne_finale)){
                                        moyennesFinales.add(moyenne_finale);
                                    }
                                }
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
                    addMoyEleveMoyClassePositionnementAuto(idEleve, idMatiere, idClasse, idsGroups,
                            idEtablissement, result, results, compteurMatiere, handler);
                } else {
                    String error = (String) response.left().getValue();
                    handler.handle(new Either.Left<>(error));
                }
            }
        });
    }

    private void addMoyEleveMoyClassePositionnementAuto(final String idEleve,
                                                        final String idMatiere,
                                                        final String idClasse,
                                                        final JsonArray idsGroups,
                                                        final String idEtablissement,
                                                        final JsonObject result,
                                                        final JsonArray results,
                                                        final AtomicInteger compteurMatiere,
                                                        Handler<Either<String, JsonArray>> handler
                                                        ) {

        noteService.getNoteElevePeriode(null, idEtablissement, idsGroups, idMatiere, null,
                new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> response) {

                if (response.isRight()) {
                    JsonArray idsEleves = new fr.wseduc.webutils.collections.JsonArray();
                    HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                            noteService.calculMoyennesEleveByPeriode(response.right().getValue(), result, idEleve,
                                    idsEleves);
                    //calculer le positionnement de l'élève
                    noteService.getCompetencesNotesReleve(idEtablissement, null, null, idMatiere, null,
                            idEleve, null, false, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> response) {

                            if (response.isRight()) {

                                JsonArray listNotes = response.right().getValue();
                                noteService.calculPositionnementAutoByEleveByMatiere(listNotes, result);

                                if (idsEleves.size() != 0) {
                                    //calculer les moyennes de la Classe pour chaque
                                    noteService.getColonneReleve(idsEleves, null, idMatiere, idClasse,
                                            "moyenne", new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> response) {
                                            if (response.isRight()) {
                                                JsonArray moyFinalesEleves = response.right().getValue();
                                                noteService.calculAndSetMoyenneClasseByPeriode(moyFinalesEleves,
                                                        notesByDevoirByPeriodeClasse, result);
                                                compteurMatiere.decrementAndGet();
                                                results.add(result);
                                                if (compteurMatiere.intValue() == 0) {
                                                    String [] sortedField = new  String[1];
                                                    sortedField[0] = "libelleMatiere";
                                                    handler.handle(new Either.Right<>(
                                                            new DefaultUtilsService().sortArray(results,
                                                                    sortedField)));
                                                }

                                            } else {
                                                String error = response.left().getValue();
                                                log.error(error);
                                                handler.handle(new Either.Left<>(error));

                                            }
                                        }
                                    });

                                } else {
                                    result.put("moyennesClasse", new fr.wseduc.webutils.collections.JsonArray());
                                    compteurMatiere.decrementAndGet();
                                    results.add(result);
                                    if (compteurMatiere.intValue() == 0) {
                                        String [] sortedField = new  String[1];
                                        sortedField[0] = "libelleMatiere";
                                        handler.handle(new Either.Right<>(
                                                new DefaultUtilsService().sortArray(results,
                                                        sortedField)));
                                    }
                                }
                            } else {
                                String error = response.left().getValue();
                                log.error(error);
                                handler.handle(new Either.Left<>(error));
                            }
                        }
                    });

                } else {
                    String error = response.left().getValue();
                    log.error(error);
                    handler.handle(new Either.Left<>(error));
                }

            }
        });
    }
}
