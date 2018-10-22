package fr.openent.competences.controllers;

import com.mongodb.util.JSON;
import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.DevoirService;
import fr.openent.competences.service.ElementProgramme;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultDevoirService;
import fr.openent.competences.service.impl.DefaultElementProgramme;
import fr.openent.competences.service.impl.DefaultNoteService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.openent.competences.utils.UtilsConvert;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;


import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BilanPeriodiqueController extends ControllerHelper{

    private final NoteService noteService;
    private final UtilsService utilsService;
    private final DevoirService devoirService;
    private final ElementProgramme elementProgramme;

    public BilanPeriodiqueController (EventBus eb){
        this.eb = eb;
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE,eb);
        utilsService = new DefaultUtilsService();
        devoirService = new DefaultDevoirService(eb);
        elementProgramme = new DefaultElementProgramme() ;
    }

    @Get("/bilan/periodique/eleve/:idEleve")
    @ApiDoc("renvoit tous les éléments pour le bilan périodique d'un élève")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getSuiviDesAcquisEleve(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos userInfos) {
                final String idEtablissement = request.params().get("idEtablissement");
                final String idPeriodeString = request.params().get("idPeriode");
                final Long idPeriode = (idPeriodeString != null)? Long.parseLong(idPeriodeString): null;
                final String idEleve = request.params().get("idEleve");
                final String idClasse = request.params().get("idClasse");


                //On récupère les matières de l'élève avec les enseignants de chaque matière
                devoirService.getMatiereTeacherForOneEleveByPeriode(idEleve, new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> response) {
                        if( response.isRight() ){
                            JsonArray responseArray = response.right().getValue();
                            final JsonArray results = new fr.wseduc.webutils.collections.JsonArray();
                            if( responseArray != null && responseArray.size() > 0 ){
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

                                        idsMatieresIdsTeachers.put( responseObject.getString("id_matiere"),new JsonObject().put("teachers",teachers));
                                    }else{//on récupère le JsonObject de la cle idmatiere en cours
                                        JsonArray teachers = idsMatieresIdsTeachers
                                                .get( responseObject.getString("id_matiere")).getJsonArray("teachers");

                                        teachers.add(responseObject.getString("owner"));
                                        idsTeachers.add(responseObject.getString("owner"));
                                    }
                                }
                                //2-get subject's Name
                                Utils.getLibelleMatiere( eb, idsMatieres, new Handler<Either<String, Map<String, String>>>() {
                                    @Override
                                    public void handle( Either<String, Map<String, String>> responseMatiere ) {
                                        if(responseMatiere.isRight()){
                                            Map<String, String> idsMatLibelle = responseMatiere.right().getValue();

                                            //3-get user's lastName and firstName
                                            Utils.getLastNameFirstNameUser(eb, idsTeachers, new Handler<Either<String, Map<String, JsonObject>>>() {
                                                @Override
                                                public void handle( Either<String, Map<String, JsonObject>> responseTeacherInfo ) {
                                                    if(responseTeacherInfo.isRight()){
                                                        Map<String, JsonObject> teachersInfos = responseTeacherInfo.right().getValue();
                                                        //on récupère les groups de la classe
                                                        Utils.getGroupesClasse(eb, new fr.wseduc.webutils.collections.JsonArray().add(idClasse), new Handler<Either<String, JsonArray>>() {
                                                            @Override
                                                            public void handle(Either<String, JsonArray> responseQuerry) {
                                                                //List qui contient idClasse + tous les ids groupes de la classe s'ils existent
                                                                JsonArray idsGroups = new fr.wseduc.webutils.collections.JsonArray();
                                                                if( responseQuerry.isRight()) {
                                                                    JsonArray idClasseGroups = responseQuerry.right().getValue();
                                                                    if(idClasseGroups != null ){
                                                                        idsGroups.add(idClasseGroups.getJsonObject(0).getString("id_classe"));
                                                                        JsonArray idsGroupOfClasse = idClasseGroups.getJsonObject(0).getJsonArray("id_groupes");
                                                                        if(idsGroupOfClasse != null && !idsGroupOfClasse.isEmpty()) {
                                                                            idsGroups.addAll(idsGroupOfClasse);
                                                                        }
                                                                    }

                                                                    final AtomicInteger compteurMatiere = new AtomicInteger(idsMatieresIdsTeachers.size());
                                                                    //4-for each subject build the result
                                                                    //idMAtTeachersGroups = map<idMat,JsonObject(JsonArray(Teachers),JsonArray(Groups)>
                                                                    for (Map.Entry<String, JsonObject> idMAtTeachersGroups : idsMatieresIdsTeachers.entrySet()) {
                                                                        final JsonObject result = new JsonObject();
                                                                        String idMatiere = idMAtTeachersGroups.getKey();
                                                                        JsonObject teachersObject = idMAtTeachersGroups.getValue();
                                                                        JsonArray idsTeachers = teachersObject.getJsonArray("teachers");
                                                                        //5-add subject
                                                                        result.put("id_matiere", idMatiere).put("libelleMatiere", idsMatLibelle.get(idMatiere));
                                                                        //6-add teachers infos
                                                                        if (idsTeachers != null) {
                                                                            JsonArray teachers = new fr.wseduc.webutils.collections.JsonArray();
                                                                            for (Object idTeacher : idsTeachers) {
                                                                                teachers.add(teachersInfos.get(idTeacher));
                                                                            }
                                                                            result.put("teachers", teachers);
                                                                        }

                                                                        //7-addElementProgramme
                                                                        elementProgramme.getElementProgrammeClasses(idPeriode, idMatiere, idsGroups, new Handler<Either<String, JsonArray>>() {
                                                                            @Override
                                                                            public void handle(Either<String, JsonArray> responseEltProg) {
                                                                                if (responseEltProg.isRight()) {
                                                                                    JsonArray eltsProg = responseEltProg.right().getValue();
                                                                                    //on récupère tous les élts du programme de tous les classes et groupes de élèves
                                                                                    String elementsProg = new String();
                                                                                    if (eltsProg != null && eltsProg.size() > 0) {
                                                                                        for (int i = 0; i < eltsProg.size(); i++) {
                                                                                            if (elementsProg.isEmpty()) {
                                                                                                elementsProg = eltsProg.getJsonObject(i).getString("texte");
                                                                                            } else {
                                                                                                elementsProg += " " + eltsProg.getJsonObject(i).getString("texte");
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    result.put("elementsProgramme", elementsProg);
                                                                                    //8-addAppréciation //moyenneFinale//positionnementModifié(pour une mat)
                                                                                    addMoyenneFinaleAppreciationPositionnementEleve(idEleve, idMatiere, idPeriode,
                                                                                            idClasse, idsGroups, idEtablissement, request, result, results, compteurMatiere);


                                                                                } else {
                                                                                    JsonObject error = (new JsonObject()).put("error",
                                                                                            (String) responseEltProg.left().getValue());
                                                                                    Renders.renderJson(request, error, 400);
                                                                                }
                                                                            }
                                                                        });
                                                                    }
                                                                }
                                                            }
                                                        });
                                                    }else{
                                                        log.error("bilanPeriodiqueController ");
                                                        log.error("getUsers lastName and firstName 's teacher :" + responseTeacherInfo.left().getValue());
                                                        JsonObject error = new JsonObject().put("error", "failed get firstName and lastName 's teachers " + responseTeacherInfo.left().getValue());
                                                        Renders.renderJson( request, error, 400);
                                                    }
                                                }
                                            });
                                        }else{
                                            log.error("matiere.getMatieres : " + responseMatiere.left().getValue());
                                            JsonObject error = new JsonObject().put("error", "failed get name 's subject " + responseMatiere.left().getValue());
                                            Renders.renderJson( request, error, 400 );
                                        }
                                    }
                                });
                            }else{//si pas d'évaluation on retourne le tableau vide
                                Renders.renderJson(request, results);
                            }
                        }else{
                            JsonObject error = ( new JsonObject() ).put("error",
                                   (String) response.left().getValue());
                            Renders.renderJson( request, error, 400 );
                        }
                    }
                });
            }
        });
    }

    private void addMoyenneFinaleAppreciationPositionnementEleve( final String idEleve,
                                                                  final String idMatiere,
                                                                  final Long idPeriode,
                                                                  final String idClasse,
                                                                  final JsonArray idsGroups,
                                                                  final String idEtablissement,
                                                                  final HttpServerRequest request,
                                                                  final JsonObject result,
                                                                  final JsonArray results,
                                                                  final AtomicInteger compteurMatiere){

        noteService.getAppreciationMoyFinalePositionnement(idEleve, idMatiere, idPeriode, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> response) {

                if(response.isRight()){
                    JsonArray allAppMoyPosi = response.right().getValue();
                    String appreciation = new String();

                    if( allAppMoyPosi != null ){

                        for(int i = 0; i < allAppMoyPosi.size(); i++){
                            JsonObject appMoyPosi = allAppMoyPosi.getJsonObject(i);

                            if(appMoyPosi.getString("appreciation_matiere_periode") != null ) {
                                if(appreciation.isEmpty()){
                                    appreciation = appMoyPosi.getString("appreciation_matiere_periode");
                                }else{
                                    appreciation += " " + appMoyPosi.getString("appreciation_matiere_periode");
                                }
                            }
                            //on récupère la moyenne finale de l'élève pour sa classe principale = idClasse passé en paramètre

                            if( appMoyPosi.getDouble("moyenne_finale") != null) {
                                if(appMoyPosi.getString("id_classe_moyfinale").equals(idClasse)) {
                                    result.put("moyenne_finale",appMoyPosi.getDouble("moyenne_finale" ));
                                }
                            }
                            //Pour le positionnement on ne peut en avoir qu'un par matière
                            //le positionnement n'est pas enregistré par classe
                            if(appMoyPosi.getDouble("positionnement_final") != null){
                                result.put("positionnement_final", appMoyPosi.getDouble("positionnement_final"));

                            }
                        }
                    }
                    result.put("appreciation",appreciation);
                    addMoyEleveMoyClassePositionnementAuto(idEleve, idMatiere, idClasse, idsGroups,
                            idEtablissement, request, result, results, compteurMatiere);
                }else{
                    JsonObject error = ( new JsonObject() ).put( "error", (String) response.left().getValue());
                    Renders.renderJson( request, error, 400 );
                    log.error(error.getString("error"));
                }
            }
        });

    }

    private void addMoyEleveMoyClassePositionnementAuto(final String idEleve,
                                                        final String idMatiere,
                                                        final String idClasse,
                                                        final JsonArray idsGroups,
                                                        final String idEtablissement,
                                                        final HttpServerRequest request,
                                                        final JsonObject result,
                                                        final JsonArray results,
                                                        final AtomicInteger compteurMatiere ){

        noteService.getNoteElevePeriode(null, idEtablissement, idsGroups, idMatiere, null, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> response) {

                if(response.isRight()) {
                    JsonArray idsEleves = new fr.wseduc.webutils.collections.JsonArray();
                    HashMap<Long, HashMap<Long, ArrayList<NoteDevoir>>> notesByDevoirByPeriodeClasse =
                            noteService.calculMoyennesEleveByPeriode(response.right().getValue(), result, idEleve, idsEleves);
                    //calculer le positionnement de l'élève
                    noteService.getCompetencesNotesReleve(idEtablissement, null, idMatiere, null, idEleve, null, false, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> response) {

                            if(response.isRight()) {

                                JsonArray listNotes = response.right().getValue();
                                noteService.calculPositionnementAutoByEleveByMatiere(listNotes,result);

                                if (idsEleves.size() != 0) {
                                    //calculer les moyennes de la Classe pour chaque
                                    noteService.getColonneReleve(idsEleves, null, idMatiere, idClasse, "moyenne", new Handler<Either<String, JsonArray>>() {
                                        @Override
                                        public void handle(Either<String, JsonArray> response) {
                                            if (response.isRight()) {
                                                JsonArray moyFinalesEleves = response.right().getValue();
                                                noteService.calculAndSetMoyenneClasseByPeriode(moyFinalesEleves, notesByDevoirByPeriodeClasse, result);
                                                compteurMatiere.decrementAndGet();
                                                results.add(result);
                                                if(compteurMatiere.intValue() == 0){
                                                    Renders.renderJson(request, results);
                                                }

                                            } else {
                                                JsonObject error = new JsonObject().put("error", response.left().getValue());
                                                Renders.renderJson(request, error, 400);
                                                log.error(error.getString("error"));
                                            }
                                        }
                                    });

                                } else {
                                    result.put("moyennesClasse", new fr.wseduc.webutils.collections.JsonArray());
                                    compteurMatiere.decrementAndGet();
                                    results.add(result);
                                    if(compteurMatiere.intValue() == 0){
                                        Renders.renderJson(request, results);
                                    }
                                }
                            }else{
                                JsonObject error = new JsonObject().put("error", response.left().getValue());
                                Renders.renderJson(request, error, 400);
                                log.error(error.getString("error"));
                            }
                        }
                    });

                }else {
                    JsonObject error = new JsonObject().put("error", response.left().getValue());
                    Renders.renderJson( request, error, 400);
                    log.error(error.getString("error"));
                }

            }
        });
    }

}
