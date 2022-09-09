package fr.openent.competences.service;

import fr.openent.competences.model.MultiTeaching;
import fr.openent.competences.model.Structure;
import fr.openent.competences.model.Student;
import fr.openent.competences.model.StudentEvenement;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ExportBulletinService {
 /**
  * Récupère les retards et absences d'un élève
  * @param student Sttudent to handle
  * @param promise promise renvoyant la liste des evenements
  */
 void getEvenements(Student student,
                    Promise<List<StudentEvenement>> promise);

 /**
  * Service de récupération des donnéees nécessaires pour générer un bulletin
  * @param answered Atomic booléen seté lorsqu'on lance l'export
  * @param idEleve idEleve
  * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
  * @param idEleves
  * @param idPeriode idPeriode
  * @param params paramètres de la requête
  * @param classe Object contenant les information sur la classe
  * @param host de la request
  * @param acceptLanguage de la request
  * @param vertx
  * @param finalHandler handler servant à la synchronisation des services
  */
 void getExportBulletin(final AtomicBoolean answered, String idEleve,
                        Map<String, JsonObject> elevesMap, Student student, JsonArray idEleves, Long idPeriode, JsonObject params,
                        final JsonObject classe, String host, String acceptLanguage,
                        Vertx vertx, Handler<Either<String, JsonObject>> finalHandler);


 /**
  * Rajoute tous les libelles i18n nécessaires pour la génération des bulletins
  * @param idEleve idEleve
  * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
  * @param params paramètres de la requête
  * @param finalHandler handler servant à la synchronisation des services
  */
 void putLibelleForExport(String idEleve, Map<String , JsonObject> elevesMap, JsonObject params,
                          Handler<Either<String, JsonObject>> finalHandler);

 /**
  * Récupération des responsables légaux d'un élève
  * @param student élève à traiter
  * @param promise handler servant à la synchronisation des services
  */
 void getResponsables( Student student, Promise promise);

 /**
  * Récupère le suivi des acquis d'un élève
  * @param student student export
  * @param idEleves
  * @param params  export parameters
  * @param promise promise called at the end of the function
  */
 void getSuiviAcquis(Student student, JsonArray idEleves, JsonObject classe,
                     JsonObject params, Promise<JsonObject> promise);

 /**
  *  - Ordonne les élèves par classe et  par nom
  *  - Permet de gérer autant d'exports qu'il y a de responsables légaux
  * @param mapEleves Map contenant les champs nécessaires à l'export du Bulletin par élève
  * @return
  */
 JsonArray sortResultByClasseNameAndNameForBulletin(Map<String, JsonObject> mapEleves);


 /**
  * Récupère les EPI, AP et Parcours d'un élève
  * @param student Student to handle
  * @param promise handler servant à la synchronisation des services
  */
 void getProjets (Student student,
                  Promise<Object> promise);

 /**
  * Récupère la synthèse du bilan périodique d'un élève
  * @param student student a handle
  * @param promise promise recevant le resultat de la fonction
  */
 void getSyntheseBilanPeriodique (Student student,
                                  Boolean isBulletinLycee, Promise<JsonObject> promise);

 /**
  * Récupère le libelle de l'établissement de l'élève
  * @param idEleve idEleve
  * @param elevesObject contient le json object de l'élève idEleve
  * @param finalHandler handler servant à la synchronisation des services
  */
 void getStructure( String idEleve, JsonObject elevesObject, Handler<Either<String, JsonObject>> finalHandler);

 /**
  * Récupère le(s) professeur(s) princip(al/aux) de la classe de l'élève
  * @param idEleve identifiant de l'élève
  * @param idClasse identifiant de la classe
  * @param eleveObject contient le json object de l'élève idEleve
  * @param finalHandler handler servant à la synchronisation des services
  */
 void getHeadTeachers( String idEleve, String idClasse, JsonObject eleveObject,
                       Handler<Either<String, JsonObject>> finalHandler);

 /**
  * Récupère le cycle de la classe de l'élève
  * @param student student
  * @param promise handler servant à la synchronisation des services
  */
 void getCycle (Student student,
                Promise<JsonObject> promise);

 /**
  * récupère le libelle de la periode idPeriode est passé en paramètre
  * @param idEleve idEleve
  * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
  * @param idPeriode idperiode
  * @param host de la reqquest
  * @param acceptLanguage de la request
  * @param finalHandler handler servant à la synchronisation des services
  */
 void getLibellePeriode(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                        String host, String acceptLanguage, Handler<Either<String, JsonObject>> finalHandler);


 /**
  * Calcul de l'année scolaire en fonction des périodes de la classe
  * le résultat est
  * @param idEleve idEleve
  * @param idClasse id de la classe
  * @param eleve contient le jsonObject des informations concernant l'élève référencé par l'idEleve
  * @param finalHandler handler servant à la synchronisation des services
  */
 void getAnneeScolaire(String idEleve, String idClasse, JsonObject eleve,
                       Handler<Either<String, JsonObject>> finalHandler) ;

 /**
  *  @param student  student
  * @param promise  promise with the result of the function
  */
 void getAvisConseil(Student student,
                     Promise<JsonObject> promise, String beforeAvisConseil);

 /**
  *  @param student  student
  * @param promise  promise called at the end function
  */
 void getAvisOrientation(Student student,
                         Promise<JsonObject> promise, String beforeAvisOrientation);


 /**
  * Récupération de tous les enseignements
  * @param student Student to handle
  * @param promise handler servant à la synchronisation des services.
  */
 void getArbreDomaines(Student student, Promise<Object> promise);


 /**
  * Contruction du handler de synchronisation de tous les services
  * @param request la requête
  * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
  * @param vertx nécessaire pour lancer la génération du pdf
  * @param config la config du module
  * @param elevesFuture Future le nombre d'élèves pour l'export
  * @param params paramètres de la requête
  * @return
  */
 Handler<Either<String, JsonObject>>  getFinalBulletinHandler(final HttpServerRequest request,
                                                              Map<String, JsonObject> elevesMap,
                                                              Vertx vertx, JsonObject config,
                                                              Future<JsonArray> elevesFuture,
                                                              JsonObject params);

 /**
  * Construit le libelle correspondant à la date de naissance
  * @param eleve
  */
 void setBirthDate(JsonObject eleve);

 /**
  * Rajoute l'id de l'image correspondant au graph par domaine
  * @param eleve
  * @param images
  */
 void setIdGraphPerDomaine(JsonObject eleve, JsonObject images);

 /**
  * Construit le libelle du niveau
  * @param eleve
  */
 void setLevel(JsonObject eleve);

 /**
  *  @param answered
  * @param eleves
  * @param elevesMap
  * @param idPeriode
  * @param params
  * @param classe
  * @param showBilanPerDomaines
  * @param host de la request
  * @param acceptLanguage de la request
  * @param finalHandler
  * @param vertx
  */
 void buildDataForStudent(final AtomicBoolean answered, JsonArray eleves,
                          Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                          final JsonObject classe, Boolean showBilanPerDomaines,
                          String host, String acceptLanguage, Handler<Either<String, JsonObject>> finalHandler, Vertx vertx);

 /**
  *  @param idEtablissement
  * @param idClasse
  * @param idStudents
  * @param idPeriode
  * @param params
  * @param elevesFuture
  * @param elevesMap
  * @param answered
  * @param host de la request
  * @param acceptLanguage de la request
  * @param finalHandler
  * @param future
  * @param vertx
  */
 void runExportBulletin(String idEtablissement, String idClasse, JsonArray idStudents, Long idPeriode,
                        JsonObject params, Future<JsonArray> elevesFuture, final Map<String, JsonObject> elevesMap,
                        final AtomicBoolean answered, String host, String acceptLanguage,
                        final Handler<Either<String, JsonObject>> finalHandler, Future<JsonObject> future, Vertx vertx);



 void savePdfInStorage(JsonObject eleve, Buffer file, Handler<Either<String, JsonObject>> handler);

 void runSavePdf(JsonObject bulletinEleve, JsonObject bulletin, Vertx vertx, JsonObject config,
                 Handler<Either<String, String>> bulletinHandlerWork);

 void generateAndSavePdf(HttpServerRequest request, JsonObject resultFinal, String templateName,
                         String prefixPdfName, JsonObject eleve, Vertx vertx, JsonObject config,
                         Handler<Either<String, String>> finalHandler);

 void generateImagesFromPathForBulletin (JsonObject eleve, Vertx vertx, Handler<Either<String, JsonObject>> handler);

 void checkBulletinsExist(JsonArray students, Integer idPeriode, String idStructure, Handler<Either<String, Boolean>> handler);

 void setMultiTeaching(Structure structure, JsonArray multiTeachinJsonArray, List<MultiTeaching> multiTeachings, String idClasse);

 /**
  * Récupère le libelle de l'établissement de l'élève
  * @param idStructure idStructure
  * @param promise contient la promise visant à récupérer ces données
  */
 void getStructure(String idStructure, Promise<Structure> promise);
}
