/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.Domaine;
import fr.openent.competences.bean.Eleve;
import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.openent.competences.service.*;
import fr.openent.competences.utils.HomeworkUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.utils.StringUtils;

import javax.swing.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.Competences.ACTION;
import static fr.openent.competences.Utils.*;
import static fr.openent.competences.service.impl.DefaultExportBulletinService.*;
import static fr.openent.competences.helpers.FormateFutureEvent.formate;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.sql.SqlResult.validRowsResultHandler;

/**
 * Created by vogelmt on 29/03/2017.
 */
public class DefaultBFCService extends SqlCrudService implements BFCService {

    private EventBus eb;
    private static AtomicInteger indexClasse;
    private static AtomicInteger indexClasseStart;
    private CompetenceNoteService competenceNoteService;
    private DomainesService domaineService;
    private CompetencesService competenceService;
    private DispenseDomaineEleveService dispenseDomaineEleveService;
    private BfcSyntheseService bfcSynthseService;
    private EleveEnseignementComplementService eleveEnseignementComplementService;
    private UtilsService utilsService;
    private static final Logger log = LoggerFactory.getLogger(DefaultBFCService.class);
    private Storage storage;
    private ExportBulletinService exportBulletinService;
    private StructureOptionsService structureOptionsService;

    private static final String CODIFICATION = "codification";
    private static final String VALEUR = "valeur";
    private static final String ID_DOMAINE = "id_domaine";
    private static final String EMPTY = "empty";
    private static final String CLASSES = "classes";
    private static final String NOM_CLASSE ="nomClasse";

