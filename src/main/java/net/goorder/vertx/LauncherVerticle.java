package net.goorder.vertx;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 *
 * @author witoldsz
 */
public class LauncherVerticle extends Verticle {
    

    @Override
    public void start() {
        Config config = new Config(container.config());
        EventBus eb = vertx.eventBus();
        Logger log = container.logger();
        
        vertx.createHttpServer().websocketHandler(socket -> {
            log.info("socket handler path = " + socket.path());
            if (socket.path().equals(config.getHttpPath())) {
                Handler<Message> messageHandler = event -> {
                    socket.writeTextFrame(event.body().toString());
                };
                eb.registerHandler("goorder.message", messageHandler);
                socket.closeHandler(v -> {
                    eb.unregisterHandler("goorder.message", messageHandler);
                });
                socket.dataHandler(buffer -> {
                    log.info("socket data = " + buffer);
                    JsonObject data = new JsonObject(buffer.toString());
                    JsonObject action = new JsonObject();
                    action.putString("action", "save");
                    action.putString("collection", "test");
                    action.putObject("document", data);
                    eb.send(config.getMongoAddress(), action, (Message event) -> {
                        eb.publish("goorder.message", data);
                    });
                });
            }
        }).requestHandler(request -> {
            if (request.path().equals("/index.html")) request.response().sendFile("docroot/index.html");
        }).listen(config.getHttpPort());

        container.deployModule(config.getMongoModuleName(), config.getMongo());
        log.info("START");
    }
}
