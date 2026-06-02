package fr.openent.competences.service.impl;

import fr.openent.competences.helper.LogHelper;
import fr.openent.competences.repository.RepositoryFactory;
import fr.openent.competences.repository.UserRepository;
import fr.openent.competences.service.UserService;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class DefaultUserService implements UserService {

    private final UserRepository userRepository;

    public DefaultUserService(RepositoryFactory repositoryFactory) {
        this.userRepository = repositoryFactory.userRepository();
    }

    @Override
    public Future<Boolean> isUserInThirdClassLevel(String id) {
        Promise<Boolean> promise = Promise.promise();

        userRepository.getNeoUserById(id)
                .onSuccess(neoUser -> promise.complete(neoUser.isPresent() && neoUser.get().isInThirdClassLevel()))
                .onFailure(err -> {
                    String errorMessage = "Fail to retrieve user with id: " + id;
                    LogHelper.logError(this, "isUserInThirdClassLevel", errorMessage, err.getMessage());
                    promise.fail(err);
                });

        return promise.future();
    }
}
