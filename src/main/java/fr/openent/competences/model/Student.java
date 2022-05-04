package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.competences.Utils.getLibelle;

public class Student extends Model {

    private String firstName;
    private String lastName;
    private String INE;
    private boolean hasIne = false;
    private Classe classe;
    private Structure structure;
    private String birthDate;
    private ParamsBulletins paramBulletins;
    private boolean hasLvl = false;
    private Level level;
    private final List<Group> groupes = new ArrayList<>();
    private final List<StudentEvenement> evenements = new ArrayList<>();
    private final List<Group> manualGroupes = new ArrayList<>();
    private String externalId;
    private String deleteDate;


    public Student() {
        super();
    }
    public void formatBirthDate() {
        try {
            if (birthDate != null) {
                String[] be = birthDate.split("-");
                this.birthDate = be[2] + '/' + be[1] + '/' + be[0];
            }
        }catch (Exception ignored) {
        }
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Classe getClasse() {
        return classe;
    }

    public void addGroupe(Group group) {
        groupes.add(group);
    }

    public void addManualGroupe(Group group) {
        groupes.add(group);
    }
    public List<Group> getGroupes() {
        return groupes;
    }

    public List<Group> getManualGroupes() {
        return manualGroupes;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
        this.hasLvl = true;
    }

    public boolean hasLvl() {
        return hasLvl;
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
        hasIne = true;
        this.INE = INE;
    }
    public void addEvenement (StudentEvenement evenement) {
        evenements.add(evenement);
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

    public boolean hasIne() {
        return hasIne;
    }

    public String getDeleteDate() {
        return deleteDate;
    }

    public void setDeleteDate(String deleteDate) {
        this.deleteDate = deleteDate;
    }

    public List<StudentEvenement> getEvenements() {
        return evenements;
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        List<String> idManualGroupes = manualGroupes.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
        List<String> idGroupes = groupes.stream()
                .map(Group::getId)
                .collect(Collectors.toList());

        result.put("idEleve", this.id)
                .put("firstName", this.firstName)
                .put("lastName", this.lastName)
                .put("ine", this.INE)
                .put("hasINENumber", this.hasIne)
                .put("hasLevel", this.hasLvl)
                .put("idClasse", this.classe.getId())
                .put("u.deleteDate", this.deleteDate)
                .put("classeName", this.classe.getName())
                .put("classeNameToShow", this.classe.getDisplayName())
                .put("idEtablissement", this.structure.getId())
                .put("birthDate", this.birthDate)
                .put("birthDateLibelle", this.birthDate)
                .put("externalId", this.externalId)
                .put("idPeriode", this.classe.getPeriode().getIdPeriode())
                .put("typePeriode", this.classe.getPeriode().getType())
                .put("idGroupes", idGroupes)
                .put("idLanualGroupes", idManualGroupes)
                .put("periode", classe.getPeriode().getName())
                .put("schoolYear", getLibelle("school.year")
                        + classe.getPeriode().getStartDate()
                        + "-" + classe.getPeriode().getEndDate())
        .put("evenements",evenements.stream().map(StudentEvenement::toString).collect(Collectors.toList()))
        ;
        if(hasLvl)
            result.mergeIn(level.toJsonObject());
        result.mergeIn(paramBulletins.toJson());
        result.put("structureLibelle", structure.toJsonObject());
        return result;
    }
}
