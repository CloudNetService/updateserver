package eu.cloudnetservice.cloudnet.repository.web;

import eu.cloudnetservice.cloudnet.repository.CloudNetUpdateServer;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetParentVersion;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

public class CloudNetOpenApiOptions {

    public static OpenApiOptions create(CloudNetUpdateServer server) {
        OpenApiOptions options = new OpenApiOptions(() -> new OpenAPI()
                .info(new Info()
                        .version("1.0")
                        .description("CloudNet 2/3 UpdateServer API")
                        .title("CloudNet Update"))
                .addServersItem(new Server().url("https://update.cloudnetservice.eu").description("CloudNetService")))
                .path("/api/json-docs")
                .ignorePath("/docs/*")
                .ignorePath("/versions/*")
                .ignorePath("/admin/*")
                .ignorePath("/internal/*")
                .swagger(new SwaggerOptions("/api/docs").title("CloudNet Updates"));
        for (CloudNetParentVersion parentVersion : server.getParentVersions()) {
            options.ignorePath(parentVersion.getGitHubWebHookPath());
        }
        return options;
    }

}
