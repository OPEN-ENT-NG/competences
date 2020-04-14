package fr.openent.competences.enums.report_model_print_export;

public enum ReportModelPrintExportModelPreferencesText {
    MENTION_OPTION("mentionOpinion"),
    ORIENTATION_OPTION("orientationOpinion"),
    NAME_CE("nameCE"),
    IMG_STRUCTURE("imgStructure"),
    IMG_SIGNATURE("imgSignature"),
    OTHER_TEACHER_ID("otherTeacherId"),
    OTHER_TEACHER_NAME("otherTeacherName"),
    MENTION_CLASS ("mentionClass"),
    ID_MODEL("idModel"),
    FUNCTION_OTHER_TEACHER("functionOtherTeacher");

    private String preference;

    ReportModelPrintExportModelPreferencesText(String element) {
        this.preference = element;
    }

    public String getString() {
        return this.preference;
    }
}
