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
public class AccessStructureAdminTeacherFilterTest {
    AccessStructureAdminTeacherFilter access;
    HttpServerRequest request;
    Binding binding;
    UserInfos user;
    List<UserInfos.Action> actions;
    UserInfos.Action role1;
    List<String> groupsId;
    MultiMap params;
    ArrayList<String> structures;

    @Before
    public void setUp(){
        access = new AccessStructureAdminTeacherFilter();
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
    public void testAuthorize(TestContext ctx) {
        user.setType(Field.TEACHER);
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        String structureId = "11111";
        structures.add(structureId);
        user.setStructures(structures);
        params.set("structureId", "11111");
        Mockito.doReturn(params).when(request).params();
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }


    @Test
    public void testBadRightButTeacher(TestContext ctx) {
        user.setType(Field.TEACHER);
        role1.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        String structureId = "11111";
        structures.add(structureId);
        user.setStructures(structures);
        params.set("structureId", "11111");
        Mockito.doReturn(params).when(request).params();
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadTypeButAdmin(TestContext ctx) {
        user.setType("random");
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        String structureId = "11111";
        structures.add(structureId);
        user.setStructures(structures);
        params.set("structureId", "11111");
        Mockito.doReturn(params).when(request).params();
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadTypeBadRight(TestContext ctx) {
        user.setType("random");
        role1.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        String structureId = "11111";
        structures.add(structureId);
        user.setStructures(structures);
        params.set("structureId", "11111");
        Mockito.doReturn(params).when(request).params();
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadStructureBadType(TestContext ctx) {
        user.setType("random");
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        String structureId = "aaaaa";
        structures.add(structureId);
        user.setStructures(structures);
        params.set("structureId", "11111");
        Mockito.doReturn(params).when(request).params();
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadStructureBadRight(TestContext ctx) {
        user.setType(Field.TEACHER);
        role1.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        String structureId = "aaaaa";
        structures.add(structureId);
        user.setStructures(structures);
        params.set("structureId", "11111");
        Mockito.doReturn(params).when(request).params();
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }
}
