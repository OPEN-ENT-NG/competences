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
public class AccessEvaluationFilterTest {
    AccessEvaluationFilter access;
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
        access = new AccessEvaluationFilter();
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
    public void testAuthorizeAdminInStructure(TestContext ctx){
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
        user.setAuthorizedActions(actions);
        map.set("idEtablissement", "9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        Mockito.doReturn(HttpMethod.GET).when(request).method();
        Mockito.doReturn(map).when(request).params();
        structures.add("9af51dc6-ead0-4edb-8978-da14a3e9f49a");
        user.setStructures(structures);
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
        });
    }


}
