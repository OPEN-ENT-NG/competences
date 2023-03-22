package fr.openent.competences.service.impl;

import fr.openent.competences.constants.Field;
import fr.openent.competences.service.BilanPeriodiqueService;
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

import static fr.openent.competences.Competences.*;
import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultBilanPerioqueServiceTest {

    private static final String STRUCTURE_ID = "111";
    private static final String STUDENT_ID = "222";
    private static final long PERIOD_ID = 3;
    private static final String GROUP_ID = "444";
    private final Sql sql = mock(Sql.class);
    private BilanPeriodiqueService bilanPeriodiqueService;

    @Before
    public void setUp() throws NoSuchFieldException {
        Sql.getInstance().init(Vertx.vertx().eventBus(), "");
        this.bilanPeriodiqueService = new DefaultBilanPerioqueService(sql, Vertx.vertx().eventBus());
    }

    @Test
    public void testGetSubjectSkillsValidatedPercentageRequest_with_avg_skills_calculation(TestContext ctx) throws Exception {
        String expectedQuery = " SELECT id_eleve as student_id, id_matiere as subject_id, " +
                " (SUM(is_validated::int) * 100) / COUNT(id_competence) as skills_validated_percentage " +
                " FROM ( " +
                " SELECT cn.id_eleve, d.id_matiere, cn.id_competence, " +
                " AVG(COALESCE(cnf.niveau_final, cn.evaluation)) > 1 as is_validated " +
                String.format(" FROM %s.%s cn ", COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE) +
                String.format(" INNER JOIN %s.%s d on d.id = cn.id_devoir ", COMPETENCES_SCHEMA, DEVOIR_TABLE) +
                String.format(" INNER JOIN %s.%s rdg on d.id = rdg.id_devoir ", COMPETENCES_SCHEMA, REL_DEVOIRS_GROUPES) +
                String.format(" INNER JOIN %s.%s t on d.id_type = t.id ", COMPETENCES_SCHEMA, Field.TYPE_TABLE) +
                String.format(" LEFT JOIN %s.%s cnf ", COMPETENCES_SCHEMA, Field.COMPETENCE_NIVEAU_FINAL) +
                " on d.id_matiere = cnf.id_matiere AND cn.id_eleve = cnf.id_eleve AND cn.id_competence = cnf.id_competence " +
                " WHERE d.id_etablissement = ? AND cn.id_eleve = ? " +
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
                .add(STUDENT_ID)
                .add(PERIOD_ID)
                .add(GROUP_ID);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    expectedQuery.trim().replaceAll("\\s+", " "));
            ctx.assertEquals(paramsResult, expectedParams);
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));

        Whitebox.invokeMethod(bilanPeriodiqueService, "getSubjectSkillsValidatedPercentageRequest",
                STRUCTURE_ID, STUDENT_ID, PERIOD_ID, GROUP_ID, true);
    }

    @Test
    public void getSubjectSkillsIsValidatedQuery_with_max_skills_calculation(TestContext ctx) throws Exception {
        String expectedQuery = " SELECT cn.id_eleve, d.id_matiere, cn.id_competence, " +
                " MAX(COALESCE(cnf.niveau_final, cn.evaluation)) > 1 as is_validated " +
                String.format(" FROM %s.%s cn ", COMPETENCES_SCHEMA, COMPETENCES_NOTES_TABLE) +
                String.format(" INNER JOIN %s.%s d on d.id = cn.id_devoir ", COMPETENCES_SCHEMA, DEVOIR_TABLE) +
                String.format(" INNER JOIN %s.%s rdg on d.id = rdg.id_devoir ", COMPETENCES_SCHEMA, REL_DEVOIRS_GROUPES) +
                String.format(" INNER JOIN %s.%s t on d.id_type = t.id ", COMPETENCES_SCHEMA, Field.TYPE_TABLE) +
                String.format(" LEFT JOIN %s.%s cnf ", COMPETENCES_SCHEMA, Field.COMPETENCE_NIVEAU_FINAL) +
                " on d.id_matiere = cnf.id_matiere AND cn.id_eleve = cnf.id_eleve AND cn.id_competence = cnf.id_competence " +
                " WHERE d.id_etablissement = ? AND cn.id_eleve = ? " +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL" +
                " AND t.formative IS FALSE " +
                " AND d.id_periode = ? " +
                " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? " +
                " GROUP BY cn.id_eleve, d.id_matiere, cn.id_competence";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .add(STUDENT_ID)
                .add(PERIOD_ID)
                .add(GROUP_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(bilanPeriodiqueService, "getSubjectSkillsIsValidatedQuery",
                STRUCTURE_ID, STUDENT_ID, PERIOD_ID, GROUP_ID, false, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }

    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_minimum_filters(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve = ? " +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .add(STUDENT_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(bilanPeriodiqueService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_ID, null, null, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }

    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_period(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve = ? " +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE " +
                " AND d.id_periode = ? ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .add(STUDENT_ID)
                .add(PERIOD_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(bilanPeriodiqueService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_ID, PERIOD_ID, null, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }

    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_group(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve = ? " +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE " +
                " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .add(STUDENT_ID)
                .add(GROUP_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(bilanPeriodiqueService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_ID, null, GROUP_ID, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }
    @Test
    public void getSubjectSkillsIsValidatedQueryFilters_with_all_filters(TestContext ctx) throws Exception {
        String expectedQuery = " WHERE d.id_etablissement = ? AND cn.id_eleve = ? " +
                " AND d.eval_lib_historise IS FALSE " +
                " AND d.id_matiere IS NOT NULL " +
                " AND t.formative IS FALSE " +
                " AND d.id_periode = ? " +
                " AND rdg.type_groupe = 1 AND rdg.id_groupe = ? ";

        JsonArray expectedParams = new JsonArray()
                .add(STRUCTURE_ID)
                .add(STUDENT_ID)
                .add(PERIOD_ID)
                .add(GROUP_ID);

        JsonArray params = new JsonArray();
        String queryResult = Whitebox.invokeMethod(bilanPeriodiqueService, "getSubjectSkillsIsValidatedQueryFilters",
                STRUCTURE_ID, STUDENT_ID, PERIOD_ID, GROUP_ID, params);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                expectedQuery.trim().replaceAll("\\s+", " "));
        ctx.assertEquals(params, expectedParams);
    }
}
