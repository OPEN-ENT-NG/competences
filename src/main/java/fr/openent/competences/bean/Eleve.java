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

package fr.openent.competences.bean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * @author rollinq
 *
 * Classe correspondant a un eleve. Permet la generation d'un JsonObject contenant ses evaluations par domaines.
 */
public class Eleve {

    private String idEleve;

    private String lastName;

    private String firstName;

    private String idClasse;

    private String nomClasse;

    private String cycle;

    private String syntheseCycle;

    private Map<Long, Map<String, String>> domainesRacines;

    private Map<String, Long> enseignmentComplements;

    private Map<Long, Integer> notes;

    private Map<Integer, String> libelleNiveau;

    private boolean isDomainessReady;

    private boolean isNiveauxReady;

    private boolean isNotesReady;

    /**
     * Constructeur de l'Eleve. Initialise les collections domainesRacines, notes et libelleNiveau avec des
     * collections vides. Initialise les booleens isNotesReady, isNiveauxReady et isDomainessReady a false.
     *
     * @param idEleve    Identifiant de l'Eleve.
     * @param lastName   Nom de l'Eleve.
     * @param firstName  Prenom de l'Eleve.
     * @param idClasse   Identifiant de la classe de l'Eleve.
     * @param nomClasse  Nom de la classe de l'Eleve.
     */
    public Eleve(String idEleve, String lastName, String firstName, String idClasse, String nomClasse) {
        this.idEleve = idEleve;
        this.lastName = lastName;
        this.firstName = firstName;
        this.idClasse = idClasse;
        this.nomClasse = nomClasse;
        this.domainesRacines = new LinkedHashMap<>();
        this.enseignmentComplements = new LinkedHashMap<>();
        this.notes = new HashMap<>();
        this.libelleNiveau = new HashMap<>();
        this.isNotesReady = false;
        this.isNiveauxReady = false;
        this.isDomainessReady = false;
    }

    /**
     * Retourne l'identifiant de l'Eleve.
     *
     * @return  l'identifiant de l'Eleve.
     */
    public String getIdEleve() {
        return this.idEleve;
    }

    /**
     * Initialise le cycle de la classe de l'Eleve.
     *
     * @param cycle  Le libelle du cycle de la classe de l'Eleve.
     */
    public void setCycle(String cycle) {
        this.cycle = cycle;
    }

    /**
     * Retourne l'identifiant de la classe de l'Eleve.
     *
     * @return  l'identifiant de la classe de l'Eleve.
     */
    public String getIdClasse() {
        return this.idClasse;
    }

    /**
     * Retourne le nom de la classe de l'Eleve.
     *
     * @return le nom de la classe de l'Eleve.
     */
    public String getNomClasse() {
        return this.nomClasse;
    }

    /**
     * get last name of student
     * @return last name of student
     */
    public String getLastName(){
        return this.lastName;
    }

    /**
     *  get first Name of student
     * @return first Name of student
     */
    public String getFirstName(){
        return this.firstName;
    }

    /**
     * Initialize la collection de notes par domaine de l'Eleve.
     * Met a jour le booleen isNotesReady afin de notifier que les notes ont ete initialisees,
     * meme si aucunes notes n'a ete trouvee, dans le cas ou l'etudiant n'est pas evalue par exemple.
     *
     * @param notes  La collection de notes par domaines.
     */
    public void setNotes(Map<Long, Integer> notes) {
        if(notes != null) {
            this.notes.putAll(notes);
        }
        this.isNotesReady = true;
    }

    /**
     * Initialise la collection de domaines racines pour la classe de l'Eleve.
     * Met a jour le booleen isDomainessReady afin de notifier que les domaines ont ete initialises.
     *
     * @param domainesRacines  Map des proprietes de domaines par id de domaine.
     */
    public void setDomainesRacines(Map<Long, Map<String, String>> domainesRacines) {
        if (domainesRacines != null) {
            this.domainesRacines.putAll(domainesRacines);
        }
        this.isDomainessReady = true;
    }

    /**
     * Initialise la collection de libelle de niveau de maitrise.
     * Met a jour le booleen isNiveauxReady afin de notifier que les niveaux ont ete initialises.
     *
     * @param libelleNiveau  Map des libelles de niveau par niveau de maitrise.
     */
    public void setLibelleNiveau(Map<Integer, String> libelleNiveau) {
        if (libelleNiveau != null) {
            this.libelleNiveau.putAll(libelleNiveau);
        }
        this.isNiveauxReady = true;
    }

    /**
     * getter des enseignements de compléments : Libellé de l'enseignement et id de l'objectif
     * @return enseignements de compléments
     */
    public Map<String, Long> getEnseignmentComplements() {
        return enseignmentComplements;
    }

