package fr.openent.competences.model;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.security.PrivilegedAction;

import static fr.openent.competences.Utils.getLibelle;
import static fr.openent.competences.Utils.isNotNull;
import static fr.openent.competences.service.impl.DefaultExportService.COEFFICIENT;

public class ParamsBulletins {
    private String imGraph;
    private  boolean hasGraphPerDomaine;
    private final JsonObject params = new JsonObject();
    private  boolean hasImgLoaded;
    private static final String GRAPH_PER_DOMAINE = "graphPerDomaine";
    private static final String SUIVI_ACQUIS_LIBELLE = "suiviAcquisLibelle";
    private static final String COMMUNICATION_LIBELLE = "communicationLibelle";
    private static final String COMMUNICATION_HEADER_RIGHT_FIRST = "communicationHeaderRightFirst";
    private static final String COMMUNICATION_HEADER_RIGHT_SECOND = "communicationHeaderRightSecond";
    private static final String MOYENNE_CLASSE_LIBELLE = "moyenneClasseLibelle";
    private static final String SUIVI_ELEMENT_LIBELLE = "suiviElementLibelle";
    private static final String SUIVI_ACQUISITION_LIBELLE = "suiviAcquisitionLibelle";
    private static final String POSITIONEMENT_LIBELLE = "positionementLibelle";
    private static final String STUDENT_RANK_LIBELLE = "studentRankLibelle";
    private static final String CLASS_AVERAGE_MIN_LIBELLE = "classAverageMinLibelle";
    private static final String CLASS_AVERAGE_MAX_LIBELLE = "classAverageMaxLibelle";
    private static final String MOYENNE_STUDENT_LIBELLE = "moyenneStudentLibelle";
    private static final String BILAN_PERIODIQUE_LIBELLE = "bilanAcquisitionLibelle";
    private static final String VIESCOLAIRE_LIBELLE = "viescolaireLibelle";
    private static final String FAMILY_VISA = "familyVisa";
    private static final String SIGNATURE = "signature";
    private static final String BORN_AT = "bornAt";
    private static final String CLASS_OF = "classeOf";
    private static final String NUMBER_INE = "numberINE";
    private static final String BILAN_PER_DOMAINE_LIBELLE = "bilanPerDomainesLibelle";
    private static final String LEVEL_STUDENT = "levelStudent";
    private static final String LEVEL_CLASS = "levelClass";
    private static final String LEVEL_ITEMS = "levelItems";
    private static final String AVERAGE_STUDENT = "averageStudent";
    private static final String AVERAGE_CLASS = "averageClass";
    private static final String AVERAGES = "averages";
    private static final String EVALUATION_BY_DOMAINE = "evaluationByDomaine";
    private static final String COEFFICIENT_LIBELLE = "coefficientLibelle";
    private static final String MOYENNE_ANUELLE_LIBELLE = "moyenneAnnuelleLibelle";
    private static final String MOYENNE_GENERALE_LIBELLE = "moyenneGeneraleLibelle";
    // Parameter Key
    private static final String GET_MOYENNE_CLASSE = "getMoyenneClasse";
    private static final String GET_MOYENNE_ELEVE = "getMoyenneEleve";
    private static final String GET_STUDENT_RANK = "getStudentRank";
    private static final String GET_CLASS_AVERAGE_MINMAX = "getClassAverageMinMax";
    private static final String GET_POSITIONNEMENT = "getPositionnement";
    private static final String GET_PROGRAM_ELEMENT = "getProgramElements";
    private static final String HAS_IMG_STRUCTURE = "hasImgStructure";
    private static final String HAS_IMG_SIGNATURE = "hasImgSignature";
    private static final String IMG_STRUCTURE = "imgStructure";
    private static final String IMG_SIGNATURE = "imgSignature";
    private static final String NAME_CE = "nameCE";
    private static final String SHOW_PROJECTS = "showProjects";
    private static final String SHOW_BILAN_PER_DOMAINE = "showBilanPerDomaines";
    private static final String SHOW_FAMILY = "showFamily";
    private static final String GET_RESPONSABLE = "getResponsable";
    private static final String NEUTRE = "neutre";
    private static final String PRINT_SOUS_MATIERES = "printSousMatieres";
    private static final String PRINT_COEFFICIENT = "printCoefficient";
    private static final String PRINT_MOYENNE_GENERALE = "printMoyenneGenerale";
    private static final String PRINT_MOYENNE_ANNUELLE = "printMoyenneAnnuelle";
    private static final String HIDE_HEADTEACHER = "hideHeadTeacher";
    private static final String ADD_OTHER_TEACHER = "addOtherTeacher";
    private static final String FUNCTION_OTHER_TEACHER = "functionOtherTeacher";
    private static final String OTHER_TEACHER_NAME = "otherTeacherName";
    private static final String AGRICULTURE_LOGO = "agricultureLogo";
    private static final String MOYENNE_CLASSE = "moyenneClasse";
    private static final String MOYENNE_ELEVE = "moyenneEleve";
    private static final String STUDENT_RANK = "studentRank";
    private static final String CLASS_AVERAGE_MINMAX = "classAverageMinMax";
    private static final String POSITIONNEMENT = "positionnement";
    private static final String MOYENNE_ANNUELLE = "moyenneAnnuelle";
    private static final String NIVEAU_COMPETENCE = "niveauCompetences";
    private static final String LIBELLE = "libelle";
    private static final String LOGO_PATH = "pathLogoImg";

