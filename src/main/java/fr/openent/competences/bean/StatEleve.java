package fr.openent.competences.bean;

import fr.openent.competences.constants.Field;
import fr.openent.competences.model.Service;
import fr.openent.competences.model.SubTopic;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatEleve {

    private List<NoteDevoir> noteDevoirList;

    private Map<Long, ArrayList<NoteDevoir>> notesBySousMat;
    private Double finalAverage;
    private Double averageAuto;
    private UtilsService utilsService;
    protected static final Logger log = LoggerFactory.getLogger(StatEleve.class);

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

    public Map<Long, ArrayList<NoteDevoir>> getNotesBySousMat() {
        return notesBySousMat;
    }

    public void setNotesBySousMat(Map<Long, ArrayList<NoteDevoir>> notesBySousMat) {
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
            boolean stat = false;
            boolean annual = false;

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

                Double moyenSousMat = utilsService.calculMoyenne(subEntry.getValue(), stat, Field.DIVISEUR_NOTE, annual).getDouble(Field.MOYENNE);

                total += coeff * moyenSousMat;
                totalCoeff += coeff;
            }
            if (totalCoeff == 0) {
                log.error("Found a 0 or negative coefficient in getMoyenneAuto, please check your subtopics " +
                        "coefficients (value of totalCoeff : " + totalCoeff + ")");
                return null;
            }
            averageAuto = Math.round((total / totalCoeff) * Field.ROUNDER) / Field.ROUNDER;
        }
        else {
            averageAuto = utilsService.calculMoyenne(this.noteDevoirList,false, Field.DIVISEUR_NOTE,false).getDouble(Field.MOYENNE);
        }

        return averageAuto;
    }

    public void setMoyenneAuto(Double moyenneAuto) {
        this.averageAuto =  moyenneAuto;
    }
}
