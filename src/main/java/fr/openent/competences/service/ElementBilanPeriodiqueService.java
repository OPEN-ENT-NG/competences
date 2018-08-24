package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ElementBilanPeriodiqueService {

    /**
     * Enregistrement d'une nouvelle thématique EPI ou parcours.
     * @param thematique nouvelle thématique à enregistrer
     * @param handler Handler de retour
     */
    public void insertThematiqueBilanPeriodique (JsonObject thematique,
                                                 Handler<Either<String, JsonObject>> handler);

    /**
     * Enregistremet d'un nouvel élément EPI, AP ou parcours.
     * @param element nouvel élément à enregistrer
     * @param handler Handler de retour
     */
    public void insertElementBilanPeriodique (JsonObject element, Handler<Either<String, JsonObject>> handler);

    /**
     * Retourne l'ensemble des thématiques d'EPI ou de parcours.
     * @param typeElement type de l'élément
     * @param handler Handler de retour
     */
    public void getThematiqueBilanPeriodique (Long typeElement, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les éléments correspondants à la thématique passée en paramètre..
     * @param idThematique
     * @param handler Handler de retour
     */
    public void getElementsOnThematique (String idThematique, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les élèments du bilan périodique sur la classe et/ou l'enseignant et/ou l'établissement.
     * @param idEnseignant enseignant connecté
     * @param idClasse id classe
     * @param idEtablissement id établissement
     * @param handler Handler de retour
     */
    public void getElementBilanPeriodique (String idEnseignant, String idClasse, String idEtablissement,
                                           Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les élèments du bilan périodique sur la classe et/ou l'enseignant et/ou l'établissement.
     * @param idElements id des éléments
     * @param handler Handler de retour
     */
    public void getEnseignantsElementsBilanPeriodique (List<String> idElements, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les groupes de l'élèment du bilan périodique dont l'id est passé en paramètre.
     * @param idElement id élèment bilan périodique
     * @param handler Handler de retour
     */
    public void getGroupesElementBilanPeriodique (String idElement, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les groupes de l'élèment du bilan périodique dont l'id est passé en paramètre.
     * @param idElement id élèment bilan périodique
     * @param handler Handler de retour
     */
    public void getIntervenantMatiereElementBilanPeriodique (String idElement,
                                                             Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les appéciations de classe dont les id sont passés en paramètre.
     * @param idClasse
     * @param idElements id des élèments du bilan périodique
     * @param handler Handler de retour
     */
    public void getApprecBilanPerClasse (String idClasse, String idPeriode, List<String> idElements, Handler<Either<String, JsonArray>> handler);

    /**
     * Retourne les appéciations d'élèves les id sont passés en paramètre.
     * @param idClasse
     * @param idElements id des élèments du bilan périodique
     * @param handler Handler de retour
     */
    public void getApprecBilanPerEleve (String idClasse, String idPeriode, List<String> idElements, Handler<Either<String, JsonArray>> handler);

    /**
     * Enregistrement d'une appreciation pour un élève.
     * @param idEleve id élève
     * @param idEltBilanPeriodique id élément bilan périodique
     * @param idPeriode id période
     * @param commentaire appréciation laissée par le professeur
     * @param handler Handler de retour
     */
    public void insertOrUpdateAppreciationElement (String idEleve, Long idPeriode, Long idEltBilanPeriodique,
                                           String commentaire, JsonArray groupes, Handler<Either<String, JsonObject>> handler);

    /**
     * Mise à jour d'un élément EPI, AP ou parcours.
     * @param idElement id de l'élément à mettre à jour
     * @param element données à mettre à jour
     * @param handler Handler de retour
     */
    public void updateElementBilanPeriodique (Long idElement, JsonObject element, Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression d'un élément EPI, AP ou parcours.
     * @param idEltBilanPeriodique id des éléments à supprimer
     * @param handler Handler de retour
     */
    public void deleteElementBilanPeriodique (List<String> idEltBilanPeriodique, Handler<Either<String, JsonArray>> handler);
    /**
     * Suppression d'une thématique.
     * @param idThematique id des éléments à supprimer
     * @param handler Handler de retour
     */
    public void deleteThematique (String idThematique, Handler<Either<String, JsonArray>> handler);

    /**
     * Mise à jour d'une appreciation pour un élève ou une classe.
     * @param idThematique id de la thématique à mettre à jour
     * @param thematique nouvelles données de la thématique
     * @param handler Handler de retour
     */
    public void updateThematique (String idThematique, JsonObject thematique,
                                                   Handler<Either<String, JsonObject>> handler);

    /**
     * Suppression des appréciations d'élève d'un élément EPI, AP ou parcours.
     * @param idEltBilanPeriodique id de l'élément
     * @param handler Handler de retour
     */
    public void deleteAppreciationElement (String idEleve, Long idPeriode, Long idEltBilanPeriodique,
                                           JsonArray groupes, Handler<Either<String, JsonObject>> handler);

    /**
     * Mise à jour d'une appreciation pour un élève ou une classe.
     * @param idAppreciation id de l'appréciation à mettre à jour
     * @param commentaire nouvelle appréciation laissée par le professeur
     * @param type eleve ou classe
     * @param handler Handler de retour
     */
    public void updateAppreciationBilanPeriodique (Long idAppreciation, String commentaire, String type,
                                                   Handler<Either<String, JsonObject>> handler);
}