    /**
     * setter des enseignements de compléments : Libellé de l'enseignement et id de l'objectif
     * @param enseignmentComplements
     */
    public void setEnseignmentComplements(Map<String, Long> enseignmentComplements) {
        this.enseignmentComplements = enseignmentComplements;
    }

    /**
     * getter de la synthèse du BFC
     * @return synthèse du BFC
     */
    public String getSyntheseCycle() {
        return syntheseCycle;
    }

    /**
     * setter de la synthèse du BFC
     * @param syntheseCycle
     */
    public void setSyntheseCycle(String syntheseCycle) {
        this.syntheseCycle = syntheseCycle;
    }


    /**
     * Retourne un booleen indiquant si l'Eleve possède bien toutes les donnees necessaires a l'export Json.
     *
     * @return  Un booleen indiquant que les notes, les domaines et les niveaux d'evaluation sont initialises.
     */
    public boolean isReady() {
        return this.isNotesReady && this.isDomainessReady && this.isNiveauxReady;
    }

    /**
     * Exporte une description de l'Eleve au sein d'un JsonObject.
     * les donnees exportees sont lastName, firstName, nomClasse, cycle, les niveaux d'evaluation, puis pour chaque
     * domaine, le libelle du domaine, ainsi qu'un tableau de booleen (de meme taille que celui indiquant les niveaux
     * d'evaluation), tous a false, sauf pour la valeur correspondant a la note de l'Eleve dans ce domaine.
     *
     * @return  Un JsonObject decrivant l'Eleve.
     */
    public JsonObject toJson() {
        JsonObject result = new JsonObject();

        result.put("lastName", this.lastName);
        result.put("firstName", this.firstName);
        result.put("nomClasse", this.nomClasse);
        result.put("cycle", this.cycle);

        List<Object> listNiveaux = new ArrayList<Object>(this.libelleNiveau.values());
        result.put("niveau", new fr.wseduc.webutils.collections.JsonArray(listNiveaux));

        JsonArray evaluations = new fr.wseduc.webutils.collections.JsonArray();
        for(Map.Entry<Long, Map<String, String>> domaine : this.domainesRacines.entrySet()) {
            JsonObject notes = new JsonObject();
            //notes.put("domaine", domaine.getValue().get("libelle"));
            JsonObject domaineObj = new JsonObject();
            domaineObj.put("libelle", domaine.getValue().get("libelle"));
            if(domaine.getValue().containsKey("dispense")){
                domaineObj.put("dispense", Boolean.valueOf(domaine.getValue().get("dispense")));
            }else{
                domaineObj.put("dispense",false);
            }
            notes.put("domaine",domaineObj);

            // le tableau de l'evaluation est de meme longueur que celui des niveaux, et est remplit de booleen, tous a
            // false.
            List<Object> evaluation = new ArrayList<Object>(Collections.nCopies(listNiveaux.size(), false));
            if (this.notes.containsKey(domaine.getKey())) {
                evaluation.set(this.notes.get(domaine.getKey()) - 1, true);
            }
            notes.put("notes", new fr.wseduc.webutils.collections.JsonArray(evaluation));
            evaluations.add(notes);
        }
        JsonArray enseignementComplements = new fr.wseduc.webutils.collections.JsonArray();
        if(this.enseignmentComplements != null
                && this.enseignmentComplements.size() > 0){
            for(Map.Entry<String, Long> enseignementComplement : this.enseignmentComplements.entrySet()) {
                JsonObject enseignmentComplementJson = new JsonObject();
                //si l'élève n'a aucun enseignement de complément et que si dans le pdf on ne veut pas que cela n'apparaisse
                //alors
                //if(enseignementComplement.getValue()!=0) {
                    enseignmentComplementJson.put("enseignementComplement", enseignementComplement.getKey());
                //}
                List<Object> objectifs = new ArrayList<Object>(Collections.nCopies(2, false));
                if(enseignementComplement.getValue()!=0) {
                    objectifs.set(enseignementComplement.getValue().intValue() - 1, true);
                }
                enseignmentComplementJson.put("objectifs", new fr.wseduc.webutils.collections.JsonArray(objectifs));
                enseignementComplements.add(enseignmentComplementJson);
            }
            result.put("enseignementComplements", enseignementComplements);
            result.put("hasEnseignementComplements", true);
        } else {
            result.put("hasEnseignementComplements", false);
        }

        if(syntheseCycle != null
                && !syntheseCycle.isEmpty()){
            result.put("syntheseBFC", syntheseCycle);
            result.put("hasSynthese", true);
        } else {
            result.put("hasSynthese", false);
        }

        result.put("domaines", evaluations);

        return result;
    }
}
