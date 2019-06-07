/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.bean;

import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class Eleves {

    protected List<Eleve> elevesList;
    protected UtilsService utilsService;
    protected TreeMap<String, String> mapNameEleveIdEleve;

    public Eleves(){
        elevesList = new ArrayList<>();
        mapNameEleveIdEleve = new TreeMap<String, String>();
        utilsService = new DefaultUtilsService();
    }

    public List<Eleve> getEleves() {
        return elevesList;
    }

    public void setEleves(List<Eleve> eleves) {
        this.elevesList = eleves;
    }

    public boolean containIdEleve(String idNeo4j){

        for(Eleve el: elevesList){
            if( el.getIdEleve().equals(idNeo4j)){
                return true;
            }
        }
        return false;
    }

    public Eleve getEleveById(String idNeo4j ){

        for(Eleve el: elevesList){
            if( el.getIdEleve().equals(idNeo4j)){
                return el;
            }
        }
        return null;
    }


   /* public JsonArray sortResultByClasseNameAndNameForBulletin(Map<String, JsonObject> mapEleves) {

        List<JsonObject> eleves = new ArrayList<>(mapEleves.values());
        Collections.sort(eleves, new Comparator<JsonObject>() {
            private static final String KEY_NAME = NAME;
            private static final String KEY_CLASSE_NAME = CLASSE_NAME;

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    valA = a.getString(KEY_CLASSE_NAME) + a.getString(KEY_NAME);
                    valB = b.getString(KEY_CLASSE_NAME) + b.getString(KEY_NAME);
                } catch (Exception e) {
                    //do something
                }
                return valA.compareTo(valB);
            }
        });*/

    public void setEleves(JsonArray eleves, String field1, String field2, String field3, String field4){
//field1 id_matiere field2 moyenne ou note field3 eleveMoyByMat field4 moyenneGenerale

        for(int i = 0; i < eleves.size(); i++){

            JsonObject eleveJsonO = eleves.getJsonObject(i);
            String idEleve = eleveJsonO.getString("id_eleve");

            if(this.containIdEleve(idEleve)){
                Eleve eleve = this.getEleveById(idEleve);
                setMapIdMatNotesAndListNote(eleve, eleveJsonO, field1, field2, field3, field4);

            }else{
                Eleve eleve = new Eleve(idEleve, eleveJsonO.getString("lastName"),
                        eleveJsonO.getString("firstName"), eleveJsonO.getString("id_class"),
                        eleveJsonO.getString("nameClasse") );
                setMapIdMatNotesAndListNote(eleve, eleveJsonO, field1, field2, field3, field4);
                this.elevesList.add(eleve);
            }
        }
    }

    public JsonArray buildJsonArrayEleves(Map<String, List<NoteDevoir>> mapIdMatListMoyByEleve,
                                          SortedMap<String, Set<String>> mapAllidMatAndidTeachers,
                                          List<NoteDevoir> moysElevesByYear){
        JsonArray elevesJA = new fr.wseduc.webutils.collections.JsonArray();

        Collections.sort(this.elevesList);

        for(Eleve eleve : this.getEleves()){
            if (mapIdMatListMoyByEleve != null && mapAllidMatAndidTeachers != null) {
                eleve.setJsonArrayIdMatMoyWithParams(mapIdMatListMoyByEleve,mapAllidMatAndidTeachers);
            }else{
                eleve.setJsonArrayIdMatMoy();
            }

            JsonObject eleveJO = new JsonObject();
            if(eleve.getIdEleve() != null){
                eleveJO.put("id_eleve", eleve.getIdEleve());
            }
            if(eleve.getLastName() != null){
                eleveJO.put("lastName", eleve.getLastName());
            }
            if(eleve.getFirstName() != null){
                eleveJO.put("firstName", eleve.getFirstName());
            }
            if(eleve.getNomClasse() != null){
                eleveJO.put("nameClasse", eleve.getNomClasse());
            }
            if(eleve.getJsonArrayIdMatMoy() != null){
                eleveJO.put("eleveMoyByMat", eleve.getJsonArrayIdMatMoy());
            }
            if(eleve.getListNotes() != null ){
                if(eleve.getListNotes().isEmpty()){
                   eleveJO.put("moyGeneraleEleve", "NN");
                }else{
                    Double moyGeneraleYear = utilsService.calculMoyenneParDiviseur(eleve.getListNotes(),
                            false).getDouble("moyenne");
                    eleveJO.put("moyGeneraleEleve", moyGeneraleYear);
                    if(moysElevesByYear != null){
                        moysElevesByYear.add(new NoteDevoir(moyGeneraleYear, new Double(20),
                                false,1.0));
                    }
                }
            }else{
                eleveJO.put("moyGeneraleEleve", "NN");
            }
            elevesJA.add(eleveJO);
        }

        return elevesJA;
    }

    private void setMapIdMatNotesAndListNote(Eleve eleve, JsonObject eleveJsonO, String field1,
                                             String field2, String field3, String field4){
        if(field3 != null){
            JsonArray eleveMoyByMatJson = eleveJsonO.getJsonArray(field3);
            eleve.setIdMatListNote(eleveMoyByMatJson,field1,field2);
        }
        if(field4 != null){
            Object moyGeneraleEleve = eleveJsonO.getValue(field4);
            // list<NoteDevoir> listDesMoyGénérale de l'élève
            if(moyGeneraleEleve != null && !moyGeneraleEleve.equals("") && !moyGeneraleEleve.equals("NN")){
                eleve.setListNotes(new NoteDevoir((Double)moyGeneraleEleve,new Double(20),
                        false, 1.0));
            }
        }

    }
}
