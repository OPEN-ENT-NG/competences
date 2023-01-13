package fr.openent.competences.security;


import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import fr.openent.competences.constants.Field;
import fr.openent.competences.model.Devoir;
import fr.openent.competences.security.utils.FilterDevoirUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HeadersAdaptor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.vertx.testtools.VertxAssert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(VertxUnitRunner.class)
@PowerMockRunnerDelegate(VertxUnitRunner.class)
public class AccessEvaluationFilterTest {
    AccessEvaluationFilter access;
    HttpServerRequest request;
    Binding binding;
    MultiMap map;
    List<String> structures;
    UserInfos user = new UserInfos();
    List<UserInfos.Action> actions;
    Devoir devoir;
    List<String> childrenIds;
    UserInfos.Action role1;

    FilterDevoirUtils filterDevoirUtils;
    @Before
    public void setUp() throws NoSuchFieldException {
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        map = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
        structures = new ArrayList<>();
        user = mock(UserInfos.class);
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
        childrenIds = new ArrayList<>();
        devoir = Mockito.mock(Devoir.class);
        filterDevoirUtils = Mockito.spy(new FilterDevoirUtils());
        access = new AccessEvaluationFilter();
    }



    @Test
    public void testAuthorizeAdmin(TestContext ctx){
        //a tester si user est admin => return true;
        // s'il n'est pas admin, tester s'il est enseignant
            //s'il est enseignant:
                //Tester si la requete contient un parametre IDDEVOIR
                    //Si elle ne contient pas le paramètre IDDEVOIR => on renvoie false
                    //Si on a bien le param IDDEVOIR:
                        //On récupère l'id du devoir et on regarde si le user peut y accéder
                        //sinon on renvoie false
            //Si pas enseignant : on renvoie false
        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
        actions.add(role1);
        Async async = ctx.async();
        user.setAuthorizedActions(actions);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

//    @Test
//    public void testAuthorizeTeacherWithGoodDevoirID(TestContext ctx) throws Exception {
//        filterDevoirUtils = mock(FilterDevoirUtils.class);
//        access = Mockito.spy(new AccessEvaluationFilter());
//        PowerMockito.whenNew(AccessEvaluationFilter.class).withNoArguments().thenReturn(access);
//        PowerMockito.whenNew(FilterDevoirUtils.class).withNoArguments().thenReturn(filterDevoirUtils);
//        //PowerMockito.whenNew(AccessEvaluationFilter.class).withArguments(filterDevoirUtils).thenReturn(access);
//        doAnswer(invocation -> {
//            Handler<Boolean> handler = invocation.getArgument(2);
//            handler.handle(true);
//            return null;
//        }).when(filterDevoirUtils).validateAccessDevoir(anyLong(), any(), any());
//
//        when(user.getType()).thenReturn("Teacher");
//        map.set(Field.IDDEVOIR, "1111111");
//        Mockito.doReturn(map).when(request).params();
//
//        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
//        actions.add(role1);
//        user.setAuthorizedActions(actions);
//        Async async = ctx.async();
//        access.authorize(request, null, user, result -> {
//            ctx.assertEquals(true, result);
//            async.complete();
//        });
//        async.awaitSuccess(10000);
//    }
//
//
//    @Test
//    public void testAuthorizeTeacherWithBadDevoirID(TestContext ctx) {
//        filterDevoirUtils = mock(FilterDevoirUtils.class);
//        access = mock(AccessEvaluationFilter.class);
//        try {
//            PowerMockito.whenNew(AccessEvaluationFilter.class).withArguments(filterDevoirUtils).thenReturn(access);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        role1.setDisplayName(WorkflowActions.ADMIN_RIGHT.toString());
//        actions.add(role1);
//        user.setAuthorizedActions(actions);
//
//        when(user.getType()).thenReturn("Teacher");
//        map.set(Field.IDDEVOIR, "1111111");
//        Mockito.doReturn(map).when(request).params();
//
//        Async async = ctx.async();
//        user.setAuthorizedActions(actions);
//        access.authorize(request, binding, user, result -> {
//            ctx.assertEquals(true, result);
//            async.complete();
//        });
//        async.awaitSuccess(10000);
//    }
}
