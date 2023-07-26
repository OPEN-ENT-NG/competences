package fr.openent.competences.service.impl;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jRest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Neo4j.class, DefaultUtilsService.class})
public class DefaultUtilsServiceTest {

    private static final String USER_ID = "111";
    private final Neo4j neo4j = Neo4j.getInstance();

   @Mock
    private Neo4jRest neo4jRest;

   @InjectMocks
   private DefaultUtilsService utilsService;

    @Before
    public void setUp() throws NoSuchFieldException {
        FieldSetter.setField(neo4j, neo4j.getClass().getDeclaredField("database"), neo4jRest);
    }

    @Test
    public void getMyChildreen() throws Exception{

        StringBuilder queryTest = new StringBuilder();
        queryTest.append("MATCH (m:`User`{id: {id}})<-[:RELATED]-(n:`User`)-[:IN]->(g:ProfileGroup)-[:DEPENDS]->(s:`Structure`) ")
                .append("WITH n.id as id, n.displayName as displayName, n.classes as externalIdClasse, s.id as idStructure,  ")
                .append("n.firstName as firstName, n.lastName as lastName MATCH(c:Class) WHERE c.externalId IN externalIdClasse")
                .append(" RETURN distinct id, displayName, firstName, lastName, c.id as idClasse, idStructure");


        JsonObject expectedParams = new JsonObject()
                .put("id",USER_ID);

        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonObject paramsResult = invocation.getArgument(1);
            assertEquals(queryResult.trim().replaceAll("\\s+", " "),
                    queryTest.toString().trim().replaceAll("\\s+", " "));
            assertEquals(expectedParams, paramsResult);
            return null;

        }).when(neo4jRest).execute(Mockito.anyString(),Mockito.any(JsonObject.class), Mockito.any(Handler.class));
        this.utilsService.getEnfants(USER_ID, null);

    }

}
