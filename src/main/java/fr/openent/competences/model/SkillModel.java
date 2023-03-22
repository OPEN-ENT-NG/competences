package fr.openent.competences.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public interface SkillModel<I extends SkillModel<I>> {
    JsonObject toJson();

    I model(JsonObject model);

    I set(JsonObject model);

    @SuppressWarnings("unchecked")
    default List<I> toList(JsonArray results) {
        return ((List<JsonObject>) results.getList()).stream().map(this::model).collect(Collectors.toList());
    }

    default JsonArray toArray(List<I> models) {
        return new JsonArray(models.stream().map(SkillModel::toJson).collect(Collectors.toList()));
    }

}

