/*
For add new preference checkbox or preference text,
just you must add key in reportModelPrintExportConstants frontend and backend
 */

package fr.openent.competences.model;

import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportModelPreferencesText;
import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportMongo;
import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportPreferencesCheckbox;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class ReportModelPrintExport extends Model implements Cloneable {

    private String id;
    private String userId;
    private String structureId;
    private String title;
    private Boolean selected;
    private List<String> preferencesCheckbox;
    private JsonObject preferencesText;
    private ArrayList<String> allKeyPreferencesCheckbox = new ArrayList();
    private ArrayList<String> allKeyPreferencesText = new ArrayList();

    // Constructor new report model
    public ReportModelPrintExport() {
        super();
    }

    public ReportModelPrintExport(
            String structureId,
            String title,
            Boolean selected,
            JsonObject preferencesCheckbox,
            JsonObject preferencesText) {
        super();
        this.initListPreferencesCheckbox();
        this.initListPreferencesText();
        this.structureId = structureId;
        this.title = title;
        this.selected = selected;
        if (preferencesCheckbox != null) this.setPreferencesCheckbox(preferencesCheckbox);
        if (preferencesText != null) this.setPreferencesText(preferencesText);
    }

    //Getters
    public String getId() {
        return this.id;
    }

    public String getUserId() {
        return this.userId;
    }
    public String getStructureId() {
        return this.structureId;
    }

    public String getTitle() {
        return this.title;
    }

    public Boolean getSelected() {
        return this.selected;
    }

    public List<String> getPreferencesCheckbox() {
        return this.preferencesCheckbox;
    }

    public JsonObject getPreferencesText() {
        return this.preferencesText;
    }

    //Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setStructureId(String structureId) {
        this.structureId = structureId;
    }

    private void setPreferencesCheckbox(JsonObject dirtyPreferences) {
        if (dirtyPreferences != null) {
            List<String> cleanPreferences = new ArrayList<String>();
            for (String preferenceKey : this.allKeyPreferencesCheckbox) {
                if (dirtyPreferences.containsKey(preferenceKey)
                        && dirtyPreferences.getValue(preferenceKey) != null) {
                    if (dirtyPreferences.getBoolean(preferenceKey)) cleanPreferences.add(preferenceKey);
                }
            }
            this.preferencesCheckbox = cleanPreferences;
        }
    }

    private void setPreferencesText(JsonObject dirtyPreferences) {
        if (dirtyPreferences != null) {
            JsonObject cleanPreferences = new JsonObject();
            for (String preferenceKey : this.allKeyPreferencesText) {
                if (dirtyPreferences.containsKey(preferenceKey)
                        && dirtyPreferences.getValue(preferenceKey) != null) {
                    if(dirtyPreferences.getValue(preferenceKey).getClass().equals(String.class) ){
                        cleanPreferences.put(preferenceKey, dirtyPreferences.getString(preferenceKey));
                    }
                    if(dirtyPreferences.getValue(preferenceKey).getClass().equals(Integer.class) ){
                        cleanPreferences.put(preferenceKey, dirtyPreferences.getInteger(preferenceKey));
                    }
                }
            }
            this.preferencesText = cleanPreferences;
        }
    }

    //Methods
    @Override
    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        if (this.getId() != null) json.put(ReportModelPrintExportMongo.KEY_ID.getString(), this.getId());
        if (this.getStructureId() != null) json.put(ReportModelPrintExportMongo.KEY_STRUCTUREID.getString(), this.getStructureId());
        if (this.getUserId() != null) json.put(ReportModelPrintExportMongo.KEY_USER_ID.getString(), this.getUserId());
        if (this.getTitle() != null) json.put(ReportModelPrintExportMongo.KEY_TITLE.getString(), this.getTitle());
        if (this.getSelected() != null)
            json.put(ReportModelPrintExportMongo.KEY_SELECTED.getString(), this.getSelected());
        if (this.getPreferencesCheckbox() != null)
            json.put(ReportModelPrintExportMongo.KEY_PREFERENCES_CHECKBOX.getString(), this.getPreferencesCheckbox());
        if (this.getPreferencesText() != null)
            json.put(ReportModelPrintExportMongo.KEY_PREFERENCES_TEXT.getString(), this.getPreferencesText());
        return json;
    }

    @Override
    public ReportModelPrintExport clone() {
        try {
            return (ReportModelPrintExport) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    private void initListPreferencesCheckbox() {
        for (ReportModelPrintExportPreferencesCheckbox preference : ReportModelPrintExportPreferencesCheckbox.values()) {
            this.allKeyPreferencesCheckbox.add(preference.getString());
        }
    }

    private void initListPreferencesText() {
        for (ReportModelPrintExportModelPreferencesText preference : ReportModelPrintExportModelPreferencesText.values()) {
            this.allKeyPreferencesText.add(preference.getString());
        }
    }
}
