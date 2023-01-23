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

import static fr.openent.competences.Competences.ID_CLASSE_KEY;

@RunWith(VertxUnitRunner.class)
public class AccessSuiviClasseTest {
    AccessSuiviClasse access;
    HttpServerRequest request;
    Binding binding;
    MultiMap map;
    List<String> structures;
    List<String> classes;
    List<String> groupsIds;
    UserInfos user = new UserInfos();
    List<UserInfos.Action> actions;
    UserInfos.Action role1;

    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AccessSuiviClasse();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        map = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
        structures = new ArrayList<>();
        classes = new ArrayList<>();
        groupsIds = new ArrayList<>();
        user = new UserInfos();
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
    }

    @Test
    public void testAuthorize(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_SUIVI_CLASSE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        map.set(ID_CLASSE_KEY, "c20c05e1-d77f-4a8e-8439-2fe5e491cbce");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        classes.add("c20c05e1-d77f-4a8e-8439-2fe5e491cbce");
        user.setStructures(structures);
        user.setClasses(classes);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testAuthorizeBadStructure(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_SUIVI_CLASSE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "aaa123");
        map.set(ID_CLASSE_KEY, "c20c05e1-d77f-4a8e-8439-2fe5e491cbce");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        classes.add("c20c05e1-d77f-4a8e-8439-2fe5e491cbce");
        user.setStructures(structures);
        user.setClasses(classes);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testAuthorizeBadRights(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_MODEL_BULLETIN.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "aaa123");
        map.set(ID_CLASSE_KEY, "c20c05e1-d77f-4a8e-8439-2fe5e491cbce");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        classes.add("c20c05e1-d77f-4a8e-8439-2fe5e491cbce");
        user.setStructures(structures);
        user.setClasses(classes);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testAuthorizeBadClass(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ACCESS_SUIVI_CLASSE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("structureId", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        map.set(ID_CLASSE_KEY, "c20c05e1-d77f-4a8e-8439-2fe5e491cbce");
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        classes.add("aaaaa");
        groupsIds.add("azerty1234");
        user.setStructures(structures);
        user.setClasses(classes);
        user.setGroupsIds(groupsIds);
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }
}
