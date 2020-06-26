package eu.cloudnetservice.cloudnet.repository.web;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.web.registry.*;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;

import java.util.Arrays;
import java.util.Collection;

// todo some documentation entries have no 200 response
public class WebServer {

    private static final Collection<JavalinHandlerRegistry> REGISTRIES = Arrays.asList(
            new GeneralHandlerRegistry(),
            new FaqHandlerRegistry(),
            new ModuleHandlerRegistry(),
            new VersionsHandlerRegistry()
    );

    private final CloudNetUpdateServer server;

    private Javalin javalin;
    private boolean apiAvailable = System.getProperty("cloudnet.repository.api.enabled", "true").equalsIgnoreCase("true");

    public WebServer(CloudNetUpdateServer server) {
        this.server = server;
    }

    public boolean isApiAvailable() {
        return this.apiAvailable;
    }

    public void setApiAvailable(boolean apiAvailable) {
        this.apiAvailable = apiAvailable;
    }

    public Javalin getJavalin() {
        return this.javalin;
    }

    public void init() {
        this.javalin = Javalin.create(config -> {
            config.enableWebjars();

            OpenApiOptions options = CloudNetOpenApiOptions.create(this.server);

            config.registerPlugin(new OpenApiPlugin(options));

            config.enableCorsForAllOrigins();
            config.requestCacheSize = 16384L;
        });

        JavalinJson.setToJsonMapper(JsonDocument.GSON::toJson);
        JavalinJson.setFromJsonMapper(JsonDocument.GSON::fromJson);

        this.javalin.config.accessManager(new CloudNetAccessManager(this.server));

        // sometimes we can't use the Context#json methods to set the response because they don't accept null input

        for (JavalinHandlerRegistry registry : REGISTRIES) {
            registry.init(this.server, this, this.javalin);
        }

        this.javalin.start(this.server.getConfiguration().getWebPort());
    }

    public void stop() {
        if (this.javalin != null) {
            this.javalin.stop();
        }
    }

}
