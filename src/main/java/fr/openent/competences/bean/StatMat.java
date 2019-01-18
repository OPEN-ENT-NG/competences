package fr.openent.competences.bean;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class StatMat {

    Map<String,StatClass> mapIdMatStatclass ;


    public StatMat (){
        if(mapIdMatStatclass == null){
            mapIdMatStatclass = new HashMap<String,StatClass>();
        }
    }

    public Map<String, StatClass> getMapIdMatStatclass() {
        return mapIdMatStatclass;
    }

    public void setMapIdMatStatclass(JsonArray listNotes) {
        for(int i = 0; i < listNotes.size(); i++){
            JsonObject note = listNotes.getJsonObject(i);

            if( note.getString("id_matiere") != null && this.mapIdMatStatclass.containsKey(note.getString("id_matiere"))){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString("id_matiere"));

                if(note.getString("id_eleve_moyenne_finale") != null) {

                    statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                            Double.valueOf(note.getString("moyenne")),null);
                }
                if (note.getString("id_eleve") != null && note.getString("valeur") != null) {
                    statClass.putMapEleveStat(note.getString("id_eleve"),null,
                            new NoteDevoir(
                                    Double.valueOf(note.getString("valeur")),
                                    Double.valueOf(note.getInteger("diviseur")),
                                    note.getBoolean("ramener_sur"),
                                    Double.valueOf(note.getString("coefficient"))));
                }

            }else {
                StatClass statClass = new StatClass();

                if (note.getString("id_eleve_moyenne_finale") != null) {
                    statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                            Double.valueOf(note.getString("moyenne")), null);
                }
                if (note.getString("id_eleve") != null && note.getString("valeur") != null) {
                    if (note.getString("id_eleve") != null && note.getString("valeur") != null) {
                        statClass.putMapEleveStat(note.getString("id_eleve"), null,
                                new NoteDevoir(
                                        Double.valueOf(note.getString("valeur")),
                                        Double.valueOf(note.getInteger("diviseur")),
                                        note.getBoolean("ramener_sur"),
                                        Double.valueOf(note.getString("coefficient"))));
                    }
                }
                this.mapIdMatStatclass.put(note.getString("id_matiere"),statClass);
            }
            if(note.getString("id_matiere") == null && this.mapIdMatStatclass.containsKey(note.getString("id_matiere_moyf"))){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString("id_matiere_moyf"));
                statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                        Double.valueOf(note.getString("moyenne")),null);
            }else if(note.getString("id_matiere") == null && !this.mapIdMatStatclass.containsKey(note.getString("id_matiere_moyf"))){

                StatClass statClass = new StatClass();
                statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                        Double.valueOf(note.getString("moyenne")), null);

                this.mapIdMatStatclass.put(note.getString("id_matiere_moyf"),statClass);
            }

        }

    }



}
