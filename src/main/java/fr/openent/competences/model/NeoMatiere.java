package fr.openent.competences.model;

import fr.openent.competences.helper.ModelHelper;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.constants.Field.*;

public class NeoMatiere implements IModel<NeoMatiere> {

    private String id;
    private String rank;
    private String label;
    private String lastUpdated;
    private String code;
    private String externalId;
    private String source;

    private String idStructure;
    private String name;
    private String externalIdSubject;

    // Constructor

    public NeoMatiere(JsonObject neoMatiere) {
        this.setId(neoMatiere.getString(ID, null))
                .setIdStructure(neoMatiere.getString(ID_STRUCTURE, null))
                .setName(neoMatiere.getString(NAME, null))
                .setRank(neoMatiere.getString(RANK, null))
                .setLastUpdated(neoMatiere.getString(LASTUPDATE, null))
                .setCode(neoMatiere.getString(CODE, null))
                .setLabel(neoMatiere.getString(LABEL, null));
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getRank() {
        return rank;
    }

    public String getLabel() {
        return label;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public String getCode() {
        return code;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getSource() {
        return source;
    }

    public String getIdStructure() {
        return idStructure;
    }

    public String getName() {
        return name;
    }

    public String getExternalIdSubject() {
        return externalIdSubject;
    }

    // Setters

    public NeoMatiere setId(String id) {
        this.id = id;
        return this;
    }

    public NeoMatiere setRank(String rank) {
        this.rank = rank;
        return this;
    }

    public NeoMatiere setLabel(String label) {
        this.label = label;
        return this;
    }

    public NeoMatiere setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public NeoMatiere setCode(String code) {
        this.code = code;
        return this;
    }

    public NeoMatiere setExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public NeoMatiere setSource(String source) {
        this.source = source;
        return this;
    }

    public NeoMatiere setIdStructure(String idStructure) {
        this.idStructure = idStructure;
        return this;
    }

    public NeoMatiere setName(String name) {
        this.name = name;
        return this;
    }

    public NeoMatiere setExternalIdSubject(String externalIdSubject) {
        this.externalIdSubject = externalIdSubject;
        return this;
    }

    // Functions

    public JsonObject toJson() {
        return ModelHelper.toJson(this, false, false);
    }
}
