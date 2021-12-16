package fr.openent.competences.service;

import fr.openent.competences.service.impl.DefaultBFCService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.junit.runner.RunWith;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import io.vertx.ext.unit.TestContext;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

@RunWith(VertxUnitRunner.class)
public class BFCServiceTest {

    private final Sql sql = mock(Sql.class);
    private BFCService bfcService;

    @Before
    public void setUp() throws NoSuchFieldException {
        Sql.getInstance().init(Vertx.vertx().eventBus(), "");
        this.bfcService = new DefaultBFCService(Vertx.vertx().eventBus());
        FieldSetter.setField(bfcService, bfcService.getClass().getSuperclass().getDeclaredField("sql"), sql);
        FieldSetter.setField(bfcService, bfcService.getClass().getSuperclass().getDeclaredField("resourceTable"), "notes.bilan_fin_cycle");
    }

    @Test
    public void testBfcDelete_Should_Delete_Correct_Data_Into_SQLPrepare(TestContext ctx) {
        long idBFC = 5;
        String idEleve = "studentId_454545";

        String expectedQuery = "DELETE FROM notes.bilan_fin_cycle WHERE id_domaine = ? AND id_eleve = ?";
        JsonArray expectedParams = new JsonArray()
                .add(idBFC)
                .add(idEleve);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult, expectedQuery);
            ctx.assertEquals(paramsResult.toString(), expectedParams.toString());
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        this.bfcService.deleteBFC(idBFC, idEleve, null, null);
    }

    @Test
    public void getCalcMillesimeValues_Should_Get_Correct_Data_Into_SQLPrepare(TestContext ctx) {
        JsonArray expectedParams = new fr.wseduc.webutils.collections.JsonArray();
        String expectedQuery = "SELECT * FROM notes.calc_millesime";

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult, expectedQuery);
            ctx.assertEquals(paramsResult.toString(), expectedParams.toString());
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        this.bfcService.getCalcMillesimeValues(event -> {});
    }
}
