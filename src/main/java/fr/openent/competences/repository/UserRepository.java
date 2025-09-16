package fr.openent.competences.repository;

import fr.openent.competences.model.NeoUser;
import io.vertx.core.Future;

import java.util.Optional;

public interface UserRepository {

    /**
     * Retrieves a NeoUser by their unique identifier.
     *
     * @param id the unique identifier of the NeoUser
     * @return a Future containing an Optional of NeoUser if found, or an empty Optional if not found
     */
    Future<Optional<NeoUser>> getNeoUserById(String id);
}
