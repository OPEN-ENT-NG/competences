package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public interface IModel<I extends IModel<I>> {
    JsonObject toJson();
}