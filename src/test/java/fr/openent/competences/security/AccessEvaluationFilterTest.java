package fr.openent.competences.security;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import fr.openent.competences.security.utils.FilterDevoirUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.headers.HeadersAdaptor;
import io.vertx.ext.unit.TestContext;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import java.util.ArrayList;
import java.util.List;

@RunWith(VertxUnitRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
public class AccessEvaluationFilterTest {
    AccessEvaluationFilter access;
    HttpServerRequest request;
    Binding binding;
    MultiMap map;
    UserInfos user = new UserInfos();
    List<UserInfos.Action> actions;
    UserInfos.Action role1;

    FilterDevoirUtils filterDevoirUtils;
    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AccessEvaluationFilter();
        filterDevoirUtils = new FilterDevoirUtils();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        user = new UserInfos();
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
        map = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
    }
    @Test
    public void testAuthorizeAdmin(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }
    @Test
    public void testAuthorizeNoAdmin(TestContext ctx){
        role1.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
        });
    }
}
