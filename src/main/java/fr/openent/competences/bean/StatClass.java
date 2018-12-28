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
    private UtilsService utilsService;

    public StatClass(){
        if(mapEleveStat == null ){
            mapEleveStat = new HashMap<String, StatEleve>();
        }
        utilsService = new DefaultUtilsService();
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
            this.averageClass = utilsService.calculMoyenneParDiviseur(averageStudentList,
                    false).getDouble("moyenne");
        }else{
            this.averageClass = null;
        }

        return averageClass;
    }

    public void setAverageClass(Double averageClass) {
        this.averageClass = averageClass;
    }
}
