package net.goorder.vertx;

import java.util.concurrent.CompletableFuture;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

/**
 *
 * @author witoldsz
 */
public class LauncherSockJsVerticle extends Verticle {

    JsonObject mongoCfg, persistorCfg, sockJsCfg;
    JsonObject httpCfg;
    HttpServer httpServer;
    SockJSServer sockJSServer;
    Logger logger;

    private SockJSSocket socket;

    @Override
    public void start() {
        logger = container.logger();
        httpCfg = container.config().getObject("http");
        httpServer = vertx.createHttpServer();

        mongoCfg = container.config().getObject("mongo");
        persistorCfg = new JsonObject()
                .mergeIn(mongoCfg)
                .putString("address", "goorder.mongo");

        sockJsCfg = container.config().getObject("sockJS");
        sockJSServer = vertx.createSockJSServer(httpServer);
        Logger logger = container.logger();
        CompletableFuture.allOf(deployMongo(), runWebServer())
        .thenAccept(result -> {
            logger.info("LauncherSockJsVerticle started");
        })
        .exceptionally(e -> {
            container.exit();
            return null;
        });
    }

    private CompletableFuture<Void> deployMongo() {
        CompletableFuture<Void> promise = new CompletableFuture<>();
        container.deployModule(mongoCfg.getString("moduleName"), persistorCfg, event -> {
            if (event.failed()) {
                promise.completeExceptionally(event.cause());
            } else {
                logger.info("mongodb started: " + event.result());
                promise.complete(null);
            }
        });
        return promise;
    }

    private CompletableFuture<Void> runWebServer() {
        CompletableFuture<Void> promise = new CompletableFuture<>();
        httpServer.listen(httpCfg.getInteger("port"), event -> {
            if (event.failed()) {
                promise.completeExceptionally(event.cause());
            } else {
                logger.info("HTTP server started");
                promise.complete(null);
            }
        });
        return promise;
    }

    private CompletableFuture<Void> runSockJs() {
        logger.info("sockjs " + sockJsCfg);
        CompletableFuture<Void> promise = new CompletableFuture<>();
        sockJSServer.installApp(sockJsCfg, socket -> {
            this.socket = socket;
            logger.info("sockjs started: " + socket.writeHandlerID());
            promise.complete(null);
        });
        return promise;
    }
}
