package fr.openent.competences.service.impl;

import fr.openent.competences.constants.Field;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DefaultTransitionService.class, Neo4j.class, Sql.class})
public class DefaultTransitionServiceTest {

    @InjectMocks
    private DefaultTransitionService transitionService;

    @Mock
    private Sql sqlAdmin;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Neo4j.class);
        PowerMockito.mockStatic(Sql.class);
        when(Neo4j.getInstance()).thenReturn(mock(Neo4j.class));
    }

    @Test
    public void createStatements() throws Exception {
        // Test data
        String currentYear = "2023";
        String sqlVersion = "V1";

        // Perform the method call
        JsonArray statements = Whitebox.invokeMethod(transitionService, "createStatements",
                currentYear, sqlVersion);

        // Verify the statements
        assertEquals(7, statements.size());

        JsonObject statement1 = statements.getJsonObject(0);
        assertEquals("ALTER SCHEMA " + Field.SCHEMA_VIESCO_SIMPLE + " RENAME TO " + Field.SCHEMA_VIESCO_SIMPLE
                        + "_" + currentYear,
                statement1.getString(Field.STATEMENT));
        assertEquals(new JsonArray(), statement1.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement1.getString(Field.ACTION));

        JsonObject statement2 = statements.getJsonObject(1);
        assertEquals("SELECT function_clone_schema_with_sequences(?::text, ?::text, TRUE)",
                statement2.getString(Field.STATEMENT));
        assertEquals(new JsonArray().add(Field.SCHEMA_VIESCO_SIMPLE + "_" + currentYear).add(Field.SCHEMA_VIESCO_SIMPLE),
                statement2.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement2.getString(Field.ACTION));

        JsonObject statement3 = statements.getJsonObject(2);
        assertEquals("ALTER SCHEMA " + Field.SCHEMA_COMPETENCES + " RENAME TO " + Field.SCHEMA_COMPETENCES
                        + "_" + currentYear,
                statement3.getString(Field.STATEMENT));
        assertEquals(new JsonArray(), statement3.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement3.getString(Field.ACTION));

        JsonObject statement4 = statements.getJsonObject(3);
        assertEquals("SELECT function_clone_schema_with_sequences(?::text, ?::text, TRUE)",
                statement4.getString(Field.STATEMENT));
        assertEquals(new JsonArray().add(Field.SCHEMA_COMPETENCES + "_" + currentYear).add(Field.SCHEMA_COMPETENCES),
                statement4.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement4.getString(Field.ACTION));

        JsonObject statement5 = statements.getJsonObject(4);
        assertEquals("SELECT " + Field.SCHEMA_COMPETENCES + ".function_renameConstraintFromViescoAfterClonning() ",
                statement5.getString(Field.STATEMENT));
        assertEquals(new JsonArray(), statement5.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement5.getString(Field.ACTION));

        // Verify grantPrivilegesToAppsStatement
        JsonObject statement6 = statements.getJsonObject(5);
        assertEquals("SELECT function_grants_permission_to_apps_user(?::text)",
                statement6.getString(Field.STATEMENT));
        assertEquals(new JsonArray().add(Field.SCHEMA_COMPETENCES), statement6.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement6.getString(Field.ACTION));


        JsonObject statement7 = statements.getJsonObject(6);
        assertEquals("SELECT function_grants_permission_to_apps_user(?::text)",
                statement7.getString(Field.STATEMENT));
        assertEquals(new JsonArray().add(Field.SCHEMA_VIESCO_SIMPLE), statement7.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement7.getString(Field.ACTION));
    }
    @Test
    public void createStatements_with_sql_v2() throws Exception {
        // Test data
        String currentYear = "2023";
        String sqlVersion = "V2";

        // Perform the method call
        JsonArray statements = Whitebox.invokeMethod(transitionService, "createStatements",
                currentYear, sqlVersion);

        // Verify the statements
        assertEquals(7, statements.size());

        JsonObject statement4 = statements.getJsonObject(3);
        assertEquals("SELECT function_clone_schema_with_sequences_v2(?::text, ?::text, TRUE)",
                statement4.getString(Field.STATEMENT));
        assertEquals(new JsonArray().add(Field.SCHEMA_COMPETENCES + "_" + currentYear).add(Field.SCHEMA_COMPETENCES),
                statement4.getJsonArray(Field.VALUES));
        assertEquals(Field.PREPARED, statement4.getString(Field.ACTION));
    }
}
