package fr.openent.competences.bean;

import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;


import java.util.ArrayList;
import java.util.List;

public class StatEleve {

    private List<NoteDevoir> noteDevoirList;
    private Double finalAverage;
    private Double averageAuto;
    private UtilsService utilsService;

    public StatEleve(){
        if(noteDevoirList == null){
            noteDevoirList = new ArrayList<NoteDevoir>();
        }
        utilsService = new DefaultUtilsService();
    }
    public Double getMoyenne (){

        if(this.finalAverage != null){
            return finalAverage;
        }else if(this.averageAuto != null){
            return this.averageAuto;
        }else{
            if(!this.noteDevoirList.isEmpty()){

              return getMoyenneAuto();
            }else {
                return null;
            }

        }

    }

    public List<NoteDevoir> getNoteDevoirList() {
        return noteDevoirList;
    }

    public void setNoteDevoirList(List<NoteDevoir> noteDevoirList) {
        this.noteDevoirList = noteDevoirList;
    }

    public Double getMoyenneFinale() {
        return finalAverage;
    }

    public void setMoyenneFinale(Double moyenneFinale) {
        this.finalAverage = moyenneFinale;
    }

    public Double getMoyenneAuto() {
        if(averageAuto != null) {
            return averageAuto;
        }
        averageAuto = utilsService.calculMoyenne(this.noteDevoirList,false,20,false).getDouble("moyenne");

        return averageAuto;
    }

    public void setMoyenneAuto(Double moyenneAuto) {
        this.averageAuto =  moyenneAuto;
    }
}
