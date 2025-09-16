package fr.openent.competences.repository.impl;

import fr.openent.competences.helper.ModelHelper;
import fr.openent.competences.model.NeoUser;
import fr.openent.competences.repository.RepositoryFactory;
import fr.openent.competences.repository.UserRepository;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.Optional;

import static fr.openent.competences.constants.Field.ID;

public class DefaultUserRepository implements UserRepository {
    private final Neo4j neo4j;

    public DefaultUserRepository(RepositoryFactory repositoryFactory) {
        this.neo4j = repositoryFactory.neo4j();
    }

    @Override
    public Future<Optional<NeoUser>> getNeoUserById(String id) {
        Promise<Optional<NeoUser>> promise = Promise.promise();

        String query =  "MATCH (u:User {id: {id}}) " +
                        "RETURN u.id AS id, " +
                        "u.displayName AS displayName, " +
                        "u.firstName AS firstName, " +
                        "u.lastName AS lastName, " +
                        "u.module AS module, " +
                        "u.moduleName AS moduleName;";

        JsonObject params = new JsonObject().put(ID, id);

        String errorMessage = String.format("[Competences@DefaultUserRepository::getNeoUserById] Fail to retrieve user with id %s : ", id);
        neo4j.execute(query, params, Neo4jResult.validUniqueResultHandler(ModelHelper.uniqueResultToIModel(promise, NeoUser.class, errorMessage)));

        return promise.future();
    }
}
