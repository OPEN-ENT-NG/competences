package fr.openent.competences.helpers;

import fr.openent.competences.Competences;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.ProxyOptions;
import org.entcore.common.controller.ControllerHelper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;



public class NodePdfGeneratorClientHelper extends ControllerHelper{
    public static final String CONNECTION_RESET_BY_PEER = "Connection reset by peer";
    public static final String BAD_GATEWAY = "Bad Gateway";
    public static final String CONNECTION_WAS_CLOSED = "Connection was closed";
    public static final String SERVICE_UNAVAILABLE = "Service Unavailable";
    public static final String FAILED_TO_CREATE_SSL_CONNECTION = "Failed to create SSL connection";

    public NodePdfGeneratorClientHelper() {
        super();
    }

    public static HttpClient createHttpClient(Vertx vertx) {
        final HttpClientOptions options = new HttpClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);
        options.setVerifyHost(false);
        options.setKeepAlive(true);
        if (System.getProperty("httpclient.proxyHost") != null) {
            ProxyOptions proxyOptions = new ProxyOptions()
                    .setHost(System.getProperty("httpclient.proxyHost"))
                    .setPort(Integer.parseInt(System.getProperty("httpclient.proxyPort")));
            //COMMENT THIS IF NO PORXY LOCAL
            options.setProxyOptions(proxyOptions);
            
        }
        return  vertx.createHttpClient(options);
    }


    public static void webServiceNodePdfGeneratorPost(HttpClient httpClient, String file,
                                                      Handler<Either<String, Buffer>> handler) {
        String nodePdfGeneratorUrl = Competences.NODE_PDF_GENERATOR.getString("url");
        URI url;
        try {
            url = new URI(nodePdfGeneratorUrl);
        } catch (URISyntaxException e) {
            handler.handle(new Either.Left<>("Bad request"));
            return;
        }
        HttpClientRequest httpClientRequest = createPostToNodepdfGenerator(httpClient, url, handler);

        httpClientRequest.exceptionHandler( event -> {
            log.error("[ webServiceNodePdfGeneratorPost ] :" + event);
            handler.handle(new Either.Left<>(event.getMessage()));
        }).setFollowRedirects(true);

        final String boundary = UUID.randomUUID().toString();
        httpClientRequest.setChunked(true)
                .putHeader(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                .putHeader(HttpHeaders.AUTHORIZATION, Competences.NODE_PDF_GENERATOR.getString("authorization"))
                .putHeader(HttpHeaders.ACCEPT, "*/*")
                .end(multipartBody(file, boundary));

    }

    private static HttpClientRequest createPostToNodepdfGenerator(HttpClient httpClient, URI url,
                                                                  Handler<Either<String, Buffer>> handler){
        RequestOptions requestOptions = new RequestOptions()
                .setAbsoluteURI(url.toString())
                .setMethod(HttpMethod.POST);

        Future<HttpClientRequest> requestFuture = httpClient.request(requestOptions)
                .onSuccess(request -> request.send()
                    .onSuccess(response -> {
                        if (response.statusCode() == 200) {
                            final Buffer buff = Buffer.buffer();
                            response.handler(event -> buff.appendBuffer(event));
                            response.endHandler(end -> handler.handle(new Either.Right<>(buff)));
                        } else {
                            log.error("Fail to post to node-pdf-generator: " + response.statusMessage());
                            response.bodyHandler(event -> handler.handle(new Either.Left<>(event.toString("UTF-8"))));
                        }
                    })
                    .onFailure(throwable -> {
                        log.error("An error occurred during the request: ", throwable);
                        handler.handle(new Either.Left<>(throwable.getMessage()));
                    })
                );
        return requestFuture.result();
    }

    private static void appendBuffer(Buffer dest, Buffer src){
        dest.appendBuffer(src);
    }

    private static Buffer multipartBody(String content, String boundary) {
        Buffer buffer = Buffer.buffer();
        // Add name
        buffer.appendString("--" + boundary + "\r\n")
                .appendString("Content-Disposition: form-data; name=\"name\"\r\n")
                .appendString("\r\n")
                .appendString("export\r\n");

        // Add file
        buffer.appendString("--" + boundary + "\r\n")
                .appendString("Content-Disposition: form-data; name=\"template\"; filename=\"file\"\r\n")
                .appendString("Content-Type: application/xml\r\n")
                .appendString("\r\n")
                .appendString(content)
                .appendString("\r\n")
                .appendString("--" + boundary + "--\r\n");

        return buffer;
    }
}
