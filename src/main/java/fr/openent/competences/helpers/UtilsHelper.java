package fr.openent.competences.helpers;

import fr.openent.competences.model.SubTopic;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;

import java.util.List;

public class UtilsHelper {


    public static Future<Void> completeVariablesForServices(Future<JsonArray> servicesFuture,
                                                            Future<JsonArray> multiTeachersFuture,
                                              Future<List<SubTopic>> subTopicCoefFuture, final JsonArray servicesJson,
                                              final JsonArray allMultiTeachers, final List<SubTopic> subTopics) {
        Promise<Void> promise = Promise.promise();
        CompositeFuture.all(servicesFuture, multiTeachersFuture, subTopicCoefFuture)
                .onSuccess((r) -> {
                    servicesJson.addAll(servicesFuture.result());
                    allMultiTeachers.addAll(multiTeachersFuture.result());
                    subTopics.addAll(subTopicCoefFuture.result());
                    promise.complete();
                })
                .onFailure(promise::fail);
        return promise.future();
    }
}


