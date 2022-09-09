package fr.openent.competences.bean;

import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatClass {
    /**
     * Map idEleve StatEleve
     */
    private Map<String,StatEleve> mapEleveStat;
    private Double averageClass;
    private Double min;
    private Double max;
    private UtilsService utilsService;

    public StatClass(){
        if(mapEleveStat == null ){
            mapEleveStat = new HashMap<String, StatEleve>();
        }
        utilsService = new DefaultUtilsService();
    }

    public Double getMin () {
        return min;
    }

    public void setMin (Double min) {
        this.min = min;
    }

    public Double getMax () {
        return max;
    }

    public void setMax (Double max) {
        this.max = max;
    }

    public Double getMoyenneEleve (String idEleve) {
        Double moyEleve = null;
        if(idEleve != null ) {
            StatEleve statEleve = this.mapEleveStat.get(idEleve);

            if (statEleve != null) {
                moyEleve = statEleve.getMoyenne();
            }
        }
        return moyEleve;
    }

    public void putMapEleveStat(String idEleve,Double moyFinale, NoteDevoir note){
        if(moyFinale != null) {//une moyenne finale par eleve
            if (!this.mapEleveStat.containsKey(idEleve)) {
                StatEleve statEleve = new StatEleve();
                statEleve.setMoyenneFinale(moyFinale);
                this.mapEleveStat.put(idEleve, statEleve);
            }else{
                if(this.mapEleveStat.get(idEleve).getMoyenneFinale() == null){
                    this.mapEleveStat.get(idEleve).setMoyenneFinale(moyFinale);
                }
            }
        }
        if(note != null) {
            if (this.mapEleveStat.containsKey(idEleve)) {
                StatEleve statEleve = this.mapEleveStat.get(idEleve);
                statEleve.getNoteDevoirList().add(note);
            } else {
                StatEleve statEleve = new StatEleve();
                statEleve.getNoteDevoirList().add(note);
                this.mapEleveStat.put(idEleve, statEleve);
            }
        }
    }

    public void putSousMatiereMapEleveStat(String idEleve, Long sousMatiereId, NoteDevoir note){
        if(note != null) {
            if (this.mapEleveStat.containsKey(idEleve)) {
                StatEleve statEleve = this.mapEleveStat.get(idEleve);
                utilsService.addToMap(sousMatiereId, statEleve.getNotesBySousMat(), note);
            } else {
                StatEleve statEleve = new StatEleve();
                utilsService.addToMap(sousMatiereId, statEleve.getNotesBySousMat(), note);
                this.mapEleveStat.put(idEleve, statEleve);
            }
        }
    }

    public Map<String, StatEleve> getMapEleveStat() {
        return mapEleveStat;
    }

    public void setMapEleveStat(Map<String, StatEleve> mapEleveStat) {
        this.mapEleveStat = mapEleveStat;
    }

    public Double getAverageClass() {

        if(averageClass != null) {
            return averageClass;
        }
        List<NoteDevoir> averageStudentList = new ArrayList<NoteDevoir>();
        if(!this.mapEleveStat.isEmpty()){
            for(Map.Entry<String,StatEleve> entrySetMapEleve : this.mapEleveStat.entrySet() ){

                averageStudentList.add(new NoteDevoir(entrySetMapEleve.getValue().getMoyenne(),
                        false, new Double(1)));

            }
            this.averageClass = ( !averageStudentList.isEmpty() ) ?
            utilsService.calculMoyenneParDiviseur(averageStudentList,false).getDouble("moyenne") : null;
        }else{
            this.averageClass = null;
        }

        return averageClass;
    }

    public Double getMinMaxClass(boolean min) {
        Double moyMin = null;
        Boolean first = true;
        for (Map.Entry<String, StatEleve> entry : this.mapEleveStat.entrySet()) {
            StatEleve statEleve = entry.getValue();
            if (first) {
                moyMin = statEleve.getMoyenne();
                first = false;
            } else if ((min)?(moyMin > statEleve.getMoyenne()):(moyMin < statEleve.getMoyenne())) {
                    moyMin = statEleve.getMoyenne();
            }
        }
        return moyMin;
    }

    public void setAverageClass(Double averageClass) {
        this.averageClass = averageClass;
    }
}
