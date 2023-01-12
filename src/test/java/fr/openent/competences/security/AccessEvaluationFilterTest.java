package fr.openent.competences.security;

import fr.openent.competences.bean.NoteDevoir;
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
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
@RunWith(VertxUnitRunner.class)
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
        access = new AccessEvaluationFilter();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        map = Mockito.spy(new HeadersAdaptor(new DefaultHttpHeaders()));
        structures = new ArrayList<>();
        user = new UserInfos();
        actions = new ArrayList<>();
        role1 = new UserInfos.Action();
        childrenIds = new ArrayList<>();
        devoir = Mockito.mock(Devoir.class);
        filterDevoirUtils = Mockito.mock(FilterDevoirUtils.class);
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

    @Test
    public void testAuthorizeTeacherWithGoodDevoirID(TestContext ctx){
        //a tester si user est admin => return true;
        // s'il n'est pas admin, tester s'il est enseignant
        //s'il est enseignant:
        //Tester si la requete contient un parametre IDDEVOIR
        //Si elle ne contient pas le paramètre IDDEVOIR => on renvoie false
        //Si on a bien le param IDDEVOIR:
        //On récupère l'id du devoir et on regarde si le user peut y accéder
        //sinon on renvoie false
        //Si pas enseignant : on renvoie false
        role1.setDisplayName(WorkflowActions.ACCESS_SUIVI_CLASSE.toString());
        actions.add(role1);
        user.setAuthorizedActions(actions);
        user.setType("Teacher");
        user.setUserId("abcd");
        map.set(Field.IDDEVOIR, "11111");
        devoir.setOwner("abcd");
        devoir.setId("11111");
        Async async = ctx.async();

        Mockito.doReturn(map).when(request).params();
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Handler handler = invocation.getArgument(2);
            handler.handle(true);
            return null;
        }).when(filterDevoirUtils).validateAccessDevoir(Mockito.anyLong(),Mockito.any(UserInfos.class), Mockito.any(Handler.class));
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }


}
