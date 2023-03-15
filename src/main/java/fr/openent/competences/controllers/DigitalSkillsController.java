package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.security.AccessConseilDeClasse;
import fr.openent.competences.security.AccessIfMyStructure;
import fr.openent.competences.security.DigitalSkillsFilter;
import fr.openent.competences.service.digitalSkills.ClassAppreciationDigitalSkillsService;
import fr.openent.competences.service.digitalSkills.DigitalSkillsService;
import fr.openent.competences.service.digitalSkills.StudentDigitalSkillsService;
import fr.openent.competences.service.digitalSkills.StudentAppreciationDigitalSkillsService;
import fr.openent.competences.service.digitalSkills.impl.DefaultClassAppreciationDigitalSkills;
import fr.openent.competences.service.digitalSkills.impl.DefaultDigitalSkillsService;
import fr.openent.competences.service.digitalSkills.impl.DefaultStudentDigitalSkills;
import fr.openent.competences.service.digitalSkills.impl.DefaultStudentAppreciationDigitalSkills;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class DigitalSkillsController extends ControllerHelper {
    private final DigitalSkillsService digitalSkillsService;
    private final ClassAppreciationDigitalSkillsService classAppreciationDigitalSkillsService;
    private final StudentAppreciationDigitalSkillsService studentAppreciationDigitalSkillsService;
    private final StudentDigitalSkillsService studentDigitalSkillsService;

    public DigitalSkillsController() {
        classAppreciationDigitalSkillsService = new DefaultClassAppreciationDigitalSkills(Competences.COMPETENCES_SCHEMA,
                Competences.CLASS_APPRECIATION_DIGITAL_SKILLS);
        studentAppreciationDigitalSkillsService = new DefaultStudentAppreciationDigitalSkills(Competences.COMPETENCES_SCHEMA,
                Competences.STUDENT_APPRECIATION_DIGITAL_SKILLS);
        studentDigitalSkillsService = new DefaultStudentDigitalSkills(Competences.COMPETENCES_SCHEMA,
                Competences.STUDENT_DIGITAL_SKILLS_TABLE);
        digitalSkillsService = new DefaultDigitalSkillsService(classAppreciationDigitalSkillsService,
                studentAppreciationDigitalSkillsService, studentDigitalSkillsService);
    }

    @Get("/digitalSkills/appreciations/evaluation")
    @ApiDoc("get student digital skills and appreciations of digital skills")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessConseilDeClasse.class)
    public void getDigitalSkills(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, userInfo -> {
            if (userInfo != null) {
                final String idStudent = request.params().get("idStudent");
                final String idClass = request.params().get("idClass");
                final String idStructure = request.params().get("idStructure");

                digitalSkillsService.getDigitalSkillsByStudentByClass(idStudent, idClass, idStructure,
                        DefaultResponseHandler.defaultResponseHandler(request));
            } else {
                unauthorized(request);
            }
        });
    }

    @Get("/digitalSkills")
    @ApiDoc("get digital skills with their domains")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessIfMyStructure.class)
    public void getAllDigitalSkillsByDomaine(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, userInfo -> {
            if (userInfo != null) {
                digitalSkillsService.getAllDigitalSkillsByDomaine(arrayResponseHandler(request));
            } else {
                unauthorized(request);
            }
        });
    }

    @Post("/digitalSkills")
    @ApiDoc("create or update level of digital skills")
    @SecuredAction(value = "digital.skills", type = ActionType.WORKFLOW)
    public void createOrUpdateLevelDigitalSkills(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, userInfo -> {
            if (userInfo != null) {
                RequestUtils.bodyToJson(request, pathPrefix +
                        Competences.SCHEMA_LEVEL_DIGITAL_SKILLS, digitalSkill -> {
                    studentDigitalSkillsService.createOrUpdateLevel(digitalSkill, defaultResponseHandler(request));
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Delete("/digitalSkills")
    @ApiDoc("delete level of digital skills")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DigitalSkillsFilter.class)
    public void deleteEvaluationDigitalSkills(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user ->{
            if(user != null){
                try{
                    Long idDigSkill = Long.parseLong(request.params().get("idDigSkill"));
                    studentDigitalSkillsService.deleteDigitalSkillLevel(idDigSkill, defaultResponseHandler(request));
                } catch (NumberFormatException e){
                    log.error("idDigSkill is not a Long object " + e.getMessage());
                    badRequest(request, e.getMessage());
                }
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/classAppreciationDigitalSkills")
    @ApiDoc("create class appreciation digital skills")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DigitalSkillsFilter.class)
    public void createOrUpdateClassAppreciationDigitalSkills(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, userInfo -> {
            if (userInfo != null) {
                RequestUtils.bodyToJson(request, pathPrefix +
                        Competences.SCHEMA_CLASS_APPRECIATION_DIGITAL_SKILLS, classApp -> {

                    classAppreciationDigitalSkillsService.createOrUpdateClassAppreciation(classApp, defaultResponseHandler(request));
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Delete("/classAppreciationDigitalSkills")
    @ApiDoc("delete class appreciation digital skills")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DigitalSkillsFilter.class)
    public void deleteClassAppreciationDigitalSkills(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user ->{
            if(user != null){
                try{
                    Long idClassApp = Long.parseLong(request.params().get("idClassApp"));
                    classAppreciationDigitalSkillsService.deleteClassAppreciation(idClassApp, defaultResponseHandler(request));
                } catch (NumberFormatException e){
                    log.error("idClassApp is not a Long object " + e.getMessage());
                    badRequest(request, e.getMessage());
                }
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Post("/studentAppreciationDigitalSkills")
    @ApiDoc("create student appreciation digital skills")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DigitalSkillsFilter.class)
    public void createOrUpdateStudentAppreciationDigitalSkills(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, userInfo -> {
            if (userInfo != null) {
                RequestUtils.bodyToJson(request, pathPrefix +
                        Competences.SCHEMA_STUDENT_APPRECIATION_DIGITAL_SKILLS, studentApp -> {

                    studentAppreciationDigitalSkillsService.createOrUpdateStudentAppreciation(studentApp, defaultResponseHandler(request));
                });
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }

    @Delete("/studentAppreciationDigitalSkills")
    @ApiDoc("delete student appreciation digital skills")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(DigitalSkillsFilter.class)
    public void deleteStudentAppreciationDigitalSkills(final HttpServerRequest request){
        UserUtils.getUserInfos(eb, request, user ->{
            if(user != null){
                try{
                    Long idStudentApp = Long.parseLong(request.params().get("idStudentApp"));
                    studentAppreciationDigitalSkillsService.deleteStudentAppreciation(idStudentApp, defaultResponseHandler(request));
                } catch (NumberFormatException e){
                    log.error("idStudentApp is not a Long object " + e.getMessage());
                    badRequest(request, e.getMessage());
                }
            } else {
                log.debug("User not found in session.");
                Renders.unauthorized(request);
            }
        });
    }
}
