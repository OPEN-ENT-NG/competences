package fr.openent.competences.enums.report_model_print_export;

public enum ReportModelPrintExportMongo {
    COLLECTION_REPORT_MODEL("report_model_print_export"),
    KEY_ID("_id"),
    KEY_USER_ID("userId"),
    KEY_STRUCTUREID("structureId"),
    KEY_TITLE("title"),
    KEY_SELECTED("selected"),
    KEY_PREFERENCES_CHECKBOX("preferencesCheckbox"),
    KEY_PREFERENCES_TEXT("preferencesText"),
    STATUS("status");

    private String elementMongo;

    ReportModelPrintExportMongo(String element) {
        this.elementMongo = element;
    }

    public String getString() {
        return this.elementMongo;
    }
}
