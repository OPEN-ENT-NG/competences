package fr.openent.competences.security;

import fr.openent.competences.Competences;
import fr.openent.competences.security.utils.FilterAppreciationUtils;
import fr.openent.competences.security.utils.FilterNoteUtils;
import fr.openent.competences.security.utils.WorkflowActionUtils;
import fr.openent.competences.security.utils.WorkflowActions;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by anabah on 02/03/2017.
 */
public class AccessAppreciationFilter implements ResourcesProvider {

    protected static final Logger log = LoggerFactory.getLogger(AccessAppreciationFilter.class);

    @Override
    public void authorize(final HttpServerRequest resourceRequest, Binding binding, UserInfos user,
                          final Handler<Boolean> handler) {
        if (new WorkflowActionUtils().hasRight(user,WorkflowActions.ADMIN_RIGHT.toString())){
            handler.handle(true);
        }
        else {
            switch (user.getType()) {
                case "Teacher": {
                    resourceRequest.pause();

                    Long idAppreciation;
                    try {
                        idAppreciation = Long.parseLong(resourceRequest.params().get("idAppreciation"));
                    } catch (NumberFormatException e) {
                        log.error("Error : idAppreciation must be a long object", e);
                        handler.handle(false);
                        return;
                    }
                    WorkflowActionUtils.hasHeadTeacherRight(user, null, new JsonArray().add(idAppreciation),
                            Competences.APPRECIATIONS_TABLE, null, null,
                            new Handler<Either<String, Boolean>>() {
                                @Override
                                public void handle(Either<String, Boolean> event) {
                                    Boolean isHeadTecher;
                                    if(event.isLeft()){
                                        isHeadTecher = false;
                                    }
                                    else {
                                        isHeadTecher = event.right().getValue();
                                    }

                                    // Si l'enseignant est prof principal dans la classe de l'élève,
                                    // alors il a le droit d'accéder à l'appréciation de l'élève.
                                    if (isHeadTecher) {
                                        resourceRequest.resume();
                                        handler.handle(true);
                                    }
                                    else {
                                        new FilterAppreciationUtils().validateAccessAppreciation(idAppreciation, user,
                                                new Handler<Boolean>() {
                                                    @Override
                                                    public void handle(Boolean isValid) {
                                                        resourceRequest.resume();
                                                        handler.handle(isValid);
                                                    }
                                                });
                                    }
                                }
                            });

                }
                break;
                default: {
                    handler.handle(false);
                }
            }
        }
    }
}
