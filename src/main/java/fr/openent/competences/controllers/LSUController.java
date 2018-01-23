package fr.openent.competences.controllers;


import fr.openent.competences.Competences;
import fr.openent.competences.bean.lsun.*;
import fr.openent.competences.service.*;
import fr.openent.competences.service.impl.DefaultBFCService;
import fr.openent.competences.service.impl.DefaultBfcSyntheseService;
import fr.openent.competences.service.impl.DefaultNiveauEnseignementComplementService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.leftToResponse;


/**
 * Created by agnes.lapeyronnie on 30/06/2017.
 */
public class LSUController extends ControllerHelper {

    protected static final Logger log = LoggerFactory.getLogger(LSUController.class);
    private ObjectFactory objectFactory = new ObjectFactory();
    private UtilsService utilsService;
    private BFCService bfcService;
    private BfcSyntheseService bfcSynthseService;
    private NiveauEnseignementComplementService niveauEnsCpl;
    private JsonArray listErreursEleves;
    private EventBus eb;

    public LSUController(EventBus eb) {
        this.eb = eb;
        utilsService = new DefaultUtilsService();
        bfcService = new DefaultBFCService(Competences.COMPETENCES_SCHEMA, Competences.BFC_TABLE);
        bfcSynthseService = new DefaultBfcSyntheseService(Competences.COMPETENCES_SCHEMA, Competences.BFC_SYNTHESE_TABLE, eb);
        niveauEnsCpl = new DefaultNiveauEnseignementComplementService(Competences.COMPETENCES_SCHEMA,Competences.ELEVE_ENSEIGNEMENT_COMPLEMENT);
    }

