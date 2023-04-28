package fr.openent.competences.service;

import fr.openent.competences.service.impl.DefaultBilanPerioqueService;
import fr.openent.competences.service.impl.DefaultCompetenceNoteService;
import fr.openent.competences.service.impl.DefaultStructureOptions;
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
        return new DefaultBilanPerioqueService(this.sql, this.eventBus());
    }

    public CompetenceNoteService competenceNoteService() {
        return new DefaultCompetenceNoteService(this.sql, this.structureOptionsService());
    }

    public StructureOptionsService structureOptionsService() {
        return new DefaultStructureOptions(this.eventBus());
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
