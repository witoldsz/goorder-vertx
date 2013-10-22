package net.goorder.vertx;

import lombok.Data;
import org.vertx.java.core.json.JsonObject;

/**
 *
 * @author witoldsz
 */
@Data
public class Config {
    
    private static final String MONGO_ADDR = "goorder.mongo";
    
    private final JsonObject config;
    
    public String getMongoModuleName() {
        return config.getObject("mongo").getString("moduleName");
    }
    
    public JsonObject getMongo() {
        return new JsonObject()
                .mergeIn(config.getObject("mongo"))
                .putString("address", MONGO_ADDR);
    }
    
    public String getMongoAddress() {
        return MONGO_ADDR;
    }

    public int getHttpPort() {
        return config.getObject("http").getInteger("port");
    }
    
    public String getHttpPath() {
        return config.getObject("http").getString("path");
    }
}
