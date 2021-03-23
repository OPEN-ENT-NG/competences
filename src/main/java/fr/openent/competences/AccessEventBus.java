package fr.openent.competences;
import io.vertx.core.eventbus.EventBus;

public class AccessEventBus {
    private EventBus eb;

    private AccessEventBus() {
    }

    public static AccessEventBus getInstance() {
        return AccessEventBusHolder.instance;
    }

    public void init(EventBus eb) {
        this.eb = eb;
    }

    public EventBus getEventBus() {
        return this.eb;
    }

    private static class AccessEventBusHolder {
        private static final AccessEventBus instance = new AccessEventBus();

        private AccessEventBusHolder() {
        }
    }
}
