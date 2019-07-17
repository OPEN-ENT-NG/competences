package fr.openent.competences.utils;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.Server;
import fr.wseduc.webutils.collections.JsonUtils;
import fr.wseduc.webutils.data.FileResolver;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static fr.openent.competences.Competences.*;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class MustachHelper {

    protected static final Logger log = LoggerFactory.getLogger(MustachHelper.class);
    protected String pathPrefix;
    private final I18n i18n;
    protected Vertx vertx;
    private static final ConcurrentMap<String, Template> templates = new ConcurrentHashMap();
    protected JsonObject config;

    public MustachHelper (Vertx vertx, JsonObject config) {
        this.config = config;
        if (config != null) {
            this.pathPrefix = Server.getPathPrefix(config);
        }

        this.i18n = I18n.getInstance();
        this.vertx = vertx;
    }

    public void processTemplate(JsonObject p, String resourceName, Reader r, String path,
                                String host, String acceptLanguage, Boolean forwardedFor,
                                final Handler<Writer> handler) {
        final JsonObject params = p == null ? new JsonObject() : p.copy();
        this.getTemplate(path, resourceName, r, true, t ->{
            if (t != null) {
                try {
                    Writer writer = new StringWriter();
                    Map<String, Object> ctx = JsonUtils.convertMap(params);
                    MustachHelper.this.setLambdaTemplateRequest(ctx, host, acceptLanguage, forwardedFor);
                    t.execute(ctx, writer);
                    handler.handle(writer);
                } catch (Exception var4) {
                    MustachHelper.log.error(var4.getMessage(), var4);
                    handler.handle((Writer) null);
                }
            } else {
                handler.handle((Writer) null);
            }

        });
    }

    private Handler<Writer> handlerGeneratePdf(final JsonObject templateProps,
                                               final String templateName, String host, String scheme,
                                               EventBus eb, Handler<Either<String, Buffer>> handler) {
        return  writer -> {
            String processedTemplate = ((StringWriter) writer).getBuffer().toString();
            if (processedTemplate == null) {
                if (templateProps != null) {
                    log.error("processing error : \ntemplateProps : " + templateProps.toString()
                            + "\ntemplateName : " + templateName);
                }
                return;
            }
            JsonObject actionObject = new JsonObject();
            byte[] bytes;
            try {
                bytes = processedTemplate.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                bytes = processedTemplate.getBytes();
                log.error(e.getMessage(), e);
            }
            final String baseUrl = scheme + "://" + host + config.getString("app-address") + "/public/";

            String node = (String) vertx.sharedData().getLocalMap("server").get("node");
            if (node == null) {
                node = "";
            }
            final String _node = node;
            actionObject
                    .put("content", bytes)
                    .put("baseUrl", baseUrl);
            eb.send(_node + "entcore.pdf.generator", actionObject, DELIVERY_OPTIONS,
                    handlerToAsyncHandler( reply -> {
                        JsonObject pdfResponse = reply.body();
                        if (!OK.equals(pdfResponse.getString(STATUS))) {
                            String error = pdfResponse.getString(MESSAGE);
                            handler.handle(new Either.Left<>(error));
                            return;
                        }
                        byte[] pdf = pdfResponse.getBinary("content");
                        handler.handle(new Either.Right<>(Buffer.buffer(pdf)));
                    }));
        };
    }
    public void generatePdf(JsonObject p, String resourceName, Reader r, String path, String host,
                            String acceptLanguage, Boolean forwardedFor, String scheme,
                            EventBus eb, final Handler<Either<String, Buffer>> handler) {
        this.processTemplate(p, resourceName, r, path, host, acceptLanguage, forwardedFor,
                handlerGeneratePdf(p, resourceName, host, scheme, eb, handler ));
    }

    protected void setLambdaTemplateRequest(Map<String, Object> ctx, String host, String acceptLanguage,
                                            Boolean forwardedFor) {
        ctx.put("i18n", new Mustache.Lambda() {
            public void execute(Template.Fragment frag, Writer out) throws IOException {
                String key = frag.execute();
                String text = MustachHelper.this.i18n.translate(key, host, acceptLanguage, new String[0]);
                out.write(text);
            }
        });
        ctx.put("static", new Mustache.Lambda() {
            public void execute(Template.Fragment frag, Writer out) throws IOException {
                String path = frag.execute();
                out.write(MustachHelper.this.staticResource(MustachHelper.this.config.getBoolean("ssl", false),
                        (String)null, MustachHelper.this.pathPrefix + "/public", path, host, forwardedFor));
            }
        });
        ctx.put("infra", new Mustache.Lambda() {
            public void execute(Template.Fragment frag, Writer out) throws IOException {
                String path = frag.execute();
                out.write(MustachHelper.this.staticResource(MustachHelper.this.config.getBoolean("ssl", false),
                        "8001", "/infra/public", path, host, forwardedFor));
            }
        });
        ctx.put("formatBirthDate", new Mustache.Lambda() {
            public void execute(Template.Fragment frag, Writer out) throws IOException {
                String date = frag.execute();
                if (date != null && date.trim().length() > 0) {
                    String[] splitted = date.split("-");
                    if (splitted.length == 3) {
                        out.write(splitted[2] + "/" + splitted[1] + "/" + splitted[0]);
                        return;
                    }
                }

                out.write(date);
            }
        });
    }
    private void getTemplate(String RequestPath, String resourceName, Reader r, final boolean escapeHTML,
                             final Handler<Template> handler) {
        if (resourceName != null && r != null && !resourceName.trim().isEmpty()) {
            Mustache.Compiler compiler = Mustache.compiler().defaultValue("");
            if (!escapeHTML) {
                compiler = compiler.escapeHTML(escapeHTML);
            }

            handler.handle(compiler.compile(r));
        } else {
            String path;
            String _p;
            if (resourceName != null && !resourceName.trim().isEmpty()) {
                path = "view/" + resourceName;
            } else {
                _p = RequestPath.substring(this.pathPrefix.length());
                if (_p.trim().isEmpty()) {
                    _p = this.pathPrefix.substring(1);
                }
                path = "view/" + _p + ".html";
            }

            if (!"dev".equals(this.config.getString("mode")) && templates.containsKey(path)) {
                handler.handle(templates.get(path));
            } else {
                final String p = FileResolver.absolutePath(path);
                this.vertx.fileSystem().readFile(p, ar -> {
                    if (ar.succeeded()) {
                        Mustache.Compiler compiler = Mustache.compiler().defaultValue("");
                        if (!escapeHTML) {
                            compiler = compiler.escapeHTML(escapeHTML);
                        }

                        Template template = compiler.compile((ar.result()).toString("UTF-8"));
                        if ("dev".equals(MustachHelper.this.config.getString("mode"))) {
                            MustachHelper.templates.put(p, template);
                        } else {
                            MustachHelper.templates.putIfAbsent(p, template);
                        }

                        handler.handle(template);
                    } else {
                        handler.handle(null);
                    }
                });
            }

        }
    }

    private String staticResource(boolean https, String infraPort, String publicDir, String path, String host,
                                  Boolean forwardedFor) {
        String protocol = https ? "https://" : "http://";
        if (infraPort != null && forwardedFor) {
            host = host.split(":")[0] + ":" + infraPort;
        }

        return protocol + host + (publicDir != null && publicDir.startsWith("/") ? publicDir : "/" + publicDir)
                + "/" + path;
    }

}
