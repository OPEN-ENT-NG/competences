package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

//creer une classe mère dont Teacher et Student héritent
public class Teacher extends Model{
    private String firstName;
    private String lastName;
    private final List<Group> groupes = new ArrayList<>();
    private final List<Classe> classes = new ArrayList<>();
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

    @Override
    public JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        List<String> idGroupes = groupes.stream()
                .map(Group::getId)
                .collect(Collectors.toList());
        List<String> idClasses = classes.stream()
                .map(Classe::getId)
                .collect(Collectors.toList());
        result.put("idTeacher", this.id)
                .put("firstName", this.firstName)
                .put("lastName", this.lastName)
                .put("idGroupes", idGroupes)
                .put("idClasses", idClasses);

        return result;
    }
}
