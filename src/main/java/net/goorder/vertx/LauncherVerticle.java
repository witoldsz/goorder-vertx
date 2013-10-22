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

                //initial load
                eb.send(config.getMongoAddress(), new JsonObject()
                        .putString("collection", "test")
                        .putString("action", "findone")
                        .putObject("matcher", new JsonObject().putValue("_id", 1)),
                        (Message<JsonObject> event) -> {
                    eb.publish("goorder.message", event.body().getObject("result"));
                });

                //register + unregister peers
                Handler<Message> messageHandler = event -> {
                    socket.writeTextFrame(event.body().toString());
                };
                eb.registerHandler("goorder.message", messageHandler);
                socket.closeHandler(v -> {
                    eb.unregisterHandler("goorder.message", messageHandler);
                });

                //incoming data
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
            if (request.path().equals("/")) request.response().sendFile("docroot/index.html");
        }).listen(config.getHttpPort());

        container.deployModule(config.getMongoModuleName(), config.getMongo());
        log.info("START");
    }
}
