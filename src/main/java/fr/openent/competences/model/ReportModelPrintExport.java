package fr.openent.competences.model;

import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportMongo;
import fr.openent.competences.enums.report_model_print_export.ReportModelPrintExportPreferences;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class ReportModelPrintExport extends Model implements Cloneable{

    private String id;
    private String userId;
    private String title;
    private Boolean selected;
    private List<String> preferences;
    private ArrayList<String> allKeyPreferences = new ArrayList();

    // Constructor new report model
    public ReportModelPrintExport(String id, String userId, String title, Boolean selected, JsonObject preferences) {
        super();
        this.initListPreferences();
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.selected = selected;
        if(preferences != null) this.setPreferences(preferences);
    }

    //Getters
    public String getId(){ return this.id;}
    public String getUserId(){ return this.userId;}
    public String getTitle(){ return this.title;}
    public Boolean getSelected(){ return this.selected;}
    public List<String> getPreferences(){ return this.preferences;}

    //Setters
    public void setPreferences (JsonObject dirtyPreferences){
        if(dirtyPreferences != null){
            List<String> cleanPreferences = new ArrayList<String>();
            for(String preferenceKey:  this.allKeyPreferences){
                if(dirtyPreferences.containsKey(preferenceKey)
                        && dirtyPreferences.getValue(preferenceKey) != null){
                    if(dirtyPreferences.getBoolean(preferenceKey)) cleanPreferences.add(preferenceKey);
                }
            }
            this.preferences = cleanPreferences;
        }
    }

    //Methods
    @Override
    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        if(this.id != null) json.put(ReportModelPrintExportMongo.KEY_ID.getString(), this.getId());
        if(this.userId != null) json.put(ReportModelPrintExportMongo.KEY_USER_ID.getString(), this.getUserId());
        if(this.title != null) json.put(ReportModelPrintExportMongo.KEY_TITLE.getString(), this.getTitle());
        if(this.selected != null) json.put(ReportModelPrintExportMongo.KEY_SELECTED.getString(), this.getSelected());
        if(this.preferences != null) json.put(ReportModelPrintExportMongo.KEY_PREFERENCES.getString(), this.getPreferences());
        return json;
    }

    @Override
    public ReportModelPrintExport clone(){
        try {
            return (ReportModelPrintExport) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    private void initListPreferences(){
        for(ReportModelPrintExportPreferences preference : ReportModelPrintExportPreferences.values()) {
            this.allKeyPreferences.add(preference.getString());
        }
    }
}
