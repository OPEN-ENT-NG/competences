package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
public class AccessCompetencesAdminTeacherPersonnelTest {

    AccessCompetencesAdminTeacherPersonnel access;
    HttpServerRequest request;
    Binding binding;
    UserInfos user = new UserInfos();
    MultiMap map;
    List<String> structures;
    List<UserInfos.Action> actions;
    UserInfos.Action role1;
    UserInfos.Action role2;
    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AccessCompetencesAdminTeacherPersonnel();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        map = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
        structures = new ArrayList<>();
        user = new UserInfos();
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
        role2 = new UserInfos.Action();
    }

    @Test
    public void testAuthorize(TestContext ctx) {
        role1.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        role2.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        actions.add(role2);
        user.setAuthorizedActions(actions);
        map.set("id_etablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }

    @Test
    public void testAuthorizeWrongRights(TestContext ctx) {
        role1.setDisplayName(WorkflowActions.CREATE_DISPENSE_DOMAINE_ELEVE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("id_etablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(map).when(request).params();
        user.setType("admin");
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
        });
    }
}
