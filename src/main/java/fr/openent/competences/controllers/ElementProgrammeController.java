package fr.openent.competences.controllers;

import fr.openent.competences.security.AccessElementProgrammeFilter;
import fr.openent.competences.service.ElementProgramme;
import fr.openent.competences.service.impl.DefaultElementProgramme;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.http.HttpServerRequest;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ElementProgrammeController extends ControllerHelper {

    /**
     * Déclaration des services
     */
    private final ElementProgramme elementProgramme;


    public ElementProgrammeController() {
        elementProgramme = new DefaultElementProgramme();
    }


    @Get("/element/programme/domaines")
    @ApiDoc("Récupère les domaines d'enseignement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementProgrammeFilter.class)
    public void getDomainesEnseignement(final HttpServerRequest request) {
        elementProgramme.getDomainesEnseignement(arrayResponseHandler(request));
    }

    @Get("/element/programme/sous/domaines")
    @ApiDoc("Récupère les domaines d'enseignement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementProgrammeFilter.class)
    public void getSousDomainesEnseignement(final HttpServerRequest request) {
        elementProgramme.getSousDomainesEnseignement(arrayResponseHandler(request));
    }

    @Get("/element/programme/propositions")
    @ApiDoc("Récupère les domaines d'enseignement")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessElementProgrammeFilter.class)
    public void getPropositions(final HttpServerRequest request) {
        elementProgramme.getPropositions(arrayResponseHandler(request));
    }
}
