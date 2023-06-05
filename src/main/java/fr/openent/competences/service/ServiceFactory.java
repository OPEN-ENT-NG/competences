package fr.openent.competences.service;

import fr.openent.competences.constants.Field;
import fr.openent.competences.model.Config;
import fr.openent.competences.service.impl.*;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;

public class ServiceFactory {
    private final Vertx vertx;
    private final Sql sql;
    private final Storage storage;
    private final Config config;

    public ServiceFactory(Vertx vertx, Storage storage, Sql sql, JsonObject config) {
        this.vertx = vertx;
        this.storage = storage;
        this.sql = sql;
        this.config = new Config(config);
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

    public SyntheseBilanPeriodiqueService syntheseBilanPeriodiqueService() {
        return new DefaultSyntheseBilanPeriodiqueService();
    }

    public UtilsService utilsService() {
        return new DefaultUtilsService(eventBus());
    }

    public NoteService noteService() {
        return new DefaultNoteService(Field.SCHEMA_COMPETENCES, Field.NOTES_TABLE, eventBus());
    }

    public AverageService averageService() {
        return new DefaultAverageService(noteService(), utilsService(), syntheseBilanPeriodiqueService());
    }

    public MongoExportService mongoExportService() {
        return new DefaultMongoExportService();
    }

    public AppreciationService appreciationService() {
        return new DefaultAppreciationService(Field.SCHEMA_COMPETENCES, Field.APPRECIATIONS_TABLE);
    }

    public ExportBulletinService exportBulletinService() {
        return new DefaultExportBulletinService(eventBus(), storage());
    }

    public ExportService exportService() {
        return new DefaultExportService(eventBus(), storage());
    }

    public DomainesService domainService() {
        return new DefaultDomaineService(Field.SCHEMA_COMPETENCES, Field.DOMAINES_TABLE);
    }

    public BFCService BFCService() {
        return new DefaultBFCService(eventBus(), storage());
    }

    public DevoirService devoirService() {
        return new DefaultDevoirService(eventBus());
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

    public Config config() {
        return this.config;
    }
}
