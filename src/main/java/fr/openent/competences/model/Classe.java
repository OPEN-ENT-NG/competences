package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Classe extends Model{
    private String name;
    private Cycle cycle;
    private Periode periode;
    private String displayName;
    private final List<Group> groupes = new ArrayList<>();
    private final List<Service> services = new ArrayList<>();
    private final List<MultiTeaching> multiTeachers = new ArrayList<>();
    public Classe(){
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public void setCycle(Cycle cycle) {
        this.cycle = cycle;
    }

    public List<Group> getGroupes() {
        return groupes;
    }

    public void addGroup(Group group){
        groupes.add(group);
    }

    public Periode getPeriode() {
        return periode;
    }


    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public List<Service> getServices() {
        return services;
    }
    public void addServices(List<Service> services){
        this.services.addAll(services);
    }

    public List<MultiTeaching> getMultiTeachers() {
        return multiTeachers;
    }
    public void addMultiTeaching(List<MultiTeaching> multiTeachers){
        this.multiTeachers.addAll(multiTeachers);
    }
    public String getDisplayName() {
       return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

}