    public DefaultBFCService(EventBus eb, Storage storage) {
        super(COMPETENCES_SCHEMA,  BFC_TABLE);
        this.eb = eb;
        this.storage = storage;

        competenceNoteService = new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE);
        domaineService = new DefaultDomaineService(COMPETENCES_SCHEMA, DOMAINES_TABLE);
        competenceService = new DefaultCompetencesService(eb);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(COMPETENCES_SCHEMA,DISPENSE_DOMAINE_ELEVE);
        bfcSynthseService = new DefaultBfcSyntheseService(COMPETENCES_SCHEMA, BFC_SYNTHESE_TABLE, eb);
        eleveEnseignementComplementService = new DefaultEleveEnseignementComplementService(COMPETENCES_SCHEMA,
                ELEVE_ENSEIGNEMENT_COMPLEMENT);
        utilsService = new DefaultUtilsService(eb);
        exportBulletinService = new DefaultExportBulletinService(eb, storage);
        structureOptionsService = new DefaultStructureOptions();
    }

    public DefaultBFCService(EventBus eb) {
        super(COMPETENCES_SCHEMA,  BFC_TABLE);
        this.eb = eb;
        this.storage = null;

        competenceNoteService = new DefaultCompetenceNoteService(COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE);
        domaineService = new DefaultDomaineService(COMPETENCES_SCHEMA, DOMAINES_TABLE);
        competenceService = new DefaultCompetencesService(eb);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(COMPETENCES_SCHEMA,DISPENSE_DOMAINE_ELEVE);
        bfcSynthseService = new DefaultBfcSyntheseService(COMPETENCES_SCHEMA, BFC_SYNTHESE_TABLE, eb);
        eleveEnseignementComplementService = new DefaultEleveEnseignementComplementService(COMPETENCES_SCHEMA,
                ELEVE_ENSEIGNEMENT_COMPLEMENT);
        utilsService = new DefaultUtilsService(eb);
        structureOptionsService = new DefaultStructureOptions();
    }

    /**
     * Créer un BFC pour un élève
     * @param bfc objet contenant les informations relative au BFC
     * @param user utilisateur
     * @param handler handler portant le résultat de la requête
     */
    public void createBFC(final JsonObject bfc, final UserInfos user, final Handler<Either<String, JsonObject>> handler){
        super.create(bfc, user, handler);
    }

    /**
     * Mise à jour d'un BFC pour un élève
     * @param data appreciation à mettre à jour
     * @param user utilisateur
     * @param handler handler portant le resultat de la requête
     */
    public void updateBFC(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler){
        data.remove(ID_KEY);
        StringBuilder sb = new StringBuilder();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for (String attr : data.fieldNames()) {
            sb.append(attr).append(" = ?, ");
            values.add(data.getValue(attr));
        }
        String query =
                "UPDATE " + resourceTable +
                        " SET " + sb.toString() + "modified = NOW() " +
                        "WHERE id_domaine = ?  AND id_eleve = ? ";

        values.add((Number)data.getValue("id_domaine"))
                .add(data.getValue("id_eleve"));
        sql.prepared(query, values,validRowsResultHandler(handler));
    }

    /**
     * Suppression d'un BFC pour un élève
     * @param idBFC identifiant de la note
     * @param user user
     * @param handler handler portant le résultat de la requête
     */
    public void deleteBFC(long idBFC, String idEleve, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + resourceTable + " WHERE id_domaine = ? AND id_eleve = ?";
        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(idBFC).add(idEleve), validRowsResultHandler(handler));
    }

    /**
     * Récupère les BFCs d'un élève pour chaque domaine
     * @param idEleves
     * @param idEtablissement
     * @param handler
     */
    @Override
    public void getBFCsByEleve(String[] idEleves, String idEtablissement, Long idCycle,
                               Handler<Either<String,JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT * ")
                .append(" FROM notes.bilan_fin_cycle")
                .append(" INNER JOIN notes.domaines ON bilan_fin_cycle.id_domaine = domaines.id")
                .append(" WHERE bilan_fin_cycle.id_eleve IN " + Sql.listPrepared(idEleves))
                .append(" AND bilan_fin_cycle.id_etablissement = ? AND valeur >= 0 ");

        for(String s : idEleves) {
            values.add(s);
        }

        values.add(idEtablissement);

        if(idCycle != null) {
            query.append("AND domaines.id_cycle = ? ");
            values.add(idCycle);
        }

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    /**
     * Recupere la liste des domaines et des competences evalues au cours du cycle auquel appartient la classe dont
     * l'identifiant est passe en parametre.
     * Construit un objet {@link Domaine} par domaine ainsi recupere, contenant les competences qu'il couvre, ainsi que
     * son arborescence (sous-domaines et domaine parent).
     *
     * @param idClasse  Identifiant de la classe appartenant au cycle dont on souhaite recuperer les domaines et les
     *                  competences.
     * @param handler   Handler portant un map contenant les domaines indexes par leur identifiant.
     *
     * @see Domaine
     */
    private void getDomaines(final String idClasse, Long idCycle,
                             final Handler<Either<String, Map<Long, Domaine>>> handler) {
        domaineService.getArbreDomaines(idClasse,null, idCycle, arbreDomainesEvent -> {
            if (arbreDomainesEvent.isRight()) {
                JsonArray domainesResultArray = arbreDomainesEvent.right().getValue();
                if (domainesResultArray.size() == 0) {
                    handler.handle(new Either.Left<>("Erreur lors de la recuperation des domaines : " +
                            "aucun domaine de competences n'a ete trouve."));
                    log.error("getDomaines (" + idClasse + ") : aucun domaine de competences n'a ete trouve.");
                }

                final Map<Long, Domaine> domaines = new HashMap<>();
                for (int i = 0; i < domainesResultArray.size(); i++) {
                    JsonObject _o = domainesResultArray.getJsonObject(i);
                    Domaine _d = new Domaine(_o.getLong(ID_KEY), _o.getBoolean("evaluated"));
                    if (domaines.containsKey(_o.getLong("id_parent"))) {
                        Domaine parent = domaines.get(_o.getLong("id_parent"));
                        parent.addSousDomaine(_d);
                        _d.addParent(parent);
                    }
                    domaines.put(_d.getId(), _d);
                }

                if(!domaines.isEmpty()) {
                    competenceService.getCompetencesDomaines(idClasse, domaines.keySet().toArray(new Long[0]), event -> {
                        if (event.isRight()) {
                            JsonArray competencesResultArray = event.right().getValue();
                            if (competencesResultArray.size() == 0) {
                                handler.handle(new Either.Left<>("Erreur lors de la recuperation des competences " +
                                        "pour les domaines : aucun competence pour les domaines selectionnes."));
                                log.error("getDomaines : getCompetencesDomaines : aucun competence pour les domaines selectionnes.");
                            }

                            for (int i = 0; i < competencesResultArray.size(); i++) {
                                JsonObject _o = competencesResultArray.getJsonObject(i);

                                domaines.get(_o.getLong("id_domaine")).addCompetence(_o.getLong("id_competence"));
                            }
                            handler.handle(new Either.Right<>(domaines));
                        } else {
                            handler.handle(new Either.Left<>("Erreur lors de la recuperation des competences " +
                                    "pour les domaines :\n" + event.left().getValue()));
                            log.error("getDomaines : getCompetencesDomaines : " + event.left().getValue());
                        }
                    });
                } else {
                    handler.handle(new Either.Left<>("La classe " + idClasse + " n'est rattachee a aucun cycle."));
                    log.error("La classe " + idClasse + " n'est rattachée a aucun cycle.");
                }
            } else {
                handler.handle(new Either.Left<>("Erreur lors de la recuperation " +
                        "des domaines :\n" + arbreDomainesEvent.left().getValue()));
                log.error("getDomaines (" + idClasse + ") : " + arbreDomainesEvent.left().getValue());
            }
        });
    }

    /**
     * Recupere l'evaluation maximale pour chaque competence des eleves passes en parametre, et ce sur la periode passee
     * en parametre.
     *
     * @param idEleves   Tableau des identifiants des eleves dont on souhaite recuperer les evaluations.
     * @param idPeriode  Identifiant de la periode au cours de laquelle on souhaite recuperer les evaluations. Peut etre null.
     * @param handler    Handler contenant une map de note par competence, pour chaque eleve.
     */
    private void getMaxOrAverageNoteCompetenceEleve(final String[] idEleves, Long idPeriode, Long idCycle, Boolean isYear,
                                           Boolean recapEval, Boolean isSkillAverage,
                                                    final Handler<Either<String, Map<String, Map<Long, Float>>>> handler) {
        if (idCycle == null || recapEval) {
            competenceNoteService.getMaxOrAverageCompetenceNoteEleveByPeriod(idEleves, idPeriode, isYear, isSkillAverage,
                    event -> {
                if (event.isRight()) {
                    Map<String, Map<Long, Float>> moyMaxMatCompEleve = getStringMapMap(event);

                    handler.handle(new Either.Right<>(moyMaxMatCompEleve));
                } else {
                    handler.handle(new Either.Left<>("Erreur lors de la recuperation des evaluations " +
                            "de competences de la periode :\n" + event.left().getValue()));
                    log.error("getMaxOrAverageCompetenceNoteEleveByPeriod : " + event.left().getValue());
                }
            });
        } else {
            competenceNoteService.getMaxOrAverageCompetenceNoteEleveByCycle(idEleves, idCycle, isSkillAverage, event -> {
                if (event.isRight()) {
                    Map<String, Map<Long, Float>> moyMaxMatCompEleve = getStringMapMap(event);

                    handler.handle(new Either.Right<>(moyMaxMatCompEleve));
                } else {
                    handler.handle(new Either.Left<>("Erreur lors de la recuperation des evaluations " +
                            "des competences du cycle :\n" + event.left().getValue()));
                    log.error("getMaxOrAverageCompetenceNoteEleveByCycle : " + event.left().getValue());
                }
            });
        }
    }

    private Map<String, Map<Long, Float>> getStringMapMap (Either<String, JsonArray> event) {
        Map<String, Map<Long,Map<String, Double>>> notesCompetencesEleve = new HashMap<>();
        Map<String, Map<Long,Float>> moyMaxMatCompEleve = new HashMap<>();
        Map<Long,List<String>> pastYear = new HashMap<>();

        JsonArray notesResultArray = event.right().getValue();
        for (int i = 0; i < notesResultArray.size(); i++) {
            JsonObject _o = notesResultArray.getJsonObject(i);
            String id_eleve = _o.getString("id_eleve");
            String id_matiere = _o.getString("id_matiere");
            Long id_competence = _o.getLong("id_competence");
            Double noteComp = HomeworkUtils.safeGetDouble(_o, "evaluation");
            Double niveauFinalAnnuel = HomeworkUtils.safeGetDouble(_o, "niveau_final_annuel");
            Double niveauFinal = HomeworkUtils.safeGetDouble(_o, "niveau_final");

            if(noteComp < 0) {
                continue;
            }
            if (!notesCompetencesEleve.containsKey(id_eleve)) {
                notesCompetencesEleve.put(id_eleve, new HashMap<>());
                moyMaxMatCompEleve.put(id_eleve, new HashMap<>());
            }
            if (!notesCompetencesEleve.get(id_eleve).containsKey(id_competence)) {
                notesCompetencesEleve.get(id_eleve).put(id_competence, new HashMap<>());
            }

            Map<String,Double> notesCompetencesEleveIdComp = notesCompetencesEleve.get(id_eleve).get(id_competence);

            //si la competence n'est pas dans la map
            if(!notesCompetencesEleveIdComp.containsKey(id_matiere)) {
                //on set la competence avec la note ou le niveau final annuel ou périodique s'il existe
                if(niveauFinalAnnuel!= null) {
                    notesCompetencesEleveIdComp.put(id_matiere, niveauFinalAnnuel);
                }else {
                    if(niveauFinal!= null) {
                        notesCompetencesEleveIdComp.put(id_matiere, niveauFinal);
                    }else {
                        notesCompetencesEleveIdComp.put(id_matiere, noteComp);
                    }
                }
                if(_o.getString("owner").equals("id-user-transition-annee"))
                    if(pastYear.containsKey(id_competence))
                        pastYear.get(id_competence).add(id_matiere);
                    else {
                        pastYear.put(id_competence, new ArrayList<>());
                        pastYear.get(id_competence).add(id_matiere);
                    }
            }else if(!_o.getString("owner").equals("id-user-transition-annee")){
                //si il s'agit d'une compétence noté sur la même année, sinon on ne prends pas en compte les années passées
                if(pastYear.containsKey(id_competence) && pastYear.get(id_competence).contains(id_matiere)){
                    notesCompetencesEleveIdComp.put(id_matiere, noteComp);
                    pastYear.get(id_competence).remove(id_matiere);
                }
                //sinon on récupère la valeur de la competence déjà enregistrée
                Double niveauOfThisCompetence = notesCompetencesEleveIdComp.get(id_matiere);
                // on met le niveau_final annuel ou périodique s'il existe ou on le compare à la note de l'élève
                if(niveauFinalAnnuel!= null && niveauOfThisCompetence < niveauFinalAnnuel){
                    notesCompetencesEleveIdComp.put(id_matiere, niveauFinalAnnuel);
                }else{
                    if(niveauFinal!= null && niveauOfThisCompetence < niveauFinal
                            && niveauFinalAnnuel == null){
                        notesCompetencesEleveIdComp.put(id_matiere, niveauFinal);
                    }else{
                        if(niveauOfThisCompetence < noteComp&& niveauFinal== null
                                && niveauFinalAnnuel== null){
                            notesCompetencesEleveIdComp.put(id_matiere, noteComp);
                        }
                    }
                }
            }
        }

        //on fait la moyenne des maxs dans chaque matière pour une compétence
        for (Map.Entry<String, Map<Long, Map<String, Double>>> competences : notesCompetencesEleve.entrySet()) {
            String id_eleve = competences.getKey();
            for (Map.Entry<Long, Map<String, Double>> competenceMaxMat : competences.getValue().entrySet()) {
                Long id_competence = competenceMaxMat.getKey();
                Map<String, Double> maxMats = competenceMaxMat.getValue();
                float sum = 0;
                float nbrofMat = 0;
                float moyenneToSend = 1f;
                for(Map.Entry<String, Double> maxMat : maxMats.entrySet()){
                    String id_matiere = maxMat.getKey();
                    Double max = maxMat.getValue();
                    if(!pastYear.containsKey(id_competence) || !pastYear.get(id_competence).contains(id_matiere)){
                        sum += max;
                        nbrofMat++;
                    }
                }
                if (nbrofMat == 0){
                    for(Double max : maxMats.values()){
                        sum += max;
                        nbrofMat++;
                    }
                }
                if(nbrofMat != 0)
                    moyenneToSend = sum / nbrofMat;
                moyMaxMatCompEleve.get(id_eleve).put(id_competence, moyenneToSend);
            }
        }
        return moyMaxMatCompEleve;
    }

    /**
     * Recupere l'echelle de conversion entre l'evaluation calculee pour chaque eleve par domaine, et le niveau de
     * maitrise correspondant, en fonction de l'etablissement dont l'identifiant est passe en parametre.
     *
     * @param idStructure  Identifiant de l'etablissement dont on souhaite recuperer l'echelle de conversion.
     * @param idClasse     Identifiant d'une classe appartenant au cycle pour lequel on souhaite recuperer l'echelle.
     * @param handler      Handler contenant un set ordonne des bornes pour chaque niveau (donc n+1 bornes, n etant le
     *                     nombre de niveau de maitrise).
     */
    private void getEchelleConversion(String idStructure, String idClasse,
                                      final Handler<Either<String, SortedSet<Double>>> handler) {
        competenceNoteService.getConversionNoteCompetence(idStructure, idClasse, event -> {
            if (event.isRight()) {
                JsonArray conversion = event.right().getValue();

                if(conversion.size() == 0) {
                    handler.handle(new Either.Left<>("Erreur lors de la recuperation de l'echelle de conversion : " +
                            "aucun echelle de conversion n'a ete trouvee."));
                    log.error("getEchelleConversion : aucun echelle de conversion n'a ete trouvee.");
                }
                SortedSet<Double> bornes = new TreeSet<>();

                for (int i = 0; i < conversion.size(); i++) {
                    JsonObject _o = conversion.getJsonObject(i);
                    bornes.add(_o.getDouble("valmin"));
                    bornes.add(_o.getDouble("valmax"));
                }
                handler.handle(new Either.Right<>(bornes));
            } else {
                handler.handle(new Either.Left<>("Erreur lors de la recuperation " +
                        "de l'echelle de conversion :\n" + event.left().getValue()));
                log.error("getEchelleConversion : " + event.left().getValue());
            }
        });
    }

    /**
     * Calcule la moyenne des eleves dont les evaluations sont passees en parametre, en fonction des domaines, des bornes
     * et des BFC passes en parametre.
     *
     * @param idEleves                Liste des identifiants d'élève
     * @param domainesRacine          liste des domaines racines pour lesquels evaluer les eleves
     * @param echelleConversion                  set ordonne des bornes de niveaux de maitrise pour les evaluations
     * @param notesCompetencesEleves  map des evaluations de competence pour chaque eleve.
     * @param bfcEleves               map des evaluations effectuees manuellement par le chef etablissement pour chaque
     *                                eleve
     * @param handler                 handler contenant une map des niveaux de maitrise par domaine de chaque eleve
     */
    private void calcMoyBFC(boolean recapEval, String[] idEleves, Map<String, Map<Long, Integer>> bfcEleves,
                            Map<String, Map<Long, Float>> notesCompetencesEleves, SortedSet<Double> echelleConversion,
                            List<Domaine> domainesRacine, Handler<Either<String, JsonObject>> handler) {
        if(notesCompetencesEleves.get(EMPTY) != null && bfcEleves.get(EMPTY) != null) {
            //Si les domaines et les bornes sont renseignées mais qu'aucune compétence n'a été évaluée, une réponse vide est retournée.
            handler.handle(new Either.Right(new JsonObject()));
            return;
        }

        JsonObject result = new JsonObject();

        for(String eleve : idEleves) {
            if(!bfcEleves.containsKey(eleve) && !notesCompetencesEleves.containsKey(eleve)) {
                continue;
            }

            JsonArray resultEleve = new JsonArray();

            DecimalFormat decimalFormat = new DecimalFormat("#.0");
            decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

            for (Domaine d : domainesRacine) {
                JsonObject note = new JsonObject();
                if (bfcEleves.containsKey(eleve) && bfcEleves.get(eleve).containsKey(d.getId()) &&
                        bfcEleves.get(eleve).get(d.getId()) >= echelleConversion.first() &&
                        bfcEleves.get(eleve).get(d.getId()) <= echelleConversion.last()) {
                    note.put("idDomaine", d.getId());
                    note.put("niveau", bfcEleves.get(eleve).get(d.getId()));
                    if(recapEval){
                        if(bfcEleves.get(eleve).containsKey(d.getId())){
                            note.put("moyenne", bfcEleves.get(eleve).get(d.getId()));
                        } else {
                            Double moy = calculMoyenne(d, notesCompetencesEleves, eleve, echelleConversion);
                            if (moy != null)
                                note.put("moyenne", Double.valueOf(decimalFormat.format(moy).replaceAll(",", ".")));
                        }
                    }
                } else if (notesCompetencesEleves.containsKey(eleve)) {
                    Double moy = calculMoyenne(d, notesCompetencesEleves, eleve, echelleConversion);
                    if (moy != null) {
                        Iterator<Double> echelleConversionIterator = echelleConversion.iterator();
                        int simplifiedMoy = 0;
                        while (moy >= echelleConversionIterator.next() && echelleConversionIterator.hasNext()) {
                            simplifiedMoy++;
                        }
                        if(simplifiedMoy >= echelleConversion.first() && simplifiedMoy <= echelleConversion.last()) {
                            note.put("idDomaine",d.getId());
                            note.put("niveau", simplifiedMoy);
                            if(recapEval)
                                note.put("moyenne", Double.valueOf(decimalFormat.format(moy).replaceAll(",", ".")));
                        }
                    }
                }
                if(note.size() > 0) {
                    resultEleve.add(note);
                }
            }
            if(resultEleve.size() > 0) {
                result.put(eleve, resultEleve);
            }
        }
        if(recapEval){
            JsonArray domainesR = new JsonArray();
            for (Domaine d : domainesRacine) {
                domainesR.add(d.getId());
            }
            result.put("domainesRacine", domainesR);
        }

        handler.handle(new Either.Right<>(result));
    }

    private Double calculMoyenne(Domaine d, Map<String, Map<Long, Float>> notesCompetencesEleves, String eleve,
                                 SortedSet<Double> echelleConversion){
        float total = 0;
        long diviseur = 0;
        for (Long idCompetence : d.getCompetences()) {
            if (notesCompetencesEleves.get(eleve) != null && notesCompetencesEleves.get(eleve).containsKey(idCompetence)) {
                //convertir les moyennes des maxs dans chaque matière en un chiffre rond
                float moy = notesCompetencesEleves.get(eleve).get(idCompetence) + 1;
                Iterator<Double> echelleConversionIterator = echelleConversion.iterator();
                int simplifiedMoy = 0;
                while (moy >= echelleConversionIterator.next() && echelleConversionIterator.hasNext()) {
                    simplifiedMoy++;
                }
                if(simplifiedMoy >= echelleConversion.first() && simplifiedMoy <= echelleConversion.last()) {
                    total += simplifiedMoy;
                    diviseur++;
                }
            }
        }
        return diviseur != 0 ? ((double) total / diviseur) : null;
    }

    @Override
    public void buildBFC(final boolean recapEval, final String[] idEleves, final String idClasse,
                         final String idStructure, final Long idPeriode, final Long idCycle, final Boolean isYear,
                         final Handler<Either<String, JsonObject>> handler) {
        final Map<String, Map<Long, Integer>> bfcEleve = new HashMap<>();
        final Map<String, Map<Long, Float>> notesCompetencesEleve = new HashMap<>();
        final SortedSet<Double> echelleConversion = new TreeSet<>();
        final List<Domaine> domainesRacine = new ArrayList<>();

        structureOptionsService.getIsAverageSkills(idStructure, responseOption -> {

            if(responseOption.isLeft()) {
                handler.handle(new Either.Left<>("Impossible de recuperer le type de calcul des competences notes \n"
                        + responseOption.left().getValue()));
                log.error("buildBFC : getIsAverageSkills : " + responseOption.left().getValue());
            }
            Boolean isSkillAverage = new Boolean(responseOption.right().getValue().getBoolean("is_average_skills"));

            List<Future> listOfFutures = new ArrayList<>();

            Promise<Void> getBFCsByElevePromise = Promise.promise();
            listOfFutures.add(getBFCsByElevePromise.future());
            getBfCsByEleve(idEleves, idStructure, idPeriode, idCycle, isYear, handler, bfcEleve, getBFCsByElevePromise);

            Promise<Void> getMaxNoteCompetenceElevePromise = Promise.promise();
            listOfFutures.add(getMaxNoteCompetenceElevePromise.future());
            getMaxOrAverageNoteCompetenceEleve(recapEval, idEleves, idPeriode, idCycle, isYear, handler,
                    notesCompetencesEleve, isSkillAverage, getMaxNoteCompetenceElevePromise);

            Promise<Void> echelleConversionPromise = Promise.promise();
            listOfFutures.add(echelleConversionPromise.future());
            getEchelleConversion(idClasse, idStructure, handler, echelleConversion, echelleConversionPromise);

            Promise<Void> domainesPromise = Promise.promise();
            listOfFutures.add(domainesPromise.future());
            getDomaines(idClasse, idCycle, handler, domainesRacine, domainesPromise);

            CompositeFuture.all(listOfFutures)
                    .onSuccess(event -> calcMoyBFC(recapEval, idEleves, bfcEleve,
                            notesCompetencesEleve, echelleConversion, domainesRacine, handler))
                    .onFailure(err -> {
                        String error = err.getMessage();
                        log.error("[buildBFC] : " + error);
                        handler.handle(new Either.Left<>("[buildBFC] Failed : " + error));
                    });
        });

    }

    private void getDomaines (String idClasse, Long idCycle, Handler<Either<String, JsonObject>> handler,
                              List<Domaine> domainesRacine, Promise<Void> domainesPromise) {
        getDomaines(idClasse, idCycle, event -> {
            if (event.isRight()) {
                Set<Domaine> setDomainesRacine = new LinkedHashSet<>();

                final Map<Long, Domaine> domaines = new HashMap<>();
                domaines.putAll(event.right().getValue());
                for (Domaine domaine : domaines.values()) {
                    if (domaine.getParentRacine() != null) {
                        setDomainesRacine.add(domaine.getParentRacine());
                    }
                }
                domainesRacine.addAll(setDomainesRacine);

                domainesPromise.complete();
            } else {
                domainesPromise.fail("Impossible de recuperer les domaines racines pour la" +
                        " classe selectionne :\n" + event.left().getValue());
                handler.handle(new Either.Left<>("Impossible de recuperer les domaines racines pour la" +
                        " classe selectionne :\n" + event.left().getValue()));
                log.error("buildBFC : getDomaines : " + event.left().getValue());
            }
        });
    }

    private void getEchelleConversion (String idClasse, String idStructure, Handler<Either<String, JsonObject>> handler,
                                       SortedSet<Double> echelleConversion, Promise<Void> echelleConversionPromise) {
        getEchelleConversion(idStructure, idClasse, event -> {
            if (event.isRight()) {
                echelleConversion.addAll(event.right().getValue());
                echelleConversionPromise.complete();
            } else {
                echelleConversionPromise.fail("Impossible de recuperer l'echelle de conversion pour la " +
                        "classe selectionnee :\n" + event.left().getValue());
                handler.handle(new Either.Left<>("Impossible de recuperer l'echelle de conversion pour la " +
                        "classe selectionnee :\n" + event.left().getValue()));
                log.error("buildBFC : getEchelleConversion : " + event.left().getValue());
            }
        });
    }

    private void getMaxOrAverageNoteCompetenceEleve (boolean recapEval, String[] idEleves, Long idPeriode, Long idCycle,
                                                     Boolean isYear, Handler<Either<String, JsonObject>> handler,
                                                     Map<String, Map<Long, Float>> notesCompetencesEleve, Boolean isSkillAverage,
                                                     Promise<Void> getMaxNoteCompetenceElevePromise) {
        getMaxOrAverageNoteCompetenceEleve(idEleves, idPeriode, idCycle, isYear, recapEval, isSkillAverage, event -> {
            if (event.isRight()) {
                notesCompetencesEleve.putAll(event.right().getValue());
                if (notesCompetencesEleve.isEmpty()) {
                    notesCompetencesEleve.put(EMPTY, new HashMap<>());
                }
                getMaxNoteCompetenceElevePromise.complete();
            } else {
                getMaxNoteCompetenceElevePromise.fail("Impossible de recuperer les evaluations pour " +
                        "la classe selectionnee :\n" + event.left().getValue());
                handler.handle(new Either.Left<>("Impossible de recuperer les evaluations pour " +
                        "la classe selectionnee :\n" + event.left().getValue()));
                log.error("buildBFC : getMaxNoteCompetenceEleve : " + event.left().getValue());
            }
        });
    }

    private void getBfCsByEleve (String[] idEleves, String idStructure, Long idPeriode, Long idCycle, Boolean isYear,
                                 Handler<Either<String, JsonObject>> handler, Map<String, Map<Long, Integer>> bfcEleve,
                                 Promise<Void> getBFCsByElevePromise) {
        getBFCsByEleve(idEleves, idStructure, idCycle, event -> {
            if (event.isRight()) {
                JsonArray bfcResultArray = event.right().getValue();

                for (int i = 0; i < bfcResultArray.size(); i++) {
                    JsonObject _o = bfcResultArray.getJsonObject(i);
                    if (_o.getInteger(VALEUR) < 0) {
                        continue;
                    }
                    if (!bfcEleve.containsKey(_o.getString(ID_ELEVE))) {
                        bfcEleve.put(_o.getString(ID_ELEVE), new HashMap<>());
                    }
                    bfcEleve.get(_o.getString(ID_ELEVE)).put(_o.getLong(ID_DOMAINE), _o.getInteger(VALEUR));
                }

                if (bfcEleve.isEmpty() || idPeriode != null || isYear) {
                    bfcEleve.put(EMPTY, new HashMap<>());
                    // Ajouter une valeur inutilisee dans la map permet de s'assurer que le traitement a ete effectue
                }
                getBFCsByElevePromise.complete();
            } else {
                getBFCsByElevePromise.fail("Impossible de recuperer le bilan de fin de cycle pour la " +
                        "classe selectionnee :\n" + event.left().getValue());
                handler.handle(new Either.Left<>("Impossible de recuperer le bilan de fin de cycle pour la " +
                        "classe selectionnee :\n" + event.left().getValue()));
                log.error("buildBFC : getBFCsByEleve : " + event.left().getValue());
            }
        });
    }

    @Override
    public void getCalcMillesimeValues (Handler<Either<String,JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * FROM notes.calc_millesime";
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }
    @Override
    public void setVisibility(String structureId, Integer idVisibility, UserInfos user, Integer visible,
                              Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder().append("INSERT INTO ")
                .append( COMPETENCES_SCHEMA + ".visibility (id_etablissement, visible, id_visibility) ")
                .append(" VALUES " )
                .append(" ( ? , ?, ? )" )
                .append(" ON CONFLICT (id_etablissement, id_visibility) DO UPDATE SET visible = ?");
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        values.add(structureId).add(visible).add(idVisibility).add(visible);
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getVisibility(String structureId,Integer idVisibility, UserInfos user, Handler<Either<String, JsonArray>> handler) {

        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        StringBuilder query = new StringBuilder().append(" SELECT id_etablissement, visible, id_visibility ")
                .append(" FROM " + COMPETENCES_SCHEMA + ".visibility ")
                .append(" WHERE id_etablissement = ? " )
                .append("AND id_visibility = ?");
        values.add(structureId).add(idVisibility);

        query.append(" UNION ALL " )
                .append(" SELECT ? , ")
                .append(" 1 , ?");
        values.add(structureId).add(idVisibility);

        query.append(" WHERE NOT EXISTS (SELECT id_etablissement, visible, id_visibility ")
                .append(" FROM " + COMPETENCES_SCHEMA + ".visibility")
                .append(" WHERE id_etablissement = ? AND id_visibility = ? );  ");

        values.add(structureId).add(idVisibility);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }

    private void runMoyenneControleContinue(final JsonArray moyControlesContinusEleves, final Integer maxBareme,
                                            Map<String, Map<Long, Boolean>> dispensesDomainesEleves,
                                            final Integer nbDomainesRacines , final JsonObject resultsElevesByDomaine,
                                            final Map<Integer, Integer> mapOrdreBaremeBrevet,
                                            final Map.Entry<String, List<String>> classe,
                                            final Handler<Either<String, JsonArray>> handler){
        for (String idEleve : classe.getValue()) {
            Integer sommeBareme = 0;
            Integer totalMaxBareme = 0;
            //si l'élève est dans le json resultsElevesByDomaine alors il a au moins un niveau pour un domaine
            if (resultsElevesByDomaine.containsKey(idEleve)) {
                JsonArray idDomainesNiveaux = resultsElevesByDomaine.getJsonArray(idEleve);
                //si l'élève n'a pas de dispense
                if (!dispensesDomainesEleves.containsKey(idEleve)) {
                    totalMaxBareme = nbDomainesRacines * maxBareme;
                    for (int i = 0; i < idDomainesNiveaux.size(); i++) {
                        JsonObject idDomaineNiveau = idDomainesNiveaux.getJsonObject(i);
                        sommeBareme += mapOrdreBaremeBrevet.get(idDomaineNiveau.getInteger("niveau"));
                    }
                    //si l'élève a une dispen
                } else {
                    Map<Long, Boolean> idDomaineDispense = dispensesDomainesEleves.get(idEleve);
                    totalMaxBareme = (nbDomainesRacines - idDomaineDispense.size()) * maxBareme;
                    for (int i = 0; i < idDomainesNiveaux.size(); i++) {
                        JsonObject idDomaineNiveau = idDomainesNiveaux.getJsonObject(i);
                        //Si idDomaine en cours n'est pas dispensé alors on ajouter le niveau à la somme
                        if (!idDomaineDispense.containsKey(idDomaineNiveau.getLong("idDomaine"))) {
                            //on somme les niveaux en convertissant le niv(=ordre) en barème
                            sommeBareme += mapOrdreBaremeBrevet.get(idDomaineNiveau.getInteger("niveau"));
                        }
                    }
                }
                JsonObject moyControlesContinusByEleve = new JsonObject();
                moyControlesContinusByEleve.put("id_eleve", idEleve);
                moyControlesContinusByEleve.put("controlesContinus_brevet", sommeBareme);
                moyControlesContinusByEleve.put("totalMaxBaremeBrevet", totalMaxBareme);
                moyControlesContinusEleves.add(moyControlesContinusByEleve);

                //sinon l'élève n'a pas d'évaluation alors la moyenne sera = somme nulle,
                // il faut seulement affecter le totalMaxBareme en fct des domaines dispensés ou non
            } else {
                if (!dispensesDomainesEleves.containsKey(idEleve)) {
                    totalMaxBareme = nbDomainesRacines * maxBareme;
                } else {
                    Map<Long, Boolean> idDomaineDispense = dispensesDomainesEleves.get(idEleve);
                    totalMaxBareme = (nbDomainesRacines - idDomaineDispense.size()) * maxBareme;
                }
                JsonObject moyControlesContinusByEleve = new JsonObject();
                moyControlesContinusByEleve.put("id_eleve", idEleve);
                moyControlesContinusByEleve.put("controlesContinus_brevet", sommeBareme);
                moyControlesContinusByEleve.put("totalMaxBaremeBrevet", totalMaxBareme);
                moyControlesContinusEleves.add(moyControlesContinusByEleve);
            }
        }
        handler.handle(new Either.Right<String, JsonArray>(moyControlesContinusEleves));
    }

    @Override
    public void getMoyenneControlesContinusBrevet(EventBus eb, List<String> idsClasses,final Long idPeriode,
                                                  Boolean isCycle, final Long idCycle,
                                                  final Handler<Either<String, JsonArray>> handler) {

        // j'ai besoin de récupérer les idsEleve et idStructure à partir de l'idClass
        final JsonArray moyControlesContinusEleves = new fr.wseduc.webutils.collections.JsonArray();
        getParamsMethodGetMoyenne(idsClasses, idPeriode,  respParam -> {
            if(respParam.isLeft()) {
                handler.handle(new Either.Left(respParam.left().getValue()));
                log.error("getMoyenneControlesContinusBrevet : " + respParam.left().getValue());
            }
            else {
                Map<String, Map<String, List<String>>> mapStructureClassesEleves = respParam.right().getValue();
                final String idStructure = mapStructureClassesEleves.entrySet().iterator().next().getKey();
                final Map<String, List<String>> mapClassesEleves = mapStructureClassesEleves.entrySet().iterator().next().getValue();

                for (final Map.Entry<String, List<String>> classe : mapClassesEleves.entrySet()) {
                    final String[] idsEleves = classe.getValue().toArray(new String[0]);
                    final String idClasse = classe.getKey();
                    //On récupère la valeur max du barèmebrevet et map des ordres(=niveau ds le JsonObject du buildBFC)/bareme
                    Promise<Map<Integer, Map<Integer, Integer>>> maxBaremPromise = Promise.promise();
                    competenceNoteService.getMaxBaremeMapOrderBaremeBrevet(idStructure, idClasse, max ->
                            formate(maxBaremPromise, max));

                    //On récupère le nb de DomainesRacines
                    Promise<JsonArray> domainesRacinePromise = Promise.promise();
                    domaineService.getDomainesRacines(idClasse, null,
                            event -> formate(domainesRacinePromise, event));

                    //On récupère les élèves qui sont dispensés pour un domaine racine
                    Promise<Map<String, Map<Long, Boolean>>> dispenseDomainePromise = Promise.promise();
                    dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(classe.getValue(),event ->
                            formate(dispenseDomainePromise, event));

                    //On récupère pour tous les élèves de la classe leurs résultats pour chaque domainesRacines évalué
                    Promise<JsonObject> bfcPromise = Promise.promise();
                    buildBFC(false, idsEleves, idClasse, idStructure, idPeriode, idCycle, false,
                            event-> formate(bfcPromise, event));

                    Future.all(maxBaremPromise.future(), domainesRacinePromise.future(), dispenseDomainePromise.future(), bfcPromise.future())
                            .onComplete(event -> {
                                if(event.failed()){
                                    handler.handle(new Either.Left(event.cause().getMessage()));
                                    log.error("getMoyenneControlesContinusBrevet : " + event.cause().getMessage());
                                    return;
                                }
                                Map<String, Map<Long, Boolean>> dispensesDomainesEleves =
                                        dispenseDomainePromise.future().result();
                                final Integer nbDomainesRacines = domainesRacinePromise.future().result().size();
                                final JsonObject resultsElevesByDomaine = bfcPromise.future().result();
                                final Integer maxBareme = maxBaremPromise.future().result().entrySet().iterator().next().getKey();
                                final Map<Integer, Integer> mapOrdreBaremeBrevet =
                                        maxBaremPromise.future().result().entrySet().iterator().next().getValue();

                                runMoyenneControleContinue(moyControlesContinusEleves, maxBareme,
                                        dispensesDomainesEleves, nbDomainesRacines,resultsElevesByDomaine,
                                        mapOrdreBaremeBrevet, classe, handler);
                            });
                }
            }
        });
    }

    public void getMoyenneControlesContinusBrevet(EventBus eb, String idClasse, String idEleve, String idStructure,
                                                  Long idPeriode, Boolean isCycle, Long idCycle,
                                                  final Handler<Either<String, JsonArray>> handler) {
        final JsonArray moyControlesContinusEleves = new fr.wseduc.webutils.collections.JsonArray();

        List<String> idsEleves = new ArrayList<>();
        idsEleves.add(idEleve);

        //On récupère la valeur max du barèmebrevet et map des ordres(=niveau ds le JsonObject du buildBFC)/bareme
        Promise<Map<Integer, Map<Integer, Integer>>> maxBaremPromise = Promise.promise();
        competenceNoteService.getMaxBaremeMapOrderBaremeBrevet(idStructure, idClasse, event ->
                formate(maxBaremPromise, event));

        //On récupère le nb de DomainesRacines
        Promise<JsonArray> domainesRacinePromise = Promise.promise();
        domaineService.getDomainesRacines(idClasse, null, event ->
                formate(domainesRacinePromise, event));

        //On récupère les élèves qui sont dispensés pour un domaine racine
        Promise<Map<String, Map<Long, Boolean>>> dispDomainePromise = Promise.promise();
        dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(idsEleves, event ->
                formate(dispDomainePromise, event));

        //On récupère pour tous les élèves de la classe leurs résultats pour chaque domainesRacines évalué
        Promise<JsonObject> bfcPromise = Promise.promise();
        buildBFC(false, new String[]{idEleve}, idClasse, idStructure, idPeriode, idCycle, false,
                event -> formate(bfcPromise, event));

        Future.all(maxBaremPromise.future(), domainesRacinePromise.future(), dispDomainePromise.future(), bfcPromise.future()).onComplete(event -> {
            if(event.failed()){
                returnFailure("getMoyenneControlesContinusBrevet ", event, handler);
                return;
            }
            Map<String, Map<Long, Boolean>> dispensesDomainesEleves = dispDomainePromise.future().result();
            final Integer nbDomainesRacines = domainesRacinePromise.future().result().size();
            final JsonObject resultsElevesByDomaine = bfcPromise.future().result();
            final Integer maxBareme = maxBaremPromise.future().result().entrySet().iterator().next().getKey();
            final Map<Integer, Integer> mapOrdreBaremeBrevet = maxBaremPromise.future().result().entrySet().iterator().next().getValue();

            Map<String, List<String>> classe = new HashMap<>();
            classe.put(idClasse, new ArrayList<>());
            classe.get(idClasse).add(idEleve);
            for(Map.Entry<String, List<String>> c :  classe.entrySet()) {
                runMoyenneControleContinue(moyControlesContinusEleves, maxBareme, dispensesDomainesEleves,
                        nbDomainesRacines, resultsElevesByDomaine, mapOrdreBaremeBrevet, c, handler);
            }
        });
    }

    //récupérer les paramètres nécessaire pour les méthodes
    private void getParamsMethodGetMoyenne (final List<String> idsClasses, final Long idPeriode,
                                            final Handler<Either<String, Map<String, Map<String, List<String>>>>> handler){
        final Map<String, Map<String, List<String>>> paramsMethods = new HashMap<>();

        Promise<String> structurePromise =  Promise.promise();
        Utils.getStructClasses(eb, idsClasses.toArray(new String[0]), event -> formate(structurePromise, event));

        Promise<Map<String, List<String>>> repElevesPromise = Promise.promise();
        Utils.getElevesClasses(eb, idsClasses.toArray(new String[0]), idPeriode, event -> formate(repElevesPromise, event));

        Future.all(structurePromise.future(), repElevesPromise.future()).onComplete(event -> {
            if(event.failed()){
                String error = event.cause().getMessage();
                log.error("[getParamsMethodGetMoyenne] : " +  error);
                handler.handle(new Either.Left<>(error));
                return;
            }
            final String idStructure = structurePromise.future().result();
            paramsMethods.put(idStructure,new LinkedHashMap<>());
            for (final Map.Entry<String, List<String>> idclasse : repElevesPromise.future().result().entrySet()) {
                paramsMethods.get(idStructure).put(idclasse.getKey(),idclasse.getValue());
            }
            handler.handle(new Either.Right<>(paramsMethods));
        });
    }

    public void checkHeadTeacherForBFC(UserInfos user, String id_eleve, String id_etablissement,
                                       final Handler<Boolean> handler) {
        WorkflowActionUtils.hasHeadTeacherRight(user, null, null, null,
                new JsonArray().add(id_eleve), eb, id_etablissement, new Handler<Either<String, Boolean>>() {
                    @Override
                    public void handle(Either<String, Boolean> event) {
                        Boolean isHeadTecher;
                        if(event.isLeft()) {
                            isHeadTecher = false;
                        }
                        else {
                            isHeadTecher = event.right().getValue();
                        }
                        // on autorise si la personne est prof principal ou que c'est un chef etab
                        handler.handle(isHeadTecher ||
                                new WorkflowActionUtils().hasRight(user, WorkflowActions.ADMIN_RIGHT.toString()));
                    }
                });
    }

    private void getNameEntity(String[] name, String fieldUsed, Long idPeriode, JsonObject result,
                               Handler<Either<String, JsonObject>> handler){
        utilsService.getNameEntity(name, fieldUsed, nameEvent -> {
            if (nameEvent.isRight()) {
                final String structureName = nameEvent.right().getValue().getJsonObject(0)
                        .getString(NAME).replace(BLANK_SPACE, UNDERSCORE);
                result.put(NAME, structureName).put(ID_PERIODE, idPeriode);
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left(nameEvent.left().getValue()));
                log.error("getNameEntity : Unable to get the name of the specified entity (" + fieldUsed + ").");
            }
        });
    }

    public void generateBFC(final String idStructure, final JsonArray idClassesArray, final JsonArray idElevesArray,
                            final Long idCycle, final Long idPeriode, Vertx vertx, final Handler<Either<String, JsonObject>> handler){
        final List<String> idClasses = idClassesArray.getList();
        final List<String> idEleves = idElevesArray.getList();

        try {
            getParamBFC(idStructure, idClasses, idEleves, idPeriode, idCycle, event -> {
                if (event.isLeft()) {
                    handler.handle(new Either.Left(event.left()));
                    log.error("getParamBFC : Unable to gather parameters, parameter unknown.");
                    return;
                }

                final String idStructureGot = event.right().getValue().entrySet().iterator().next().getKey();
                final Map<String, List<Eleve>> classes = event.right().getValue().entrySet().iterator().next().getValue();

                getBFCParClasse(classes, idStructureGot, idPeriode, idCycle, vertx, bfcEnvent -> {
                    if (bfcEnvent.isLeft()) {
                        handler.handle(new Either.Left(bfcEnvent.left()));
                        log.error("getBFC : Unable to get BFC for the specified parameters.");
                        return;
                    }
                    final JsonObject result = new JsonObject().put(CLASSES, bfcEnvent.right().getValue());

                    if (idStructure != null) {
                        getNameEntity(new String[]{idStructureGot}, ID_STRUCTURE_KEY, idPeriode, result, handler);
                    } else if (!idClasses.isEmpty()) {
                        getNameEntity(classes.keySet().toArray(new String[1]), ID_CLASSE_KEY, idPeriode, result,
                                handler);
                    } else {
                        getNameEntity(idEleves.toArray(new String[1]), ID_ELEVE_KEY, idPeriode, result, handler);
                    }
                });
            });
        }
        catch (Exception e){
            log.error("generateBFC : " +e.getMessage() + "\n stack : " + e.getStackTrace());
        }
    }

    /**
     * Se charge d'appeler les methodes permettant la recuperation des parametres manquants en fonction du parametre
     * fournit.
     * Appelle  getParamStruct(String,Long, Handler)} si seul l'identifiant de la structure est fourni.
     * Appelle {@link #getParamEleves(List, String,Long, Handler)} si seuls les identifiants d'eleves sont fournis.
     *
     * @param idStructure Identifiant de la structure dont on souhaite generer le BFC.
     * @param idClasses   Identifiants des classes dont on souhaite generer le BFC.
     * @param idEleves    Identifiants des eleves dont on souhaite generer le BFC.
     * @param handler     Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamBFC(final String idStructure, final List<String> idClasses, final List<String> idEleves,
                             final Long idPeriode, Long idCycle,
                             final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {

        if (idStructure != null && (idEleves == null || idEleves.isEmpty()) ) {
            getParamStruct(idStructure, idPeriode, event -> {
                if (event.isRight()) {
                    handler.handle(new Either.Right<>(event.right().getValue()));
                } else {
                    handler.handle(new Either.Left<>(event.left().getValue()));
                    log.error("getParamStruct : failed to get related idClasses and/or idEleves.");
                }
            });
            return;
        }

        if (!idClasses.isEmpty()) {
            getParamClasses(idClasses, idPeriode, idCycle, event -> {
                if (event.isRight()) {
                    handler.handle(new Either.Right<>(event.right().getValue()));
                } else {
                    handler.handle(new Either.Left<>(event.left().getValue()));
                    log.error("getParamClasses : failed to get related idStructure and/or idEleves.");
                }
            });
            return;
        }

        if (!idEleves.isEmpty()) {
            getParamEleves(idEleves, idStructure, idCycle, event -> {
                if (event.isRight()) {
                    handler.handle(new Either.Right<>(event.right().getValue()));
                } else {
                    handler.handle(new Either.Left<>(event.left().getValue()));
                    log.error("getParamEleves : failed to get related idStructure and/or idClasses.");
                }
            });
        } else {
            handler.handle(new Either.Left<>("Aucun parametre renseigne."));
            log.error("getParamBFC : called with more than one null parameter.");
        }
    }


    private void getPrefixPdfNameResult(Long idPeriode, final String host,
                                        final String acceptLanguage, Promise<String> periodeNamePromise){

        if (idPeriode == null) {
            periodeNamePromise.complete(StringUtils.EMPTY_STRING);
        }
        else {
            JsonObject jsonRequest = new JsonObject()
                    .put("headers", new JsonObject().put(ACCEPT_LANGUAGE,  acceptLanguage))
                    .put(HOST, host);
            JsonObject action = new JsonObject()
                    .put(ACTION, "periode.getLibellePeriode")
                    .put("idType", idPeriode)
                    .put("request", jsonRequest);

            eb.request(Competences.VIESCO_BUS_ADDRESS, action, DELIVERY_OPTIONS,
                    handlerToAsyncHandler(message -> {
                        JsonObject body = message.body();
                        if (OK.equals(body.getString(STATUS))) {
                            String periodeNameStr = body.getString(RESULT);
                            periodeNameStr = periodeNameStr.replace(" ", "_");
                            periodeNamePromise.complete(periodeNameStr);
                        } else {
                            periodeNamePromise.fail(body.getString(MESSAGE));
                            log.error("getPeriode : Unable to get the label of the specified entity (idPeriode).");
                        }
                    }));
        }
    }
    public void generateBFCExport(final Long idPeriode, final String idStructure, final JsonArray idClasses,
                                  final JsonArray idEleves, final Long idCycle,
                                  final String host, final String acceptLanguage, Vertx vertx, JsonObject config,
                                  Promise<JsonObject> exportResultPromise, Promise<String> periodeNameResultPromise){
        getPrefixPdfNameResult(idPeriode, host, acceptLanguage, periodeNameResultPromise);
        generateBFC(idStructure, idClasses, idEleves, idCycle, idPeriode, vertx, exportResultEvent ->
                formate(exportResultPromise, exportResultEvent));
    }

    /**
     * Recupere les parametres manquant afin de pouvoir generer le BFC dans le cas ou seul des identifiants d'eleves
     * sont fournis.
     * @param idEtablissement Identifiant de l'établissement du modèle
     * @param idEleves Identifiants des eleves dont on souhaite generer le BFC.
     * @param handler  Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamEleves(final List<String> idEleves, String idEtablissement, Long idCycle,
                                final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {
        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();
        Utils.getInfoEleve(eb, idEleves.toArray(new String[1]), idCycle, idEtablissement, infoEvent -> {
            if (infoEvent.isRight()) {
                final Map<String, List<Eleve>> classes = new LinkedHashMap<>();
                for (Eleve e : infoEvent.right().getValue()) {
                    if (!classes.containsKey(e.getIdClasse())) {
                        classes.put(e.getIdClasse(), new ArrayList());
                    }
                    classes.get(e.getIdClasse()).add(e);
                }
                Utils.getStructClasses(eb, classes.keySet().toArray(new String[0]), StructEvent -> {
                    if (StructEvent.isRight()) {
                        population.put(StructEvent.right().getValue(), classes);
                        handler.handle(new Either.Right(population));
                    } else {
                        handler.handle(new Either.Left(StructEvent.left().getValue()));
                        log.error("getParamEleves : getStructClasses : " + StructEvent.left().getValue());
                    }
                });
            } else {
                handler.handle(new Either.Left(infoEvent.left().getValue()));
                log.error("getParamEleves : getInfoEleve : " + infoEvent.left().getValue());
            }
        });

    }
    private void formatConversionNote(String idStructure, Map.Entry<String, List<Eleve>> classe,
                                      final Map<String, JsonObject> result,final Map<Integer, String> libelleEchelle,
                                      JsonArray conversion){
        if (conversion.size() == 0) {
            String logError = "getBFC : getConversionNoteCompetence (" +
                    classe.getValue().get(0).getNomClasse() + ", " +
                    classe.getKey() + ") : aucune echelle de conversion pour cette classe.";
            log.error(logError);
            return;
        }

        for (int i = 0; i <conversion.size(); i++) {
            JsonObject _o = conversion.getJsonObject(i);
            libelleEchelle.put(_o.getInteger(ORDRE), _o.getString(LIBELLE));
        }
        for (Eleve e : classe.getValue()) {
            e.setLibelleNiveau(libelleEchelle);
        }
    }
    private void getConversionNoteCompetenceClasse(String idStructure, Map.Entry<String, List<Eleve>> classe,
                                                   final Map<String, JsonObject> result,
                                                   final Map<Integer, String> libelleEchelle,
                                                   final Handler<Either<String, JsonArray>> handler){

        competenceNoteService.getConversionNoteCompetence(idStructure, classe.getKey(),  event -> {
            if (event.isRight()) {
                formatConversionNote(idStructure,classe, result, libelleEchelle, event.right().getValue());
                handler.handle(event);
            } else {
                String error = event.left().getValue();
                log.error(error);

                if(error.contains(TIME)){
                    getConversionNoteCompetenceClasse(idStructure,classe, result, libelleEchelle, handler);
                }
            }
        });
    }

    private void formatEleve (JsonObject eleveJson, String idClasse, Promise<Void> formatPromise) {
        eleveJson.put(CLASSE_NAME_TO_SHOW, eleveJson.getValue(NOM_CLASSE));
        exportBulletinService.setLevel(eleveJson);
        exportBulletinService.setBirthDate(eleveJson);
        eleveJson.put("familyVisa", getLibelle("evaluations.export.bulletin.visa.libelle"))
                .put("signature", getLibelle("evaluations.export.bulletin.date.name.visa.responsable"))
                .put("ceVisa", getLibelle("evaluations.export.bfc.visa.ce.libelle"))
                .put("headteacherVisa", getLibelle("evaluations.export.bfc.visa.headteacher.libelle"))
                .put("signatureSample",getLibelle("evaluations.add.file.signature"))
                .put("bornAt", getLibelle("born.on"))
                .put("classeOf", getLibelle("classe.of"))
                .put("today",new SimpleDateFormat("dd/MM/yyyy").format(new Date().getTime()));

        final String idEleve = eleveJson.getString(ID_ELEVE_KEY);

        Promise<JsonObject> structurePromise = Promise.promise();
        exportBulletinService.getStructure(idEleve, eleveJson, st -> formate(structurePromise, st));

        Promise<JsonObject> anneeScolairePromise = Promise.promise();
        exportBulletinService.getAnneeScolaire(idEleve, idClasse, eleveJson, year ->
                formate(anneeScolairePromise, year));

        Promise<JsonObject> headTeachersPromise = Promise.promise();
        exportBulletinService.getHeadTeachers(idEleve, idClasse, eleveJson, hdTeacher ->
                formate(headTeachersPromise, hdTeacher));

        Future.all(structurePromise.future(), anneeScolairePromise.future(), headTeachersPromise.future()).onComplete(
                event -> {
                    if(event.failed()) {
                        log.error(event.cause().getMessage());
                    }
                    formatPromise.complete();
                }
        );
    }

    private void getEnsComplByStudent(Map<String, Map<String, Long>> niveauEnseignementComplementEleve,
                                      Map<String, Map<String, Long>> niveauLangueCultureRegionaleEleve,
                                      JsonArray niveauEnseignementComplementEleveResultArray ) {
        for (int i = 0; i < niveauEnseignementComplementEleveResultArray.size(); i++) {
            JsonObject _o = niveauEnseignementComplementEleveResultArray.getJsonObject(i);
            String idEleve = _o.getString(ID_ELEVE);
            if (!niveauEnseignementComplementEleve.containsKey(idEleve)) {
                niveauEnseignementComplementEleve.put(idEleve, new HashMap<>());
            }
            if (!niveauLangueCultureRegionaleEleve.containsKey(idEleve)) {
                niveauLangueCultureRegionaleEleve.put(idEleve, new HashMap<>());
            }
            niveauEnseignementComplementEleve.get(idEleve).put(_o.getString(LIBELLE), _o.getLong("niveau"));
            niveauLangueCultureRegionaleEleve.get(idEleve).put(_o.getString("libelle_lcr"),
                    _o.getLong("niveau_lcr"));
        }

    }
    private void buildBFCParClasse(final List<String> idEleves,
                                   final String idStructure,final Long idPeriode,final Long idCycle,
                                   Map.Entry<String, List<Eleve>> classe, final Map<String, JsonObject> result,
                                   final Map<String, Map<Long, Integer>> resultatsEleves, Vertx vertx,
                                   final Handler<Either<String, JsonArray>> handler){

        final Map<Integer, String> libelleEchelle = new HashMap<>();

        Promise conversionNotePromise = Promise.promise();
        getConversionNoteCompetenceClasse(idStructure, classe, result, libelleEchelle,
                event -> formate(conversionNotePromise, event ));

        Promise domaineRacinePromise = Promise.promise();
        getDomainesRacinesByClass(classe, idEleves, idCycle,
                event -> formate(domaineRacinePromise, event ));

        Promise<JsonObject> buildBfcPromise = Promise.promise();
        final Boolean recapEvalForBfc = false;
        buildBFC(recapEvalForBfc, idEleves.toArray(new String[0]), classe.getKey(), idStructure, idPeriode, idCycle,
                false, buildBfcEvent -> formate(buildBfcPromise, buildBfcEvent));

        Promise<JsonArray> listCplByElevePromise = Promise.promise();
        eleveEnseignementComplementService.listNiveauCplByEleves(idEleves.toArray(new String[1]),
                eventNCPL -> formate(listCplByElevePromise, eventNCPL));

        Promise<JsonArray> synthesePromise = Promise.promise();
        bfcSynthseService.getBfcSyntheseByIdsEleveAndClasse(idEleves.toArray(new String[1]), classe.getKey(),
                repSynthese -> formate(synthesePromise, repSynthese));

        Future.all(buildBfcPromise.future(), listCplByElevePromise.future(), synthesePromise.future(), conversionNotePromise.future(),
                domaineRacinePromise.future()).onComplete(event -> {
                    if(event.failed()){
                        String error = classe.getValue().get(0).getNomClasse() + ";\n" + event.cause().getMessage();
                        collectBFCEleve(classe.getKey(), new JsonObject().put(ERROR, error), result, handler);
                        log.error("getBFC:(Array of idEleves, " + classe.getKey() + ", " + idStructure +") : " + error);
                        return ;
                    }

                    // On récupère les enseignements de complément par élève
                    JsonArray niveauEnseignementComplementEleveResultArray = listCplByElevePromise.future().result();
                    Map<String, Map<String, Long>> niveauEnseignementComplementEleve = new HashMap<>();
                    Map<String, Map<String, Long>> niveauLangueCultureRegionaleEleve = new HashMap<>();
                    getEnsComplByStudent( niveauEnseignementComplementEleve,niveauLangueCultureRegionaleEleve,
                            niveauEnseignementComplementEleveResultArray );

                    // On récupère les synthèses des bfcs par cycle par élève
                    Map<String, String> syntheseEleve = new HashMap<>();
                    JsonArray syntheseEleveResultArray = synthesePromise.future().result();
                    for (int i = 0; i < syntheseEleveResultArray.size(); i++) {
                        JsonObject _o = syntheseEleveResultArray.getJsonObject(i);
                        String id_eleve = _o.getString(ID_ELEVE);
                        if (!syntheseEleve.containsKey(id_eleve)) {
                            syntheseEleve.put(id_eleve, _o.getString("texte"));
                        }
                    }

                    // On récupère les résultats par domaine par élève
                    JsonObject resultByDomaine = buildBfcPromise.future().result();
                    for (int i = 0; i <idEleves.size() ; i++) {
                        JsonArray resultats = resultByDomaine.getJsonArray(idEleves.get(i));
                        Map<Long, Integer> resultEleves = new HashMap<>();
                        if (resultats != null) {
                            for (Object resultat : resultats) {
                                resultEleves.put(((JsonObject) resultat).getLong("idDomaine"),
                                        ((JsonObject) resultat).getInteger("niveau"));
                            }
                        }

                        resultatsEleves.put(idEleves.get(i), resultEleves);
                    }

                    List<Eleve> elevesNonEvalues = new ArrayList<Eleve>();
                    // On modifie l'objet élève avec les informations récupérées précédemment
                    for (Eleve e : classe.getValue()) {
                        Map<Long, Integer> notes = resultatsEleves.get(e.getIdEleve());
                        Map<String, Long> enseignmentComplements = niveauEnseignementComplementEleve.get(e.getIdEleve());
                        Map<String, Long> langueCultureRegionale = niveauLangueCultureRegionaleEleve.get(e.getIdEleve());
                        String syntheseCycle = syntheseEleve.get(e.getIdEleve());

                        e.setNotes(notes);
                        e.setEnseignmentComplements(enseignmentComplements);
                        e.setLangueCultureRegionale(langueCultureRegionale);
                        e.setSyntheseCycle(syntheseCycle);

                        if(notes.size() == 0 && enseignmentComplements == null &&
                                langueCultureRegionale == null && syntheseCycle == null){
                            elevesNonEvalues.add(e);
                        }
                    }
                    classe.getValue().removeAll(elevesNonEvalues);

                    JsonArray eleves = formatBFC(classe.getValue());
                    final JsonArray classeResult = sortElevesByDisplayName(eleves);
                    if(classe.getValue().size() != 0) {
                        final String idClasse = classe.getValue().get(0).getIdClasse();
                        if (classeResult != null) {
                            List<Future<?>> listeFutures = new ArrayList<>();
                            for (int i = 0; i < classeResult.size(); i++) {
                                JsonObject eleveJson = classeResult.getJsonObject(i);
                                eleveJson.put(ID_ETABLISSEMENT_KEY, idStructure);
                                // formatEleve se charge de lancer la récupération des informations manquants et rajoute
                                // les appels dans la listeFutures
                                Promise<Void> formatElevePromise = Promise.promise();
                                listeFutures.add(formatElevePromise.future());
                                formatEleve(eleveJson, idClasse, formatElevePromise);
                            }

                            // Récupération du logo de l'établissment
                            Promise<JsonObject> imageStructurePromise = Promise.promise();
                            utilsService.getParametersForExport(idStructure, img ->
                                    formate(imageStructurePromise, img));
                            listeFutures.add(imageStructurePromise.future());

                            // Une fois la récupération des informations de tous les élèves
                            Future.all(listeFutures).onComplete(listeEvent -> {
                                if (listeEvent.failed()) {
                                    String error = "Une erreur est survenue lors de la recuperation des adresses et de " +
                                            "l'image de l'établissement : " + classe.getValue().get(0).getNomClasse()
                                            + ";\n" + event.toString();
                                    collectBFCEleve(classe.getKey(), new JsonObject().put(ERROR, error), result, handler);
                                    log.error("getBFC: buildBFC (Array of idEleves, " + classe.getKey() + ", "
                                            + idStructure + ") : " + event.toString());
                                } else {
                                    List<Future<JsonObject>> futures = new ArrayList<>();
                                    for (Object eleve : classeResult) {
                                        JsonObject eleveJson = ((JsonObject) eleve);
                                        JsonObject imgStructure = imageStructurePromise.future().result();
                                        eleveJson.put("pathLogoImg", "img/education_nationale.png");
                                        if (imgStructure != null && imgStructure.containsKey("imgStructure")) {
                                            eleveJson.put("imgStructure", imgStructure.getJsonObject("imgStructure")
                                                    .getValue(PATH));
                                            eleveJson.put("hasImgStructure", true);
                                        }
                                        if (imgStructure != null && imgStructure.containsKey("nameAndBrad")) {
                                            eleveJson.put("nameCE", imgStructure.getJsonObject("nameAndBrad")
                                                    .getValue(NAME));
                                            eleveJson.put("imgSignature", imgStructure.getJsonObject("nameAndBrad")
                                                    .getValue(PATH));
                                            eleveJson.put("hasNameAndBrad", true);
                                        }
                                        Promise<JsonObject> getImagesBase64Promise = Promise.promise();
                                        futures.add(getImagesBase64Promise.future());
                                        exportBulletinService.generateImagesFromPathForBulletin(eleveJson, vertx,
                                                eventImage -> {
                                                    formate(getImagesBase64Promise, eventImage);
                                                } );
                                    }
                                    Future.all(futures).onComplete(eventFutureStudent -> {

                                        collectBFCEleve(classe.getKey(), new JsonObject().put(ELEVES, classeResult), result,
                                                handler);
                                    });

                                }
                            });
                        }
                    }
                }
        );

    }
    private void getDomainesRacinesByClass(Map.Entry<String, List<Eleve>> classe,
                                           final List<String> idEleves, final Long idCycle,
                                           final Handler<Either<String, JsonArray>> handler){

        Promise<JsonArray> domsPromise = Promise.promise();

        domaineService.getDomainesRacines(classe.getKey(), idCycle, event -> formate(domsPromise, event));

        Promise<Map<String, Map<Long, Boolean>>> dispenseDomainePromise = Promise.promise();
        dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(idEleves, event ->
                formate(dispenseDomainePromise, event));

        Future.all(domsPromise.future(), dispenseDomainePromise.future()).onComplete(event -> {
            if(event.failed()){
                String error = event.cause().getMessage();
                if(error.contains(TIME)){
                    getDomainesRacinesByClass(classe, idEleves, idCycle, handler);
                    return;
                }
                handler.handle(new Either.Right<>((new JsonArray())));
                log.error("getDomainesRacinesByClasse (" + classe.getKey() + ") : " + error);
                return ;
            }
            final JsonArray queryResult = domsPromise.future().result();
            Map<String, Map<Long, Boolean>> mapIdsElevesIdsDomainesDispenses = dispenseDomainePromise.future().result();

            if (queryResult.size() == 0) {
                String error = "Une erreur est survenue lors de la recuperation des domaines pour " +
                        "la classe " + classe.getValue().get(0).getNomClasse() +
                        " : aucun domaine racine pour cette classe.";
                log.error(error);
                handler.handle(new Either.Right<>((new JsonArray())));
                return;
            }
            //On récupère les domaines dispensés pour tous les élèves de la classe

            for (final Eleve e : classe.getValue()) {
                final Map<Long, Map<String, String>> domainesRacines = new LinkedHashMap<>();
                for (int i = 0; i < queryResult.size(); i++) {
                    final JsonObject domaine = queryResult.getJsonObject(i);
                    final Map<String, String> infoDomaine = new HashMap<>();
                    infoDomaine.put(ID_KEY, String.valueOf(domaine.getLong(ID_KEY)));
                    infoDomaine.put(CODIFICATION, domaine.getString(CODIFICATION));
                    infoDomaine.put(LIBELLE, domaine.getString(LIBELLE));

                    //On vérifie si l'id de l'élève en cours est ds la map des élève
                    // qui sont dispensés pour un domaine
                    if (mapIdsElevesIdsDomainesDispenses.containsKey(e.getIdEleve())) {
                        Map<Long, Boolean> idsDomainesDomaine = mapIdsElevesIdsDomainesDispenses
                                .get(e.getIdEleve());
                        Long idInfoDomaine = Long.valueOf(infoDomaine.get(ID_KEY));
                        if (idsDomainesDomaine.containsKey(idInfoDomaine)) {
                            infoDomaine.put("dispense", String.valueOf(
                                    idsDomainesDomaine.get(idInfoDomaine)));
                        }
                    }
                    domainesRacines.put(Long.valueOf(infoDomaine.get(ID_KEY)), infoDomaine);
                }
                e.setDomainesRacines(domainesRacines);
            }

            handler.handle(new Either.Right<>((new JsonArray())));
        });
    }
    /**
     * Recupere pour chaque classe l'echelle de conversion des moyennes, les domaines racines a evaluer ainsi que les
     * notes des eleves.
     * Appelle les 3 services simultanement pour chaque classe, ajoutant l'information renvoyee par le service a chaque
     * Eleve de la classe, puis appelle {@link #collectBFCEleve(String, JsonObject, Map, Handler) collectBFCEleve} avec son propre
     * handler afin de renseigner une reponse si la classe est prete a etre exporter.
     *
     * @param classes     L'ensemble des Eleves, rassembles par classe et indexant en fonction de l'identifiant de la
     *                    classe
     * @param idStructure L'identifiant de la structure. Necessaire afin de recuperer l'echelle de conversion.0
     * @param idPeriode   L'identifiant de la periode pour laquelle on souhaite recuperer le BFC.
     * @param idCycle     L'identifiant du cycle pour lequel on souhaite recuperer le BFC.
     * @param handler     Handler contenant le BFC final.
     * @see Eleve
     */
    private void getBFCParClasse(final Map<String, List<Eleve>> classes, final String idStructure, Long idPeriode,
                                 Long idCycle, Vertx vertx, final Handler<Either<String, JsonArray>> handler) {
        // Contient toutes les classes sous forme JsonObject, indexant en fontion de l'identifiant de la classe
        // correspondante.

        if(idStructure == null ){
            log.info(" \n  getBFCParClasse idSTRUCTURE NULL  \n " );
        }
        final Map<String, JsonObject> result = new LinkedHashMap<>();

        // La map result avec les identifiants des classes, contenus dans "classes", afin de s'assurer qu'aucune ne
        // manque.
        for (String s : classes.keySet()) {
            result.put(s, null);
        }

        if(classes.isEmpty()) {
            log.error(" EMPTY getBFCParClasse ");
            handler.handle(new Either.Right<>(new JsonArray()));
        }

        for (final Map.Entry<String, List<Eleve>> classe : classes.entrySet()) {
            final Map<String, Map<Long, Integer>> resultatsEleves = new HashMap<>();
            final List<String> idEleves = new ArrayList<>();

            // La liste des identifiants des Eleves de la classe est necessaire pour "buildBFC"
            for (Eleve e : classe.getValue()) {
                idEleves.add(e.getIdEleve());
            }

            buildBFCParClasse(idEleves, idStructure, idPeriode, idCycle, classe, result, resultatsEleves, vertx, handler);
        }
    }

    /**
     * Recupere les parametres manquant afin de pouvoir generer le BFC dans le cas ou seul l'identifiant de la
     * structure est fourni.
     *
     * @param idStructure Identifiant de la structure dont on souhaite generer le BFC.
     * @param idPeriode  Identifiant de la période
     * @param handler     Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamStruct(final String idStructure, final long idPeriode,
                                final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {

        final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();
        population.put(idStructure, new LinkedHashMap());

        Utils.getClassesStruct(eb, idStructure, ClassesEvent -> {
            if (ClassesEvent.isRight()) {
                final List<String> classes = ClassesEvent.right().getValue();
                Utils.getElevesClasses(eb, classes.toArray(new String[0]), idPeriode, ElevesEvent -> {
                    if (ElevesEvent.isRight()) {
                        for (final Map.Entry<String, List<String>> classe : ElevesEvent.right().getValue().entrySet()){
                            population.get(idStructure).put(classe.getKey(), null);
                            Utils.getInfoEleve(eb, classe.getValue().toArray(new String[0]),null,
                                    idStructure, event -> {
                                        if (event.isRight()) {
                                            population.get(idStructure).put(classe.getKey(), event.right().getValue());
                                            // Si population.get(idStructure).values() contient une valeur null,
                                            // cela signifie qu'une classe n'a pas encore recupere sa liste d'eleves
                                            if (!population.get(idStructure).values().contains(null)) {
                                                handler.handle(new Either.Right(population));
                                            }
                                        } else {
                                            handler.handle(new Either.Left(event.left().getValue()));
                                            log.error("getParamStruct : getInfoEleve : " + event.left().getValue());
                                        }
                                    });
                        }
                    } else {
                        handler.handle(new Either.Left(ElevesEvent.left().getValue()));
                        log.error("getParamStruct : getElevesClasses : " + ElevesEvent.left().getValue());
                    }
                });
            } else {
                handler.handle(new Either.Left(ClassesEvent.left().getValue()));
                log.error("getParamStruct : getClassesStruct : " + ClassesEvent.left().getValue());
            }
        });
    }

    private void getInfoEleveForClasses(String[] idStudents, String idStructure, String idClasse,
                                        Map<String, Map<String, List<Eleve>>> population,
                                        final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {
        Utils.getInfoEleve(eb, idStudents,null, idStructure, info -> {
            if (info.isRight()) {
                if(isNull(idStructure)){
                    log.error(" getInfoEleveForClasses null ");
                }
                population.get(idStructure).put(idClasse, info.right().getValue());
                // Si population.get(idStructure).values() contient une valeur null,
                // cela signifie qu'une classe n'a pas encore recupere sa liste d'eleves
                if (!population.get(idStructure).values().contains(null)) {
                    handler.handle(new Either.Right(population));
                } else {
                    log.error(" not get yet population ");
                    handler.handle(new Either.Right(population));
                }
            } else {
                String error = info.left().getValue();
                log.error("getParamClasses : getInfoEleve : " + error);
                if(error.contains(TIME)){
                    getInfoEleveForClasses(idStudents, idStructure, idClasse, population, handler);
                    return;
                }
                handler.handle(new Either.Left<>(error));
            }
        });
    }

    /**
     * Recupere les parametres manquant afin de pouvoir generer le BFC dans le cas ou seul des identifiants de classes
     * sont fournis.
     *
     * @param idClasses Identifiants des classes dont on souhaite generer le BFC.
     * @param handler   Handler contenant les listes des eleves, indexees par classes.
     */
    private void getParamClasses(final List<String> idClasses, final Long idPeriode, final Long idCycle,
                                 final Handler<Either<String, Map<String, Map<String, List<Eleve>>>>> handler) {
        // Récupération de l'idStructure  de la classe
        Promise<String> structurePromise = Promise.promise();
        Utils.getStructClasses(eb, idClasses.toArray(new String[0]),
                event -> formate(structurePromise, event));

        // Récupération des élèves de la classe
        Promise<Map<String, JsonArray>> studentsPromise = Promise.promise();
        getClassesEleves(eb, idClasses.toArray(new String[0]), idPeriode,
                event -> formate(studentsPromise, event));

        Future.all(structurePromise.future(), studentsPromise.future()).onComplete(event -> {
            if (event.failed()) {
                String error = event.cause().getMessage();
                log.error("getParamClasses : " + error);
                handler.handle(new Either.Left<>(error));
                return;
            }

            final String idStructure = structurePromise.future().result();
            final Map<String, Map<String, List<Eleve>>> population = new HashMap<>();
            population.put(idStructure, new LinkedHashMap());

            Map<String, JsonArray> mapEleves = studentsPromise.future().result();
            if(mapEleves.isEmpty()){
                log.info("getParamClasses  NO student ");
                handler.handle(new Either.Right(population));
                return;
            }

            for (Map.Entry<String, JsonArray> classe : mapEleves.entrySet()) {
                final String idClasse = classe.getKey();
                JsonArray eleves = classe.getValue();

                population.get(idStructure).put(idClasse, null);
                List<Eleve> eleveList = toListEleve(eleves);
                Utils.getCycleElevesForBfcCycle(mapEleves.keySet(), idCycle, eleveList, cycle -> {

                });

                population.get(idStructure).put(idClasse, eleveList);
            }

            // Si population.get(idStructure).values() contient une valeur null,
            // cela signifie qu'une classe n'a pas encore recupere sa liste d'eleves
            if (!population.get(idStructure).values().contains(null)) {
                handler.handle(new Either.Right(population));
            } else {
                log.error(" not get yet population ");
                handler.handle(new Either.Right(population));
            }
        });
    }

    /**
     * Retourne un JsonArray contenant les JsonObject de chaque Eleve passe en parametre, seulement si tous les Eleves
     * sont prets.
     * Les Eleves sont pret lorsque la fonction <code>Eleve.isReady()</code> renvoit true. Dans le cas
     * contraire, la fonction s'arrete en retournant null.
     *
     * @param classe La liste des Eleves de la classe.
     * @return Un JsonArray contenant les JsonObject de tous les Eleves de la classe; null si un
     * Eleve n'est pas pret.
     * @see Eleve
     */
    private JsonArray formatBFC(List<Eleve> classe) {
        JsonArray result = new fr.wseduc.webutils.collections.JsonArray();

        for (Eleve eleve : classe) {
            if (!eleve.isReady()) {
                return null;
            }
            result.add(eleve.toJson());
        }

        return result;
    }

    /**
     * Ajoute le JsonObject a <code>collection</code>, et si les JsonObject de toutes les classes ont ete
     * renseignees dans <code>collection</code>, assemble tous JsonObject au sein d'un JsonArray qui sera fournit
     * au handler.
     *
     * @param key        Identifiant de la classe a ajoute dans <code>collection</code>.
     * @param value      JsonObject de la classe a ajoute dans <code>collection</code>.
     * @param collection Map des JsonObject de toutes les classes, indexant par leur identifiant.
     * @param handler    Handler manipulant le JsonArray lorsque celui-ci est assemble.
     */
    private void collectBFCEleve(String key, JsonObject value, Map<String, JsonObject> collection,
                                 Handler<Either<String, JsonArray>> handler) {
        if (!collection.values().contains(null)) {
            return;
        } else {
            collection.put(key, value);
        }

        // La collection est initilisee avec les identifiants de toutes les classes, associes avec une valeur null.
        // Ainsi, s'il demeure un null dans la collection, c'est que toutes les classes ne possede pas encore
        // leur JsonObject.
        if (!collection.values().contains(null)) {
            JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
            for (JsonObject classe : collection.values()) {
                result.add(classe);
            }
            handler.handle(new Either.Right(result));
        }
    }
}