    public ParamsBulletins() {
        params.put(SUIVI_ACQUIS_LIBELLE, getLibelle("evaluation.bilan.periodique.suivi.acquis")
                + " " + getLibelle("of.student"))
                .put(COMMUNICATION_LIBELLE, getLibelle("viescolaire.communication.with.familly"))
                .put(COMMUNICATION_HEADER_RIGHT_FIRST,
                        getLibelle("evaluations.export.bulletin.communication.header.right.first"))
                .put(COMMUNICATION_HEADER_RIGHT_SECOND,
                        getLibelle("evaluations.export.bulletin.communication.header.right.second"))
                .put(MOYENNE_CLASSE_LIBELLE, getLibelle("average.min.classe"))
                .put(SUIVI_ELEMENT_LIBELLE,
                        getLibelle("evaluations.export.bulletin.element.programme.libelle"))
                .put(SUIVI_ACQUISITION_LIBELLE,
                        getLibelle("evaluations.export.bulletin.element.appreciation.libelle"))
                .put(POSITIONEMENT_LIBELLE, getLibelle("evaluations.releve.positionnement.min") + '*')
                .put(STUDENT_RANK_LIBELLE,getLibelle("sudent.rank"))
                .put(CLASS_AVERAGE_MIN_LIBELLE, getLibelle("classaverage.min"))
                .put(CLASS_AVERAGE_MAX_LIBELLE, getLibelle("classaverage.max"))
                .put(MOYENNE_STUDENT_LIBELLE, getLibelle("average.min.eleve"))
                .put(BILAN_PERIODIQUE_LIBELLE, getLibelle("viescolaire.suivi.des.acquis.libelle.export"))
                .put(VIESCOLAIRE_LIBELLE, getLibelle("evaluations.export.bulletin.viescolaireLibelle"))
                .put(FAMILY_VISA, getLibelle("evaluations.export.bulletin.visa.libelle"))
                .put(SIGNATURE, getLibelle("evaluations.export.bulletin.date.name.visa.responsable"))
                .put(BORN_AT, getLibelle("born.on"))
                .put(CLASS_OF, getLibelle("classe.of"))
                .put(NUMBER_INE, getLibelle("number.INE"))
                .put(BILAN_PER_DOMAINE_LIBELLE, getLibelle("evaluations.bilan.by.domaine"))
                .put(LEVEL_STUDENT, getLibelle("level.student"))
                .put(LEVEL_CLASS, getLibelle("level.class"))
                .put(AVERAGE_STUDENT, getLibelle("average.student"))
                .put(AVERAGE_CLASS, getLibelle("average.class"))
                .put(LEVEL_ITEMS, getLibelle("level.items"))
                .put(AVERAGES, getLibelle("average"))
                .put(EVALUATION_BY_DOMAINE, getLibelle("evaluation.by.domaine"))
                .put(COEFFICIENT_LIBELLE, getLibelle("viescolaire.utils.coef"))
                .put(MOYENNE_ANUELLE_LIBELLE, getLibelle("average.annual"))
                .put(MOYENNE_GENERALE_LIBELLE, getLibelle("average.general"));

    }

