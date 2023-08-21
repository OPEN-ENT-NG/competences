package fr.openent.competences.service;

import fr.openent.competences.service.impl.DefaultBFCService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import org.entcore.common.sql.Sql;
import org.junit.runner.RunWith;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import io.vertx.ext.unit.TestContext;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
@PrepareForTest({Sql.class})
public class BFCServiceTest {

    private Sql sql;
    private BFCService bfcService;

    @Before
    public void setUp() throws NoSuchFieldException {
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
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
        Async async = ctx.async();
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult, expectedQuery);
            ctx.assertEquals(paramsResult.toString(), expectedParams.toString());
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        this.bfcService.deleteBFC(idBFC, idEleve, null, null);
        async.awaitSuccess(10000);
    }
}
