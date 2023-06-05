package fr.openent.competences.service.impl;

import fr.openent.competences.constants.Field;
import fr.openent.competences.service.AverageService;
import fr.openent.competences.service.ServiceFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultAverageServiceTest {
    private AverageService averageService;

    @Before
    public void setUp() throws NoSuchFieldException {
        Sql.getInstance().init(Vertx.vertx().eventBus(), "");
        ServiceFactory serviceFactory = new ServiceFactory(Vertx.vertx(), null, null, null);
        this.averageService = serviceFactory.averageService();
    }

    @Test
    public void testSetTotalColspan_with_averages_options(TestContext ctx) throws Exception {
        JsonObject result = new JsonObject();
        result.put(Field.MATIERES, Arrays.asList(new JsonObject(), new JsonObject()));

        Whitebox.invokeMethod(averageService, "setTotalColspan",
                result, true);

        ctx.assertEquals(4, result.getInteger(Field.TOTALCOLUMN));
    }

    @Test
    public void testSetTotalColspan_without_averages_options(TestContext ctx) throws Exception {
        JsonObject result = new JsonObject();
        result.put(Field.MATIERES, Arrays.asList(new JsonObject(), new JsonObject()));

        Whitebox.invokeMethod(averageService, "setTotalColspan",
                result, false);

        ctx.assertEquals(3, result.getInteger(Field.TOTALCOLUMN));
    }

    @Test
    public void testSetStudentsSummary_with_matching_studentId(TestContext ctx) throws Exception {
        JsonObject result = new JsonObject()
                .put(Field.ELEVES, Collections.singletonList(
                        new JsonObject().put(Field.ID_ELEVE, "1")
                ));

        List<JsonObject> summaries = Collections.singletonList(
                new JsonObject()
                        .put(Field.ID_ELEVE, "1")
                        .put(Field.SYNTHESE, Field.SYNTHESE)
        );

        result.put(Field.MATIERES, Arrays.asList(new JsonObject(), new JsonObject()));

        Whitebox.invokeMethod(averageService, "setStudentsSummary",
                result, summaries);

        ctx.assertEquals(Field.SYNTHESE,
                result.getJsonArray(Field.ELEVES, new JsonArray()).getJsonObject(0).getString(Field.SUMMARY));
    }

    @Test
    public void testSetStudentsSummary_without_matching_studentId(TestContext ctx) throws Exception {
        JsonObject result = new JsonObject()
                .put(Field.ELEVES, Collections.singletonList(
                        new JsonObject().put(Field.ID_ELEVE, "1")
                ));

        List<JsonObject> summaries = Collections.singletonList(
                new JsonObject()
                        .put(Field.ID_ELEVE, "2")
                        .put(Field.SYNTHESE, Field.SYNTHESE)
        );

        result.put(Field.MATIERES, Arrays.asList(new JsonObject(), new JsonObject()));

        Whitebox.invokeMethod(averageService, "setStudentsSummary",
                result, summaries);

        ctx.assertEquals(null,
                result.getJsonArray(Field.ELEVES, new JsonArray()).getJsonObject(0).getString(Field.SUMMARY));
    }
}
