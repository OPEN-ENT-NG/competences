package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ExportBulletinService {
    /**
     * Récupère les retards et absences d'un élève
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idperiode
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getEvenements(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                       Handler<Either<String, JsonObject>> finalHandler );

    /**
     * Service de récupération des donnéees nécessaires pour générer un bulletin
     * @param request requête
     * @param answered Atomic booléen seté lorsqu'on lance l'export
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idPeriode
     * @param classe Object contenant les information sur la classe
     * @param params paramètres de la requête
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getExportBulletin(final HttpServerRequest request,
                           final AtomicBoolean answered, String idEleve,
                           Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                           final JsonObject classe,
                           Handler<Either<String, JsonObject>> finalHandler);

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
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getResponsables( String idEleve, Map<String,JsonObject> elevesMap,
                          Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le suivi des acquis d'un élève
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idPeriode
     * @param classe Object contenant les information sur la classe
     * @param getProgrammeElement  Booleen pour savoir si on affiche les élement du programme
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getSuiviAcquis(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                        final JsonObject classe, boolean getProgrammeElement,
                        Handler<Either<String, JsonObject>> finalHandler );

    /**
     *  - Ordonne les élèves par classe et  par nom
     *  - Permet de gérer autant d'exports qu'il y a de responsables légaux
     * @param mapEleves Map contenant les champs nécessaires à l'export du Bulletin par élève
     * @return
     */
    JsonArray sortResultByClasseNameAndNameForBulletin(Map<String, JsonObject> mapEleves);


    /**
     * Récupère les EPI, AP et Parcours d'un élève
     * @param idEleve ideleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idperiode
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getProjets ( String idEleve,  Map<String,JsonObject> elevesMap,Long idPeriode,
                      Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère la synthèse du bilan périodique d'un élève
     * @param idEleve IdEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idTypePeriode IdPeriode
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getSyntheseBilanPeriodique ( String idEleve,  Map<String,JsonObject> elevesMap, Long idTypePeriode,
                                      Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le libelle de l'établissement de l'élève
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getStructure( String idEleve, Map<String,JsonObject> elevesMap,
                              Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le(s) professeur(s) princip(al/aux) de la classe de l'élève
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getHeadTeachers( String idEleve, Map<String,JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le cycle de la classe de l'élève
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idPeriode
     * @param finalHandler handler servant à la synchronisation des services
     */
     void getCycle ( String idEleve,  Map<String,JsonObject> elevesMap,Long idPeriode,
                           Handler<Either<String, JsonObject>> finalHandler);

    /**
     * récupère le libelle de la periode idPeriode est passé en paramètre
     * @param request request
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idperiode
     * @param finalHandler handler servant à la synchronisation des services
     */
     void getLibellePeriode(final HttpServerRequest request, String idEleve,
                                  Map<String, JsonObject> elevesMap, Long idPeriode,
                                  Handler<Either<String, JsonObject>> finalHandler);


    /**
     * Calcul de l'année scolaire en fonction des périodes de la classe
     * le résultat est
     * @param idEleve idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler handler servant à la synchronisation des services
     */
     void getAnneeScolaire(String idEleve,Map<String, JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler) ;

    /**
     *
     * @param idEleve  idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idType of the periode
     * @param finalHandler handler servant à la synchronisation des services
     */
    void getAvisConseil(String idEleve, Map<String, JsonObject> elevesMap, Long idPeriode,
                        Handler<Either<String, JsonObject>> finalHandler);


    /**
     * Récupération de tous les enseignements
     * @param idEleve l'identifiant de l'élève.
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler handler servant à la synchronisation des services.
     */
    void getArbreDomaines(String idEleve, Map<String, JsonObject> elevesMap,
                          Handler<Either<String, JsonObject>> finalHandler);


    /**
     * Contruction du handler de synchronisation de tous les services
     * @param request la requête
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param vertx nécessaire pour lancer la génération du pdf
     * @param config la config du module
     * @param nbrEleves le nombre d'élèves pour l'export
     * @param answered Atomic booléen seté à true si tous les services ont répondu
     * @param params paramètres de la requête
     * @return
     */
    Handler<Either<String, JsonObject>>  getFinalBulletinHandler(final HttpServerRequest request,
                                                                        Map<String, JsonObject> elevesMap,
                                                                        Vertx vertx, JsonObject config,
                                                                        final int nbrEleves,
                                                                        final AtomicBoolean answered,
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
     *
     * @param request
     * @param answered
     * @param eleves
     * @param elevesMap
     * @param idPeriode
     * @param params
     * @param classe
     * @param showBilanPerDomaines
     * @param finalHandler
     */
    void buildDataForStudent(final HttpServerRequest request, final AtomicBoolean answered, JsonArray eleves,
                             Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                             final JsonObject classe, Boolean showBilanPerDomaines,
                             Handler<Either<String, JsonObject>> finalHandler);
}
