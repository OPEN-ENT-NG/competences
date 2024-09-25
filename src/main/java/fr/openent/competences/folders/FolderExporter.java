package fr.openent.competences.folders;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.StringUtils;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


public class FolderExporter {
    public static final Logger log = LoggerFactory.getLogger(FolderExporter.class);

    public static class FolderExporterContext {
        public final String basePath;
        public Map<String, String> namesByIds = new HashMap<>();
        public Map<String, List<JsonObject>> docByFolders = new HashMap<>();
        public Set<String> folders = new HashSet<>();
        public JsonArray errors = new JsonArray();

        public FolderExporterContext(String basePath) {
            super();
            this.basePath = basePath;
        }

    }

    protected final FileSystem fs;
    protected final Storage storage;
    protected final boolean throwErrors;

    public FolderExporter(Storage storage, FileSystem fs) {
        this(storage, fs, true);
    }

    public FolderExporter(Storage storage, FileSystem fs, boolean throwErrors) {
        this.fs = fs;
        this.storage = storage;
        this.throwErrors = throwErrors;
    }

    public static String getParent(JsonObject doc) {
        return doc.getString("parent");
    }

    private void buildPath(List<JsonObject> rows, JsonObject current, List<String> paths) {
        String parent = getParent(current);
        if (!StringUtils.isEmpty(parent)) {
            Optional<JsonObject> founded = rows.stream().filter(r -> parent.equals(getId(r)))
                    .findFirst();
            if (founded.isPresent()) {
                String name = cleanName(founded.get());
                buildPath(rows, founded.get(), paths);
                paths.add(name);
            }
        }
    }


    //
    private void buildMapping(List<JsonObject> rows, FolderExporterContext context) {
        for (JsonObject row : rows) {
            context.namesByIds.put(row.getString("id"), cleanName(row));
        }

        for (JsonObject row : rows) {
            if (isFile(row)) {
                List<String> paths = new ArrayList<>();
                buildPath(rows, row, paths);
                String folderPath = paths.stream().reduce("", (t, u) -> t + File.separator + u);
                String fullFolderPath = Paths.get(context.basePath, folderPath).normalize().toString();
                context.folders.add(fullFolderPath);
                context.docByFolders.putIfAbsent(fullFolderPath, new ArrayList<>());
                context.docByFolders.get(fullFolderPath).add(row);
            }
        }
        log.info("folders Size : " + context.folders.size());
    }

    private Future<Void> mkdirs(FolderExporterContext context) {
        Set<String> uniqFolders = new HashSet<String>();
        for (String f1 : context.folders) {

            boolean ignore = false;
            for (String f2 : context.folders) {
                // if one folder f2 include this one => ignore f1
                if (!f1.equals(f2) && f2.contains(f1)) {
                    ignore = true;
                }
            }
            if (!ignore) {
                uniqFolders.add(f1);
            }
        }
        //
        Promise<Void> rootPromise = Promise.promise();
        fs.mkdirs(context.basePath, rootPromise);
        return rootPromise.future().compose(resRoot -> {
            List<Future<Void>> futures = new ArrayList<>();
            for (String path : uniqFolders) {
                Promise<Void> promise = Promise.promise();
                fs.mkdirs(path, res -> {
                    log.info("Folder creation result: " + "/" + path);
                    promise.handle(res);
                });
                futures.add(promise.future());
            }
            return Future.all(futures).map(res -> null);
        });
    }

    public static String getId(JsonObject doc) {
        return doc.getString("id");
    }

    public static boolean isFile(JsonObject doc) {
        return getType(doc).equals("file");
    }
    public static String getType(JsonObject doc) {
        return doc.getString("type", "");
    }

    private static String cleanName(JsonObject doc) {
        String name = getName(doc, "undefined");
        return name.replaceAll("/", "_").replaceAll("\\\\", "_").trim();
    }

    public static String getName(JsonObject doc, String def) {
        return doc.getString("name", def);
    }
    private CompositeFuture copyFiles(FolderExporterContext context) {
        @SuppressWarnings("rawtypes")
        List<Future> futures = new ArrayList<>();
        for (String folderPath : context.docByFolders.keySet()) {
            Promise<JsonObject> promise = Promise.promise();
            futures.add(promise.future());
            List<JsonObject> docs = context.docByFolders.get(folderPath);
//
            JsonObject nameByFileId = new JsonObject();
            Map<String, Integer> nameCount = new HashMap<>();
            for (JsonObject doc : docs) {
                String fileId = getFileId(doc);
                String name = cleanName(doc);
                Integer count = nameCount.merge(name, 1, Integer::sum) - 1;
//                 if name already exists ... add suffix
                if (count > 0) {
                    if (name.contains(".")) {
                        name = name.substring(0, name.indexOf(".")) + "_" + count + name.substring(name.indexOf("."));
                    } else {
                        name = name + "_" + count;
                    }
                }
                name = replaceForbiddenCharacters(name);
                nameByFileId.put(fileId, name);
                context.namesByIds.put(fileId,name);
            }
//
            String[] ids = nameByFileId.fieldNames().stream().toArray(String[]::new);
            storage.writeToFileSystem(ids, folderPath, nameByFileId, res -> {
                if ("ok".equals(res.getString("status"))) {
                    promise.complete(res);
                } else if (throwErrors) {
                    promise.fail(res.getString("error"));
                } else {
                    context.errors.addAll(res.getJsonArray("errors"));
                    promise.complete();
                    log.error("Failed to export file : " + folderPath + " - " + nameByFileId + "- "
                            + new fr.wseduc.webutils.collections.JsonArray(Arrays.asList(ids)).encode() + " - "
                            + res.encode());
                }
            });
        }
        return CompositeFuture.all(futures);
    }
    public static String replaceForbiddenCharacters(String s) {
        return s.replaceAll("(\\\\|\\/|\\*|\\\"|\\<|\\>|:|\\?|\\|)","_");
    }
    public static String getFileId(JsonObject doc) {
        return doc.getString("file", "");
    }

    //
    public Future<FolderExporterContext> export(FolderExporterContext context, List<JsonObject> rows) {
        this.buildMapping(rows, context);
        return this.mkdirs(context).compose(res -> {
            return this.copyFiles(context);
        }).map(context);
    }
}
