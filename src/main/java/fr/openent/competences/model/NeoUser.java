package fr.openent.competences.model;

import fr.openent.competences.helper.ModelHelper;
import io.vertx.core.json.JsonObject;

import static fr.openent.competences.constants.Field.*;
import static fr.openent.competences.constants.ThirdClassLevelMefCode.THIRD_CLASS_LEVEL_MEF_CODES;

public class NeoUser implements IModel<NeoUser> {

    private String id;
    private String displayName;
    private String firstName;
    private String lastName;
    private String module;
    private String moduleName;

    // Constructor

    public NeoUser(JsonObject neoUser) {
        this.setId(neoUser.getString(ID, null))
            .setDisplayName(neoUser.getString(DISPLAYNAME, null))
            .setFirstName(neoUser.getString(FIRSTNAME, null))
            .setLastName(neoUser.getString(LASTNAME, null))
            .setModule(neoUser.getString(MODULE, null))
            .setModuleName(neoUser.getString(MODULENAME, null));
    }

    // Getter

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getModule() {
        return module;
    }

    public String getModuleName() {
        return moduleName;
    }

    // Setter

    public NeoUser setId(String id) {
        this.id = id;
        return this;
    }

    public NeoUser setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public NeoUser setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public NeoUser setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public NeoUser setModule(String module) {
        this.module = module;
        return this;
    }

    public NeoUser setModuleName(String moduleName) {
        this.moduleName = moduleName;
        return this;
    }

    // Functions

    public boolean isInThirdClassLevel() {
        return this.module != null && THIRD_CLASS_LEVEL_MEF_CODES.contains(this.module);
    }

    public JsonObject toJson() {
        return ModelHelper.toJson(this, false, false);
    }
}