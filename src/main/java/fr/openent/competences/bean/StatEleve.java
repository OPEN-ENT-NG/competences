package fr.openent.competences.bean;

import fr.openent.competences.model.Service;
import fr.openent.competences.model.SubTopic;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatEleve {

    private List<NoteDevoir> noteDevoirList;

    private HashMap<Long, ArrayList<NoteDevoir>> notesBySousMat;
    private Double finalAverage;
    private Double averageAuto;
    private UtilsService utilsService;

    public StatEleve(){
        if(noteDevoirList == null){
            noteDevoirList = new ArrayList<NoteDevoir>();
        }
        utilsService = new DefaultUtilsService();
        this.notesBySousMat = new HashMap<>();
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

    public HashMap<Long, ArrayList<NoteDevoir>> getNotesBySousMat() {
        return notesBySousMat;
    }

    public void setNotesBySousMat(HashMap<Long, ArrayList<NoteDevoir>> notesBySousMat) {
        this.notesBySousMat = notesBySousMat;
    }

    public Double getMoyenneAuto() {
        if(averageAuto != null) {
            return averageAuto;
        }

        if(!this.notesBySousMat.isEmpty()) {
            //Si on a des sous-matières, on calcule la moyenne par sous-matière, puis la moyenne de la matière.
            double total = 0;
            double totalCoeff = 0;

            for (Map.Entry<Long, ArrayList<NoteDevoir>> subEntry :
                    notesBySousMat.entrySet()) {
                Long idSousMat = subEntry.getKey();
                Service serv = subEntry.getValue().get(0).getService();
                double coeff = 1.d;
                if (serv != null && serv.getSubtopics() != null && serv.getSubtopics().size() > 0) {
                    SubTopic subTopic = serv.getSubtopics().stream()
                            .filter(el ->
                                    el.getId().equals(idSousMat)
                            ).findFirst().orElse(null);
                    if (subTopic != null)
                        coeff = subTopic.getCoefficient();
                }

                Double moyenSousMat = utilsService.calculMoyenne(subEntry.getValue(), false, 20, false).getDouble("moyenne");

                total += coeff * moyenSousMat;
                totalCoeff += coeff;
            }
            averageAuto = Math.round((total / totalCoeff) * 10.0) / 10.0;
        }
        else {
            averageAuto = utilsService.calculMoyenne(this.noteDevoirList,false,20,false).getDouble("moyenne");
        }

        return averageAuto;
    }

    public void setMoyenneAuto(Double moyenneAuto) {
        this.averageAuto =  moyenneAuto;
    }
}
