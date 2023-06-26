package fr.openent.competences.service.impl;

import fr.openent.competences.constants.Field;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.ServiceFactory;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;

import static fr.openent.competences.constants.Field.DEVOIRS_TABLE;
import static fr.openent.competences.constants.Field.TYPE_TABLE;
import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultNoteServiceTest {

    private static final List<String> STUDENT_IDS = Arrays.asList("111","222");
    private static final List<String> GROUP_IDS = Arrays.asList("333","444");
    private static final Integer PERIOD_ID = 1;
    private NoteService noteService;
    private final Sql sql = mock(Sql.class);


    @Before
    public void setUp() throws NoSuchFieldException {
        Sql.getInstance().init(Vertx.vertx().eventBus(), "");
        ServiceFactory serviceFactory = new ServiceFactory(Vertx.vertx(), null, Sql.getInstance(), null);
        this.noteService = serviceFactory.noteService();
    }

    @Test
    public void getAssessmentScoresValidatedQuery_with_minimum_parameter(TestContext ctx) throws Exception {
        StringBuilder queryTest = new StringBuilder();
        queryTest.append("SELECT " + Field.NOTES_TABLE + ".id_devoir, " +  Field.NOTES_TABLE + ".id_eleve, " )
                .append( Field.NOTES_TABLE + ".valeur, " + Field.DEVOIRS_TABLE + ".coefficient, " + DEVOIRS_TABLE + ".diviseur, ")
                .append(DEVOIRS_TABLE +  ".ramener_sur, " + DEVOIRS_TABLE + ".owner, " + DEVOIRS_TABLE + ".id_matiere, ")
                .append(DEVOIRS_TABLE + ".id_sousmatiere, grp.id_groupe ")
                .append("FROM " + Field.SCHEMA_COMPETENCES + "." + Field.NOTES_TABLE )
                .append(" LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + DEVOIRS_TABLE + " ON " + DEVOIRS_TABLE + ".id = " + Field.NOTES_TABLE + ".id_devoir ")
                .append("INNER JOIN " + Field.SCHEMA_COMPETENCES + "." + TYPE_TABLE + " ON " + DEVOIRS_TABLE + ".id_type = " + TYPE_TABLE + ".id ")
                .append("LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + Field.REL_DEVOIRS_GROUPES_TABLE + " AS grp ON " + Field.DEVOIRS_TABLE + ".id = grp.id_devoir ")
                .append("WHERE " + Field.DEVOIRS_TABLE + ".is_evaluated = true AND " + TYPE_TABLE + ".formative IS FALSE AND ");

        JsonArray expectedParams = new JsonArray();

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    queryTest.toString().substring(0, queryTest.length() - 5).toString().trim().replaceAll("\\s+", " "));
            ctx.assertEquals(expectedParams, paramsResult);
            return null;

        }).when(sql).prepared(Mockito.anyString(),Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(noteService,"getAssessmentScores",
                (List<String>)null, null, null);

    }

    @Test
    public void getAssessmentScoresValidatedQuery_with_one_parameter(TestContext ctx) throws Exception {
        StringBuilder queryTest = new StringBuilder();
        queryTest.append("SELECT " + Field.NOTES_TABLE + ".id_devoir, " +  Field.NOTES_TABLE + ".id_eleve, " )
                .append( Field.NOTES_TABLE + ".valeur, " + Field.DEVOIRS_TABLE + ".coefficient, " + DEVOIRS_TABLE + ".diviseur, ")
                .append(DEVOIRS_TABLE +  ".ramener_sur, " + DEVOIRS_TABLE + ".owner, " + DEVOIRS_TABLE + ".id_matiere, ")
                .append(DEVOIRS_TABLE + ".id_sousmatiere, grp.id_groupe ")
                .append("FROM " + Field.SCHEMA_COMPETENCES + "." + Field.NOTES_TABLE )
                .append(" LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + DEVOIRS_TABLE + " ON " + DEVOIRS_TABLE + ".id = " + Field.NOTES_TABLE + ".id_devoir ")
                .append("INNER JOIN " + Field.SCHEMA_COMPETENCES + "." + TYPE_TABLE + " ON " + DEVOIRS_TABLE + ".id_type = " + TYPE_TABLE + ".id ")
                .append("LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + Field.REL_DEVOIRS_GROUPES_TABLE + " AS grp ON " + Field.DEVOIRS_TABLE + ".id = grp.id_devoir ")
                .append("WHERE " + Field.DEVOIRS_TABLE + ".is_evaluated = true AND " + TYPE_TABLE + ".formative IS FALSE ")
                .append(Field.NOTES_TABLE + ".id_eleve IN " + Sql.listPrepared(STUDENT_IDS) + " AND ");

        JsonArray expectedParams = new JsonArray()
                .add(STUDENT_IDS);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    queryTest.toString().substring(0, queryTest.length() - 5).trim().replaceAll("\\s+", " "));
            ctx.assertEquals(expectedParams, paramsResult);
            return null;

        }).when(sql).prepared(Mockito.anyString(),Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(noteService,"getAssessmentScores",
                STUDENT_IDS, null, null);
    }

    @Test
    public void getAssessmentScoresValidatedQuery_with_two_parameters(TestContext ctx) throws Exception {
        StringBuilder queryTest = new StringBuilder();
        queryTest.append("SELECT " + Field.NOTES_TABLE + ".id_devoir, " +  Field.NOTES_TABLE + ".id_eleve, " )
                .append( Field.NOTES_TABLE + ".valeur, " + Field.DEVOIRS_TABLE + ".coefficient, " + DEVOIRS_TABLE + ".diviseur, ")
                .append(DEVOIRS_TABLE +  ".ramener_sur, " + DEVOIRS_TABLE + ".owner, " + DEVOIRS_TABLE + ".id_matiere, ")
                .append(DEVOIRS_TABLE + ".id_sousmatiere, grp.id_groupe ")
                .append("FROM " + Field.SCHEMA_COMPETENCES + "." + Field.NOTES_TABLE )
                .append(" LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + DEVOIRS_TABLE + " ON " + DEVOIRS_TABLE + ".id = " + Field.NOTES_TABLE + ".id_devoir ")
                .append("INNER JOIN " + Field.SCHEMA_COMPETENCES + "." + TYPE_TABLE + " ON " + DEVOIRS_TABLE + ".id_type = " + TYPE_TABLE + ".id ")
                .append("LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + Field.REL_DEVOIRS_GROUPES_TABLE + " AS grp ON " + Field.DEVOIRS_TABLE + ".id = grp.id_devoir ")
                .append("WHERE " + Field.DEVOIRS_TABLE + ".is_evaluated = true AND " + TYPE_TABLE + ".formative IS FALSE ")
                .append(Field.NOTES_TABLE + ".id_eleve IN " + Sql.listPrepared(STUDENT_IDS) + " AND ")
                .append("grp.id_groupe IN " + Sql.listPrepared(GROUP_IDS)+ " AND ");

        JsonArray expectedParams = new JsonArray()
                .add(STUDENT_IDS)
                .add(GROUP_IDS);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    queryTest.toString().substring(0, queryTest.length() - 5).trim().replaceAll("\\s+", " "));
            ctx.assertEquals(expectedParams, paramsResult);
            return null;

        }).when(sql).prepared(Mockito.anyString(),Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(noteService,"getAssessmentScores",
                STUDENT_IDS, GROUP_IDS, null);
    }

    @Test
    public void getAssessmentScoresValidatedQuery_with_all_parameters(TestContext ctx) throws Exception {
        StringBuilder queryTest = new StringBuilder();
        queryTest.append("SELECT " + Field.NOTES_TABLE + ".id_devoir, " +  Field.NOTES_TABLE + ".id_eleve, " )
                .append( Field.NOTES_TABLE + ".valeur, " + Field.DEVOIRS_TABLE + ".coefficient, " + DEVOIRS_TABLE + ".diviseur, ")
                .append(DEVOIRS_TABLE +  ".ramener_sur, " + DEVOIRS_TABLE + ".owner, " + DEVOIRS_TABLE + ".id_matiere, ")
                .append(DEVOIRS_TABLE + ".id_sousmatiere, grp.id_groupe ")
                .append("FROM " + Field.SCHEMA_COMPETENCES + "." + Field.NOTES_TABLE )
                .append(" LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + DEVOIRS_TABLE + " ON " + DEVOIRS_TABLE + ".id = " + Field.NOTES_TABLE + ".id_devoir ")
                .append("INNER JOIN " + Field.SCHEMA_COMPETENCES + "." + TYPE_TABLE + " ON " + DEVOIRS_TABLE + ".id_type = " + TYPE_TABLE + ".id ")
                .append("LEFT JOIN " + Field.SCHEMA_COMPETENCES + "." + Field.REL_DEVOIRS_GROUPES_TABLE + " AS grp ON " + Field.DEVOIRS_TABLE + ".id = grp.id_devoir ")
                .append("WHERE " + Field.DEVOIRS_TABLE + ".is_evaluated = true AND " + TYPE_TABLE + ".formative IS FALSE ")
                .append(Field.NOTES_TABLE + ".id_eleve IN " + Sql.listPrepared(STUDENT_IDS) + " AND ")
                .append("grp.id_groupe IN " + Sql.listPrepared(GROUP_IDS)+ " AND ")
                .append(DEVOIRS_TABLE + ".id_periode = ? AND ");

        JsonArray expectedParams = new JsonArray()
                .add(STUDENT_IDS)
                .add(GROUP_IDS)
                .add(PERIOD_ID);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    queryTest.toString().substring(0, queryTest.length() - 5).trim().replaceAll("\\s+", " "));
            ctx.assertEquals(expectedParams, paramsResult);
            return null;

        }).when(sql).prepared(Mockito.anyString(),Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        Whitebox.invokeMethod(noteService,"getAssessmentScores",
                STUDENT_IDS, GROUP_IDS, PERIOD_ID);
    }
}