    /**
     * complete la balise entete et la set a lsunBilans
     * @param lsunBilans
     * @param idStructure
     * @param handler
     */
    private void getBaliseEntete(final LsunBilans lsunBilans, final String idStructure, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "user.getUAI")
                .putString("idEtabl", idStructure);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    JsonObject valueUAI = body.getObject("result");
                    if (valueUAI != null) {
                        Entete entete = objectFactory.createEntete("EDITEUR","LOGICIEL", valueUAI.getString("uai"));
                        lsunBilans.setEntete(entete);
                        handler.handle("success");
                    } else {
                        handler.handle("UAI de l'établissement null");
                        log.error("UAI etablissement null");
                    }
                } else {
                    handler.handle("method getBaliseEntete : error when collecting UAI  " + body.getString("message"));
                    log.error("An error occured when collecting UAI for " + idStructure + " structure");
                }
            }
        });
    }

    //récupère chaque responsable d'établissement et les ajouter à la balise responsables-etab puis à la balise donnees
    private void getBaliseResponsables(final Donnees donnees, final List<String> idsResponsable, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "user.getResponsablesEtabl")
                .putArray("idsResponsable", new JsonArray(idsResponsable.toArray()));
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray value = body.getArray("results");
                    Donnees.ResponsablesEtab responsablesEtab = objectFactory.createDonneesResponsablesEtab();
                    try {
                        for (int i = 0; i < value.size(); i++) {
                            JsonObject responsableJson = value.get(i);
                            if (!responsableJson.getString("externalId").isEmpty()  && !responsableJson.getString("displayName").isEmpty()) {
                                ResponsableEtab responsableEtab = objectFactory.createResponsableEtab(responsableJson.getString("externalId"),responsableJson.getString("displayName"));
                                responsablesEtab.getResponsableEtab().add(responsableEtab);
                            } else {
                                throw new Exception("attributs responsableEtab null");
                            }
                        }
                        donnees.setResponsablesEtab(responsablesEtab);
                        handler.handle("success");
                    }catch (Throwable e){
                        handler.handle("method getBaliseResponsable : " +e.getMessage());
                        log.error("method getBaliseResponsable : " +e.getMessage());
                    }
                } else {
                    handler.handle("getBaliseResponsable : error when collecting Responsable " + body.getString("message"));
                    log.error("method getBaliseResponsable an error occured when collecting Responsable " + idsResponsable);
                }
            }
        });
    }

    /**
     * pour une liste de classe mise a jour des attributs de l'eleve et de son responsable.
     *
     * @param donnees la liste des eleves est ajoutee a la balise donnees
     * @param Classids liste des classes pour lesquelles le fichier xml doit etre genere
     * @param handler  renvoie  "success" si tout c'est bien passe
     */

    private void getBaliseEleves(final Donnees donnees, final List<String> Classids, final Handler<String> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "user.getElevesRelatives")
                .putArray("idsClass", new JsonArray(Classids.toArray()));
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray jsonElevesRelatives = body.getArray("results");
                    Eleve eleve;
                    Responsable responsable = null;
                    Adresse adresse = null;
                    Donnees.Eleves eleves = objectFactory.createDonneesEleves();
                    if (jsonElevesRelatives.size() > 0) {
                       /* try {*/
                        for (int i = 0; i < jsonElevesRelatives.size(); i++) {
                            JsonObject o = jsonElevesRelatives.get(i);
                            if (!eleves.containIdEleve(o.getString("idNeo4j"))) {
                                eleve = objectFactory.createEleve(o.getString("externalId"), o.getString("attachmentId"), o.getString("firstName"),
                                        o.getString("lastName"),o.getString("nameClass"),o.getString("idNeo4j"),o.getString("idClass"),o.getString("level"));
                                eleves.add(eleve);
                            } else {
                                eleve = eleves.getEleveById(o.getString("idNeo4j"));
                            }
                            if(o.getString("address")!= null && o.getString("zipCode")!=null && o.getString("city")!= null){
                                adresse = objectFactory.createAdresse(o.getString("address"), o.getString("zipCode"), o.getString("city"));
                            }
                            if (o.getString("externalIdRelative")!= null && o.getString("lastNameRelative") !=null &&
                                    o.getString("firstNameRelative")!= null && o.getArray("relative").size() > 0 && adresse != null) {
                                //création d'un responsable Eleve avec la civilite si MERE ou PERE
                                responsable = objectFactory.createResponsable(o.getString("externalIdRelative"),o.getString("lastNameRelative"),
                                        o.getString("firstNameRelative"),o.getArray("relative"),adresse);

                            }/* else {
                                    throw new Exception("responsable Eleve non renseigne ");
                                }*/
                            if(responsable != null && responsable.getCivilite()!=null) {
                                eleve.getResponsableList().add(responsable);
                            }/*else{
                                    throw new Exception("responsable Eleve sans adresse at sans civilite "+responsable.getNom());
                                }*/
                        }
                        donnees.setEleves(eleves);
                        handler.handle("success");
                        log.info("FIN method getBaliseEleves : nombre d'eleve ajoutes :"+eleves.getEleve().size());

                       /* }catch (Exception e){
                            handler.handle( e.getMessage());
                            log.error("method getBaliseEleve : attribut relative est null " + e.getMessage());
                        }*/
                    } else {
                        handler.handle("getBaliseEleves : error when collecting Eleves " + body.getString("message"));
                        log.error("method getBaliseEleves an error occured when collecting Eleves " + body.getString("message"));
                    }
                }else{
                    handler.handle("getBaliseEleves : error when collecting Eleves " + body.getString("message"));
                    log.error("method getBaliseEleves an error occured when collecting Eleves " + body.getString("message"));
                }
            }
        });
    }

    /**
     *  M
     * @param classIds liste des idsClass dont on recherche le cycle auquel elles appartiennent
     * @param handler retourne une liste de 2 map : map<idClass,idCycle> et map<idCycle,value_cycle>
     */

    private void getIdClassIdCycleValue(List<String> classIds, final Handler<Either<String, List<Map>>> handler) {
        utilsService.getCycle(classIds, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> response) {
                if (response.isRight()) {
                    JsonArray cycles = response.right().getValue();
                    Map mapIclassIdCycle = new HashMap<>();
                    Map mapIdCycleValue_cycle = new HashMap<>();
                    List<Map> mapArrayList = new ArrayList<>();
                    try {
                        for (int i = 0; i < cycles.size(); i++) {
                            JsonObject o = cycles.get(i);
                            if(o.getString("id_groupe")!=null &&o.getLong("id_cycle")!=null && o.getLong("value_cycle")!=null) {
                                mapIclassIdCycle.put(o.getString("id_groupe"), o.getLong("id_cycle"));
                                mapIdCycleValue_cycle.put(o.getLong("id_cycle"), o.getLong("value_cycle"));
                            }else {
                                throw new Exception ("Erreur idGroupe, idCycle et ValueCycle null");
                            }
                        }
                        mapArrayList.add(mapIclassIdCycle);
                        mapArrayList.add(mapIdCycleValue_cycle);
                    }catch(Exception e){
                        handler.handle(new Either.Left<String, List<Map>>(" Exception " + e.getMessage()));
                        log.error("catch Exception in getCycle" + e.getMessage());
                    }
                    handler.handle(new Either.Right<String, List<Map>>(mapArrayList));
                } else {
                    handler.handle(new Either.Left<String, List<Map>>(" getValueCycle : error when collecting Cycles " + response.left().getValue()));
                    log.error("method getIdClassIdCycleValue an error occured when collecting Cycles " + response.left().getValue());
                }
            }
        });
    }

    /**
     * méthode qui permet de construire une Map avec id_domaine et son code_domaine (domaine de hérarchie la plus haute)
     * @param IdClass liste des idsClass
     * @param handler contient la map<IdDomaine,Code_domaine> les codes domaines : codes des socles communs au cycle
     */
    private void getMapCodeDomaineById(String IdClass, final Handler<Either<String, Map<Long, String>>> handler) {
        JsonObject action = new JsonObject()
                .putString("action", "user.getCodeDomaine")
                .putString("idClass", IdClass);

        eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject body = message.body();
                if ("ok".equals(body.getString("status"))) {
                    JsonArray domainesJson = message.body().getArray("results");
                    Map<Long, String> mapDomaines = new HashMap<>();

                    try {
                        for (int i = 0; i < domainesJson.size(); i++) {
                            JsonObject o = domainesJson.get(i);
                            if (CodeDomaineSocle.valueOf(o.getString("code_domaine")) != null) {
                                mapDomaines.put(o.getLong("id_domaine"), o.getString("code_domaine"));
                            }
                        }
                        //la mapDomaines n'est renvoyee que si elle contient les 8 codes domaine du socle commun
                        if (mapDomaines.size() == CodeDomaineSocle.values().length) {
                            handler.handle(new Either.Right<String, Map<Long, String>>(mapDomaines));
                        }
                        else{
                            throw new Exception("getMapCodeDomaine : map incomplete" );
                        }
                    }catch (Exception e) {

                        if(e instanceof IllegalArgumentException){
                            handler.handle(new Either.Left<String,Map<Long,String>>("code_domaine en base de données non valide"));
                        }else{
                            handler.handle(new Either.Left<String, Map<Long, String>>("getMapCodeDomaineById : "));
                            log.error("getMapCodeDomaineById : "+e.getMessage());
                        }
                    }
                } else {
                    handler.handle(new Either.Left<String, Map<Long, String>>("getMapCodeDomaineById : error when collecting codeDomaineById : " + body.getString("message")));
                    log.error("method getMapCodeDomaineById an error occured when collecting CodeDomaineById " + body.getString("message"));
                }
            }
        });
    }


    private JsonObject  getJsonObject(JsonArray rep ,String idEleve){
        JsonObject repSyntheseIdEleve = new JsonObject();
        for (int i=0; i<rep.size();i++){
            JsonObject o = rep.get(i);
            if((o.getString("id_eleve")).equals(idEleve)){
                repSyntheseIdEleve = o;
            }
        }
        return repSyntheseIdEleve;
    }

    private XMLGregorianCalendar getDateFormatGregorian(String dateString) {
        XMLGregorianCalendar dateGregorian = null;
        try {
            SimpleDateFormat dfYYYYMMdd = new SimpleDateFormat("yyyy-MM-dd");
            Date date = dfYYYYMMdd.parse(dateString);
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);
            dateGregorian = DatatypeFactory.newInstance().newXMLGregorianCalendar();
            dateGregorian.setYear(cal.get(Calendar.YEAR));
            dateGregorian.setMonth(cal.get(Calendar.MONTH) + 1);
            dateGregorian.setDay(cal.get(Calendar.DATE));
        } catch (DatatypeConfigurationException | ParseException e) {
            e.printStackTrace();
        }
        return dateGregorian;
    }


    private Long giveIdDomaine(Map<Long,String> mapIdDomaineCodeDomaine, String value){
        Long idDomaine = null;
        for(Map.Entry<Long, String> idDomaineCode : mapIdDomaineCodeDomaine.entrySet()){
            if(idDomaineCode.getValue().equals(value)){
                idDomaine = idDomaineCode.getKey();
            }
        }
        return idDomaine;
    }


    private void setBilanCycleElevesWithEnsCpl(Map<Long, String> mapIdDomaineCodeDomaine, String[] idsEleve, AtomicInteger nbEleveCompteur, Donnees.Eleves eleves, JsonArray ensCplsEleves, JsonArray synthesesEleves,
                                               Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition, Long valueCycle, List<ResponsableEtab> responsablesEtab, Map mapIdCycleValue, Map mapIdClassIdCycle,
                                               String idClass, JsonArray vCalcMillesimeValues, Donnees.BilansCycle bilansCycle) {
        //on récupère id du codeDomaine CPD_ETR
        Long idDomaineCPD_ETR = giveIdDomaine(mapIdDomaineCodeDomaine, "CPD_ETR");
        int millesimeClass = 0;
        for (String idEleve : idsEleve) {
            nbEleveCompteur.incrementAndGet();//compteur
            Eleve eleve = eleves.getEleveById(idEleve);
            if (eleve != null) {
                //on récupère le JsonObject synthèse
                JsonObject ensCplEleve = getJsonObject(ensCplsEleves,idEleve);
                JsonObject syntheseEleve = getJsonObject(synthesesEleves, idEleve);
                JsonObject erreursEleve = new JsonObject();
                    //si l'élève est dans la mapIdEleveIdDomainePosition
                    if (mapIdEleveIdDomainePosition.containsKey(eleve.getIdNeo4j())) {
                        //alors on récupère la map<Iddomaine,positionnement> de l'élève en cours
                        Map<Long, Integer> mapIdDomainePosition = mapIdEleveIdDomainePosition.get(eleve.getIdNeo4j());
                        //variable a true quand  la taille de map<Iddomaine,positionnement> est à 7 et qu'elle ne contient pas idDomaine
                        //correspondant au codeDomaineCPD_ETR
                        Boolean bmapSansIdDomaineCPDETR = (mapIdDomainePosition.size() == (mapIdDomaineCodeDomaine.size() - 1)
                                && !mapIdDomainePosition.containsKey(idDomaineCPD_ETR));
                        if (syntheseEleve.size() > 0 && (mapIdDomainePosition.size() == mapIdDomaineCodeDomaine.size() || bmapSansIdDomaineCPDETR)) {

                            if ( !ensCplEleve.containsField("id_eleve")) {
                                //supprimer l'élève de la liste
                                eleves.getEleve().remove(eleve);
                                //ajouter l'élève dans liste des erreurs
                                erreursEleve.putString("idEleve", idEleve).putString("prenom", eleve.getPrenom()).putString("nom", eleve.getNom()).putString("classe", eleve.getCodeDivision());
                                JsonArray erreurs = new JsonArray();
                                erreurs.add(new JsonObject().putString("ensCpl", "L'enseignement de complement n'est pas renseigne"));
                                erreursEleve.putArray("typeErreur", erreurs);
                                listErreursEleves.add(erreursEleve);
                                continue;
                            }
                            //  alors on peut ajouter le bilanCycle à l'élève avec la synthèse, les ensCpl et les codesDomaines et positionnement au socle
                            final BilanCycle bilanCycle = objectFactory.createBilanCycle();
                            BilanCycle.Socle socle = objectFactory.createBilanCycleSocle();
                            //Ajouter les CodesDomaines et positionnement
                            //on teste si la map<Iddomaine,positionnement> contient idDomaine correspondant à CPD_ETR
                            // alors on ajoute domaineSocleCycle manuellement avec le positionnement à zéro
                            if (bmapSansIdDomaineCPDETR) {
                                DomaineSocleCycle domaineSocleCycle = new DomaineSocleCycle(mapIdDomaineCodeDomaine.get(idDomaineCPD_ETR), 0);
                                socle.getDomaine().add(domaineSocleCycle);
                            }
                            // on parcours la map  idDomainePositionnement et on ajoute le code domaine qui a pour clé idDomaine en cours
                            for (Map.Entry<Long, Integer> idDomainePosition : mapIdDomainePosition.entrySet()) {
                                DomaineSocleCycle domaineSocleCycle = new DomaineSocleCycle(mapIdDomaineCodeDomaine.get(idDomainePosition.getKey()), idDomainePosition.getValue());
                                socle.getDomaine().add(domaineSocleCycle);
                            }
                            bilanCycle.setSocle(socle);

                            //la synthèse
                            bilanCycle.setSynthese(syntheseEleve.getString("texte"));
                            if (syntheseEleve.getString("modified") != null) {
                                bilanCycle.setDateVerrou(syntheseEleve.getString("modified").substring(0,19));
                            } else {
                                bilanCycle.setDateVerrou(syntheseEleve.getString("date_creation").substring(0,19));
                            }
                            XMLGregorianCalendar dateCreation = getDateFormatGregorian(syntheseEleve.getString("date_creation"));
                            bilanCycle.setDateCreation(dateCreation);

                            //enseignement Complément s'il existe pour l'élève en cours
                            if (ensCplEleve.containsField("id_eleve")) {
                                EnseignementComplement enseignementComplement = new EnseignementComplement(ensCplEleve.getString("code"), ensCplEleve.getInteger("niveau"));
                                bilanCycle.setEnseignementComplement(enseignementComplement);
                            }

                            //on ajoute les différents attributs de la balise BilanCycle de l'élève
                            ResponsableEtab responsableEtabRef = responsablesEtab.get(0);
                            //on ajoute les responsables de l'élève (attribut de clui-ci) aux responsables et au bilanCycle
                            if(eleve.getResponsableList() != null) {
                                BilanCycle.Responsables responsablesEleve = objectFactory.createBilanCycleResponsables();
                                responsablesEleve.getResponsable().addAll(eleve.getResponsableList());
                                bilanCycle.setResponsables(responsablesEleve);
                            }

                            bilanCycle.setResponsableEtabRef(responsableEtabRef);
                            bilanCycle.setEleveRef(eleve);
                            bilanCycle.setCycle(new BigInteger(String.valueOf(mapIdCycleValue.get((Long) mapIdClassIdCycle.get(idClass)))));

                            for (int i = 0; i < vCalcMillesimeValues.size() && millesimeClass == 0; i++) {
                                // On calcule le millésime si ce n'est pas déjà fait
                                JsonObject calcMillesimeValue = vCalcMillesimeValues.get(i);
                                if (null != eleve.getLevel()
                                        && !eleve.getLevel().isEmpty()
                                        && eleve.getLevel().contains(calcMillesimeValue.getString("code_level"))) {
                                    millesimeClass = getMillesimeClass(calcMillesimeValue.getInteger("increment"));
                                }
                            }
                            // Cas particulier : si aucun millésime on sette celui de cette année
                            if (millesimeClass == 0) {
                                millesimeClass = getMillesimeClass(0);
                            }
                            bilanCycle.setMillesime(Integer.toString(millesimeClass));
                            bilansCycle.getBilanCycle().add(bilanCycle);
                        } else {
                            //supprimer l'élève de la liste de la Balise ELEVES
                            eleves.getEleve().remove(eleve);
                            //affecter les différentes erreurs en fonction des conditions non respectées
                            erreursEleve.putString("idEleve", idEleve).putString("prenom", eleve.getPrenom()).putString("nom", eleve.getNom()).putString("classe", eleve.getCodeDivision());
                            JsonArray erreurs = new JsonArray();
                            if (syntheseEleve.size() > 0) {
                                erreurs.add(new JsonObject().putString("socleDomaine", "Il manque des domaines du socle commun a cet eleve"));
                                erreursEleve.putArray("typeErreur", erreurs);
                            } else if (mapIdDomainePosition.size() == mapIdDomaineCodeDomaine.size() || bmapSansIdDomaineCPDETR) {
                                erreurs.add(new JsonObject().putString("synthese", "La synthese du bilan de fin de cycle n'est pas completee "));
                                erreursEleve.putArray("typeErreur", erreurs);
                            } else {
                                erreurs.add(new JsonObject().putString("socleDomaine", "Il manque des domaines du socle commun "));
                                erreurs.add(new JsonObject().putString("synthese", "La synthese du bilan de fin de cycle n'est pas completee"));
                                erreursEleve.putArray("typeErreur", erreurs);
                            }
                            if(!ensCplEleve.containsField("id_eleve")){
                                erreurs.add(new JsonObject().putString("ensCpl", "L'enseignement de complement n'est pas renseigne"));
                                erreursEleve.putArray("typeErreur", erreurs);
                            }
                        }
                    } else {//si l'élève n'est pas dans la map alors il n'a aucune évaluation et
                        // il faut le supprimer du xml donc de la list des élèves de la balise ELEVES
                        eleves.getEleve().remove(eleve);
                        erreursEleve.putString("idEleve", idEleve).putString("prenom", eleve.getPrenom()).putString("nom", eleve.getNom()).putString("classe", eleve.getCodeDivision());
                        JsonArray erreurs = new JsonArray();
                        erreurs.add(new JsonObject().putString("socleDomaine", "Aucun domaine du socle commun "));
                        erreursEleve.putArray("typeErreur", erreurs);
                        if(!ensCplEleve.containsField("id_eleve")){
                            erreurs.add(new JsonObject().putString("ensCpl", "L'enseignement de complement n'est pas renseigne"));
                            erreursEleve.putArray("typeErreur", erreurs);
                        }
                        if(!syntheseEleve.containsField("id_eleve")){
                            erreurs.add(new JsonObject().putString("synthese", "La synthese du bilan de fin de cycle n'est pas completee "));
                            erreursEleve.putArray("typeErreur", erreurs);
                        }
                    }
                if (erreursEleve.size() > 0) {
                    listErreursEleves.add(erreursEleve);
                }
            }else
                log.info("eleve qui n'est pas dans la list des eleves " + idEleve);
        }
    }


    private void setBilanCycleElevesWithoutEnsCpl(Map<Long, String> mapIdDomaineCodeDomaine, String[] idsEleve, AtomicInteger nbEleveCompteur, Donnees.Eleves eleves,  JsonArray synthesesEleves,
                                               Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition, Long valueCycle, List<ResponsableEtab> responsablesEtab, Map mapIdCycleValue, Map mapIdClassIdCycle,
                                               String idClass, JsonArray vCalcMillesimeValues, Donnees.BilansCycle bilansCycle) {
        //on récupère id du codeDomaine CPD_ETR
        Long idDomaineCPD_ETR = giveIdDomaine(mapIdDomaineCodeDomaine, "CPD_ETR");
        int millesimeClass = 0;
        for (String idEleve : idsEleve) {
            nbEleveCompteur.incrementAndGet();//compteur
            Eleve eleve = eleves.getEleveById(idEleve);
            if (eleve != null) {
                //on récupère le JsonObject synthèse
                JsonObject syntheseEleve = getJsonObject(synthesesEleves, idEleve);
                JsonObject erreursEleve = new JsonObject();
                //si l'élève est dans la mapIdEleveIdDomainePosition
                if (mapIdEleveIdDomainePosition.containsKey(eleve.getIdNeo4j())) {
                    //alors on récupère la map<Iddomaine,positionnement> de l'élève en cours
                    Map<Long, Integer> mapIdDomainePosition = mapIdEleveIdDomainePosition.get(eleve.getIdNeo4j());
                    //variable a true quand  la taille de map<Iddomaine,positionnement> est à 7 et qu'elle ne contient pas idDomaine
                    //correspondant au codeDomaineCPD_ETR
                    Boolean bmapSansIdDomaineCPDETR = (mapIdDomainePosition.size() == (mapIdDomaineCodeDomaine.size() - 1)
                            && !mapIdDomainePosition.containsKey(idDomaineCPD_ETR));
                    if (syntheseEleve.size() > 0 && (mapIdDomainePosition.size() == mapIdDomaineCodeDomaine.size() || bmapSansIdDomaineCPDETR)) {
                        final BilanCycle bilanCycle = objectFactory.createBilanCycle();

                        BilanCycle.Socle socle = objectFactory.createBilanCycleSocle();
                        //Ajouter les CodesDomaines et positionnement
                        //on teste si la map<Iddomaine,positionnement> contient idDomaine correspondant à CPD_ETR
                        // alors on ajoute domaineSocleCycle manuellement avec le positionnement à zéro
                        if (bmapSansIdDomaineCPDETR) {
                            DomaineSocleCycle domaineSocleCycle = new DomaineSocleCycle(mapIdDomaineCodeDomaine.get(idDomaineCPD_ETR), 0);
                            socle.getDomaine().add(domaineSocleCycle);
                        }
                        // on parcours la map  idDomainePositionnement et on ajoute le code domaine qui a pour clé idDomaine en cours
                        for (Map.Entry<Long, Integer> idDomainePosition : mapIdDomainePosition.entrySet()) {
                            DomaineSocleCycle domaineSocleCycle = new DomaineSocleCycle(mapIdDomaineCodeDomaine.get(idDomainePosition.getKey()), idDomainePosition.getValue());
                            socle.getDomaine().add(domaineSocleCycle);
                        }
                        bilanCycle.setSocle(socle);

                        //la synthèse
                        bilanCycle.setSynthese(syntheseEleve.getString("texte"));
                        if (syntheseEleve.getString("modified") != null) {
                            bilanCycle.setDateVerrou(syntheseEleve.getString("modified").substring(0,19));
                        } else {
                            bilanCycle.setDateVerrou(syntheseEleve.getString("date_creation").substring(0,19));
                        }
                        XMLGregorianCalendar dateCreation = getDateFormatGregorian(syntheseEleve.getString("date_creation"));
                        bilanCycle.setDateCreation(dateCreation);
                        //on ajoute les différents attributs de la balise BilanCycle de l'élève
                        ResponsableEtab responsableEtabRef = responsablesEtab.get(0);
                        //on ajoute les responsables de l'élève (attribut de clui-ci) aux responsables du bilanCycle
                        if(eleve.getResponsableList() != null) {
                            BilanCycle.Responsables responsablesEleve = objectFactory.createBilanCycleResponsables();
                            responsablesEleve.getResponsable().addAll(eleve.getResponsableList());
                            bilanCycle.setResponsables(responsablesEleve);
                        }

                        bilanCycle.setResponsableEtabRef(responsableEtabRef);
                        bilanCycle.setEleveRef(eleve);
                        bilanCycle.setCycle(new BigInteger(String.valueOf(mapIdCycleValue.get((Long) mapIdClassIdCycle.get(idClass)))));

                        for (int i = 0; i < vCalcMillesimeValues.size() && millesimeClass == 0; i++) {
                            // On calcule le millésime si ce n'est pas déjà fait
                            JsonObject calcMillesimeValue = vCalcMillesimeValues.get(i);
                            if (null != eleve.getLevel()
                                    && !eleve.getLevel().isEmpty()
                                    && eleve.getLevel().contains(calcMillesimeValue.getString("code_level"))) {
                                millesimeClass = getMillesimeClass(calcMillesimeValue.getInteger("increment"));
                            }
                        }
                        // Cas particulier : si aucun millésime on sette celui de cette année
                        if (millesimeClass == 0) {
                            millesimeClass = getMillesimeClass(0);
                        }
                        bilanCycle.setMillesime(Integer.toString(millesimeClass));
                        //on ajoute le bilan de cycle de l'élève à la liste des bilans de cycle
                        bilansCycle.getBilanCycle().add(bilanCycle);
                    } else {
                        //supprimer l'élève de la liste de la Balise ELEVES
                        eleves.getEleve().remove(eleve);
                        //affecter les différentes erreurs en fonction des conditions non respectées
                        erreursEleve.putString("idEleve", idEleve).putString("prenom", eleve.getPrenom()).putString("nom", eleve.getNom()).putString("classe", eleve.getCodeDivision());
                        JsonArray erreurs = new JsonArray();
                        if (syntheseEleve.size() > 0) {
                            erreurs.add(new JsonObject().putString("socleDomaine", "Il manque des domaines du socle commun à cet eleve"));
                            erreursEleve.putArray("typeErreur", erreurs);
                        } else if (mapIdDomainePosition.size() == mapIdDomaineCodeDomaine.size() || bmapSansIdDomaineCPDETR) {
                            erreurs.add(new JsonObject().putString("synthese", "La synthese du bilan de fin de cycle n'est pas completee "));
                            erreursEleve.putArray("typeErreur", erreurs);
                        } else {
                            erreurs.add(new JsonObject().putString("socleDomaine", "Il manque des domaines du socle commun "));
                            erreurs.add(new JsonObject().putString("synthese", "La synthese du bilan de fin de cycle n'est pas completee"));
                            erreursEleve.putArray("typeErreur", erreurs);
                        }
                    }
                } else {//si l'élève n'est pas dans la map alors il n'a aucune évaluation et
                    // il faut le supprimer du xml donc de la list des élèves de la balise ELEVES
                    eleves.getEleve().remove(eleve);
                    erreursEleve.putString("idEleve", idEleve).putString("prenom", eleve.getPrenom()).putString("nom", eleve.getNom()).putString("classe", eleve.getCodeDivision());
                    JsonArray erreurs = new JsonArray();
                    erreurs.add(new JsonObject().putString("socleDomaine", "Aucun domaine du socle commun "));
                    erreursEleve.putArray("typeErreur", erreurs);
                }
                if (erreursEleve.size() != 0) {
                    listErreursEleves.add(erreursEleve);
                }
            }else
                log.info("eleve qui n'est pas dans la list des eleves " + idEleve);
        }
    }


    /**
     * permet de completer tous les attributs de la balise BilanCycle et de la setter à donnees
     * sauf les attributs de date, synthese et enseignements de complement
     * @param donnees permet de recuperer les eleves
     * @param idsClass
     * @param idStructure
     * @param handler
     */

    private void getBaliseBilansCycle(final Donnees donnees,final JsonArray calcMillesimeValues, final List<String> idsClass, final String idStructure, final Handler<Either<String,JsonArray>> handler) {
        final Donnees.BilansCycle bilansCycle = objectFactory.createDonneesBilansCycle();
        final List<ResponsableEtab> responsablesEtab = donnees.getResponsablesEtab().getResponsableEtab();
        final Donnees.Eleves eleves = donnees.getEleves();
        final Integer nbElevesTotal = eleves.getEleve().size();
        final JsonArray vCalcMillesimeValues = calcMillesimeValues;
        final AtomicInteger nbEleveCompteur = new AtomicInteger(0);
        final Map<String, List<String>> mapIdClassIdsEleve = eleves.getMapIdClassIdsEleve();
        log.info("DEBUT : method getBaliseBilansCycle : nombreEleve : "+eleves.getEleve().size());
        listErreursEleves = new JsonArray();
        getIdClassIdCycleValue(idsClass, new Handler<Either<String, List<Map>>>() {
                    @Override
            public void handle(Either<String, List<Map>> repIdClassIdCycleValue) {

                if (repIdClassIdCycleValue.isRight()) {
                    final List<Map> mapIdClassIdCycleValue = repIdClassIdCycleValue.right().getValue();
                    final Map mapIdClassIdCycle = mapIdClassIdCycleValue.get(0);//map<IdClass,IdCycle>
                    final Map mapIdCycleValue = mapIdClassIdCycleValue.get(1);//map<IdCycle,ValueCycle>

                        for (Map.Entry<String, List<String>> stringListEntry : mapIdClassIdsEleve.entrySet()) {
                            final String[] idsEleve = stringListEntry.getValue().toArray(new String[stringListEntry.getValue().size()]);
                            final String idClass = stringListEntry.getKey();
                            bfcService.buildBFC(idsEleve, idClass, idStructure, null, (Long) mapIdClassIdCycle.get(idClass), new Handler<Either<String, Map<String, Map<Long, Integer>>>>() {
                                @Override
                                public void handle(Either<String, Map<String, Map<Long, Integer>>> repBuildBFC) {
                                    if (repBuildBFC.isRight()) {
                                        final Map<String, Map<Long, Integer>> mapIdEleveIdDomainePosition = repBuildBFC.right().getValue();
                                        getMapCodeDomaineById(idClass, new Handler<Either<String, Map<Long, String>>>() {
                                            @Override
                                            public void handle(Either<String, Map<Long, String>> repMapCodeDomaineId) {
                                                if (repMapCodeDomaineId.isRight()) {
                                                    final Map<Long, String> mapIdDomaineCodeDomaine = repMapCodeDomaineId.right().getValue();
                                                    final Long idCycle = (Long) mapIdClassIdCycle.get(idClass);
                                                    final Long  valueCycle = (Long) mapIdCycleValue.get(idCycle);
                                                    if(idCycle!=null){
                                                        bfcSynthseService.getBfcSyntheseByIdsEleve(idsEleve, idCycle, new Handler<Either<String, JsonArray>>() {
                                                            @Override
                                                            public void handle(Either<String, JsonArray> repSynthese) {
                                                                if (repSynthese.isRight()) {
                                                                    final JsonArray synthesesEleves = repSynthese.right().getValue();
                                                                        niveauEnsCpl.listNiveauCplByEleves(idsEleve,new Handler<Either<String, JsonArray>>() {
                                                                            @Override
                                                                            public void handle(Either<String, JsonArray> repEleveEnsCpl) {
                                                                                if (repEleveEnsCpl.isRight() ) {
                                                                                    if(valueCycle == 4){
                                                                                    final JsonArray ensCplsEleves = repEleveEnsCpl.right().getValue();
                                                                                    setBilanCycleElevesWithEnsCpl(mapIdDomaineCodeDomaine, idsEleve, nbEleveCompteur, eleves, ensCplsEleves, synthesesEleves,
                                                                                            mapIdEleveIdDomainePosition, valueCycle, responsablesEtab, mapIdCycleValue, mapIdClassIdCycle,
                                                                                            idClass, vCalcMillesimeValues, bilansCycle);
                                                                                    }else {
                                                                                        setBilanCycleElevesWithoutEnsCpl(mapIdDomaineCodeDomaine, idsEleve, nbEleveCompteur, eleves, synthesesEleves,
                                                                                                mapIdEleveIdDomainePosition, valueCycle, responsablesEtab, mapIdCycleValue, mapIdClassIdCycle,
                                                                                                idClass, vCalcMillesimeValues, bilansCycle);
                                                                                    }
                                                                                    log.info("FIN method getBaliseBilansCycle nombre d'eleve dans la classe en cours : "+idsEleve.length);
                                                                                    log.info("FIN method getBaliseBilansCycle nombre de bilans de cycle complets : "+bilansCycle.getBilanCycle().size());
                                                                                    log.info("nb d'eleves au depart : "+nbElevesTotal);
                                                                                    log.info("nb d'eleve parcouru : "+nbEleveCompteur.intValue());
                                                                                    if( nbEleveCompteur.intValue() == nbElevesTotal.intValue() ) {
                                                                                        if(bilansCycle.getBilanCycle().size()!=0) {
                                                                                            donnees.setBilansCycle(bilansCycle);
                                                                                            log.info("FIN method getBaliseBilansCycle nombre d'eleve avec un bilan cycle complet "+eleves.getEleve().size());
                                                                                        }
                                                                                        log.info("FIN method getBaliseBilansCycle nombre d'eleve avec un bilan cycle incomplet "+listErreursEleves.size());
                                                                                        handler.handle(new Either.Right<String,JsonArray>(listErreursEleves));
                                                                                    }

                                                                                }else{
                                                                                    handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansCycle XML requete enseignement complement: "+repEleveEnsCpl.left().getValue()));
                                                                                    log.error("getBaliseBilansCycle requete enseignementComplement: "+repEleveEnsCpl.left().getValue());
                                                                                }
                                                                            }
                                                                        });

                                                                }else{
                                                                    handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansCycle XML requete synthese du BFC: "+repSynthese.left().getValue()));
                                                                    log.error("getBaliseBilansCycle requete synthese du BFC: "+repSynthese.left().getValue());
                                                                }
                                                            }
                                                        });
                                                    }else{
                                                        handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansCycle XML idCycle  :  " + idCycle));
                                                        log.error("getBaliseBilansCycle idCycle :  " + idCycle);
                                                    }
                                                } else {
                                                    handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansCycle :  " + repMapCodeDomaineId.left().getValue()));
                                                    log.error("getBaliseBilansCycle XML MapIdDomaineCodeDomaine :  " + repMapCodeDomaineId.left().getValue());
                                                }

                                            }
                                        });
                                    } else {
                                        handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansEleve : bfcService.buidBFC : " + repBuildBFC.left().getValue()));
                                        log.error("getBaliseBilansCycle XML buildBFC map<idEleve,map<idDomaine,positionnement>> : " + repBuildBFC.left().getValue());
                                    }
                                }
                            });
                        }



                } else {
                    handler.handle(new Either.Left<String,JsonArray>("getBaliseBilansEleve : list (map<idclasse,idCycle>,map<idCycle,cycle>) : " + repIdClassIdCycleValue.left().getValue()));
                    log.error("getBaliseBilansCycle XML:list (map<idclasse,idCycle>,map<idCycle,cycle>) " + repIdClassIdCycleValue.left().getValue());
                }
                    }

        });
    }

    /**
     * Détermine le millésime à partir de la date du jour
     * @param increment
     * @return millesime Class
     */
    private int getMillesimeClass(int increment) {
        int millesimeClass; Calendar today = Calendar.getInstance();

        Calendar endJuly = Calendar.getInstance();
        endJuly.set(Calendar.DAY_OF_MONTH, 31);
        endJuly.set(Calendar.MONTH, 7);

        Calendar endDecember = Calendar.getInstance();
        endDecember.set(Calendar.DAY_OF_MONTH, 31);
        endDecember.set(Calendar.MONTH, 12);
        millesimeClass = today.get(Calendar.YEAR) + increment;
        // Si on est entre le 31 juillet et le 31 décembre on ajoute une année au millésime
        if(today.before(endDecember) && today.after(endJuly)){
            millesimeClass ++ ;
        }
        return millesimeClass;
    }

    /**
     * génère le fichier xml et le valide
     * @param request
     * @param lsunBilans
     */

    private void returnResponse(final HttpServerRequest request, LsunBilans lsunBilans) {
        log.info("DEBUT method returnResponse ");
        try (StringWriter response = new StringWriter()) {

            JAXBContext jc = JAXBContext.newInstance(LsunBilans.class);
            Marshaller marshaller = jc.createMarshaller();
            // écriture de la réponse
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "urn:fr:edu:scolarite:lsun:bilans:import import-bilan-complet.xsd");
            marshaller.marshal(lsunBilans, response);

            /* Vérification du fichier xml généré par rapport au xsd */
            final String templatePath = Competences.LSUN_CONFIG.getString("xsd_path");
            vertx.fileSystem().readFile(templatePath, new Handler<AsyncResult<Buffer>>() {
                @Override
                public void handle(AsyncResult<Buffer> result) {
                    if (!result.succeeded()) {
                        badRequest(request);
                        return;
                    }

                    try {
                        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                        Schema schema = schemaFactory.newSchema(new StreamSource(new ByteArrayInputStream(result.result().getBytes())));
                        log.info("method returnResponse avant la validation");
                        Validator validator = schema.newValidator();
                        Source xmlFile = new StreamSource(new ByteArrayInputStream(response.toString().getBytes("UTF-8")));
                        validator.validate(xmlFile);
                    } catch (SAXException | IOException e) {
                        e.printStackTrace();
                        badRequest(request);
                        return;
                    }
                    //préparation de la requête
                    request.response().putHeader("content-type", "text/xml");
                    request.response().putHeader("charset", "utf-8");
                    //request.response().putHeader("standalone", "yes");
                    request.response().putHeader("Content-Disposition", "attachment; filename=import_lsun_" + new Date().getTime() + ".xml");
                    request.response().end(new Buffer(response.toString()));
                    log.info("FIN method returnResponse");
                }
            });
        } catch (IOException | JAXBException e) {
            e.printStackTrace();
            badRequest(request);
            return;
        }
    }

    /**
     * Methode qui contruit le xml pour le LSU
     * @param request contient la list des idsClasse et des idsResponsable ainsi que idStructure sur laquelle sont les responsables
     */
    @Get("/exportLSU/lsu")
    @ApiDoc("Export data to LSUN xml format")
    @SecuredAction("competences.lsun.export")
    public void getXML(final HttpServerRequest request) {
        //instancier le lsunBilans qui sera composé de entete,donnees et version
        final LsunBilans lsunBilans = objectFactory.createLsunBilans();
        //donnees composée de responsables-etab, eleves et bilans-cycle
        final Donnees donnees = objectFactory.createDonnees();
        final String idStructure = request.params().get("idStructure");
        log.info("idStructure = " + idStructure);
        final List<String> idsClasse = request.params().getAll("idClasse");
        final List<String> idsResponsable = request.params().getAll("idResponsable");

        lsunBilans.setSchemaVersion("2.0");
        log.info("DEBUT  get exportLSU : export Classe : " + idsClasse);
        if(!idsClasse.isEmpty() && !idsResponsable.isEmpty()) {
            getBaliseEntete(lsunBilans, idStructure, new Handler<String>() {
                @Override
                public void handle(String event) {
                    if (event.equals("success")) {

                        getBaliseResponsables(donnees, idsResponsable, new Handler<String>() {
                            @Override
                            public void handle(String event) {
                                if (event.equals("success")) {

                                    getBaliseEleves(donnees, idsClasse, new Handler<String>() {
                                        @Override
                                        public void handle(final String event) {
                                            if (event.equals("success")) {
                                                bfcService.getCalcMillesimeValues(new Handler<Either<String, JsonArray>>(){
                                                    @Override
                                                    public void handle(Either<String, JsonArray> responseCalcMillesime) {
                                                        if (responseCalcMillesime.isRight()) {
                                                            if(responseCalcMillesime.right().getValue() != null
                                                                    && responseCalcMillesime.right().getValue().isArray()
                                                                    && responseCalcMillesime.right().getValue().size() > 0){
                                                                JsonArray calcMillesimeValues = responseCalcMillesime.right().getValue();

                                                                getBaliseBilansCycle(donnees, calcMillesimeValues, idsClasse, idStructure, new Handler<Either<String, JsonArray>>() {
                                                                    @Override
                                                                    public void handle(Either<String, JsonArray> reponseErreursJsonArray) {
                                                                        if(reponseErreursJsonArray.isRight()) {

                                                                            if( donnees.getBilansCycle()!=null && donnees.getBilansCycle().getBilanCycle().size()!=0 ) {
                                                                                lsunBilans.setDonnees(donnees);
                                                                                returnResponse(request, lsunBilans);
                                                                            }else{
                                                                                JsonArray listErreurs = reponseErreursJsonArray.right().getValue();
                                                                                if(listErreurs.size() > 0) {
                                                                                    JsonArray affichageDesELeves = new JsonArray();
                                                                                    for (int i = 0; i < listErreurs.size(); i++) {
                                                                                        JsonObject affichageEleve = new JsonObject();
                                                                                        JsonObject erreurOneEleve = listErreurs.get(i);
                                                                                        affichageEleve.putString("nom", erreurOneEleve.getString("nom"))
                                                                                                .putString("prenom", erreurOneEleve.getString("prenom"))
                                                                                                .putString("classe", erreurOneEleve.getString("classe"));
                                                                                        affichageDesELeves.add(affichageEleve);
                                                                                    }
                                                                                    Renders.renderJson(request, affichageDesELeves);
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                });
                                                            } else{
                                                                leftToResponse(request, new Either.Left<>(event));
                                                                log.error("An error occured when collecting CalcMillesime Values is empty : " + responseCalcMillesime.left().getValue());
                                                            }
                                                        } else {
                                                            leftToResponse(request, new Either.Left<>(event));
                                                            log.error("An error occured when collecting CalcMillesime Values : " + responseCalcMillesime.left().getValue());
                                                        }
                                                    }
                                                });
                                            } else {
                                                leftToResponse(request, new Either.Left<>(event));
                                                log.error("getXML : getBaliseEleves " + event);
                                            }
                                        }
                                    });
                                } else {
                                    leftToResponse(request, new Either.Left<>(event));
                                    log.error("getXML : getBaliseResponsable " + event);
                                }
                            }
                        });
                    } else {
                        leftToResponse(request, new Either.Left<>(event));
                        log.error("getXML : getBaliseEntete " + event);

                    }
                }
            });
        }else{
            badRequest(request);
        }
        log.info("FIN exportLSU : export " );

    }

    /**
     * méthode qui récupère les responsables de direction à partir de idStructure
     * @param request
     */
    @Get("/responsablesDirection")
    @ApiDoc("Retourne les responsables de direction de l'établissement passé en paramètre")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getResponsablesDirection(final HttpServerRequest request){
    UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                if (user != null && request.params().contains("idStructure")) {
                    JsonObject action = new JsonObject()
                            .putString("action", "user.getResponsablesDirection")
                            .putString("idStructure", request.params().get("idStructure"));
                    eb.send(Competences.VIESCO_BUS_ADDRESS, action, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> message) {
                            JsonObject body = message.body();
                            if ("ok".equals(body.getString("status"))) {
                                Renders.renderJson(request, body.getArray("results"));
                            } else {
                                JsonObject error = new JsonObject()
                                        .putString("error", body.getString("message"));
                                Renders.renderJson(request, error, 400);
                            }
                        }
                    });
                } else {
                    badRequest(request);
                }
            }
        });

    }

}

