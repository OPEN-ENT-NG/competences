package fr.openent.competences.service.impl;
import fr.openent.competences.constants.Field;
import fr.openent.competences.service.CompetenceNoteService;
import fr.openent.competences.service.ServiceFactory;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.Message;
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
import java.util.Arrays;
import java.util.List;


@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
@PrepareForTest({Sql.class})
public class DefaultCompetenceNoteServiceTest {

    private static final String STRUCTURE_ID = "111";
    private static final List<String> STUDENT_IDS = Arrays.asList("222", "333");
    private static final long PERIOD_ID = 3;
    private static final String GROUP_ID = "444";
    private Sql sql;
    private CompetenceNoteService competenceNoteService;

    @Before
    public void setUp() throws NoSuchFieldException {
        this.sql = Mockito.spy(Sql.getInstance());
        PowerMockito.spy(Sql.class);
        PowerMockito.when(Sql.getInstance()).thenReturn(sql);
        ServiceFactory serviceFactory = new ServiceFactory(Vertx.vertx(), null, sql, null);
        this.competenceNoteService = serviceFactory.competenceNoteService();
    }

    @Test
    public void testGetSubjectSkillsValidatedPercentageRequest_with_avg_skills_calculation(TestContext ctx) throws Exception {
        String expectedQuery = " SELECT id_eleve as student_id, id_matiere as subject_id, " +
                " (SUM(is_validated::int) * 100) / COUNT(id_competence) as skills_validated_percentage " +
                " FROM ( " +
                " SELECT cn.id_eleve, d.id_matiere, cn.id_competence, " +
                " AVG(COALESCE(cnf.niveau_final, cn.evaluation)) > 1 as is_validated " +
                String.format(" FROM %s.%s cn ", Field.NOTES_TABLE, Field.COMPETENCES_NOTES_TABLE) +
                String.format(" INNER JOIN %s.%s d on d.id = cn.id_devoir ", Field.NOTES_TABLE, Field.DEVOIRS_TABLE) +
                String.format(" INNER JOIN %s.%s rdg on d.id = rdg.id_devoir ", Field.NOTES_TABLE, Field.REL_DEVOIRS_GROUPES_TABLE) +
                String.format(" INNER JOIN %s.%s t on d.id_type = t.id ", Field.NOTES_TABLE, Field.TYPE_TABLE) +
                String.format(" LEFT JOIN %s.%s cnf ", Field.NOTES_TABLE, Field.COMPETENCE_NIVEAU_FINAL) +
                " on d.id_matiere = cnf.id_matiere AND cn.id_eleve = cnf.id_eleve AND cn.id_competence = cnf.id_competence " +
                " WHERE d.id_etablissement = ? AND cn.id_eleve IN " + Sql.listPrepared(STUDENT_IDS) +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE " +
                " AND d.id_periode = ? " +
                " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? " +
                " GROUP BY cn.id_eleve, d.id_matiere, cn.id_competence" +
                " )  as is_validated_skills_by_subjects " +
                " GROUP BY id_eleve, id_matiere";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .addAll(new JsonArray(STUDENT_IDS))
                .add(PERIOD_ID)
                .add(GROUP_ID);
        Async async = ctx.async();
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    expectedQuery.trim().replaceAll("\\s+", " "));
            ctx.assertEquals(paramsResult, expectedParams);
            async.complete();
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(), Mockito.any(Handler.class));

        Whitebox.invokeMethod(competenceNoteService, "getSubjectSkillsValidatedPercentageRequest",
                STRUCTURE_ID, STUDENT_IDS, PERIOD_ID, GROUP_ID, true);
        async.awaitSuccess(10000);
    }

    @Test
    public void getSubjectSkillsIsValidatedQuery_with_max_skills_calculation(TestContext ctx) throws Exception {
        String expectedQuery = " SELECT cn.id_eleve, d.id_matiere, cn.id_competence, " +
                " MAX(COALESCE(cnf.niveau_final, cn.evaluation)) > 1 as is_validated " +
                String.format(" FROM %s.%s cn ", Field.NOTES_TABLE, Field.COMPETENCES_NOTES_TABLE) +
                String.format(" INNER JOIN %s.%s d on d.id = cn.id_devoir ", Field.NOTES_TABLE, Field.DEVOIRS_TABLE) +
                String.format(" INNER JOIN %s.%s rdg on d.id = rdg.id_devoir ", Field.NOTES_TABLE, Field.REL_DEVOIRS_GROUPES_TABLE) +
                String.format(" INNER JOIN %s.%s t on d.id_type = t.id ", Field.NOTES_TABLE, Field.TYPE_TABLE) +
                String.format(" LEFT JOIN %s.%s cnf ", Field.NOTES_TABLE, Field.COMPETENCE_NIVEAU_FINAL) +
                " on d.id_matiere = cnf.id_matiere AND cn.id_eleve = cnf.id_eleve AND cn.id_competence = cnf.id_competence " +
                " WHERE d.id_etablissement = ? AND cn.id_eleve IN " + Sql.listPrepared(STUDENT_IDS) +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL" +
                " AND t.formative IS FALSE " +
                " AND d.id_periode = ? " +
                " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? " +
                " GROUP BY cn.id_eleve, d.id_matiere, cn.id_competence";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .addAll(new JsonArray(STUDENT_IDS))
                .add(PERIOD_ID)
                .add(GROUP_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(competenceNoteService, "getSubjectSkillsIsValidatedQuery",
                STRUCTURE_ID, STUDENT_IDS, PERIOD_ID, GROUP_ID, false, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }

    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_minimum_filters(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve IN " + Sql.listPrepared(STUDENT_IDS) +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .addAll(new JsonArray(STUDENT_IDS));

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(competenceNoteService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_IDS, null, null, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }

    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_period(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve IN " + Sql.listPrepared(STUDENT_IDS) +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE " +
                " AND d.id_periode = ? ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .addAll(new JsonArray(STUDENT_IDS))
                .add(PERIOD_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(competenceNoteService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_IDS, PERIOD_ID, null, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }

    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_group(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve IN " + Sql.listPrepared(STUDENT_IDS) +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE " +
                " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .addAll(new JsonArray(STUDENT_IDS))
                .add(GROUP_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(competenceNoteService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_IDS, null, GROUP_ID, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }

    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_all_filters(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve IN " + Sql.listPrepared(STUDENT_IDS) +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE " +
                " AND d.id_periode = ? " +
                " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .addAll(new JsonArray(STUDENT_IDS))
                .add(PERIOD_ID)
                .add(GROUP_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(competenceNoteService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_IDS, PERIOD_ID, GROUP_ID, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }
}
