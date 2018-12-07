package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ExportBulletinService {
    /**
     * Récupère les retards et absences d'un élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode
     * @param finalHandler
     */
    void getEvenements(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                       Handler<Either<String, JsonObject>> finalHandler );
    /**
     * Service de récupération des donnéees nécessaires pour générer un bulletin
     * @param request
     * @param answered
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode
     * @param classe
     * @param params
     * @param finalHandler
     */
    void getExportBulletin(final HttpServerRequest request,
                           final AtomicBoolean answered, String idEleve,
                           Map<String, JsonObject> elevesMap, Long idPeriode, JsonObject params,
                           final JsonObject classe,
                           Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Rajoute tous les libelles i18n nécessaires pour la génération des bulletins
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param params
     * @param finalHandler
     */
    void putLibelleForExport(String idEleve, Map<String , JsonObject> elevesMap, JsonObject params,
                             Handler<Either<String, JsonObject>> finalHandler);
    /**
     * Récupération des responsables légaux d'un élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler
     */
    void getResponsables( String idEleve, Map<String,JsonObject> elevesMap,
                          Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le suivi des acquis d'un élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode
     * @param classe
     * @param finalHandler
     */
    void getSuiviAcquis(String idEleve,Map<String, JsonObject> elevesMap, Long idPeriode,
                        final JsonObject classe,
                        Handler<Either<String, JsonObject>> finalHandler );
    /**
     *  - Ordonne les élèves par classe et  par nom
     *  - Permet de gérer autant d'exports qu'il y a de responsables légaux
     * @param mapEleves
     * @return
     */
    JsonArray sortResultByClasseNameAndNameForBulletin(Map<String, JsonObject> mapEleves);


    /**
     * Récupère les EPI, AP et Parcours d'un élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode
     * @param finalHandler
     */
    void getProjets ( String idEleve,  Map<String,JsonObject> elevesMap,Long idPeriode,
                      Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère la synthèse du bilan périodique d'un élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idTypePeriode
     * @param finalHandler
     */
    void getSyntheseBilanPeriodique ( String idEleve,  Map<String,JsonObject> elevesMap, Long idTypePeriode,
                                      Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le libelle de l'établissement de l'élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler
     */
    void getStructure( String idEleve, Map<String,JsonObject> elevesMap,
                              Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le(s) professeur(s) princip(al/aux) de la classe de l'élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler
     */
    void getHeadTeachers( String idEleve, Map<String,JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler);

    /**
     * Récupère le cycle de la classe de l'élève
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode
     * @param finalHandler
     */
     void getCycle ( String idEleve,  Map<String,JsonObject> elevesMap,Long idPeriode,
                           Handler<Either<String, JsonObject>> finalHandler);

    /**
     * récupère le libelle de la periode idPeriode est passé en paramètre
     * @param request
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode
     * @param finalHandler
     */
     void getLibellePeriode(final HttpServerRequest request, String idEleve,
                                  Map<String, JsonObject> elevesMap, Long idPeriode,
                                  Handler<Either<String, JsonObject>> finalHandler);


    /**
     * Calcul de l'année scolaire en fonction des périodes de la classe
     * le résultat est
     * @param idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param finalHandler
     */
     void getAnneeScolaire(String idEleve,Map<String, JsonObject> elevesMap,
                                 Handler<Either<String, JsonObject>> finalHandler) ;

    /**
     *
     * @param idEleve  idEleve
     * @param elevesMap contient à minima map <idEleve, JsonObject{idClasse, idEtablissement}>
     * @param idPeriode idType of the periode
     * @param finalHandler response
     */
    void getAvisConseil(String idEleve, Map<String, JsonObject> elevesMap, Long idPeriode,
                        Handler<Either<String, JsonObject>> finalHandler);
}
