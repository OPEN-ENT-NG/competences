package fr.openent.competences.controllers;

import fr.openent.competences.Competences;
import fr.openent.competences.bean.NoteDevoir;
import fr.openent.competences.service.NoteService;
import fr.openent.competences.service.UtilsService;
import fr.openent.competences.service.impl.DefaultNoteService;
import fr.openent.competences.service.impl.DefaultUtilsService;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.webutils.Either;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class EventBusController extends ControllerHelper {

    private UtilsService utilsService;
    private NoteService noteService;

    public EventBusController() {
        utilsService = new DefaultUtilsService();
        noteService = new DefaultNoteService(Competences.COMPETENCES_SCHEMA, Competences.NOTES_TABLE);
    }

    @BusAddress("competences")
    public void getData(final Message<JsonObject> message) {
        final String action = message.body().getString("action");

        if (action == null) {
            log.warn("[@BusAddress](competences) Invalid action.");
            message.reply(new JsonObject().put("status", "error")
                    .put("message", "invalid action."));

            return;
        }

        String service = action.split("\\.")[0];
        String method = action.split("\\.")[1];

        switch (service) {
            case "utils": {
                utilsBusService(method, message);
            }
            break;
            case "note": {
                noteBusService(method, message);
            }
            break;
        }
    }

    private void utilsBusService(String method, Message<JsonObject> message) {
        switch (method) {
            case "getCycle": {
                List<String> ids = message.body().getJsonArray("ids").getList();
                utilsService.getCycle(ids, getJsonArrayBusResultHandler(message));
            }
            break;
            case "calculMoyenne": {
                List<NoteDevoir> listeNoteDevoirs = new ArrayList<>();
                JsonArray notes = message.body().getJsonArray("listeNoteDevoirs");
                for (int i = 0; i < notes.size(); i++) {
                    JsonObject note = notes.getJsonObject(i);
                    listeNoteDevoirs.add(
                            new NoteDevoir(Double.parseDouble(note.getInteger("valeur").toString()),
                                    Double.parseDouble(note.getInteger("diviseur").toString()),
                                    note.getBoolean("ramener_sur"),
                                    Double.parseDouble(note.getInteger("coefficient").toString())));
                }

                message.reply(new JsonObject()
                        .put("status", "ok")
                        .put("result",
                                utilsService.calculMoyenne(listeNoteDevoirs,
                                        message.body().getBoolean("statistiques"),
                                        (Integer) message.body().getInteger("diviseurM"))));
            }
            break;
            default: {
                message.reply(getErrorReply("Method not found"));
            }
        }
    }

    private void noteBusService(String method, Message<JsonObject> message) {
        switch (method) {
            case "getNotesParElevesParDevoirs": {
                JsonArray idEleves = message.body().getJsonArray("idEleves");
                JsonArray idDevoirs = message.body().getJsonArray("idDevoirs");
                noteService.getNotesParElevesParDevoirs(
                        convertJsonArrayToStringArray(idEleves),
                        convertJsonArrayToLongArray(idDevoirs),
                        getJsonArrayBusResultHandler(message));
            }
            break;
            default: {
                message.reply(getErrorReply("Method not found"));
            }
        }
    }

    private Handler<Either<String, JsonArray>> getJsonArrayBusResultHandler(final Message<JsonObject> message) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> result) {
                if (result.isRight()) {
                    message.reply(new JsonObject()
                            .put("status", "ok")
                            .put("results", result.right().getValue()));
                } else {
                    message.reply(getErrorReply(result.left().getValue()));
                }
            }
        };
    }

    private JsonObject getErrorReply(String message) {
        return new JsonObject()
                .put("status", "error")
                .put("message", message);
    }

    private String[] convertJsonArrayToStringArray(JsonArray list) {
        String[] objects = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            objects[i] = list.getString(i);
        }

        return objects;
    }

    private Long[] convertJsonArrayToLongArray(JsonArray list) {
        Long[] objects = new Long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            objects[i] = list.getLong(i);
        }

        return objects;
    }
}
