package fr.openent.competences.controller;
import fr.openent.competences.controllers.EventBusController;
import fr.wseduc.webutils.StartupUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class EventBusControllerTest {
    @Test
    public void eventBus_Controller_init_Should_Not_Throw(TestContext ctx) {
        new EventBusController(StartupUtils.securedActionsToMap(new JsonArray()));
        ctx.assertTrue(true);
    }

}