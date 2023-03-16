package fr.openent.competences.security;

import fr.openent.competences.constants.Field;
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
public class AccessViescoParamServiceStructureTest {
    AccessViescoParamServiceStructure access;
    HttpServerRequest request;
    Binding binding;
    UserInfos user;
    List<UserInfos.Action> actions;
    UserInfos.Action role1;
    List<String> groupsId;
    MultiMap params;
    List structures;

    @Before
    public void setUp(){
        access = new AccessViescoParamServiceStructure();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        user = new UserInfos();
        params = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
        structures = new ArrayList<String>();
        groupsId = new ArrayList<>();
    }

    @Test
    public void testAuthorizeWrongStructureParamService(TestContext ctx){
        role1.setDisplayName(WorkflowActions.PARAM_SERVICES_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        params.set(Field.IDETABLISSEMENT,"111111");
        Mockito.doReturn(params).when(request).params();
        structures.add("0000000");
        user.setStructures(structures);
        access.authorize(request,binding,user,result -> {
            ctx.assertEquals(false,result);
        });
    }

    @Test
    public void testAuthorizeStructureParamService (TestContext ctx){
        role1.setDisplayName(WorkflowActions.PARAM_SERVICES_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        params.set(Field.IDETABLISSEMENT,"111111");
        Mockito.doReturn(params).when(request).params();
        structures.add("111111");
        user.setStructures(structures);
        access.authorize(request,binding,user,result -> {
            ctx.assertEquals(true,result);
        });
    }

    @Test
    public void testAuthorizeStructureNoParamService(TestContext ctx){
        user.setAuthorizedActions(actions);
        params.set(Field.IDETABLISSEMENT,"111111");
        Mockito.doReturn(params).when(request).params();
        structures.add("111111");
        user.setStructures(structures);
        access.authorize(request,binding,user,result -> {
            ctx.assertEquals(false, result);
        });
    }
}
