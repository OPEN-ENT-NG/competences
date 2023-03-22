package fr.openent.competences.service;

import fr.openent.competences.service.impl.DefaultBilanPerioqueService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;

public class ServiceFactory {
    private final Vertx vertx;
    private final Sql sql;
    private final Storage storage;

    public ServiceFactory(Vertx vertx, Storage storage, Sql sql) {
        this.vertx = vertx;
        this.storage = storage;
        this.sql = sql;
    }

    public BilanPeriodiqueService bilanPeriodiqueService() {
        return new DefaultBilanPerioqueService(sql, this.eventBus());
    }

    // Helpers
    public EventBus eventBus() {
        return this.vertx.eventBus();
    }

    public Storage storage() {
        return this.storage;
    }

    public Vertx vertx() {
        return this.vertx;
    }

}
