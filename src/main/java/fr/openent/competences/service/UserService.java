package fr.openent.competences.service;

import io.vertx.core.Future;

public interface UserService {

    /**
     * Checks if a user belongs to the third class level based on their ID.
     *
     * @param id the unique identifier of the user
     * @return a Future containing true if the user is in the third class level, false otherwise
     */
    Future<Boolean> isUserInThirdClassLevel(String id);
}
