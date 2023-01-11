package fr.openent.competences.security;

import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
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
public class AccessChildrenParentFilterEtablissementIdTest {

    AccessChildrenParentFilterEtablissementId access;
    HttpServerRequest request;
    Binding binding;
    MultiMap map;
    List<String> structures;
    UserInfos user = new UserInfos();
    List<UserInfos.Action> actions;

    List<String> childrenIds;
    UserInfos.Action role1;
    @Before
    public void setUp() throws NoSuchFieldException {
        access = new AccessChildrenParentFilterEtablissementId();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        map = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
        structures = new ArrayList<>();
        user = new UserInfos();
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
        childrenIds = new ArrayList<>();
    }



    @Test
    public void testAuthorizeAdmin(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        map.set("idEtablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(HttpMethod.GET).when(request).method();
        Mockito.doReturn(map).when(request).params();
        structures.add("11111111");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }


    @Test
    public void testAuthorizeInStructureAndPupil(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        user.setUserId("917544");
        map.set("idEtablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        map.set("idEleve", "917544");
        Mockito.doReturn(HttpMethod.GET).when(request).method();
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }


    @Test
    public void testAuthorizeInStructureAndParent(TestContext ctx){
        role1.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        user.setUserId("111111");
        childrenIds.add("917544");
        user.setChildrenIds(childrenIds);
        map.set("idEtablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        map.set("idEleve", "917544");
        Mockito.doReturn(HttpMethod.GET).when(request).method();
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }


    @Test
    public void testAuthorizeNotInStructureAndPupil(TestContext ctx){
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        user.setUserId("917544");
        map.set("idEtablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        map.set("idEleve", "917544");
        Mockito.doReturn(HttpMethod.GET).when(request).method();
        Mockito.doReturn(map).when(request).params();
        structures.add("1111111");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
        });
    }


    @Test
    public void testAuthorizeNotInStructureAndParent(TestContext ctx){
        role1.setDisplayName(WorkflowActions.COMPETENCES_ACCESS.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        user.setUserId("111111");
        childrenIds.add("917544");
        user.setChildrenIds(childrenIds);
        map.set("idEtablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        map.set("idEleve", "917544");
        Mockito.doReturn(HttpMethod.GET).when(request).method();
        Mockito.doReturn(map).when(request).params();
        structures.add("111111111");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
        });
    }



}
