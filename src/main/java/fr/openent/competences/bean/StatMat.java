package fr.openent.competences.bean;

import fr.openent.competences.constants.Field;
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

            if( note.getString(Field.MOYENNE) != null && this.mapIdMatStatclass.containsKey(note.getString(Field.MOYENNE))){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString(Field.MOYENNE));

                if(note.getString(Field.ID_ELEVE_MOYENNE_FINALE) != null && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")) {

                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                            Double.valueOf(note.getString(Field.MOYENNE)),null);
                }
                if (note.getString(Field.ID_ELEVE) != null && note.getString(Field.VALEUR) != null && !(note.getValue(Field.MOYENNE) != null && note.getValue(Field.MOYENNE).equals("-100"))) {
                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE),null,
                            new NoteDevoir(
                                    Double.valueOf(note.getString(Field.VALEUR)),
                                    Double.valueOf(note.getString(Field.DIVISEUR)),
                                    note.getBoolean(Field.RAMENER_SUR),
                                    Double.valueOf(note.getString(Field.COEFFICIENT))));
                }

            }else {
                StatClass statClass = new StatClass();

                if (note.getString(Field.ID_ELEVE_MOYENNE_FINALE) != null && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")) {
                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                            Double.valueOf(note.getString(Field.MOYENNE)), null);
                }
                if (note.getString(Field.ID_ELEVE) != null && note.getString(Field.COEFFICIENT) != null
                        && note.getString(Field.VALEUR) != null && !(note.getValue(Field.MOYENNE) != null && note.getValue(Field.MOYENNE).equals("-100"))) {
                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE), null,
                            new NoteDevoir(
                                    Double.valueOf(note.getString(Field.VALEUR)),
                                    Double.valueOf(note.getString(Field.DIVISEUR)),
                                    note.getBoolean(Field.RAMENER_SUR),
                                    Double.valueOf(note.getString(Field.COEFFICIENT))));
                }
                this.mapIdMatStatclass.put(note.getString(Field.ID_MATIERE),statClass);
            }
            if(note.getString(Field.ID_MATIERE) == null && this.mapIdMatStatclass.containsKey(note.getString(Field.ID_MATIERE_MOYF))
            && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString(Field.ID_MATIERE_MOYF));
                statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                        Double.valueOf(note.getString(Field.MOYENNE)),null);
            }else if(note.getString(Field.ID_MATIERE) == null && !this.mapIdMatStatclass.containsKey(note.getString(Field.ID_MATIERE_MOYF))
            && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")){

                StatClass statClass = new StatClass();
                statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                        Double.valueOf(note.getString(Field.MOYENNE)), null);

                this.mapIdMatStatclass.put(note.getString(Field.ID_MATIERE_MOYF),statClass);
            }

        }

    }

    public void setMapIdMatStatclass(JsonArray listNotes, List<Service> services, JsonArray multiTeachers, final String idClasse) {
        for(int i = 0; i < listNotes.size(); i++){
            JsonObject note = listNotes.getJsonObject(i);

            if( note.getString(Field.ID_MATIERE) != null && this.mapIdMatStatclass.containsKey(note.getString(Field.ID_MATIERE))){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString(Field.ID_MATIERE));

                if(note.getString(Field.ID_ELEVE_MOYENNE_FINALE) != null && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")) {

                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                            Double.valueOf(note.getString(Field.MOYENNE)),null);
                }
                if (note.getString(Field.ID_ELEVE) != null && note.getString(Field.VALEUR) != null && !(note.getValue(Field.MOYENNE) != null && note.getValue(Field.MOYENNE).equals("-100"))) {
                    Matiere matiere = new Matiere(note.getString(Field.ID_MATIERE));
                    Teacher teacher = new Teacher(note.getString(Field.OWNER));
                    Group group = new Group(idClasse);

                    Service service = services.stream()
                            .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                                    && matiere.getId().equals(el.getMatiere().getId())
                                    && group.getId().equals(el.getGroup().getId()))
                            .findFirst().orElse(null);

                    if (service == null){
                        //On regarde les multiTeacher
                        for(Object mutliTeachO: multiTeachers){
                            JsonObject multiTeaching = (JsonObject) mutliTeachO;
                            if(multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(teacher.getId())
                                    && multiTeaching.getString(Field.ID_CLASSE).equals(group.getId())
                                    && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())){
                                service = services.stream()
                                        .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString(Field.SECOND_TEACHER_ID))
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }

                            if(multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId())
                                    && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                                    && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())){

                                service = services.stream()
                                        .filter(el -> multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(el.getTeacher().getId())
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }
                        }
                    }

                    Long sousMatiereId = note.getLong(Field.ID_SOUSMATIERE);
                    Long periodId = note.getLong(Field.ID_PERIODE);
                    NoteDevoir noteDevoir = new NoteDevoir(
                            Double.valueOf(note.getString(Field.VALEUR)),
                            Double.valueOf(note.getString(Field.DIVISEUR)),
                            note.getBoolean(Field.RAMENER_SUR),
                            Double.valueOf(note.getString(Field.COEFFICIENT)),
                            note.getString(Field.ID_ELEVE), periodId, service, sousMatiereId);
                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE),null, noteDevoir);
                    if (isNotNull(sousMatiereId)) {
                        statClass.putSousMatiereMapEleveStat(note.getString(Field.ID_ELEVE), sousMatiereId, noteDevoir);
                    }
                }

            }else {
                StatClass statClass = new StatClass();

                if (note.getString(Field.ID_ELEVE_MOYENNE_FINALE) != null && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")) {
                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                            Double.valueOf(note.getString(Field.MOYENNE)), null);
                }
                if (note.getString(Field.ID_ELEVE) != null && note.getString(Field.COEFFICIENT) != null
                        && note.getString(Field.VALEUR) != null && !(note.getValue(Field.MOYENNE) != null && note.getValue(Field.MOYENNE).equals("-100"))) {
                    Matiere matiere = new Matiere(note.getString(Field.ID_MATIERE));
                    Teacher teacher = new Teacher(note.getString(Field.OWNER));
                    Group group = new Group(idClasse);

                    Service service = services.stream()
                            .filter(el -> teacher.getId().equals(el.getTeacher().getId())
                                    && matiere.getId().equals(el.getMatiere().getId())
                                    && group.getId().equals(el.getGroup().getId()))
                            .findFirst().orElse(null);

                    if (service == null){
                        //On regarde les multiTeacher
                        for(Object mutliTeachO: multiTeachers){
                            //multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId()
                            JsonObject multiTeaching  =(JsonObject) mutliTeachO;
                            if(multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(teacher.getId())
                                    && multiTeaching.getString(Field.ID_CLASSE).equals(group.getId())
                                    && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())){
                                service = services.stream()
                                        .filter(el -> el.getTeacher().getId().equals(multiTeaching.getString(Field.SECOND_TEACHER_ID))
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }

                            if(multiTeaching.getString(Field.SECOND_TEACHER_ID).equals(teacher.getId())
                                    && multiTeaching.getString(Field.CLASS_OR_GROUP_ID).equals(group.getId())
                                    && multiTeaching.getString(Field.SUBJECT_ID).equals(matiere.getId())){

                                service = services.stream()
                                        .filter(el -> multiTeaching.getString(Field.MAIN_TEACHER_ID).equals(el.getTeacher().getId())
                                                && matiere.getId().equals(el.getMatiere().getId())
                                                && group.getId().equals(el.getGroup().getId()))
                                        .findFirst().orElse(null);
                            }
                        }
                    }

                    Long sousMatiereId = note.getLong(Field.ID_SOUSMATIERE);
                    Long id_periode = note.getLong(Field.ID_PERIODE);
                    NoteDevoir noteDevoir = new NoteDevoir(
                            Double.valueOf(note.getString(Field.VALEUR)),
                            Double.valueOf(note.getString(Field.DIVISEUR)),
                            note.getBoolean(Field.RAMENER_SUR),
                            Double.valueOf(note.getString(Field.COEFFICIENT)),
                            note.getString(Field.ID_ELEVE), id_periode, service, sousMatiereId);
                    statClass.putMapEleveStat(note.getString(Field.ID_ELEVE),null, noteDevoir);
                    if (isNotNull(sousMatiereId)) {
                        statClass.putSousMatiereMapEleveStat(note.getString(Field.ID_ELEVE), sousMatiereId, noteDevoir);
                    }
                }
                this.mapIdMatStatclass.put(note.getString(Field.ID_MATIERE),statClass);
            }
            if(note.getString(Field.ID_MATIERE) == null && this.mapIdMatStatclass.containsKey(note.getString(Field.ID_MATIERE_MOYF))
                    && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")){
                StatClass statClass = this.mapIdMatStatclass.get(note.getString(Field.ID_MATIERE_MOYF));
                statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                        Double.valueOf(note.getString(Field.MOYENNE)),null);
            }else if(note.getString(Field.ID_MATIERE) == null && !this.mapIdMatStatclass.containsKey(note.getString(Field.ID_MATIERE_MOYF))
                    && note.getValue(Field.MOYENNE) != null && !note.getValue(Field.MOYENNE).equals("-100")){

                StatClass statClass = new StatClass();
                statClass.putMapEleveStat(note.getString(Field.ID_ELEVE_MOYENNE_FINALE),
                        Double.valueOf(note.getString(Field.MOYENNE)), null);

                this.mapIdMatStatclass.put(note.getString(Field.ID_MATIERE_MOYF),statClass);
            }

        }

    }

}
