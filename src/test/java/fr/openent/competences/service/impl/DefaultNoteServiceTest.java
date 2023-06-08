package fr.openent.competences.service.impl;

import fr.openent.competences.constants.Field;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.ServiceFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;

import static fr.openent.competences.constants.Field.DEVOIRS_TABLE;
import static fr.openent.competences.constants.Field.TYPE_TABLE;

@RunWith(VertxUnitRunner.class)
public class DefaultNoteServiceTest {

    private static final List<String> STUDENT_IDS = Arrays.asList("111","222");
    private static final List<String> GROUP_IDS = Arrays.asList("333","444");
    private NoteService noteService;


    @Before
    public void setUp() throws NoSuchFieldException {
        Sql.getInstance().init(Vertx.vertx().eventBus(), "");
        ServiceFactory serviceFactory = new ServiceFactory(Vertx.vertx(), null, Sql.getInstance());
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
                .append("WHERE " + Field.DEVOIRS_TABLE + ".is_evaluated = true AND " + TYPE_TABLE + ".formative IS FALSE ");

        JsonArray expectedParams = new JsonArray();

        String queryResult = Whitebox.invokeMethod(noteService,"getAssessmentScores", null,
                null, null);

        ctx.assertEquals(queryResult.trim().replaceAll("\\s+", " "), queryTest.)


    }
}
