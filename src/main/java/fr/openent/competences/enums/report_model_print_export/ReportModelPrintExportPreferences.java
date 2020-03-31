package fr.openent.competences.enums.report_model_print_export;

public enum ReportModelPrintExportPreferences {
    CLASS_AVERAGE ("moyenneClasse"),
    GENERAL_AVEREGE("moyenneGenerale"),
    GET_PROGRAM_ELEMENTS("getProgramElements"),
    GET_RESPONSABLE("getResponsable"),
    COEFFICIENT("coefficient"),
    CLASS_AVERAGE_MIN_MAX("classAverageMinMax"),
    STUDENT_AVERAGE("moyenneEleve"),
    SHOW_BILAN_PER_DOMAIN("showBilanPerDomaines"),
    SHOW_PROJECTS("showProjects"),
    CLASS_AVERAGE_UNDER_SUBJECT("moyenneClasseSousMat"),
    ANNUAL_AVERAGE("moyenneAnnuelle"),
    WITH_LEVELS_STUDENT("withlevelsStudent"),
    SHOW_FAMILY("showFamily"),
    WITH_LEVELS_CLASS("withLevelsClass"),
    POSITIONING("positionnement"),
    STUDENT_RANK("studentRank"),
    USE_MODEL("useModel"),
    AGRICULTURE_LOGO("agricultureLogo"),
    STUDENT_AVERAGE_UNDER_SUBJECT("moyenneEleveSousMat"),
    POSITIONING_UNDER_SUBJECT("positionnementSousMat"),
    SIMPLE("simple");
    
    private String preference ;

    ReportModelPrintExportPreferences(String preference) {
        this.preference = preference ;
    }

    public String getString() {
        return  this.preference ;
    }
}
