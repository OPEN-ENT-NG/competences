package fr.openent.competences.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.user.UserInfos;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(VertxUnitRunner.class)
public class IsRelativeTest {
    IsRelative access;
    HttpServerRequest request;
    Binding binding;
    UserInfos user;

    @Before
    public void setUp() throws NoSuchFieldException {
        access = new IsRelative();
        request = Mockito.mock(HttpServerRequest.class);
        binding = Mockito.mock(Binding.class);
        user = new UserInfos();
    }

    @Test
    public void testAuthorize(TestContext ctx) {
        Mockito.doReturn(null).when(request).params();
        user.setType("Relative");
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(true, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }

    @Test
    public void testBadUserType(TestContext ctx) {
        Mockito.doReturn(null).when(request).params();
        user.setType("Teacher");
        Async async = ctx.async();
        access.authorize(request, binding, user, result -> {
            ctx.assertEquals(false, result);
            async.complete();
        });
        async.awaitSuccess(10000);
    }
}
