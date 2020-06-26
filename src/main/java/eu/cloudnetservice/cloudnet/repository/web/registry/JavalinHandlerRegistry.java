package eu.cloudnetservice.cloudnet.repository.web.registry;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.web.WebServer;
import io.javalin.Javalin;

public interface JavalinHandlerRegistry {

    void init(CloudNetUpdateServer server, WebServer webServer, Javalin javalin);

}