    public void setParams(JsonObject otherParams){
        params.put(GET_RESPONSABLE, otherParams.getBoolean(GET_RESPONSABLE))
                .put(GET_MOYENNE_CLASSE, otherParams.getBoolean(MOYENNE_CLASSE))
                .put(GET_MOYENNE_ELEVE, otherParams.getBoolean(MOYENNE_ELEVE))
                .put(GET_STUDENT_RANK, otherParams.getBoolean(STUDENT_RANK))
                .put(GET_CLASS_AVERAGE_MINMAX, otherParams.getBoolean(CLASS_AVERAGE_MINMAX))
                .put(GET_POSITIONNEMENT, otherParams.getBoolean(POSITIONNEMENT))
                .put(SHOW_PROJECTS, otherParams.getBoolean(SHOW_PROJECTS))
                .put(SHOW_FAMILY, otherParams.getBoolean(SHOW_FAMILY))
                .put(GET_PROGRAM_ELEMENT, otherParams.getBoolean(GET_PROGRAM_ELEMENT))
                .put(SHOW_BILAN_PER_DOMAINE, otherParams.getBoolean(SHOW_BILAN_PER_DOMAINE))
                .put(IMG_STRUCTURE, otherParams.getString(IMG_STRUCTURE))
                .put(HAS_IMG_STRUCTURE, otherParams.getBoolean(HAS_IMG_STRUCTURE))
                .put(IMG_SIGNATURE, otherParams.getString(IMG_SIGNATURE))
                .put(HAS_IMG_SIGNATURE, otherParams.getBoolean(HAS_IMG_SIGNATURE))
                .put(NAME_CE, otherParams.getString(NAME_CE))
                .put(PRINT_COEFFICIENT, otherParams.getBoolean(COEFFICIENT))
                .put(PRINT_SOUS_MATIERES, otherParams.getBoolean(PRINT_SOUS_MATIERES))
                .put(PRINT_MOYENNE_ANNUELLE, otherParams.getBoolean(MOYENNE_ANNUELLE))
                .put(NEUTRE, otherParams.getBoolean(NEUTRE, false))
                .put(HIDE_HEADTEACHER, otherParams.getBoolean(HIDE_HEADTEACHER,false))
                .put(ADD_OTHER_TEACHER, otherParams.getBoolean(ADD_OTHER_TEACHER,false))
                .put(FUNCTION_OTHER_TEACHER, otherParams.getString(FUNCTION_OTHER_TEACHER,""))
                .put(OTHER_TEACHER_NAME, otherParams.getString(OTHER_TEACHER_NAME,""))
                .put(AGRICULTURE_LOGO, otherParams.getBoolean(AGRICULTURE_LOGO,false));
        JsonArray niveauCompetences;
        try{
            niveauCompetences = (JsonArray) params.getValue(NIVEAU_COMPETENCE);

        }catch (java.lang.ClassCastException e){
            niveauCompetences = new JsonArray(params.getString(NIVEAU_COMPETENCE));
        }
        JsonArray footerArray = new JsonArray();
        if(niveauCompetences != null && !niveauCompetences.isEmpty()){
            for (int i = niveauCompetences.size() - 1; i >= 0; i--) { //reverse Array
                footerArray.add(niveauCompetences.getJsonObject(i));
            }
        }
        String caption = "";
        if(!footerArray.isEmpty()){
            for (int i = 0; i < footerArray.size(); i++) {
                JsonObject niv = footerArray.getJsonObject(i);

                String lib = niv.getString(LIBELLE);
                Integer id_niv;
                Integer id_cycle = niv.getInteger("id_cycle");
                try{
                    id_niv = niv.getInteger("id_niveau");
                    if(id_cycle == 2){
                        id_niv -= 4;
                    }
                }catch (NullPointerException e){
                    id_niv = id_cycle;
                }

                caption += id_niv + " : " + lib + " - ";
            }
            caption = caption.substring(0, caption.length() - 2);
        }

        params.put(NIVEAU_COMPETENCE, niveauCompetences).put("caption", "* " + caption);

        if(isNotNull(params.getValue(AGRICULTURE_LOGO)) && params.getBoolean(AGRICULTURE_LOGO)){
            params.put(LOGO_PATH, "img/ministere_agriculture.png");
        } else {
            params.put(LOGO_PATH, "img/education_nationale.png");
        }

    }

    public boolean hasImgLoaded() {
        return hasImgLoaded;
    }

    public void setHasImgLoaded(boolean hasImgLoaded) {
        this.hasImgLoaded = hasImgLoaded;
        params.put("hasImgLoaded",hasImgLoaded);
    }

    public String getImGraph() {
        return imGraph;
    }

    public void addParams(JsonObject othersParams){
        params.mergeIn(othersParams);
    }

    public void setImGraph(String imGraph) {
        this.imGraph = imGraph;
    }

    public boolean isHasGraphPerDomaine() {
        return hasGraphPerDomaine;
    }

    public void setHasGraphPerDomaine(boolean hasGraphPerDomaine) {
        this.hasGraphPerDomaine = hasGraphPerDomaine;
    }
    public JsonObject toJson(){
        return params;
    }
}
