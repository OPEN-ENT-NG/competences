package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HeadersAdaptor;
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
public class AccessCPEConseilDeClasseTest {

    AccessCPEConseilDeClasse access;
    HttpServerRequest request;
    Binding binding;
    UserInfos user = new UserInfos();
    List<UserInfos.Action> actions;
    UserInfos.Action role1;
    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AccessCPEConseilDeClasse();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        user = new UserInfos();
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
    }

    @Test
    public void testAuthorize(TestContext ctx) {
        role1.setDisplayName(WorkflowActions.ACCESS_CONSEIL_DE_CLASSE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }

    @Test
    public void testAuthorizeWrongRights(TestContext ctx) {
        role1.setDisplayName(WorkflowActions.CREATE_DISPENSE_DOMAINE_ELEVE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
        });
    }
}
