package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HeadersAdaptor;
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
public class AdministratorRightTest {
    AdministratorRight access;
    HttpServerRequest request;
    Binding binding;
    UserInfos user = new UserInfos();
    List<UserInfos.Action> actions;
    MultiMap params;
    UserInfos.Action role1;
    UserInfos.Action role2;
    List<String> structures;

    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AdministratorRight();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        user = new UserInfos();
        role1 = new UserInfos.Action();
        role2 = new UserInfos.Action();
        actions = new ArrayList<>();
        structures = new ArrayList<String>();
        params = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
    }


    @Test
    public void testAuthorize(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        role2.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        actions.add(role2);
        user.setAuthorizedActions(actions);
        params.set(Field.IDETABLISSEMENT,"111111");
        Mockito.doReturn(params).when(request).params();
        structures.add("111111");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }

    @Test
    public void testAuthorizeBadRights(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_CONSEIL_DE_CLASSE.toString());
        role2.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        actions.add(role2);
        user.setAuthorizedActions(actions);
        params.set(Field.IDETABLISSEMENT,"111111");
        Mockito.doReturn(params).when(request).params();
        structures.add("111111");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
        });
    }

    @Test
    public void testAuthorizeBadStructure(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        role2.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        actions.add(role2);
        user.setAuthorizedActions(actions);
        params.set(Field.IDETABLISSEMENT,"111111");
        Mockito.doReturn(params).when(request).params();
        structures.add("11aaaa");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
        });
    }
}
