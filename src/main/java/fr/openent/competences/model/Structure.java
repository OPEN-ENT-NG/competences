package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Structure  extends  Model{


    private String UAI;
    private String name;
    private String academy;
    private String address;
    private String zipCode;
    private String city;
    private String phone;
    private String citeMixte;
    private String type;
    private String mail;

    public Structure(){
        super();
    }

    public String getUAI() {
        return UAI;
    }

    public void setUAI(String UAI) {
        this.UAI = UAI;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcademy() {
        return academy;
    }

    public String getCiteMixte() {
        return citeMixte;
    }

    public void setCiteMixte(String citeMixte) {
        this.citeMixte = citeMixte;
    }

    public void setAcademy(String academy) {
        this.academy = academy;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        result.put("academy",this.academy)
                .put("type",type)
                .put("nameStructure",name)
                .put("address",address)
                .put("phone",phone)
                .put("couriel",mail);
        String town = null;
        if(this.getZipCode() != null) {
            town = this.getZipCode();
        }
        if(this.getCity() != null) {
            town = (town!=null)? (town + ' ' + this.getCity()) : this.getCity();
        }
        if (town!= null) {
            result.put("town",town);
        }
        return result;
    }

}
