package fr.openent.competences.bean;

import fr.openent.competences.model.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.openent.competences.Utils.isNotNull;

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

    public void setMapIdMatStatclass (Map<String, StatClass> mapIdMatStatclass) {
        this.mapIdMatStatclass = mapIdMatStatclass;
    }

    public void setMapIdMatStatclass(JsonArray listNotes) {
        for(int i = 0; i < listNotes.size(); i++){
            JsonObject note = listNotes.getJsonObject(i);

            if( note.getString("id_matiere") != null && this.mapIdMatStatclass.containsKey(note.getString("id_matiere"))){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString("id_matiere"));

                if(note.getString("id_eleve_moyenne_finale") != null && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")) {

                    statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                            Double.valueOf(note.getString("moyenne")),null);
                }
                if (note.getString("id_eleve") != null && note.getString("valeur") != null && !(note.getValue("moyenne") != null && note.getValue("moyenne").equals("-100"))) {
                    statClass.putMapEleveStat(note.getString("id_eleve"),null,
                            new NoteDevoir(
                                    Double.valueOf(note.getString("valeur")),
                                    Double.valueOf(note.getInteger("diviseur")),
                                    note.getBoolean("ramener_sur"),
                                    Double.valueOf(note.getString("coefficient"))));
                }

            }else {
                StatClass statClass = new StatClass();

                if (note.getString("id_eleve_moyenne_finale") != null && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")) {
                    statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                            Double.valueOf(note.getString("moyenne")), null);
                }
                if (note.getString("id_eleve") != null && note.getString("coefficient") != null
                        && note.getString("valeur") != null && !(note.getValue("moyenne") != null && note.getValue("moyenne").equals("-100"))) {
                    statClass.putMapEleveStat(note.getString("id_eleve"), null,
                            new NoteDevoir(
                                    Double.valueOf(note.getString("valeur")),
                                    Double.valueOf(note.getInteger("diviseur")),
                                    note.getBoolean("ramener_sur"),
                                    Double.valueOf(note.getString("coefficient"))));
                }
                this.mapIdMatStatclass.put(note.getString("id_matiere"),statClass);
            }
            if(note.getString("id_matiere") == null && this.mapIdMatStatclass.containsKey(note.getString("id_matiere_moyf"))
            && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString("id_matiere_moyf"));
                statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                        Double.valueOf(note.getString("moyenne")),null);
            }else if(note.getString("id_matiere") == null && !this.mapIdMatStatclass.containsKey(note.getString("id_matiere_moyf"))
            && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")){

                StatClass statClass = new StatClass();
                statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                        Double.valueOf(note.getString("moyenne")), null);

                this.mapIdMatStatclass.put(note.getString("id_matiere_moyf"),statClass);
            }

        }

    }

    public void setMapIdMatStatclass(JsonArray listNotes, List<Service> services, JsonArray multiTeachers, final String idClasse) {
        for(int i = 0; i < listNotes.size(); i++){
            JsonObject note = listNotes.getJsonObject(i);

            if( note.getString("id_matiere") != null && this.mapIdMatStatclass.containsKey(note.getString("id_matiere"))){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString("id_matiere"));

                if(note.getString("id_eleve_moyenne_finale") != null && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")) {

                    statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                            Double.valueOf(note.getString("moyenne")),null);
                }
                if (note.getString("id_eleve") != null && note.getString("valeur") != null && !(note.getValue("moyenne") != null && note.getValue("moyenne").equals("-100"))) {
                    Matiere matiere = new Matiere(note.getString("id_matiere"));
                    Teacher teacher = new Teacher(note.getString("owner"));
                    Group group = new Group(idClasse);

                    Service service = services.stream()
                            .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                                    && matiere.getId().equals(el.getMatiere().getId())
                                    && group.getId().equals(el.getGroup().getId()))
                            .findFirst().orElse(null);

                    if (service == null){
                        //On regarde les multiTeacher
                        for(Object mutliTeachO: multiTeachers){
                            //multiTeaching.getString("second_teacher_id").equals(teacher.getId()
                            JsonObject multiTeaching  =(JsonObject) mutliTeachO;
                            if(multiTeaching.getString("main_teacher_id").equals(teacher.getId())
                                    && multiTeaching.getString("id_classe").equals(group.getId())
                                    && multiTeaching.getString("subject_id").equals(matiere.getId())){
                                service = services.stream()
                                        .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString("second_teacher_id"))
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }

                            if(multiTeaching.getString("second_teacher_id").equals(teacher.getId())
                                    && multiTeaching.getString("class_or_group_id").equals(group.getId())
                                    && multiTeaching.getString("subject_id").equals(matiere.getId())){

                                service = services.stream()
                                        .filter(el -> multiTeaching.getString("main_teacher_id").equals(el.getTeacher().getId())
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }
                        }
                    }

                    Long sousMatiereId = note.getLong("id_sousmatiere");
                    Long periodId = note.getLong("id_periode");
                    NoteDevoir noteDevoir = new NoteDevoir(
                            Double.valueOf(note.getString("valeur")),
                            Double.valueOf(note.getInteger("diviseur")),
                            note.getBoolean("ramener_sur"),
                            Double.valueOf(note.getString("coefficient")),
                            note.getString("id_eleve"), periodId, service, sousMatiereId);
                    statClass.putMapEleveStat(note.getString("id_eleve"),null, noteDevoir);
                    if (isNotNull(sousMatiereId)) {
                        statClass.putSousMatiereMapEleveStat(note.getString("id_eleve"), sousMatiereId, noteDevoir);
                    }
                }

            }else {
                StatClass statClass = new StatClass();

                if (note.getString("id_eleve_moyenne_finale") != null && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")) {
                    statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                            Double.valueOf(note.getString("moyenne")), null);
                }
                if (note.getString("id_eleve") != null && note.getString("coefficient") != null
                        && note.getString("valeur") != null && !(note.getValue("moyenne") != null && note.getValue("moyenne").equals("-100"))) {
                    Matiere matiere = new Matiere(note.getString("id_matiere"));
                    Teacher teacher = new Teacher(note.getString("owner"));
                    Group group = new Group(idClasse);

                    Service service = services.stream()
                            .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                                    && matiere.getId().equals(el.getMatiere().getId())
                                    && group.getId().equals(el.getGroup().getId()))
                            .findFirst().orElse(null);

                    if (service == null){
                        //On regarde les multiTeacher
                        for(Object mutliTeachO: multiTeachers){
                            //multiTeaching.getString("second_teacher_id").equals(teacher.getId()
                            JsonObject multiTeaching  =(JsonObject) mutliTeachO;
                            if(multiTeaching.getString("main_teacher_id").equals(teacher.getId())
                                    && multiTeaching.getString("id_classe").equals(group.getId())
                                    && multiTeaching.getString("subject_id").equals(matiere.getId())){
                                service = services.stream()
                                        .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString("second_teacher_id"))
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }

                            if(multiTeaching.getString("second_teacher_id").equals(teacher.getId())
                                    && multiTeaching.getString("class_or_group_id").equals(group.getId())
                                    && multiTeaching.getString("subject_id").equals(matiere.getId())){

                                service = services.stream()
                                        .filter(el -> multiTeaching.getString("main_teacher_id").equals(el.getTeacher().getId())
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }
                        }
                    }

                    Long sousMatiereId = note.getLong("id_sousmatiere");
                    Long id_periode = note.getLong("id_periode");
                    NoteDevoir noteDevoir = new NoteDevoir(
                            Double.valueOf(note.getString("valeur")),
                            Double.valueOf(note.getInteger("diviseur")),
                            note.getBoolean("ramener_sur"),
                            Double.valueOf(note.getString("coefficient")),
                            note.getString("id_eleve"), id_periode, service, sousMatiereId);
                    statClass.putMapEleveStat(note.getString("id_eleve"),null, noteDevoir);
                    if (isNotNull(sousMatiereId)) {
                        statClass.putSousMatiereMapEleveStat(note.getString("id_eleve"), sousMatiereId, noteDevoir);
                    }
                }
                this.mapIdMatStatclass.put(note.getString("id_matiere"),statClass);
            }
            if(note.getString("id_matiere") == null && this.mapIdMatStatclass.containsKey(note.getString("id_matiere_moyf"))
                    && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString("id_matiere_moyf"));
                statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                        Double.valueOf(note.getString("moyenne")),null);
            }else if(note.getString("id_matiere") == null && !this.mapIdMatStatclass.containsKey(note.getString("id_matiere_moyf"))
                    && note.getValue("moyenne") != null && !note.getValue("moyenne").equals("-100")){

                StatClass statClass = new StatClass();
                statClass.putMapEleveStat(note.getString("id_eleve_moyenne_finale"),
                        Double.valueOf(note.getString("moyenne")), null);

                this.mapIdMatStatclass.put(note.getString("id_matiere_moyf"),statClass);
            }

        }

    }

}
