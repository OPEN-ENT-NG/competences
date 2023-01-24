package fr.openent.competences.security;

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

    UserInfos.Action role1;

    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AdministratorRight();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        user = new UserInfos();
        role1 = new UserInfos.Action();
        actions = new ArrayList<>();
    }


    @Test
    public void testAuthorize(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testAuthorizeBadRights(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_CONSEIL_DE_CLASSE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }
}
