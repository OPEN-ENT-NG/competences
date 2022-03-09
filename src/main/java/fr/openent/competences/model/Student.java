package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

import java.sql.Struct;

public class Student  extends  Model{

    private String firstName;
    private String lastName;
    private String INE;
    private Classe classe;
    private Structure structure;
    private String birthDate;
    private ParamsBulletins paramBulletins;

    public  Student() {
        super();
    }
    public void formatBirthDate(){
        try {
            if (birthDate != null) {
                String[] be = birthDate.split("-");
                this.birthDate = be[2] + '/' + be[1] + '/' + be[0];
            }
        }catch (Exception ignored) {
        }
    }

    public Classe getClasse() {
        return classe;
    }

    public void setClasse(Classe classe) {
        this.classe = classe;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getINE() {
        return INE;
    }

    public void setINE(String INE) {
        this.INE = INE;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public ParamsBulletins getParamBulletins() {
        return paramBulletins;
    }

    public void setParamBulletins(ParamsBulletins paramBulletins) {
        this.paramBulletins = paramBulletins;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
