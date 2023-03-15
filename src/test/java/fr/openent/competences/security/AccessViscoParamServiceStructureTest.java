package fr.openent.competences.security;

import fr.openent.competences.Competences;
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
public class AccessViscoParamServiceStructureTest {
    AccessViscoParamServiceStructure access;
    HttpServerRequest request;
    Binding binding;
    MultiMap map;
    List<String> structures;
    List<String> groupsIds;
    UserInfos user = new UserInfos();
    List<UserInfos.Action> actions;
    UserInfos.Action role1;

    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AccessViscoParamServiceStructure();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        map = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
        structures = new ArrayList<>();
        groupsIds = new ArrayList<>();
        user = new UserInfos();
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
    }

    @Test
    public void testAuthorize(TestContext ctx){
        role1.setDisplayName(Competences.PARAM_SERVICES_RIGHT);
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testNullStructure(TestContext ctx){
        role1.setDisplayName(WorkflowActions.PARAM_SERVICES_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        //map.set("structureId", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadStructure(TestContext ctx){
        role1.setDisplayName(WorkflowActions.PARAM_SERVICES_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(map).when(request).params();
        structures.add("a1aaaa1aa1aa");
        user.setStructures(structures);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadRight(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_EXPORT_BULLETIN.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadRightAndBadStructure(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_EXPORT_BULLETIN.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(map).when(request).params();
        structures.add("aaaa123a");
        user.setStructures(structures);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }
}
