package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.Utils;
import fr.openent.competences.bean.Domaine;
import fr.openent.competences.service.*;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;

import static org.entcore.common.sql.SqlResult.validRowsResultHandler;

/**
 * Created by vogelmt on 29/03/2017.
 */
public class DefaultBFCService extends SqlCrudService implements BFCService {

    private EventBus eb;

    private CompetenceNoteService competenceNoteService;
    private DomainesService domaineService;
    private CompetencesService competenceService;
    private DispenseDomaineEleveService dispenseDomaineEleveService;
    private static final Logger log = LoggerFactory.getLogger(DefaultBFCService.class);

    public DefaultBFCService(EventBus eb) {
        super(Competences.COMPETENCES_SCHEMA, Competences.BFC_TABLE);
        this.eb = eb;
        competenceNoteService = new DefaultCompetenceNoteService(Competences.COMPETENCES_SCHEMA, Competences.COMPETENCES_NOTES_TABLE);
        domaineService = new DefaultDomaineService(Competences.COMPETENCES_SCHEMA, Competences.DOMAINES_TABLE);
        competenceService = new DefaultCompetencesService(eb);
        dispenseDomaineEleveService = new DefaultDispenseDomaineEleveService(Competences.COMPETENCES_SCHEMA,Competences.DISPENSE_DOMAINE_ELEVE);
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
        data.remove("id");
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
    public void getBFCsByEleve(String[] idEleves, String idEtablissement, Long idCycle, Handler<Either<String,JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder query = new StringBuilder()
                .append("SELECT * ")
                .append("FROM notes.bilan_fin_cycle ")
                .append("INNER JOIN notes.domaines ON bilan_fin_cycle.id_domaine=domaines.id ")
                .append("WHERE bilan_fin_cycle.id_eleve IN " + Sql.listPrepared(idEleves))
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
    private void getDomaines(final String idClasse, final Handler<Either<String, Map<Long, Domaine>>> handler) {
        domaineService.getArbreDomaines(idClasse,null, null, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    if (event.right().getValue().size() == 0) {
                        handler.handle(new Either.Left<String, Map<Long, Domaine>>("Erreur lors de la recuperation des domaines : aucun domaine de competences n'a ete trouve."));
                        log.error("getDomaines (" + idClasse + ") : aucun domaine de competences n'a ete trouve.");
                    }
                    final Map<Long, Domaine> domaines = new HashMap<>();
                    JsonArray domainesResultArray = event.right().getValue();

                    for (int i = 0; i < domainesResultArray.size(); i++) {
                        JsonObject _o = domainesResultArray.getJsonObject(i);
                        Domaine _d = new Domaine(_o.getLong("id"), _o.getBoolean("evaluated"));
                        if (domaines.containsKey(_o.getLong("id_parent"))) {
                            Domaine parent = domaines.get(_o.getLong("id_parent"));
                            parent.addSousDomaine(_d);
                            _d.addParent(parent);
                        }
                        domaines.put(_d.getId(), _d);
                    }
                    if(!domaines.isEmpty()) {

                        competenceService.getCompetencesDomaines(idClasse, domaines.keySet().toArray(new Long[0]), new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if (event.isRight()) {
                                    if (event.right().getValue().size() == 0) {
                                        handler.handle(new Either.Left<String, Map<Long, Domaine>>("Erreur lors de la recuperation des competences pour les domaines : aucun competence pour les domaines selectionnes."));
                                        log.error("getDomaines : getCompetencesDomaines : aucun competence pour les domaines selectionnes.");
                                    }
                                    JsonArray competencesResultArray = event.right().getValue();

                                    for (int i = 0; i < competencesResultArray.size(); i++) {
                                        JsonObject _o = competencesResultArray.getJsonObject(i);

                                        domaines.get(_o.getLong("id_domaine")).addCompetence(_o.getLong("id_competence"));
                                    }
                                    handler.handle(new Either.Right<String, Map<Long, Domaine>>(domaines));
                                } else {
                                    handler.handle(new Either.Left<String, Map<Long, Domaine>>("Erreur lors de la recuperation des competences pour les domaines :\n" + event.left().getValue()));
                                    log.error("getDomaines : getCompetencesDomaines : " + event.left().getValue());
                                }
                            }
                        });
                    } else {
                        handler.handle(new Either.Left<String, Map<Long, Domaine>>("La classe " + idClasse + " n'est rattachee a aucun cycle."));
                        log.error("La classe " + idClasse + " n'est rattachée a aucun cycle.");
                    }

                } else {
                    handler.handle(new Either.Left<String, Map<Long, Domaine>>("Erreur lors de la recuperation des domaines :\n" + event.left().getValue()));
                    log.error("getDomaines (" + idClasse + ") : " + event.left().getValue());
                }
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
    private void getMaxNoteCompetenceEleve(final String[] idEleves, Long idPeriode, Long idCycle, final Handler<Either<String, Map<String, Map<Long, Long>>>> handler) {
        competenceNoteService.getMaxCompetenceNoteEleve(idEleves, idPeriode,idCycle, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle (Either <String, JsonArray> event){
                if (event.isRight()) {
                    Map<String, Map<Long, Long>> notesCompetencesEleve = new HashMap<>();

                    JsonArray notesResultArray = event.right().getValue();
                    for (int i = 0; i < notesResultArray.size(); i++) {
                        JsonObject _o = notesResultArray.getJsonObject(i);
                        String id_eleve = _o.getString("id_eleve");
                        if(_o.getLong("evaluation") < 0) {
                            continue;
                        }
                        if (!notesCompetencesEleve.containsKey(id_eleve)) {
                            notesCompetencesEleve.put(id_eleve, new HashMap<Long, Long>());
                        }
                        notesCompetencesEleve.get(id_eleve).put(_o.getLong("id_competence"), _o.getLong("evaluation"));
                    }
                    handler.handle(new Either.Right<String, Map<String, Map<Long, Long>>>(notesCompetencesEleve));
                } else {
                    handler.handle(new Either.Left<String, Map<String, Map<Long, Long>>>("Erreur lors de la recuperation des evaluations de competences :\n" + event.left().getValue()));
                    log.error("getMaxNoteCompetenceEleve : " + event.left().getValue());
                }
            }
        });
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
    private void getEchelleConversion(String idStructure, String idClasse, final Handler<Either<String, SortedSet<Double>>> handler) {
        competenceNoteService.getConversionNoteCompetence(idStructure, idClasse, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    if(event.right().getValue().size() == 0) {
                        handler.handle(new Either.Left<String, SortedSet<Double>>("Erreur lors de la recuperation de l'echelle de conversion : aucun echelle de conversion n'a ete trouvee."));
                        log.error("getEchelleConversion : aucun echelle de conversion n'a ete trouvee.");
                    }
                    SortedSet<Double> bornes = new TreeSet<>();
                    JsonArray conversion = event.right().getValue();

                    for (int i = 0; i < conversion.size(); i++) {
                        JsonObject _o = conversion.getJsonObject(i);
                        bornes.add(_o.getDouble("valmin").doubleValue());
                        bornes.add(_o.getDouble("valmax").doubleValue());
                    }
                    handler.handle(new Either.Right<String, SortedSet<Double>>(bornes));
                } else {
                    handler.handle(new Either.Left<String, SortedSet<Double>>("Erreur lors de la recuperation de l'echelle de conversion :\n" + event.left().getValue()));
                    log.error("getEchelleConversion : " + event.left().getValue());
                }
            }
        });
    }

    /**
     * Calcule la moyenne des eleves dont les evaluations sont passees en parametre, en fonction des domaines, des bornes
     * et des BFC passes en parametre.
     *
     * @param idEleves                Liste des identifiants d'élève
     * @param domainesRacine          liste des domaines racines pour lesquels evaluer les eleves
     * @param bornes                  set ordonne des bornes de niveaux de maitrise pour les evaluations
     * @param notesCompetencesEleves  map des evaluations de competence pour chaque eleve.
     * @param bfcEleves               map des evaluations effectuees manuellement par le chef etablissement pour chaque
     *                                eleve
     * @param handler                 handler contenant une map des niveaux de maitrise par domaine de chaque eleve
     */
    private void calcMoyBFC(boolean recapEval,
                            String[] idEleves,
                            Map<String, Map<Long, Integer>> bfcEleves,
                            Map<String, Map<Long, Long>> notesCompetencesEleves,
                            SortedSet<Double> bornes,
                            List<Domaine> domainesRacine,
                            Handler<Either<String, JsonObject>> handler) {

        if(domainesRacine.isEmpty() || bornes.isEmpty() || notesCompetencesEleves.isEmpty() || bfcEleves.isEmpty()) {
            //Si les domaines, les bornes, les BFCs ou les notes ne sont pas remplis, la fonction s'arrête sans avoir effectuer aucun traitement.
            return;
        } else if(notesCompetencesEleves.get("empty") != null && bfcEleves.get("empty") != null) {
            //Par contre, si les domaines et les bornes sont renseignées mais qu'aucune compétence n'a été évaluée, une réponse vide est retournée.
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject()));
            return;
        }

        JsonObject result = new JsonObject();

        for(String eleve : idEleves) {
            if(!bfcEleves.containsKey(eleve) && !notesCompetencesEleves.containsKey(eleve)) {
                continue;
            }

            JsonArray resultEleve = new fr.wseduc.webutils.collections.JsonArray();

            for (Domaine d : domainesRacine) {
                JsonObject note = new JsonObject();
                if (bfcEleves.containsKey(eleve) && bfcEleves.get(eleve).containsKey(d.getId()) &&
                        bfcEleves.get(eleve).get(d.getId()) >= bornes.first() && bfcEleves.get(eleve).get(d.getId()) <= bornes.last()) {
                        note.put("idDomaine",d.getId());
                        note.put("niveau", bfcEleves.get(eleve).get(d.getId()));
                    if(recapEval){
                        if(notesCompetencesEleves.get("empty") != null){
                            note.put("moyenne", bfcEleves.get(eleve).get(d.getId()));
                        } else {
                            Double moy = calculMoyenne(d, notesCompetencesEleves, eleve);
                            if (moy != null)
                                note.put("moyenne",Math.round(moy * 100.0) / 100.0);
                        }
                    }
                } else if (notesCompetencesEleves.containsKey(eleve)) {
                    Double moy = calculMoyenne(d, notesCompetencesEleves, eleve);
                    if (moy != null) {
                        Iterator<Double> bornesIterator = bornes.iterator();
                        int simplifiedMoy = 0;
                        while (moy >= bornesIterator.next() && bornesIterator.hasNext()) {
                            simplifiedMoy++;
                        }
                        if(simplifiedMoy >= bornes.first() && simplifiedMoy <= bornes.last()) {
                            note.put("idDomaine",d.getId());
                            note.put("niveau", simplifiedMoy);
                            if(recapEval)
                                note.put("moyenne", Math.round(moy * 100.0) / 100.0);
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
            JsonArray domainesR = new fr.wseduc.webutils.collections.JsonArray();
            for (Domaine d : domainesRacine) {
                domainesR.add(d.getId());
            }
            result.put("domainesRacine", domainesR);
        }

        handler.handle(new Either.Right<String, JsonObject>(result));
    }

    private Double calculMoyenne(Domaine d, Map<String, Map<Long, Long>> notesCompetencesEleves, String eleve){
        Long total = (long) 0;
        Long diviseur = (long) 0;
        for (Long idCompetence : d.getCompetences()) {
            if (notesCompetencesEleves.get(eleve) != null && notesCompetencesEleves.get(eleve).containsKey(idCompetence)) {
                total += notesCompetencesEleves.get(eleve).get(idCompetence) + 1;
                diviseur++;
            }
        }
        Double moy = (diviseur != 0 ? ((double) total / diviseur) : null);
        return moy;
    }

    @Override
    public void buildBFC(final boolean recapEval, final String[] idEleves, final String idClasse,
                         final String idStructure,
                         final Long idPeriode,final Long idCycle, final Handler<Either<String, JsonObject>> handler) {

        final Map<String, Map<Long, Long>> notesCompetencesEleve = new HashMap<>();
        final Map<String, Map<Long, Integer>> bfcEleve = new HashMap<>();
        final SortedSet<Double> echelleConversion = new TreeSet<>();
        final Map<Long, Domaine> domaines = new HashMap<>();
        final List<Domaine> domainesRacine = new ArrayList<>();

        // La fonction récupère les BFC existants pour chaque élèves, les domaines relatifs à la classe de ces élèves,
        // les notes maximum de ces élèves par compétence ainsi que l'échelle de conversion de ces notes simultanément,
        // mais n'effectue le calcul du BFC qu'une fois que ces 4 paramètres sont remplis.
        // Cette vérification de la présence des 4 paramètres est effectuée par calcMoyBFC().

        getMaxNoteCompetenceEleve(idEleves, idPeriode,idCycle, new Handler<Either<String, Map<String, Map<Long, Long>>>>() {
            @Override
            public void handle(Either<String, Map<String, Map<Long, Long>>> event) {
                if (event.isRight()) {
                    notesCompetencesEleve.putAll(event.right().getValue());
                    if (notesCompetencesEleve.isEmpty()) {
                        notesCompetencesEleve.put("empty", new HashMap<Long, Long>());
                    }
                    calcMoyBFC(recapEval, idEleves, bfcEleve, notesCompetencesEleve, echelleConversion, domainesRacine, handler);
                } else {
                    handler.handle(new Either.Left<String, JsonObject>("Impossible de recuperer les evaluations pour la classe selectionnee :\n" + event.left().getValue()));
                    log.error("buildBFC : getMaxNoteCompetenceEleve : " + event.left().getValue());
                }
            }
        });

        getBFCsByEleve(idEleves, idStructure,idCycle, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray bfcResultArray = event.right().getValue();

                    for (int i = 0; i < bfcResultArray.size(); i++) {
                        JsonObject _o = bfcResultArray.getJsonObject(i);
                        if (_o.getInteger("valeur") < 0) {
                            continue;
                        }
                        if (!bfcEleve.containsKey(_o.getString("id_eleve"))) {
                            bfcEleve.put(_o.getString("id_eleve"), new HashMap<Long, Integer>());
                        }
                        bfcEleve.get(_o.getString("id_eleve")).put(_o.getLong("id_domaine"), _o.getInteger("valeur"));
                    }

                    if (bfcEleve.isEmpty()) {
                        bfcEleve.put("empty", new HashMap<Long, Integer>());
                        // Ajouter une valeur inutilisee dans la map permet de s'assurer que le traitement a ete effectue
                    }
                    calcMoyBFC(recapEval, idEleves, bfcEleve, notesCompetencesEleve, echelleConversion, domainesRacine, handler);
                } else {
                    handler.handle(new Either.Left<String, JsonObject>("Impossible de recuperer le bilan de fin de cycle pour la classe selectionnee :\n" + event.left().getValue()));
                    log.error("buildBFC : getBFCsByEleve : " + event.left().getValue());
                }
            }
        });

        getEchelleConversion(idStructure, idClasse, new Handler<Either<String, SortedSet<Double>>>() {
            @Override
            public void handle(Either<String, SortedSet<Double>> event) {
                if (event.isRight()) {
                    echelleConversion.addAll(event.right().getValue());
                    calcMoyBFC(recapEval, idEleves, bfcEleve, notesCompetencesEleve, echelleConversion, domainesRacine, handler);
                } else {
                    handler.handle(new Either.Left<String, JsonObject>("Impossible de recuperer l'echelle de conversion pour la classe selectionnee :\n" + event.left().getValue()));
                    log.error("buildBFC : getEchelleConversion : " + event.left().getValue());
                }
            }
        });

        getDomaines(idClasse, new Handler<Either<String, Map<Long, Domaine>>>() {
            @Override
            public void handle(Either<String, Map<Long, Domaine>> event) {
                if (event.isRight()) {
                    Set<Domaine> setDomainesRacine = new LinkedHashSet<>();

                    domaines.putAll(event.right().getValue());
                    for (Domaine domaine : domaines.values()) {
                        if (domaine.getParentRacine() != null) {
                            setDomainesRacine.add(domaine.getParentRacine());
                        }
                    }
                    domainesRacine.addAll(setDomainesRacine);
                    calcMoyBFC(recapEval, idEleves, bfcEleve, notesCompetencesEleve, echelleConversion, domainesRacine, handler);
                } else {
                    handler.handle(new Either.Left<String, JsonObject>("Impossible de recuperer les domaines racines pour la classe selectionne :\n" + event.left().getValue()));
                    log.error("buildBFC : getDomaines : " + event.left().getValue());
                }
            }
        });
    }

    @Override
    public void getCalcMillesimeValues (Handler<Either<String,JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT * " +
                "FROM notes.calc_millesime";
        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }
    @Override
    public void setVisibility(String structureId, Integer idVisibility, UserInfos user, Integer visible,
                              Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder().append("INSERT INTO ")
                .append( Competences.COMPETENCES_SCHEMA + ".visibility (id_etablissement, visible, id_visibility) ")
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
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".visibility ")
                .append(" WHERE id_etablissement = ? " )
                .append("AND id_visibility = ?");
        values.add(structureId).add(idVisibility);

        query.append(" UNION ALL " )
                .append(" SELECT ? , ")
                .append(" 1 , ?");
        values.add(structureId).add(idVisibility);

        query.append(" WHERE NOT EXISTS (SELECT id_etablissement, visible, id_visibility ")
                .append(" FROM " + Competences.COMPETENCES_SCHEMA + ".visibility")
                .append(" WHERE id_etablissement = ? AND id_visibility = ? );  ");

        values.add(structureId).add(idVisibility);

        Sql.getInstance().prepared(query.toString(), values, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getMoyenneControlesContinusBrevet(EventBus eb, List<String> idsClasses,final Long idPeriode, final Handler<Either<String, JsonArray>> handler) {
        // j'ai besoin de récupérer les idsEleve et idStructure à partir de l'idClass
        final JsonArray moyControlesContinusEleves = new fr.wseduc.webutils.collections.JsonArray();
        getParamsMethodGetMoyenne(idsClasses,
                idPeriode,
                new Handler<Either<String, Map<String, Map<String, List<String>>>>>() {
            @Override
            public void handle(Either<String, Map<String, Map<String, List<String>>>> respParam) {

                if(respParam.isRight()){

                    Map<String,Map<String,List<String>>> mapStructureClassesEleves = respParam.right().getValue();
                    final String idStructure = mapStructureClassesEleves.entrySet().iterator().next().getKey();
                    final Map<String,List<String>> mapClassesEleves = mapStructureClassesEleves.entrySet().iterator().next().getValue();

                    for (final Map.Entry<String, List<String>> classe : mapClassesEleves.entrySet()) {
                       final  String[] idsEleves =  classe.getValue().toArray(new String[0]);
                       final String idClasse = classe.getKey();
                        //On récupère le nb de DomainesRacines
                        domaineService.getDomainesRacines(idClasse, new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> responseDomainesRacines) {
                                if(responseDomainesRacines.isRight()) {
                                    final Integer nbDomainesRacines = responseDomainesRacines.right().getValue().size();

                                    //On récupère la valeur max du barèmebrevet et map des ordres(=niveau ds le JsonObject du buildBFC)/bareme
                                    competenceNoteService.getMaxBaremeMapOrderBaremeBrevet(idStructure, idClasse, new Handler<Either<String, Map<Integer, Map<Integer, Integer>>>>() {
                                        @Override
                                        public void handle(Either<String, Map<Integer, Map<Integer, Integer>>> respMaxMapOrdreBareme) {
                                            if (respMaxMapOrdreBareme.isRight()) {
                                                final Integer maxBareme = respMaxMapOrdreBareme.right().getValue().entrySet().iterator().next().getKey();
                                                final Map<Integer, Integer> mapOrdreBaremeBrevet = respMaxMapOrdreBareme.right().getValue().entrySet().iterator().next().getValue();

                                                //On récupère pour tous les élèves de la classe leurs résultats pour chaque domainesRacines évalué
                                                buildBFC(false, idsEleves, idClasse, idStructure, idPeriode, null, new Handler<Either<String, JsonObject>>() {
                                                    @Override
                                                    public void handle(Either<String, JsonObject> responseMaitriseEleves) {
                                                        if (responseMaitriseEleves.isRight()) {
                                                            final JsonObject resultsElevesByDomaine = responseMaitriseEleves.right().getValue();
                                                            //On récupère les élèves qui sont dispensés pour un domaine racine
                                                            dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve(classe.getValue(), new Handler<Either<String, Map<String, Map<Long, Boolean>>>>() {
                                                                @Override
                                                                public void handle(Either<String, Map<String, Map<Long, Boolean>>> respDisepenseEleves) {

                                                                    if (respDisepenseEleves.isRight()) {

                                                                        Map<String, Map<Long, Boolean>> dispensesDomainesEleves = respDisepenseEleves.right().getValue();
                                                                        for(String idEleve : classe.getValue()){
                                                                            Integer sommeBareme = 0;
                                                                            Integer totalMaxBareme = 0;
                                                                            //si l'élève est dans le json resultsElevesByDomaine alors il a au moins un niveau pour un domaine
                                                                            if(resultsElevesByDomaine.containsKey(idEleve)) {
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
                                                                            }else {
                                                                                if(!dispensesDomainesEleves.containsKey(idEleve)){
                                                                                    totalMaxBareme = nbDomainesRacines * maxBareme;
                                                                                }else{
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
                                                                    } else {
                                                                        handler.handle(new Either.Left<String, JsonArray>(respDisepenseEleves.left().getValue()));
                                                                        log.error("getMoyenneControlesContinusBrevet : dispenseDomaineEleveService.mapOfDispenseDomaineByIdEleve " + respDisepenseEleves.left().getValue());
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            handler.handle(new Either.Left<String, JsonArray>(responseMaitriseEleves.left().getValue()));
                                                            log.error("getMoyenneControlesContinusBrevet : buildBFC " + responseMaitriseEleves.left().getValue());
                                                        }
                                                    }
                                                });
                                            } else {
                                                handler.handle(new Either.Left<String, JsonArray>(respMaxMapOrdreBareme.left().getValue()));
                                                log.error("getMoyenneControlesContinusBrevet : competenceNoteService.getMaxBaremeMapOrderBaremeBrevet " + respMaxMapOrdreBareme.left().getValue());
                                            }
                                        }
                                    });
                                }else{
                                    handler.handle(new Either.Left<String, JsonArray>(responseDomainesRacines.left().getValue()));
                                    log.error("getMoyenneControlesContinusBrevet : getDomainesRacines " + responseDomainesRacines.left().getValue());
                                }
                            }
                        });
                    }
                }else{
                    handler.handle(new Either.Left<String,JsonArray>(respParam.left().getValue()));
                    log.error("getMoyenneControlesContinusBrevet : getParamsMethodGetMoyenne " + respParam.left().getValue() );

                }
            }
        });

    }
    //récupérer les paramètres nécessaire pour les méthodes
    private void getParamsMethodGetMoyenne (final List<String> idsClasses,
                                            final Long idPeriode,
                                            final Handler<Either<String, Map<String, Map<String, List<String>>>>> handler){
        final Map<String, Map<String, List<String>>> paramsMethods = new HashMap<>();

        Utils.getStructClasses(eb, idsClasses.toArray(new String[0]), new Handler<Either<String, String>>() {
            @Override
            public void handle(Either<String, String> repStructure) {
                if (repStructure.isRight()) {
                    final String idStructure = repStructure.right().getValue();
                    paramsMethods.put(idStructure,new LinkedHashMap<String, List<String>>());

                    Utils.getElevesClasses(eb, idsClasses.toArray(new String[0]),
                            idPeriode,
                            new Handler<Either<String, Map<String, List<String>>>>() {
                        @Override
                        public void handle(Either<String, Map<String, List<String>>> repEleves) {
                            if(repEleves.isRight()){
                                for (final Map.Entry<String, List<String>> idclasse : repEleves.right().getValue().entrySet()) {
                                    paramsMethods.get(idStructure).put(idclasse.getKey(),idclasse.getValue());
                                }
                                handler.handle(new Either.Right<String, Map<String, Map<String, List<String>>>>(paramsMethods));
                            } else {
                                handler.handle(new Either.Left<String, Map<String, Map<String, List<String>>>>(repEleves.left().getValue()));
                                log.error("getParamClasses : getElevesClasses : " + repEleves.left().getValue());
                            }
                        }

                    });
                }else {
                    handler.handle(new Either.Left<String, Map<String, Map<String, List<String>>>>(repStructure.left().getValue()));
                    log.error("getParamClasses : getStructClasses : " + repStructure.left().getValue());
                }
            }
        });
    }
}
