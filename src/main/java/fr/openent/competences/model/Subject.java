package fr.openent.competences.model;

import fr.openent.competences.Competences;
import fr.openent.competences.constants.Field;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.openent.competences.Competences.*;
import static fr.openent.competences.enums.subjects.SubjectKey.*;
import static fr.openent.competences.enums.subjects.SubjectKey.CODE;
import static fr.openent.competences.enums.subjects.SubjectKey.EXTERNAL_ID_SUBJECT;
import static fr.openent.competences.enums.subjects.SubjectKey.NAME;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Subject extends Model implements Cloneable {

    private String id;
    private int rank;
    private String label;
    private String lastUpdated;
    private int code;
    private String externalId;
    private String source;

    private String idStructure;
    private String name;
    private String externalIdSubject;

    public Subject(String id, String idStructure, int rank){
        super();
        this.setId(id);
        this.setIdStructure(idStructure);
        this.setRank(rank);
    }

    public Subject(Object subjectDirty) {
        super();
        this.initiationSubject(subjectDirty);
 }

    public Subject() {}

    // Methods

    private void initiationSubject(Object subjectDirty) {
        JsonObject subject = (JsonObject) subjectDirty;
        if (subject.containsKey(ID.getString())) this.setId(subject.getString(ID.getString()));
        if (subject.containsKey(ID_STRUCTURE.getString()))
            this.setIdStructure(subject.getString(ID_STRUCTURE.getString()));
        if (subject.containsKey(NAME.getString())) this.setName(subject.getString(NAME.getString()));
        if (subject.containsKey(RANK.getString())) this.setRank(subject.getInteger(RANK.getString()));
        if (subject.containsKey(LAST_UPDATE.getString()))
            this.setLastUpdated(subject.getString(LAST_UPDATE.getString()));
        if (subject.containsKey(CODE.getString())) this.setCode(subject.getInteger(CODE.getString()));
        if (subject.containsKey(EXTERNAL_ID.getString()))
            this.setExternalId(subject.getString(EXTERNAL_ID.getString()));
        if (subject.containsKey(SOURCE.getString())) this.setSource(subject.getString(SOURCE.getString()));
        if (subject.containsKey(LABEL.getString())) this.setLabel(subject.getString(LABEL.getString()));
        if (subject.containsKey(EXTERNAL_ID_SUBJECT.getString()))
            this.setExternalIdSubject(subject.getString(EXTERNAL_ID_SUBJECT.getString()));
    }

    // Getters in BDD neo4j
    public String getId() {
        return this.id;
    }

    public String getIdStructure() {
        return this.idStructure;
    }

    public String getName() {
        return this.name;
    }

    public int getRank() {
        return this.rank;
    }

    public String getLastUpdated() {
        return this.lastUpdated;
    }

    public int getCode() {
        return this.code;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public String getSource() {
        return this.source;
    }

    // Not in BDD neo4j

    public String getLabel() {
        return this.label;
    }

    public String getExternalIdSubject() {
        return this.externalIdSubject;
    }

    // Setters in BDD neo4j
    public void setId(String id) {
        this.id = id;
    }

    public void setIdStructure(String idStructure) {
        this.idStructure = idStructure;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void setLabel(String label) {
        this.label = label;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setCode(int code) {
        this.code = code;
    }
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setSource(String source) {
        this.source = source;
    }

    // Not in BDD neo4j

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setExternalIdSubject(String externalIdSubject) {
        this.externalIdSubject = externalIdSubject;
    }


    @Override
    public Subject clone() {
        try {
            return (Subject) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public static void getListSubject(EventBus eb,
                                      String idStructure,
                                      final Handler<Either<String, List<Subject>>> handler) {
        JsonObject action = new JsonObject()
                .put(ACTION, "matiere.listMatieresEtab")
                .put("onlyId", false)
                .put(ID_STRUCTURE_KEY, idStructure);
        eb.send(Competences.VIESCO_BUS_ADDRESS, action, Competences.DELIVERY_OPTIONS,
                handlerToAsyncHandler(message -> {
                    List<Subject> subjects = new ArrayList<>();

                    if (!message.body().getString(Field.STATUS).equals(Field.OK)
                            && !message.body().containsKey(Field.RESULT)) {
                        handler.handle(new Either.Left<>("Problem when you get subject by the bus"));
                    }
                    JsonArray resultDirty = (JsonArray) message.body()
                            .getValue(Field.RESULTS);

                    if (resultDirty.isEmpty()) handler.handle(new Either.Left<>("This structure don't have subjects"));
                    try {
                        subjects = jsonArrayToSubjectArray(resultDirty);
                    } catch (Exception error) {
                        handler.handle(new Either.Left<>("Error at the preparation list subjects"));
                    }
                    handler.handle(new Either.Right(subjects));
                })
        );
    }

    private static List<Subject> jsonArrayToSubjectArray(JsonArray jsonArray) {
        List<Subject> result = new ArrayList();
        if (!(jsonArray instanceof JsonArray) || jsonArray.isEmpty()) return result;
        for (Object subject : jsonArray) {
            result.add(new Subject(subject));
        }
        return result;
    }

}
