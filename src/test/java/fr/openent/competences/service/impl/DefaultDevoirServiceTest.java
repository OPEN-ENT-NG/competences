package fr.openent.competences.service.impl;


import fr.openent.competences.constants.Field;
import fr.openent.competences.service.DevoirService;
import fr.openent.competences.service.ServiceFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.reflect.Whitebox;


@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
@PrepareForTest({Sql.class})
public class DefaultDevoirServiceTest {
    private static final String STUDENT_ID = "8b9a7acd-eeb1-4f87-8b89-9fb4b8b2b03c";
    private static final String CLASS_ID = "36a5b4af-60cb-4db6-8c97-3c7926c98c65";
    private static final Long PERIOD_ID = 3L;
    private static final String STRUCTURE_ID = "123";
    private static final String SUBJECT_ID = "1244";
    private static final Boolean HISTORISE = Boolean.FALSE;
    private Sql sql ;
    private DevoirService devoirService;



    @Before
    public void setUp() throws NoSuchFieldException {
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        ServiceFactory serviceFactory = new ServiceFactory(Vertx.vertx(), null, sql, null);
        devoirService = serviceFactory.devoirService();
    }

    @Test
    public void testGetListDevoirWithMinimumParam (TestContext ctx) throws Exception {
        Async async = ctx.async();
        StringBuilder queryTest = new StringBuilder();
        queryTest.append("SELECT " + Field.DEVOIRS_TABLE + ".*, " + Field.TYPE_TABLE + ".nom as _type_libelle, ")
                .append(Field.TYPE_TABLE + ".formative, " + Field.VIESCO_REL_TYPE_PERIODE + ".type as _periode_type, ")
                .append(Field.VIESCO_REL_TYPE_PERIODE + ".ordre as _periode_ordre, ")
                .append(Field.USERS_TABLE + ".username as teacher, id_groupe ")
                .append("FROM ").append(Field.SCHEMA_COMPETENCES + "." + Field.DEVOIRS_TABLE)
                .append(" LEFT JOIN ").append(Field.SCHEMA_VIESCO).append(Field.VIESCO_REL_TYPE_PERIODE)
                .append(" ON " + Field.DEVOIRS_TABLE + ".id_periode = " + Field.VIESCO_REL_TYPE_PERIODE + ".id ")
                .append("INNER JOIN ").append(Field.SCHEMA_COMPETENCES + "." + Field.TYPE_TABLE)
                .append(" ON " + Field.DEVOIRS_TABLE + ".id_type = " + Field.TYPE_TABLE + ".id ")
                .append("INNER JOIN ").append(Field.SCHEMA_COMPETENCES + "."+ Field.USERS_TABLE)
                .append(" ON "+ Field.USERS_TABLE + ".id = " + Field.DEVOIRS_TABLE + ".owner ")
                .append("INNER JOIN ").append(Field.SCHEMA_COMPETENCES +"." + Field.REL_DEVOIRS_GROUPES_TABLE)
                .append(" ON " + Field.REL_DEVOIRS_GROUPES_TABLE + ".id_devoir = " + Field.DEVOIRS_TABLE + ".id ")
                .append("WHERE " + Field.DEVOIRS_TABLE + ".id_etablissement = ? AND " + Field.DEVOIRS_TABLE + ".eval_lib_historise = ? ")
                .append(" ORDER BY " + Field.DEVOIRS_TABLE + ".date ASC, " + Field.DEVOIRS_TABLE + ".id ASC");
        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .add(HISTORISE);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    queryTest.toString().trim().replaceAll("\\s+", " "));
            ctx.assertEquals(expectedParams.toString(), paramsResult.toString());
            async.complete();
            return null;

        }).when(sql).prepared(Mockito.anyString(),Mockito.any(), Mockito.any());
        Whitebox.invokeMethod(devoirService,"listDevoirs",
                (String)null, STRUCTURE_ID,null, null, null, HISTORISE);
        async.awaitSuccess(10000);
    }

    @Test
    public void testGetListDevoirWithMaximumParams (TestContext ctx) throws Exception {
        Async async = ctx.async();
        StringBuilder queryTest = new StringBuilder();
        queryTest.append("SELECT " + Field.DEVOIRS_TABLE + ".*, " + Field.TYPE_TABLE + ".nom as _type_libelle, ")
                .append(Field.TYPE_TABLE + ".formative, " + Field.VIESCO_REL_TYPE_PERIODE + ".type as _periode_type, ")
                .append(Field.VIESCO_REL_TYPE_PERIODE + ".ordre as _periode_ordre, ")
                .append(Field.USERS_TABLE + ".username as teacher, id_groupe ")
                .append(", " + Field.NOTES_TABLE + ".valeur as note, COUNT(" + Field.COMPETENCES_DEVOIRS + ".id) ")
                .append("as nbcompetences, sum.sum_notes, sum.nbr_eleves ")
                .append("FROM ").append(Field.SCHEMA_COMPETENCES + "." + Field.DEVOIRS_TABLE)
                .append(" LEFT JOIN ").append(Field.SCHEMA_VIESCO).append(Field.VIESCO_REL_TYPE_PERIODE)
                .append(" ON " + Field.DEVOIRS_TABLE + ".id_periode = " + Field.VIESCO_REL_TYPE_PERIODE + ".id ")
                .append("INNER JOIN ").append(Field.SCHEMA_COMPETENCES + "." + Field.TYPE_TABLE)
                .append(" ON " + Field.DEVOIRS_TABLE + ".id_type = " + Field.TYPE_TABLE + ".id ")
                .append("INNER JOIN ").append(Field.SCHEMA_COMPETENCES + "." + Field.USERS_TABLE)
                .append(" ON "+ Field.USERS_TABLE + ".id = " + Field.DEVOIRS_TABLE + ".owner ")
                .append("INNER JOIN ").append(Field.SCHEMA_COMPETENCES + "." + Field.REL_DEVOIRS_GROUPES_TABLE)
                .append(" ON " + Field.REL_DEVOIRS_GROUPES_TABLE + ".id_devoir = " + Field.DEVOIRS_TABLE + ".id ")
                .append("AND " + Field.REL_DEVOIRS_GROUPES_TABLE + ".id_groupe = ? ")
                .append(" LEFT JOIN ").append(Field.SCHEMA_COMPETENCES + "." + Field.COMPETENCES_DEVOIRS)
                .append(" ON " + Field.DEVOIRS_TABLE + ".id = " + Field.COMPETENCES_DEVOIRS + ".id_devoir ")
                .append("INNER JOIN ").append(Field.SCHEMA_COMPETENCES + "." + Field.NOTES_TABLE)
                .append(" ON " + Field.DEVOIRS_TABLE + ".id = " + Field.NOTES_TABLE + ".id_devoir ")
                .append("INNER JOIN ( SELECT " + Field.DEVOIRS_TABLE + ".id, SUM(" + Field.NOTES_TABLE + ".valeur) ")
                .append("as sum_notes, COUNT(" + Field.NOTES_TABLE + ".valeur) as nbr_eleves ")
                .append("FROM ").append(Field.SCHEMA_COMPETENCES + "." + Field.DEVOIRS_TABLE)
                .append(" INNER JOIN ").append(Field.SCHEMA_COMPETENCES + "." + Field.NOTES_TABLE)
                .append(" ON " + Field.DEVOIRS_TABLE + ".id = " + Field.NOTES_TABLE + ".id_devoir ")
                .append("WHERE " + Field.DEVOIRS_TABLE + ".id_etablissement = ? AND date_publication <= Now() ")
                .append("AND " + Field.DEVOIRS_TABLE + ".id_periode = ? ")
                .append("GROUP BY " + Field.DEVOIRS_TABLE + ".id) sum ON sum.id = " + Field.DEVOIRS_TABLE + ".id ")
                .append("WHERE " + Field.DEVOIRS_TABLE + ".id_etablissement = ? AND " + Field.DEVOIRS_TABLE + ".eval_lib_historise = ? ")
                .append("AND " + Field.DEVOIRS_TABLE + ".id_matiere = ? ")
                .append(" AND " + Field.NOTES_TABLE + ".id_eleve = ? AND date_publication <= Now() ")
                .append("AND " + Field.DEVOIRS_TABLE + ".id_periode = ? ")
                .append(" GROUP BY " + Field.DEVOIRS_TABLE + ".id, " + Field.VIESCO_REL_TYPE_PERIODE + ".type , ")
                .append( Field.VIESCO_REL_TYPE_PERIODE + ".ordre, " + Field.TYPE_TABLE + ".nom, " + Field.TYPE_TABLE + ".formative, ")
                .append(Field.NOTES_TABLE + ".valeur, sum_notes, nbr_eleves, "+ Field.USERS_TABLE + ".username, id_groupe ")
                .append(" ORDER BY " + Field.DEVOIRS_TABLE + ".date ASC, " + Field.DEVOIRS_TABLE + ".id ASC");

        JsonArray expectedParams = new JsonArray()
                .add(CLASS_ID)
                .add(STRUCTURE_ID)
                .add(PERIOD_ID)
                .add(STRUCTURE_ID)
                .add(HISTORISE)
                .add(SUBJECT_ID)
                .add(STUDENT_ID)
                .add(PERIOD_ID);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    queryTest.toString().trim().replaceAll("\\s+", " "));
            ctx.assertEquals(expectedParams.toString(), paramsResult.toString());
            async.complete();
            return null;

        }).when(sql).prepared(Mockito.anyString(),Mockito.any(), Mockito.any());
        Whitebox.invokeMethod(devoirService,"listDevoirs",
                STUDENT_ID, STRUCTURE_ID,CLASS_ID, SUBJECT_ID, PERIOD_ID, HISTORISE);
        async.awaitSuccess(10000);

    }
}
